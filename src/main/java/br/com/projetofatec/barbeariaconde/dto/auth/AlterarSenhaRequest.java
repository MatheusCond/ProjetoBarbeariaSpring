package br.com.projetofatec.barbeariaconde.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Troca da propria senha.
 *
 * <p>A senha atual e pedida mesmo com o usuario ja autenticado: um access token em maos
 * erradas nao deve bastar para assumir a conta em definitivo.
 */
public record AlterarSenhaRequest(

        @NotBlank(message = "Informe a senha atual")
        String senhaAtual,

        @NotBlank(message = "Informe a nova senha")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String novaSenha
) {
}
