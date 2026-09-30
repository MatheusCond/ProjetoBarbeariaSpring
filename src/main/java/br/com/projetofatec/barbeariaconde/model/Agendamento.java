package br.com.projetofatec.barbeariaconde.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Um atendimento reservado na agenda de um barbeiro.
 *
 * <p>O intervalo ocupado e materializado em {@link AgendamentoSlot}: cada fatia de
 * {@code duracaoSlotMinutos} coberta pelo atendimento gera uma linha com restricao
 * unica em (barbeiro, data, hora). E essa restricao que impede, no banco, que dois
 * clientes ocupem o mesmo horario do mesmo barbeiro.
 */
@Entity
@Table(
        name = "agendamentos",
        indexes = {
                @Index(name = "ix_agendamentos_data", columnList = "data"),
                @Index(name = "ix_agendamentos_cliente", columnList = "cliente_id"),
                @Index(name = "ix_agendamentos_barbeiro_data", columnList = "barbeiro_id, data")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_agendamento_cliente"))
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbeiro_id", nullable = false, foreignKey = @ForeignKey(name = "fk_agendamento_barbeiro"))
    private Usuario barbeiro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Servico servico;

    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAgendamento status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(length = 255)
    private String observacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    /**
     * Slots ocupados por este atendimento. Cancelar o agendamento limpa a lista
     * ({@code orphanRemoval}), devolvendo os horarios para a agenda.
     */
    @OneToMany(mappedBy = "agendamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AgendamentoSlot> slots = new ArrayList<>();

    @PrePersist
    void aoCriar() {
        Instant agora = Instant.now();
        if (criadoEm == null) {
            criadoEm = agora;
        }
        atualizadoEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = Instant.now();
    }

    /** Substitui os slots ocupados pelos horarios informados. */
    public void ocuparSlots(List<LocalTime> horas) {
        slots.clear();
        for (LocalTime hora : horas) {
            slots.add(AgendamentoSlot.builder()
                    .agendamento(this)
                    .barbeiro(barbeiro)
                    .data(data)
                    .hora(hora)
                    .build());
        }
    }

    /** Libera todos os slots ocupados por este atendimento. */
    public void liberarSlots() {
        slots.clear();
    }
}
