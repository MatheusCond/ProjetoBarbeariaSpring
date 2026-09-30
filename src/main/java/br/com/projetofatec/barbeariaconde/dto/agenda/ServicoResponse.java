package br.com.projetofatec.barbeariaconde.dto.agenda;

import br.com.projetofatec.barbeariaconde.model.Servico;

import java.math.BigDecimal;

public record ServicoResponse(
        String codigo,
        String nome,
        String descricao,
        int duracaoMinutos,
        BigDecimal preco
) {
    public static ServicoResponse de(Servico servico) {
        return new ServicoResponse(
                servico.name(),
                servico.getNome(),
                servico.getDescricao(),
                servico.getDuracaoMinutos(),
                servico.getPreco());
    }
}
