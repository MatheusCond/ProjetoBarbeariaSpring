package br.com.projetofatec.barbeariaconde.repository;

import br.com.projetofatec.barbeariaconde.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Revoga todos os tokens vivos de um usuario (logout global / suspeita de reuso). */
    @Modifying
    @Query("""
            update RefreshToken t
               set t.revogadoEm = :agora
             where t.usuario.id = :usuarioId
               and t.revogadoEm is null
            """)
    int revogarTodosDoUsuario(@Param("usuarioId") Long usuarioId, @Param("agora") Instant agora);

    /** Limpeza de tokens que ja nao servem para nada. */
    @Modifying
    @Query("delete from RefreshToken t where t.expiraEm < :limite")
    int removerExpiradosAntesDe(@Param("limite") Instant limite);
}
