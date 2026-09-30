package br.com.projetofatec.barbeariaconde.repository;

import br.com.projetofatec.barbeariaconde.model.AgendamentoSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface AgendamentoSlotRepository extends JpaRepository<AgendamentoSlot, Long> {

    List<AgendamentoSlot> findByBarbeiroIdAndData(Long barbeiroId, LocalDate data);

    /**
     * Projecao (barbeiroId, hora) dos slots ocupados no dia para um conjunto de barbeiros.
     * Usada para montar a disponibilidade com uma unica consulta.
     */
    @Query("""
            select s.barbeiro.id, s.hora
            from AgendamentoSlot s
            where s.data = :data
              and s.barbeiro.id in :barbeiroIds
            """)
    List<Object[]> ocupacaoDoDia(@Param("data") LocalDate data,
                                 @Param("barbeiroIds") Collection<Long> barbeiroIds);
}
