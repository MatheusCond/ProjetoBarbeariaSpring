package br.com.projetofatec.barbeariaconde.security;

import br.com.projetofatec.barbeariaconde.config.JwtProperties;
import br.com.projetofatec.barbeariaconde.model.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Emissao e verificacao do access token.
 *
 * <p>Diferencas em relacao a versao anterior: segredo vem de configuracao (nao mais
 * {@code "1234"} no codigo), algoritmo HMAC-512, e existe de fato um metodo de
 * verificacao, com validacao de assinatura, emissor e expiracao.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private static final String CLAIM_NOME = "nome";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_ID = "uid";

    private final JwtProperties props;
    private Algorithm algoritmo;
    private JWTVerifier verificador;

    public JwtService(JwtProperties props) {
        this.props = props;
    }

    @PostConstruct
    void inicializar() {
        String segredo = props.getSegredo();
        if (segredo == null || segredo.isBlank()) {
            byte[] aleatorio = new byte[64];
            new SecureRandom().nextBytes(aleatorio);
            segredo = Base64.getEncoder().encodeToString(aleatorio);
            log.warn("""
                    barbearia.jwt.segredo nao configurado: usando segredo aleatorio gerado no boot.
                    Todos os tokens sao invalidados a cada restart. Defina a variavel de ambiente \
                    BARBEARIA_JWT_SEGREDO antes de publicar.""");
        } else if (segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "barbearia.jwt.segredo precisa ter ao menos 32 bytes para HMAC-512");
        }
        this.algoritmo = Algorithm.HMAC512(segredo);
        this.verificador = JWT.require(algoritmo)
                .withIssuer(props.getEmissor())
                .build();
    }

    public String gerarAccessToken(Usuario usuario) {
        Instant agora = Instant.now();
        return JWT.create()
                .withIssuer(props.getEmissor())
                .withSubject(usuario.getEmail())
                .withClaim(CLAIM_ID, usuario.getId())
                .withClaim(CLAIM_NOME, usuario.getNome())
                .withClaim(CLAIM_ROLE, usuario.getRole().name())
                .withIssuedAt(agora)
                .withExpiresAt(agora.plus(props.getExpiracaoAccessToken()))
                .sign(algoritmo);
    }

    /** Retorna o token decodificado, ou vazio se assinatura, emissor ou validade falharem. */
    public Optional<DecodedJWT> verificar(String token) {
        try {
            return Optional.of(verificador.verify(token));
        } catch (JWTVerificationException e) {
            log.debug("Token rejeitado: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public long segundosDeValidadeDoAccessToken() {
        return props.getExpiracaoAccessToken().toSeconds();
    }
}
