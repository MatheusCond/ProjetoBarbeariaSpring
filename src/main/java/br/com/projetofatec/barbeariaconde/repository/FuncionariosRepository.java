package br.com.projetofatec.barbeariaconde.repository;

import br.com.projetofatec.barbeariaconde.model.Funcionarios;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuncionariosRepository extends JpaRepository<Funcionarios, Long> {
    Funcionarios findByNomeUsuario(String nomeUsuario);
}
