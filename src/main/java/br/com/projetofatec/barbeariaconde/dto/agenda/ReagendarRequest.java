package br.com.projetofatec.barbeariaconde.dto.agenda;

import br.com.projetofatec.barbeariaconde.model.Servico;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Remarcacao de um agendamento existente.
 *
 * <p>Vale por um agendamento so, identificado na URL. Antes o {@code PUT} recebia um
 * e-mail e sobrescrevia data e hora de TODOS os agendamentos daquele cliente.
 */
public record ReagendarRequest(

        @NotNull(message = "Escolha o serviço")
        Servico servico,

        @NotNull(message = "Informe a data")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate data,

        @NotNull(message = "Informe o horário")
        @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
        LocalTime hora,

        Long barbeiroId
) {
}
