package br.com.projetofatec.barbeariaconde.dto.admin;

import br.com.projetofatec.barbeariaconde.model.Usuario;

/**
 * Profissional como o administrador precisa ver: com e-mail, telefone e situacao,
 * dados que o {@code BarbeiroResponse} publico nao expoe de proposito.
 *
 * @param agendamentosFuturos atendimentos ainda marcados; serve para o painel avisar
 *                            o que continua valendo depois de uma desativacao
 */
public record BarbeiroAdminResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        boolean ativo,
        long agendamentosFuturos
) {
    public static BarbeiroAdminResponse de(Usuario usuario, long agendamentosFuturos) {
        return new BarbeiroAdminResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.isAtivo(),
                agendamentosFuturos);
    }
}
