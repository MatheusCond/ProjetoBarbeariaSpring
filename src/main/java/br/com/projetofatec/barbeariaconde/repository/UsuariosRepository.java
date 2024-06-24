package br.com.projetofatec.barbeariaconde.repository;

import br.com.projetofatec.barbeariaconde.model.Usuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public interface UsuariosRepository extends JpaRepository<Usuarios, String>{
    UserDetails findByNomeUsuario(String login);

    boolean existsByEmailUsuario(String emailUsuario);

    boolean existsByNomeUsuario(String nomeUsuario);
}