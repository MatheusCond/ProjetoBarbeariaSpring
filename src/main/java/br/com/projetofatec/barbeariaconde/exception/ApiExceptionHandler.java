package br.com.projetofatec.barbeariaconde.exception;

import br.com.projetofatec.barbeariaconde.dto.comum.ErroResposta;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Traduz excecoes em respostas JSON com formato unico, para o front nunca precisar
 * adivinhar o que deu errado nem receber stack trace.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResposta> regraNegocio(RegraNegocioException ex, HttpServletRequest req) {
        return ErroResposta.resposta(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResposta> conflito(ConflitoException ex, HttpServletRequest req) {
        return ErroResposta.resposta(HttpStatus.CONFLICT, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest req) {
        return ErroResposta.resposta(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI());
    }

    /**
     * URL sem recurso correspondente (imagem removida, caminho digitado errado).
     *
     * <p>Precisa ser tratada explicitamente: sem isto ela cai no handler genérico
     * de {@code Exception} e vira 500 com stack trace no log, quando o correto é 404.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResposta> recursoEstaticoAusente(NoResourceFoundException ex,
                                                               HttpServletRequest req) {
        return ErroResposta.resposta(HttpStatus.NOT_FOUND,
                "Recurso não encontrado.", req.getRequestURI());
    }

    /**
     * Rede de seguranca da restricao unica de slot: se duas reservas simultaneas
     * escaparem da checagem de disponibilidade, o banco rejeita a segunda e ela chega aqui.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> integridade(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violacao de integridade em {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return ErroResposta.resposta(HttpStatus.CONFLICT,
                "Não foi possível concluir a operação: o registro conflita com um já existente.",
                req.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> validacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ErroResposta.CampoInvalido> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErroResposta.CampoInvalido(f.getField(), f.getDefaultMessage()))
                .toList();
        return ErroResposta.resposta(HttpStatus.BAD_REQUEST,
                "Há campos inválidos na requisição.", req.getRequestURI(), campos);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErroResposta> requisicaoInvalida(Exception ex, HttpServletRequest req) {
        return ErroResposta.resposta(HttpStatus.BAD_REQUEST,
                "Requisição malformada: verifique os campos e os formatos enviados.", req.getRequestURI());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErroResposta> credenciaisInvalidas(BadCredentialsException ex, HttpServletRequest req) {
        // Mensagem genérica de propósito: não revela se o e-mail existe.
        return ErroResposta.resposta(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.", req.getRequestURI());
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErroResposta> contaDesativada(DisabledException ex, HttpServletRequest req) {
        return ErroResposta.resposta(HttpStatus.FORBIDDEN, "Esta conta está desativada.", req.getRequestURI());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResposta> autenticacao(AuthenticationException ex, HttpServletRequest req) {
        return ErroResposta.resposta(HttpStatus.UNAUTHORIZED, "Não autenticado.", req.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResposta> acessoNegado(AccessDeniedException ex, HttpServletRequest req) {
        return ErroResposta.resposta(HttpStatus.FORBIDDEN,
                "Você não tem permissão para esta operação.", req.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> inesperado(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {}", req.getRequestURI(), ex);
        return ErroResposta.resposta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno inesperado. Tente novamente em instantes.", req.getRequestURI());
    }
}
