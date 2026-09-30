package br.com.projetofatec.barbeariaconde.security;

import br.com.projetofatec.barbeariaconde.repository.RefreshTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Revogacao em transacao propria.
 *
 * <p>Existe por um motivo especifico: quando detectamos reuso de refresh token, revogamos
 * a familia inteira e em seguida lancamos {@code BadCredentialsException}. Se a revogacao
 * estivesse na mesma transacao, o rollback provocado por essa excecao desfaria justamente
 * a medida de seguranca. Com {@code REQUIRES_NEW} a revogacao e commitada antes de a
 * excecao subir.
 */
@Component
public class RevogacaoDeTokens {

    private final RefreshTokenRepository repository;

    public RevogacaoDeTokens(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int revogarFamiliaDoUsuario(Long usuarioId) {
        return repository.revogarTodosDoUsuario(usuarioId, Instant.now());
    }
}
