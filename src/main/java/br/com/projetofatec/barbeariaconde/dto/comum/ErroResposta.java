package br.com.projetofatec.barbeariaconde.dto.comum;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

/** Corpo unico de erro da API. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResposta(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoInvalido> campos
) {

    public record CampoInvalido(String campo, String mensagem) {
    }

    public static ResponseEntity<ErroResposta> resposta(HttpStatus status, String mensagem, String caminho) {
        return resposta(status, mensagem, caminho, null);
    }

    public static ResponseEntity<ErroResposta> resposta(HttpStatus status, String mensagem, String caminho,
                                                       List<CampoInvalido> campos) {
        ErroResposta corpo = new ErroResposta(
                Instant.now(), status.value(), status.getReasonPhrase(), mensagem, caminho,
                campos == null || campos.isEmpty() ? null : campos);
        return ResponseEntity.status(status).body(corpo);
    }
}
