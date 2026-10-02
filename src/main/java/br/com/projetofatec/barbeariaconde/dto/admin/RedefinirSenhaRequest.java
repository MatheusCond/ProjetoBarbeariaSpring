package br.com.projetofatec.barbeariaconde.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Senha provisoria definida pelo administrador para um profissional que perdeu acesso.
 *
 * <p>Nao pede a senha antiga, porque quem redefine nao e o dono da conta — por isso a
 * operacao e restrita ao perfil ADMIN e derruba as sessoes abertas do profissional.
 */
public record RedefinirSenhaRequest(

        @NotBlank(message = "Informe a nova senha")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String novaSenha
) {
}
