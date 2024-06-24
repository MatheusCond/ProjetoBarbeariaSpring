package br.com.projetofatec.barbeariaconde.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DadosUsuariosDTO {
    private Long idUsuario;
    @NotBlank
    private String nomeUsuario;
    @NotBlank
    @Email
    private String emailUsuario;
    @NotBlank
    @Pattern(regexp = "^\\(?\\d{2}\\)?\\s?\\d{4,5}-?\\d{4}$")
    private String telefone;
    @NotBlank
    private String senhaUsuario;
}
