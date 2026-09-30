# Barbearia Conde

Site e API de uma barbearia: apresenta o estabelecimento e permite que o cliente crie
conta, veja os horários realmente livres e reserve um atendimento — sem que duas pessoas
consigam ocupar o mesmo horário do mesmo profissional.

**Stack:** Java 21 · Spring Boot 3.5 · Spring Security + JWT · Spring Data JPA · H2 /
MySQL · JUnit 5 · OpenAPI (Swagger UI) · front em HTML/CSS/JS sem framework.

---

## Rodando o projeto

Basta o JDK 21+. O perfil padrão usa **H2 em memória** e cria contas de demonstração no
primeiro boot, então não é preciso instalar banco nenhum nem configurar variável alguma.

A partir da **raiz do projeto** (a pasta onde estão o `pom.xml` e o `mvnw`):

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux, no macOS ou no Git Bash:

```bash
./mvnw spring-boot:run
```

> No PowerShell use sempre `.\mvnw.cmd`. O arquivo `mvnw` sem extensão é um script de
> shell, e `./mvnw` resulta em `CommandNotFoundException`.

Depois abra <http://localhost:8080>. Para parar, `Ctrl+C` no terminal.

- Site: <http://localhost:8080/index.html>
- Tela de agendamento: <http://localhost:8080/agendar.html>
- Painel da equipe: <http://localhost:8080/painel.html>
- Documentação da API: <http://localhost:8080/swagger-ui.html>

### Contas de demonstração

Criadas automaticamente quando `barbearia.demo.carregar=true` (padrão no perfil H2).
Todas usam a senha definida em `barbearia.demo.senha` no `application.yml`.

| Perfil   | E-mail                            | O que consegue fazer                          |
|----------|-----------------------------------|-----------------------------------------------|
| ADMIN    | `admin@barbeariaconde.com.br`     | Painel completo, agendar para qualquer cliente |
| BARBEIRO | `tiago@barbeariaconde.com.br`     | Painel, concluir e cancelar atendimentos       |
| BARBEIRO | `rafael@barbeariaconde.com.br`    | Idem                                           |
| CLIENTE  | `cliente@exemplo.com`             | Agendar, remarcar e cancelar o que é seu       |

São credenciais de ambiente local. Em produção o perfil `prod` desliga esse carregamento.

### Com MySQL

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

> As aspas em torno do `-D` são necessárias no PowerShell: sem elas o argumento é
> quebrado e o Maven reclama de fase inexistente. No Bash, escreva
> `./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql`.

Variáveis de conexão e os demais parâmetros estão em [`.env.example`](.env.example).

### Testes

```powershell
.\mvnw.cmd test
```

30 testes, incluindo o cenário de concorrência descrito abaixo.

---

## Autenticação: onde o token fica e por quê

Este é o ponto que o projeto original deixava só esboçado — o token era emitido no login
e nunca mais usado. O modelo atual separa dois tokens com responsabilidades diferentes:

| | Access token | Refresh token |
|---|---|---|
| Formato | JWT assinado (HMAC-512) | valor opaco aleatório (256 bits) |
| Validade | 15 minutos | 7 dias |
| Onde fica | **memória do JavaScript** | **cookie `HttpOnly`** |
| Vai em | header `Authorization: Bearer` | cookie, só para `/api/auth/*` |
| Revogável | não (por isso é curto) | sim, registro no banco |

**Por que o access token não vai para `localStorage`.** Qualquer script que rode na
página — um XSS, uma extensão, uma dependência comprometida — lê `localStorage` e
`sessionStorage`. Um token roubado de lá vale até expirar, em qualquer aba e em qualquer
dispositivo. Mantido apenas em uma variável de módulo (`js/api.js`), ele desaparece quando
a aba fecha e não é acessível por nenhum outro script da página.

**Como a sessão sobrevive ao F5, então.** O refresh token vai em cookie `HttpOnly`,
`SameSite=Strict`, `Path=/api/auth`. O JavaScript não consegue lê-lo (`document.cookie` vem
vazio), mas o navegador o envia sozinho. Ao carregar a página, o front chama
`POST /api/auth/refresh` e recebe um access token novo. O efeito prático é "continuar
logado", sem deixar credencial legível no navegador.

**Rotação e detecção de reuso.** Cada refresh invalida o token usado e emite outro. No
banco guardamos apenas o SHA-256 do valor, então um vazamento do banco não reconstrói os
tokens em circulação. Se um token já consumido reaparecer, o sistema trata como indício de
roubo e revoga **todas** as sessões daquele usuário. Essa revogação roda em transação
própria (`RevogacaoDeTokens`), porque o rollback provocado pela exceção de credencial
inválida desfaria justamente a medida de segurança.

