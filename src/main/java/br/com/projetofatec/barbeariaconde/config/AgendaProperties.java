package br.com.projetofatec.barbeariaconde.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;

/**
 * Regras da agenda (prefixo {@code barbearia.agenda}).
 *
 * <p>Tudo o que antes estava chumbado no codigo (ou simplesmente nao existia) vira
 * configuracao: duracao do slot, expediente por dia da semana, pausa do almoco,
 * antecedencia minima e limites por cliente.
 */
@ConfigurationProperties(prefix = "barbearia.agenda")
@Getter
@Setter
public class AgendaProperties {

    /** Fuso usado para decidir o que e passado e o que e futuro. */
    private ZoneId fusoHorario = ZoneId.of("America/Sao_Paulo");

    /** Granularidade da grade de horarios, em minutos. */
    private int duracaoSlotMinutos = 30;

    /** Quanto antes do atendimento ainda e possivel reservar. */
    private Duration antecedenciaMinima = Duration.ofMinutes(30);

    /** Quanto antes do atendimento o cliente ainda pode cancelar ou reagendar. */
    private Duration antecedenciaMinimaCancelamento = Duration.ofHours(2);

    /** Quantos dias para frente a agenda fica aberta. */
    private int janelaMaximaDias = 60;

    /** Teto de reservas ativas simultaneas por cliente, para evitar bloqueio de agenda. */
    private int maxAgendamentosAtivosPorCliente = 3;

    /** Expediente por dia da semana. Dia ausente no mapa = barbearia fechada. */
    private Map<DayOfWeek, Expediente> expediente = new EnumMap<>(DayOfWeek.class);

    @Getter
    @Setter
    public static class Expediente {
        private LocalTime abertura;
        private LocalTime fechamento;
        /** Inicio da pausa (almoco). Opcional. */
        private LocalTime pausaInicio;
        /** Fim da pausa. Opcional. */
        private LocalTime pausaFim;

        public boolean temPausa() {
            return pausaInicio != null && pausaFim != null;
        }

        /** True se o intervalo [inicio, fim) invade a pausa. */
        public boolean colideComPausa(LocalTime inicio, LocalTime fim) {
            return temPausa() && inicio.isBefore(pausaFim) && fim.isAfter(pausaInicio);
        }
    }

    public Expediente expedienteDe(DayOfWeek dia) {
        return expediente.get(dia);
    }

    @PostConstruct
    void validar() {
        if (duracaoSlotMinutos <= 0 || 60 % duracaoSlotMinutos != 0) {
            throw new IllegalStateException(
                    "barbearia.agenda.duracao-slot-minutos deve ser um divisor de 60 maior que zero");
        }
        expediente.forEach((dia, exp) -> {
            if (exp.getAbertura() == null || exp.getFechamento() == null) {
                throw new IllegalStateException("Expediente de " + dia + " precisa de abertura e fechamento");
            }
            if (!exp.getAbertura().isBefore(exp.getFechamento())) {
                throw new IllegalStateException("Expediente de " + dia + ": abertura deve ser antes do fechamento");
            }
            if (exp.temPausa() && !exp.getPausaInicio().isBefore(exp.getPausaFim())) {
                throw new IllegalStateException("Pausa de " + dia + ": inicio deve ser antes do fim");
            }
        });
    }
}
