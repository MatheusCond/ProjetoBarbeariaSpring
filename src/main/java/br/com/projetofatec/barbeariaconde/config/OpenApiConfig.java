package br.com.projetofatec.barbeariaconde.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentacao OpenAPI com esquema de seguranca declarado, para dar pra testar os
 * endpoints protegidos direto no Swagger UI (botao "Authorize").
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_BEARER = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Barbearia Conde API")
                        .version("1.0.0")
                        .description("""
                                API do site da Barbearia Conde: cadastro e autenticação com JWT,
                                consulta de disponibilidade e agendamento de serviços.

                                Autenticação: chame POST /api/auth/login, copie o campo `accessToken`
                                e informe-o em Authorize. O refresh token vem em cookie httpOnly e é
                                usado apenas por POST /api/auth/refresh.""")
                        .contact(new Contact().name("Matheus Cond").url("https://github.com/MatheusCond")))
                .components(new Components().addSecuritySchemes(ESQUEMA_BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token devolvido por /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER));
    }
}