Outros ajustes na mesma frente:

- Login por e-mail (identificador único) em vez de nome de exibição.
- `UserDetailsService` lança `UsernameNotFoundException` em vez de devolver `null`, que
  antes virava HTTP 500 no lugar de 401.
- `hideUserNotFoundExceptions` ligado e mensagem única "E-mail ou senha inválidos": a API
  não serve de oráculo para descobrir quais e-mails existem.
- Segredo do JWT vem de configuração (`BARBEARIA_JWT_SEGREDO`), não mais `"1234"` no
  código. Sem a variável definida, a aplicação gera um segredo aleatório no boot e avisa
  no log.
- 401 e 403 saem em JSON, no mesmo formato dos outros erros, para o front distinguir
  "token expirou" de "sem permissão".
- Senha com mínimo de 8 caracteres, hash BCrypt, nunca devolvida pela API.

---

## Agenda: como dois clientes não ocupam o mesmo horário

O intervalo de cada atendimento é materializado em fatias de 30 minutos na tabela
`agendamento_slots`, que tem **restrição única em `(barbeiro_id, data, hora)`**. Um corte
de 30 min grava uma linha; cabelo + barba, de 60 min, grava duas.

```
Tiago, sábado          08:00  08:30  09:00  09:30  10:00  10:30  11:00
Cabelo e barba 10:00                              [====ocupado====]
Cabelo 10:30 → recusado                                  ↑ slot já existe
Cabelo e barba 09:30 → recusado                   ↑ segunda fatia colide
```

São três camadas, e cada uma resolve um problema diferente:

1. **Consulta de disponibilidade** (`GET /api/agendamentos/disponibilidade`) — a tela só
   oferece horários em que o atendimento **inteiro** cabe: todas as fatias livres, nada
   sobre o intervalo da equipe e término antes do fechamento. O cliente não digita mais um
   horário qualquer para descobrir o conflito depois.
2. **Restrição única no banco** — se duas requisições simultâneas lerem "livre" antes de
   qualquer uma gravar, o banco aceita só a primeira. A segunda recebe violação de
   integridade, traduzida em HTTP 409 com mensagem clara.
3. **Retentativa quando não há profissional escolhido** — se o cliente marcou "qualquer
   profissional", perder a corrida não deveria virar erro. A operação é repetida em uma
   transação nova, que já enxerga o slot recém-ocupado e passa para o próximo barbeiro
   livre. Com barbeiro escolhido não há alternativa, e a resposta é 409 na primeira
   tentativa.

O teste `AgendamentoConcorrenciaTest` cobre isso: 8 clientes disputando o mesmo horário ao
mesmo tempo resultam em exatamente 1 reserva com um barbeiro, e em exatamente 2 com dois
barbeiros.

### Demais regras de agendamento

- O cliente vem do token, nunca do corpo da requisição. Antes bastava trocar o e-mail do
  JSON para agendar no nome de outra pessoa.
- Nada de data passada, fora do expediente, fora da grade de 30 minutos, dentro do
  intervalo da equipe, além da janela de 60 dias ou com menos de 30 minutos de
  antecedência.
- O mesmo cliente não ocupa dois horários sobrepostos, mesmo com barbeiros diferentes.
- Limite de 3 reservas ativas por cliente, para uma conta não travar a agenda.
- Cancelar libera as fatias (o horário volta a ser oferecido) e mantém o agendamento no
  histórico com status `CANCELADO`.
- Cliente cancela ou remarca até 2 horas antes; a equipe não tem essa restrição.
- Remarcar move **um** agendamento. Antes o `PUT` recebia um e-mail e sobrescrevia data e
  hora de todos os agendamentos daquele cliente; o `DELETE`, no mesmo espírito, apagava
  todos de uma vez.
- Um cliente não vê nem cancela agendamento de outro (responde 404, sem confirmar que
  existe).

Expediente, duração da fatia, janela, limites e antecedências são configuração, não código:

```yaml
barbearia:
  agenda:
    duracao-slot-minutos: 30
    antecedencia-minima: 30m
    antecedencia-minima-cancelamento: 2h
    janela-maxima-dias: 60
    max-agendamentos-ativos-por-cliente: 3
    expediente:                     # dia ausente = fechado
      TUESDAY:  { abertura: "09:00", fechamento: "19:00", pausa-inicio: "12:00", pausa-fim: "13:00" }
      SATURDAY: { abertura: "08:00", fechamento: "18:00" }
```

