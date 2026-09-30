package br.com.projetofatec.barbeariaconde.security;

import br.com.projetofatec.barbeariaconde.model.Usuario;
import br.com.projetofatec.barbeariaconde.repository.UsuarioRepository;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Le o {@code Authorization: Bearer <token>}, valida o JWT e popula o
 * {@code SecurityContext}.
 *
 * <p>Era exatamente a peca que faltava no projeto original: o token era emitido no login,
 * mas ninguem o lia depois, e nenhuma rota era protegida de fato.
 *
 * <p>O usuario e recarregado do banco a cada requisicao. Custa uma consulta, mas garante
 * que conta desativada ou perfil alterado passem a valer na hora, sem esperar o token expirar.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            extrairToken(request)
                    .flatMap(jwtService::verificar)
                    .map(DecodedJWT::getSubject)
                    .flatMap(usuarioRepository::findByEmailIgnoreCase)
                    .filter(Usuario::isAtivo)
                    .ifPresent(usuario -> autenticar(usuario, request));
        }

        chain.doFilter(request, response);
    }

    private void autenticar(Usuario usuario, HttpServletRequest request) {
        var autenticacao = new UsernamePasswordAuthenticationToken(
                usuario, null, usuario.getAuthorities());
        autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(autenticacao);
    }

    private Optional<String> extrairToken(HttpServletRequest request) {
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho == null || !cabecalho.startsWith(PREFIXO_BEARER)) {
            return Optional.empty();
        }
        String token = cabecalho.substring(PREFIXO_BEARER.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
