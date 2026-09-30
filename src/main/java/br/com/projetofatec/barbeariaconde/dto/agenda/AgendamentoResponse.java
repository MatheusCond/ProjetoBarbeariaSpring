package br.com.projetofatec.barbeariaconde.dto.agenda;

import br.com.projetofatec.barbeariaconde.model.Agendamento;
import br.com.projetofatec.barbeariaconde.model.StatusAgendamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Agendamento como o front precisa ver: com id (antes nao vinha, e por isso a tela nao
 * conseguia editar nem cancelar um item especifico) e com o barbeiro resolvido.
 */
public record AgendamentoResponse(
        Long id,
        String servico,
        String servicoNome,
        LocalDate data,
        LocalTime horaInicio,
        LocalTime horaFim,
        StatusAgendamento status,
        BigDecimal preco,
        String observacao,
        BarbeiroResponse barbeiro,
        ClienteResumo cliente
) {

    public record ClienteResumo(Long id, String nome, String email, String telefone) {
    }

    public static AgendamentoResponse de(Agendamento a) {
        return new AgendamentoResponse(
                a.getId(),
                a.getServico().name(),
                a.getServico().getNome(),
                a.getData(),
                a.getHoraInicio(),
                a.getHoraFim(),
                a.getStatus(),
                a.getPreco(),
                a.getObservacao(),
                BarbeiroResponse.de(a.getBarbeiro()),
                new ClienteResumo(
                        a.getCliente().getId(),
                        a.getCliente().getNome(),
                        a.getCliente().getEmail(),
                        a.getCliente().getTelefone()));
    }
}
