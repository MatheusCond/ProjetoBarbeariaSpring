package br.com.projetofatec.barbeariaconde.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cadastro de um profissional pelo administrador. */
public record NovoBarbeiroRequest(

        @NotBlank(message = "Informe o nome do profissional")
        @Size(min = 3, max = 120, message = "O nome deve ter entre 3 e 120 caracteres")
        String nome,

        @NotBlank(message = "Informe o e-mail")
        @Email(message = "E-mail inválido")
        @Size(max = 180, message = "E-mail muito longo")
        String email,

        @NotBlank(message = "Defina uma senha inicial")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String senha,

        /** Opcional: o padrão vazio também é aceito pelo regex. */
        @Pattern(regexp = "^$|^\\(?\\d{2}\\)?\\s?9?\\d{4}-?\\d{4}$",
                message = "Telefone inválido. Use o formato (17) 99999-9999")
        String telefone
) {
}
