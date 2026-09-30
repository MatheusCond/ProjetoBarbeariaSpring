package br.com.projetofatec.barbeariaconde.repository;

import br.com.projetofatec.barbeariaconde.model.Agendamento;
import br.com.projetofatec.barbeariaconde.model.StatusAgendamento;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Filtros compostos da listagem da equipe.
 *
 * <p>Specifications em vez de um JPQL com {@code (:param is null or ...)}: cada filtro
 * opcional simplesmente nao entra na consulta quando nao e informado.
 */
public final class AgendamentoSpecs {

    private AgendamentoSpecs() {
    }

    public static Specification<Agendamento> data(LocalDate data) {
        return data == null ? null : (raiz, consulta, cb) -> cb.equal(raiz.get("data"), data);
    }

    public static Specification<Agendamento> apartirDe(LocalDate data) {
        return data == null ? null : (raiz, consulta, cb) -> cb.greaterThanOrEqualTo(raiz.get("data"), data);
    }

    public static Specification<Agendamento> ate(LocalDate data) {
        return data == null ? null : (raiz, consulta, cb) -> cb.lessThanOrEqualTo(raiz.get("data"), data);
    }

    public static Specification<Agendamento> status(StatusAgendamento status) {
        return status == null ? null : (raiz, consulta, cb) -> cb.equal(raiz.get("status"), status);
    }

    public static Specification<Agendamento> barbeiro(Long barbeiroId) {
        return barbeiroId == null ? null : (raiz, consulta, cb) -> cb.equal(raiz.get("barbeiro").get("id"), barbeiroId);
    }

    public static Specification<Agendamento> cliente(Long clienteId) {
        return clienteId == null ? null : (raiz, consulta, cb) -> cb.equal(raiz.get("cliente").get("id"), clienteId);
    }

    /** Combina os filtros informados, ignorando os nulos. */
    @SafeVarargs
    public static Specification<Agendamento> todos(Specification<Agendamento>... filtros) {
        Specification<Agendamento> resultado = null;
        for (Specification<Agendamento> filtro : filtros) {
            if (filtro == null) {
                continue;
            }
            resultado = resultado == null ? filtro : resultado.and(filtro);
        }
        return resultado;
    }
}
