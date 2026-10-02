# Imagem da aplicacao.
#
# Duas etapas de proposito: a primeira precisa de Maven e do JDK inteiro para compilar;
# a segunda leva apenas o jar e um JRE. Assim o que sobe para a hospedagem nao carrega
# compilador, codigo-fonte nem cache do Maven — menos coisa para baixar e menos
# superficie exposta.

# --- Etapa 1: build ---------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# O pom entra sozinho primeiro para que o download das dependencias vire uma camada
# separada: enquanto ele nao mudar, reconstruir a imagem reaproveita o cache em vez de
# baixar tudo de novo.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src

# Os testes nao rodam aqui. Sao de integracao, sobem o contexto inteiro do Spring e
# levariam alguns minutos no builder gratuito, que e lento. Rode `.\mvnw.cmd test`
# antes do push — e o que o README manda fazer.
RUN mvn -B clean package -DskipTests

# --- Etapa 2: execucao ------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Processo sem privilegio: uma falha na aplicacao nao vira root no container.
RUN useradd --system --no-create-home --shell /usr/sbin/nologin barbearia
COPY --from=build --chown=barbearia:barbearia /build/target/*.jar app.jar
USER barbearia

# Apenas documental. A porta real vem de PORT, que a hospedagem injeta e o
# application.yml le; o padrao continua 8080 quando a variavel nao existe.
EXPOSE 8080

# MaxRAMPercentage: em container pequeno a JVM reserva so 25% da memoria para o heap,
# entao uma instancia de 512 MB rodaria com 128 MB e deixaria o resto parado. 70% cobre
# o heap e ainda sobra para metaspace, pilhas e o proprio JRE.
#
# SerialGC: com uma fracao de nucleo, o G1 custa mais em memoria e em threads do que
# rende. A JVM ja escolheria o serial sozinha nesse tamanho; deixar explicito evita que
# a escolha mude junto com o plano da hospedagem.
#
# Ajuste fino sem reconstruir a imagem: a JVM tambem le JAVA_TOOL_OPTIONS.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-XX:+UseSerialGC", "-jar", "app.jar"]
