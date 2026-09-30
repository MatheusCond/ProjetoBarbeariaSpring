package br.com.projetofatec.barbeariaconde.model;

public enum StatusAgendamento {
    /** Reservado pelo cliente e ocupando o horário na agenda do barbeiro. */
    AGENDADO,
    /** Atendimento realizado. */
    CONCLUIDO,
    /** Cancelado pelo cliente ou pela equipe; o horário volta a ficar livre. */
    CANCELADO,
    /** Cliente não compareceu; o horário não é devolvido para a agenda. */
    NAO_COMPARECEU;

    public boolean isAtivo() {
        return this == AGENDADO;
    }
}
