package br.com.projetofatec.barbeariaconde.dto.agenda;

import br.com.projetofatec.barbeariaconde.model.Usuario;

/** Barbeiro exposto publicamente: apenas id e nome, sem dados de contato. */
public record BarbeiroResponse(Long id, String nome) {

    public static BarbeiroResponse de(Usuario usuario) {
        return new BarbeiroResponse(usuario.getId(), usuario.getNome());
    }
}
