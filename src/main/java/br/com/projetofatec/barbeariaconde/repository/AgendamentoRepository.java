package br.com.projetofatec.barbeariaconde.repository;

import br.com.projetofatec.barbeariaconde.model.Agendamento;
import br.com.projetofatec.barbeariaconde.model.StatusAgendamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long>,
        JpaSpecificationExecutor<Agendamento> {

    @Query("""
            select a from Agendamento a
              join fetch a.cliente
              join fetch a.barbeiro
            where a.id = :id
            """)
    Optional<Agendamento> buscarComRelacionamentos(@Param("id") Long id);

    @EntityGraph(attributePaths = {"cliente", "barbeiro"})
    Page<Agendamento> findByClienteIdOrderByDataDescHoraInicioDesc(Long clienteId, Pageable pageable);

    /** Redeclarado apenas para carregar cliente e barbeiro junto e evitar N+1 na listagem. */
    @Override
    @EntityGraph(attributePaths = {"cliente", "barbeiro"})
    Page<Agendamento> findAll(Specification<Agendamento> spec, Pageable pageable);

    long countByClienteIdAndStatus(Long clienteId, StatusAgendamento status);

    /** Atendimentos ainda marcados de um profissional, de uma data em diante. */
    long countByBarbeiroIdAndStatusAndDataGreaterThanEqual(Long barbeiroId,
                                                          StatusAgendamento status,
                                                          LocalDate data);

    /**
     * Atendimentos ainda marcados, agrupados por profissional, em uma consulta so.
     *
     * <p>Alimenta a listagem da administracao. Um {@code count} por barbeiro dentro do
     * laco resolveria o mesmo, mas seria uma consulta por linha da tela.
     *
     * @return pares {@code [barbeiroId, quantidade]}; barbeiro sem nenhum nao aparece
     */
    @Query("""
            select a.barbeiro.id, count(a)
            from Agendamento a
            where a.status = br.com.projetofatec.barbeariaconde.model.StatusAgendamento.AGENDADO
              and a.data >= :apartirDe
            group by a.barbeiro.id
            """)
    List<Object[]> contarFuturosPorBarbeiro(@Param("apartirDe") LocalDate apartirDe);

    /**
     * Agendamentos ativos do proprio cliente que se sobrepoem ao intervalo informado,
     * independente do barbeiro. Evita que a mesma pessoa reserve dois atendimentos
     * no mesmo horario com barbeiros diferentes.
     *
     * @param idIgnorado agendamento a desconsiderar (o proprio, ao reagendar);
     *                   use {@code -1} para nao ignorar nenhum
     */
    @Query("""
            select a from Agendamento a
            where a.cliente.id = :clienteId
              and a.data = :data
              and a.status = br.com.projetofatec.barbeariaconde.model.StatusAgendamento.AGENDADO
              and a.horaInicio < :fim
              and a.horaFim > :inicio
              and a.id <> :idIgnorado
            """)
    List<Agendamento> conflitosDoCliente(@Param("clienteId") Long clienteId,
                                         @Param("data") LocalDate data,
                                         @Param("inicio") LocalTime inicio,
                                         @Param("fim") LocalTime fim,
                                         @Param("idIgnorado") Long idIgnorado);
}
