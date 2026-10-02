package br.com.projetofatec.barbeariaconde.dto.admin;

import jakarta.validation.constraints.NotNull;

/**
 * Ativacao ou desativacao de um profissional.
 *
 * <p>O estado desejado vem no corpo em vez de haver dois endpoints ({@code /ativar} e
 * {@code /desativar}): a tela ja sabe em que situacao o profissional esta e, assim, dois
 * cliques simultaneos no mesmo botao convergem para o mesmo resultado.
 */
public record SituacaoBarbeiroRequest(

        @NotNull(message = "Informe se o profissional fica ativo")
        Boolean ativo
) {
}
