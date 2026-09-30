package br.com.projetofatec.barbeariaconde.security;

import br.com.projetofatec.barbeariaconde.config.JwtProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Monta e le o cookie do refresh token.
 *
 * <p>{@code HttpOnly} impede leitura por JavaScript (um XSS nao consegue roubar a sessao
 * longa), {@code SameSite=Strict} impede envio em requisicao de outro site (CSRF) e
 * {@code Path} restrito faz o navegador mandar o cookie apenas para os endpoints de
 * autenticacao.
 */
@Component
public class CookieRefresh {

    private final JwtProperties props;

    public CookieRefresh(JwtProperties props) {
        this.props = props;
    }

    public String cabecalhoDeCriacao(String valor) {
        return construir(valor, props.getExpiracaoRefreshToken()).toString();
    }

    /** Cookie vazio e expirado, para apagar o anterior no logout. */
    public String cabecalhoDeRemocao() {
        return construir("", Duration.ZERO).toString();
    }

    public Optional<String> ler(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> props.getCookieNome().equals(c.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }

    public String nomeDoCabecalho() {
        return HttpHeaders.SET_COOKIE;
    }

    private ResponseCookie construir(String valor, Duration validade) {
        return ResponseCookie.from(props.getCookieNome(), valor)
                .httpOnly(true)
                .secure(props.isCookieSeguro())
                .sameSite(props.getCookieSameSite())
                .path(props.getCookiePath())
                .maxAge(validade)
                .build();
    }
}
