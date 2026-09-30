package br.com.projetofatec.barbeariaconde.service;

import br.com.projetofatec.barbeariaconde.dto.auth.LoginRequest;
import br.com.projetofatec.barbeariaconde.dto.auth.LoginResponse;
import br.com.projetofatec.barbeariaconde.dto.auth.RegistroRequest;
import br.com.projetofatec.barbeariaconde.dto.auth.UsuarioResponse;
import br.com.projetofatec.barbeariaconde.model.Usuario;
import br.com.projetofatec.barbeariaconde.security.JwtService;
import br.com.projetofatec.barbeariaconde.security.RefreshTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra login, registro, refresh e logout.
 *
 * <p>Modelo de token adotado:
 * <ul>
 *   <li><b>access token</b> (JWT, 15 min): vai no corpo da resposta e o front guarda
 *       em memoria. Nao vai para {@code localStorage}, que e legivel por qualquer
 *       script injetado na pagina.</li>
 *   <li><b>refresh token</b> (opaco, 7 dias): vai em cookie {@code HttpOnly},
 *       {@code SameSite=Strict}, com {@code Path} restrito a {@code /api/auth}. E o que
 *       mantem a sessao viva entre recarregamentos sem expor nada ao JavaScript.</li>
 * </ul>
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(AuthenticationManager authenticationManager,
                       UsuarioService usuarioService,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public Sessao registrar(RegistroRequest dto) {
        Usuario usuario = usuarioService.registrarCliente(dto);
        return novaSessao(usuario);
    }

    @Transactional
    public Sessao login(LoginRequest dto) {
        Authentication autenticacao = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email().trim().toLowerCase(), dto.senha()));

        Usuario usuario = (Usuario) autenticacao.getPrincipal();
        return novaSessao(usuario);
    }

    /** Troca o refresh token por um novo par, invalidando o anterior. */
    @Transactional
    public Sessao renovar(String refreshTokenRecebido) {
        RefreshTokenService.Rotacao rotacao = refreshTokenService.rotacionar(refreshTokenRecebido);
        Usuario usuario = rotacao.usuario();
        return new Sessao(
                LoginResponse.de(jwtService.gerarAccessToken(usuario),
                        jwtService.segundosDeValidadeDoAccessToken(),
                        UsuarioResponse.de(usuario)),
                rotacao.novoValor());
    }

    @Transactional
    public void logout(String refreshTokenRecebido) {
        refreshTokenService.revogar(refreshTokenRecebido);
    }

    /** Encerra todas as sessoes do usuario (util quando ele troca a senha). */
    @Transactional
    public void logoutGlobal(Usuario usuario) {
        refreshTokenService.revogarTodosDoUsuario(usuario.getId());
    }

    private Sessao novaSessao(Usuario usuario) {
        String refresh = refreshTokenService.emitir(usuario);
        LoginResponse corpo = LoginResponse.de(
                jwtService.gerarAccessToken(usuario),
                jwtService.segundosDeValidadeDoAccessToken(),
                UsuarioResponse.de(usuario));
        return new Sessao(corpo, refresh);
    }

    /** Par formado pelo corpo da resposta e pelo refresh token que vai no cookie. */
    public record Sessao(LoginResponse corpo, String refreshToken) {
    }
}
