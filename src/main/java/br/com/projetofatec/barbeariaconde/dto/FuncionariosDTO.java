package br.com.projetofatec.barbeariaconde.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FuncionariosDTO extends DadosUsuariosDTO{
    @NotBlank
    private String cargo;
    @Positive
    private Double salario;
}
