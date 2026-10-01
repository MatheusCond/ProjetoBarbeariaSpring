package br.com.projetofatec.barbeariaconde.repository;

import br.com.projetofatec.barbeariaconde.model.Role;
import br.com.projetofatec.barbeariaconde.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Usuario> findByRoleAndAtivoTrueOrderByNomeAsc(Role role);

    /** Inclui os inativos: é a visão que a administração precisa ter. */
    List<Usuario> findByRoleOrderByNomeAsc(Role role);

    Optional<Usuario> findByIdAndRoleAndAtivoTrue(Long id, Role role);

    Optional<Usuario> findByIdAndRole(Long id, Role role);
}
