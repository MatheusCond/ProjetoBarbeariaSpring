package br.com.projetofatec.barbeariaconde.config;

import br.com.projetofatec.barbeariaconde.security.JwtAuthenticationFilter;
import br.com.projetofatec.barbeariaconde.security.RespostaJsonDeSeguranca;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuracao de seguranca.
 *
 * <p>A versao anterior criava um {@code SecurityFilterChain} sem nenhuma regra de
 * autorizacao e sem filtro de JWT: na pratica a API inteira era publica e o token
 * emitido no login nunca era verificado. Aqui as rotas publicas sao declaradas uma a uma
 * e todo o resto exige autenticacao.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Paginas, CSS, JS e imagens do site institucional. */
    private static final String[] RECURSOS_PUBLICOS = {
            "/", "/index.html", "/login.html", "/cadastro.html", "/agendar.html", "/painel.html",
            "/css/**", "/js/**", "/imgs/**", "/favicon.ico"
    };

    /** Endpoints de API que um visitante nao autenticado pode chamar. */
    private static final String[] API_PUBLICA_GET = {
            "/api/servicos", "/api/barbeiros", "/api/agendamentos/disponibilidade"
    };

    private static final String[] DOCUMENTACAO = {
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**"
    };

    private final JwtAuthenticationFilter jwtFilter;
    private final RespostaJsonDeSeguranca respostaJson;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter, RespostaJsonDeSeguranca respostaJson) {
        this.jwtFilter = jwtFilter;
        this.respostaJson = respostaJson;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // API stateless com token no header: nao ha sessao nem formulario para proteger
                // com CSRF. O refresh token, que vai em cookie, usa SameSite=Strict.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {
                })
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(respostaJson)
                        .accessDeniedHandler(respostaJson))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RECURSOS_PUBLICOS).permitAll()
                        .requestMatchers(DOCUMENTACAO).permitAll()
                        .requestMatchers("/api/auth/registrar", "/api/auth/login",
                                "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, API_PUBLICA_GET).permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                      PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        // Sem isso o Spring responde "usuario nao encontrado" e vira um oraculo de e-mails cadastrados.
        provider.setHideUserNotFoundExceptions(true);
        return new org.springframework.security.authentication.ProviderManager(provider);
    }
}
