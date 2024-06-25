package br.com.projetofatec.barbeariaconde.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class AgendaDTOUp {
    private String servico;
    private LocalDate dataAgenda;
    private LocalTime horaAgenda;
    private String clienteEmail;
}
