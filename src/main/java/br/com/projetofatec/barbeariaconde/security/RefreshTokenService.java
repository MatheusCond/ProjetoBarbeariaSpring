package br.com.projetofatec.barbeariaconde.security;

import br.com.projetofatec.barbeariaconde.config.JwtProperties;
import br.com.projetofatec.barbeariaconde.model.RefreshToken;
import br.com.projetofatec.barbeariaconde.model.Usuario;
import br.com.projetofatec.barbeariaconde.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Ciclo de vida do refresh token: emissao, rotacao, revogacao.
 *
 * <p>O valor em texto puro so existe na resposta HTTP (cookie httpOnly). No banco fica
 * apenas o SHA-256. A cada uso o token e rotacionado, e um token ja usado que volta a
 * aparecer e tratado como indicio de roubo: toda a familia do usuario e revogada.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int BYTES_DO_TOKEN = 32;

    private final RefreshTokenRepository repository;
    private final RevogacaoDeTokens revogacao;
    private final JwtProperties props;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository,
                               RevogacaoDeTokens revogacao,
                               JwtProperties props) {
        this.repository = repository;
        this.revogacao = revogacao;
        this.props = props;
    }

    /** Emite um refresh token novo e devolve o valor em texto puro (unico momento em que ele existe). */
    @Transactional
    public String emitir(Usuario usuario) {
        byte[] bytes = new byte[BYTES_DO_TOKEN];
        random.nextBytes(bytes);
        String valor = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        repository.save(RefreshToken.builder()
                .tokenHash(hash(valor))
                .usuario(usuario)
                .expiraEm(Instant.now().plus(props.getExpiracaoRefreshToken()))
                .build());

        return valor;
    }

    /**
     * Valida o token recebido, revoga-o e emite um substituto.
     *
     * @return o usuario dono do token e o novo valor em texto puro
     * @throws BadCredentialsException se o token for desconhecido, expirado ou reutilizado
     */
    @Transactional
    public Rotacao rotacionar(String valorRecebido) {
        RefreshToken atual = repository.findByTokenHash(hash(valorRecebido))
                .orElseThrow(() -> new BadCredentialsException("Refresh token inválido."));

        if (atual.isRevogado()) {
            // Token ja usado reaparecendo: assume-se vazamento e derruba todas as sessoes.
            // A revogacao roda em transacao separada para sobreviver ao rollback causado
            // pela excecao lancada na linha seguinte.
            log.warn("Reuso de refresh token detectado para o usuario {}. Revogando todas as sessões.",
                    atual.getUsuario().getId());
            revogacao.revogarFamiliaDoUsuario(atual.getUsuario().getId());
            throw new BadCredentialsException("Refresh token inválido.");
        }
        if (atual.isExpirado()) {
            throw new BadCredentialsException("Sessão expirada. Faça login novamente.");
        }

        atual.setRevogadoEm(Instant.now());
        repository.save(atual);

        Usuario usuario = atual.getUsuario();
        return new Rotacao(usuario, emitir(usuario));
    }

    @Transactional
    public void revogar(String valorRecebido) {
        if (valorRecebido == null || valorRecebido.isBlank()) {
            return;
        }
        repository.findByTokenHash(hash(valorRecebido)).ifPresent(token -> {
            if (!token.isRevogado()) {
                token.setRevogadoEm(Instant.now());
                repository.save(token);
            }
        });
    }

    public void revogarTodosDoUsuario(Long usuarioId) {
        revogacao.revogarFamiliaDoUsuario(usuarioId);
    }

    private String hash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }

    public record Rotacao(Usuario usuario, String novoValor) {
    }
}
