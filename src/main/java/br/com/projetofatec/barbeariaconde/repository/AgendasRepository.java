package br.com.projetofatec.barbeariaconde.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.projetofatec.barbeariaconde.model.Agendas;

import java.util.List;
import java.util.Optional;

public interface AgendasRepository extends JpaRepository<Agendas, Long> {

    void deleteByClienteEmail(String clienteEmail);

    List<Agendas> findByClienteEmail(String email);
}
