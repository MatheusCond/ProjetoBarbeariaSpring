package br.com.projetofatec.barbeariaconde.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "Agendas")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Agendas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAgenda;
    private String servico;
    private LocalDate dataAgenda;
    private LocalTime horaAgenda;
    private Integer status;
    @Email
    @Column(name = "cliente_email", nullable = false, unique = true)
    private String clienteEmail;
    @ManyToOne
    @JoinColumn(name = "cliente_email", referencedColumnName = "emailUsuario", insertable = false, updatable = false)
    private Usuarios cliente;
}
