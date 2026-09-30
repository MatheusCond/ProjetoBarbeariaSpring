package br.com.projetofatec.barbeariaconde.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Parametros de emissao e armazenamento dos tokens (prefixo {@code barbearia.jwt}). */
@ConfigurationProperties(prefix = "barbearia.jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * Segredo HMAC. Deve vir de variavel de ambiente em producao
     * ({@code BARBEARIA_JWT_SEGREDO}). Se ficar vazio, a aplicacao gera um segredo
     * aleatorio no boot e avisa no log: bom para rodar local, inaceitavel em producao,
     * porque todo restart invalida os tokens emitidos.
     */
    private String segredo = "";

    private String emissor = "barbearia-conde";

    /** Vida do access token. Curta de proposito: ele nao pode ser revogado. */
    private Duration expiracaoAccessToken = Duration.ofMinutes(15);

    /** Vida do refresh token, que fica no cookie httpOnly e pode ser revogado. */
    private Duration expiracaoRefreshToken = Duration.ofDays(7);

    private String cookieNome = "refreshToken";

    /**
     * Caminho do cookie de refresh. Restrito aos endpoints de autenticacao, para o
     * navegador nao enviar o refresh token em toda chamada da API.
     */
    private String cookiePath = "/api/auth";

    /** Em producao (HTTPS) deve ser {@code true}. */
    private boolean cookieSeguro = false;

    private String cookieSameSite = "Strict";
}
