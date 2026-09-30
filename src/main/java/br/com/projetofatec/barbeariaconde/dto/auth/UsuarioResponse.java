package br.com.projetofatec.barbeariaconde.dto.auth;

import br.com.projetofatec.barbeariaconde.model.Role;
import br.com.projetofatec.barbeariaconde.model.Usuario;

/** Dados publicos do usuario autenticado. Nunca inclui hash de senha. */
public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        String endereco,
        Role role
) {
    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getEndereco(),
                usuario.getRole());
    }
}
