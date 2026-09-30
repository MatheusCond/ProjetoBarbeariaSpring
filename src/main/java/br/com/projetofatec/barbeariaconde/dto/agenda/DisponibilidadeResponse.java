package br.com.projetofatec.barbeariaconde.dto.agenda;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Horarios realmente livres para um servico em uma data.
 *
 * <p>E o que faltava para a tela de agendamento fazer sentido: em vez de um campo de hora
 * livre onde o cliente digitava qualquer coisa (e descobria o conflito depois), a interface
 * mostra apenas o que cabe na agenda.
 *
 * @param horarios uniao dos horarios livres considerando todos os barbeiros elegiveis
 * @param porBarbeiro detalhamento por barbeiro, para quem quiser escolher o profissional
 * @param motivoFechado preenchido quando nao ha horario nenhum por regra (ex.: dia fechado)
 */
public record DisponibilidadeResponse(
        LocalDate data,
        String servico,
        int duracaoMinutos,
        boolean aberto,
        String motivoFechado,
        List<LocalTime> horarios,
        List<BarbeiroDisponibilidade> porBarbeiro
) {

    public record BarbeiroDisponibilidade(Long barbeiroId, String nome, List<LocalTime> horarios) {
    }

    public static DisponibilidadeResponse fechado(LocalDate data, String servico, int duracao, String motivo) {
        return new DisponibilidadeResponse(data, servico, duracao, false, motivo, List.of(), List.of());
    }
}
