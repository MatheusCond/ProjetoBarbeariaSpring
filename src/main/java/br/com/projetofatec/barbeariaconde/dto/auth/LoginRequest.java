package br.com.projetofatec.barbeariaconde.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Credenciais de login. O identificador e o e-mail, nao mais o nome de exibicao. */
public record LoginRequest(

        @NotBlank(message = "Informe seu e-mail")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "Informe sua senha")
        String senha
) {
}
