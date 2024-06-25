package br.com.projetofatec.barbeariaconde.dto;

import br.com.projetofatec.barbeariaconde.model.Usuarios;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class AgendasDTO{
     private String servico;
     private LocalDate dataAgenda;
     private LocalTime horaAgenda;
     private String clienteEmail;
     private Integer status;
}
