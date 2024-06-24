package br.com.projetofatec.barbeariaconde.model;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "Funcionarios")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "usuarioEmail")
public class Funcionarios extends Usuarios{
    private String cargo;
    private Double salario;
}