---

## Endpoints

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/api/auth/registrar` | público | Cria conta de cliente e já devolve a sessão |
| `POST` | `/api/auth/login` | público | Access token no corpo, refresh no cookie |
| `POST` | `/api/auth/refresh` | cookie | Rotaciona o refresh e emite novo access token |
| `POST` | `/api/auth/logout` | cookie | Revoga o refresh e apaga o cookie |
| `POST` | `/api/auth/logout-global` | autenticado | Encerra a sessão em todos os dispositivos |
| `GET` | `/api/auth/eu` | autenticado | Dados do usuário logado |
| `GET` | `/api/servicos` | público | Catálogo com duração e preço |
| `GET` | `/api/barbeiros` | público | Profissionais ativos |
| `GET` | `/api/agendamentos/disponibilidade` | público | Horários livres por data, serviço e profissional |
| `POST` | `/api/agendamentos` | autenticado | Reserva um horário |
| `GET` | `/api/agendamentos/meus` | autenticado | Histórico do próprio usuário |
| `GET` | `/api/agendamentos/{id}` | dono ou equipe | Detalhe |
| `PUT` | `/api/agendamentos/{id}` | dono ou equipe | Remarca |
| `PATCH` | `/api/agendamentos/{id}/cancelar` | dono ou equipe | Cancela e libera o horário |
| `GET` | `/api/agendamentos` | equipe | Agenda completa, com filtros |
| `PATCH` | `/api/agendamentos/{id}/concluir` | equipe | Marca como realizado |
| `PATCH` | `/api/agendamentos/{id}/nao-compareceu` | equipe | Registra falta |

Todos os erros seguem o mesmo corpo, com a lista de campos inválidos quando é validação:

```json
{
  "timestamp": "2026-10-03T13:00:00Z",
  "status": 409,
  "erro": "Conflict",
  "mensagem": "Este horário acabou de ser reservado por outro cliente. Escolha outro horário.",
  "caminho": "/api/agendamentos"
}
```

---

## Estrutura

```
src/main/java/br/com/projetofatec/barbeariaconde/
├── config/          # SecurityConfig, OpenAPI, CORS, properties, dados de demonstração
├── controller/      # AuthController, AgendamentosController, CatalogoController
├── dto/             # records de entrada e saída (auth, agenda, comuns)
├── exception/       # exceções de negócio e o @RestControllerAdvice
├── model/           # Usuario, Agendamento, AgendamentoSlot, RefreshToken, enums
├── repository/      # Spring Data + Specifications dos filtros
├── security/        # JwtService, filtro JWT, refresh token, cookie, respostas 401/403
└── service/         # UsuarioService, AuthService, DisponibilidadeService, AgendamentoService

src/main/resources/static/
├── index.html  agendar.html  painel.html  login.html  cadastro.html
├── css/
└── js/         api.js (sessão e chamadas) · nav.js · agendar.js · painel.js · login.js · cadastro.js
```

### Decisões de modelagem

- **Um `Usuario` com `Role`** (`CLIENTE`, `BARBEIRO`, `ADMIN`) no lugar da herança JOINED
  `Usuarios`/`Clientes`/`Funcionarios`, que exigia uma tabela por perfil sem ganho real.
- **Serviço como enum** com nome, descrição, duração e preço. A duração precisa morar junto
  do serviço, porque é ela que define quantas fatias da agenda o atendimento ocupa. Isso
  também substituiu os três endpoints quase idênticos (`/agendar-cabelo`, `/agendar-barba`,
  `/agendar-cabelo-barba`) por um único `POST`.
- **`StatusAgendamento` como enum** em vez do `Integer status` com valor mágico `1`.
- **`open-in-view=false`** e mapeamento para DTO dentro da transação, para nenhuma
  associação preguiçosa virar consulta durante a serialização da resposta.
- **Envelope próprio de paginação** (`PaginaResposta`), porque serializar `PageImpl`
  direto não tem contrato estável entre versões do Spring Data.
- A restrição `unique` que existia em `Agendas.cliente_email` foi removida: ela permitia
  **um único agendamento por cliente em toda a vida do sistema**.

---

## Próximos passos possíveis

- Migrações versionadas com Flyway, substituindo o `ddl-auto: update`.
- Notificação de confirmação e lembrete por e-mail ou WhatsApp.
- Bloqueio de datas específicas (feriados, férias do profissional).
- Cadastro de barbeiros e serviços pela interface do administrador.
- Rate limiting no login, hoje protegido apenas por mensagem genérica e BCrypt.
