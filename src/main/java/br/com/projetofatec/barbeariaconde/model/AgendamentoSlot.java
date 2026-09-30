package br.com.projetofatec.barbeariaconde.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Uma fatia da agenda de um barbeiro (por padrao 30 minutos).
 *
 * <p>A restricao unica em (barbeiro, data, hora) e a garantia de que dois agendamentos
 * nao se sobrepoem: mesmo que duas requisicoes cheguem no mesmo instante e as duas
 * passem pela consulta de disponibilidade, apenas uma consegue gravar o slot. A outra
 * recebe violacao de integridade, que a camada de servico traduz em HTTP 409.
 */
@Entity
@Table(
        name = "agendamento_slots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_slot_barbeiro_data_hora",
                columnNames = {"barbeiro_id", "data", "hora"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgendamentoSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agendamento_id", nullable = false, foreignKey = @ForeignKey(name = "fk_slot_agendamento"))
    private Agendamento agendamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbeiro_id", nullable = false, foreignKey = @ForeignKey(name = "fk_slot_barbeiro"))
    private Usuario barbeiro;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private LocalTime hora;
}
