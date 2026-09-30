package br.com.projetofatec.barbeariaconde.security;

import br.com.projetofatec.barbeariaconde.dto.comum.ErroResposta;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Faz 401 e 403 sairem como JSON, no mesmo formato dos outros erros da API.
 *
 * <p>Sem isso o Spring Security responde com a pagina de erro padrao do container e o
 * front nao consegue distinguir "token expirou" de "sem permissao".
 */
@Component
public class RespostaJsonDeSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RespostaJsonDeSeguranca(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        escrever(request, response, HttpStatus.UNAUTHORIZED,
                "Autenticação necessária. Envie um access token válido.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escrever(request, response, HttpStatus.FORBIDDEN,
                "Você não tem permissão para esta operação.");
    }

    private void escrever(HttpServletRequest request, HttpServletResponse response,
                          HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ErroResposta corpo = new ErroResposta(Instant.now(), status.value(), status.getReasonPhrase(),
                mensagem, request.getRequestURI(), null);
        objectMapper.writeValue(response.getOutputStream(), corpo);
    }
}
