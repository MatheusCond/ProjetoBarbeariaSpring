package br.com.projetofatec.barbeariaconde.model;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Clientes")
@PrimaryKeyJoinColumn(name = "usuarioEmail")
public class Clientes extends Usuarios{
    private String endereco;
}
