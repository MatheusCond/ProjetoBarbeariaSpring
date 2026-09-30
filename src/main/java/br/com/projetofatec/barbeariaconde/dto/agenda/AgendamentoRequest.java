package br.com.projetofatec.barbeariaconde.dto.agenda;

import br.com.projetofatec.barbeariaconde.model.Servico;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Pedido de reserva.
 *
 * <p>O cliente nao e informado no corpo: ele vem do token. Antes qualquer um podia
 * agendar no nome de outra pessoa apenas trocando o e-mail do JSON.
 *
 * @param barbeiroId barbeiro desejado; {@code null} deixa o sistema escolher um livre
 * @param clienteEmail apenas para a equipe agendar em nome de um cliente (ignorado para CLIENTE)
 */
public record AgendamentoRequest(

        @NotNull(message = "Escolha o serviço")
        Servico servico,

        @NotNull(message = "Informe a data")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate data,

        @NotNull(message = "Informe o horário")
        @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
        LocalTime hora,

        Long barbeiroId,

        @Email(message = "E-mail do cliente inválido")
        String clienteEmail,

        @Size(max = 255, message = "Observação muito longa")
        String observacao
) {
}
