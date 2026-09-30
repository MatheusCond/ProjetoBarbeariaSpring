package br.com.projetofatec.barbeariaconde.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS para o caso de o front ser servido em outra origem (por exemplo, um Vite em
 * {@code localhost:5173} consumindo esta API). Com o site estatico desta propria
 * aplicacao nada disso e necessario, mas deixa a API pronta para ser reaproveitada.
 *
 * <p>{@code allowCredentials} e obrigatorio para o cookie de refresh atravessar origens,
 * e por isso a lista de origens precisa ser explicita: curinga e {@code allowCredentials}
 * sao mutuamente exclusivos.
 */
@Configuration
@ConfigurationProperties(prefix = "barbearia.cors")
@Getter
@Setter
public class WebConfig {

    private List<String> origensPermitidas = List.of("http://localhost:8080");

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origensPermitidas);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
