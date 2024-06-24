package br.com.projetofatec.barbeariaconde.repository;

import br.com.projetofatec.barbeariaconde.model.Clientes;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientesRepository extends JpaRepository<Clientes, Long> {
    Clientes findByNomeUsuario(String nomeUsuario);

    Clientes findByEmailUsuario(String emailUsuario);
}
