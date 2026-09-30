package br.com.projetofatec.barbeariaconde.controller;

import br.com.projetofatec.barbeariaconde.suporte.BaseDeIntegracao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo de autenticacao pela API.
 *
 * <p>Cobre justamente o que estava quebrado: o token nao era verificado, nenhuma rota era
 * protegida, o login respondia com a string crua do JWT e o front nunca guardava nada.
 */
@AutoConfigureMockMvc
class AutenticacaoFluxoTest extends BaseDeIntegracao {

    private static final String EMAIL = "novo.cliente@exemplo.com";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    private String corpoDeRegistro() {
        return """
                {
                  "nome": "Novo Cliente",
                  "email": "%s",
                  "senha": "%s",
                  "telefone": "(17) 99999-1234",
                  "endereco": "Rua Teste, 10"
                }
                """.formatted(EMAIL, SENHA_PADRAO);
    }

    private String corpoDeLogin(String email, String senha) {
        return """
                { "email": "%s", "senha": "%s" }
                """.formatted(email, senha);
    }

    @Test
    @DisplayName("registro devolve access token no corpo e refresh token em cookie httpOnly")
    void registroDevolveSessao() throws Exception {
        MvcResult resultado = mvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeRegistro()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEmSegundos").value(900))
                .andExpect(jsonPath("$.usuario.email").value(EMAIL))
                .andExpect(jsonPath("$.usuario.role").value("CLIENTE"))
                // O hash da senha nunca sai da API.
                .andExpect(jsonPath("$.usuario.senhaHash").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        String setCookie = resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .contains("refreshToken=")
                .contains("HttpOnly")
                .contains("SameSite=Strict")
                .contains("Path=/api/auth");
    }

    @Test
    @DisplayName("e-mail repetido é recusado com 409")
    void emailDuplicado() throws Exception {
        mvc.perform(post("/api/auth/registrar").contentType(MediaType.APPLICATION_JSON)
                .content(corpoDeRegistro())).andExpect(status().isCreated());

        mvc.perform(post("/api/auth/registrar").contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeRegistro()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Já existe uma conta com este e-mail."));
    }

    @Test
    @DisplayName("cadastro inválido lista os campos com problema")
    void validacaoDeCampos() throws Exception {
        mvc.perform(post("/api/auth/registrar").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "A", "email": "nao-e-email", "senha": "123", "telefone": "abc" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.campos").isArray())
                .andExpect(jsonPath("$.campos.length()").value(4));
    }

    @Test
    @DisplayName("rota protegida devolve 401 em JSON sem token e 200 com token")
    void rotaProtegidaExigeToken() throws Exception {
        mvc.perform(get("/api/agendamentos/meus"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").isNotEmpty());

        String token = registrarEObterToken();

        mvc.perform(get("/api/agendamentos/meus").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isArray());
    }

    @Test
    @DisplayName("token adulterado é rejeitado")
    void tokenAdulterado() throws Exception {
        String token = registrarEObterToken();
        String adulterado = token.substring(0, token.lastIndexOf('.') + 1) + "assinaturaFalsa";

        mvc.perform(get("/api/auth/eu").header(HttpHeaders.AUTHORIZATION, "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("senha errada devolve 401 com mensagem que não revela se o e-mail existe")
    void senhaErrada() throws Exception {
        mvc.perform(post("/api/auth/registrar").contentType(MediaType.APPLICATION_JSON)
                .content(corpoDeRegistro())).andExpect(status().isCreated());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeLogin(EMAIL, "senhaErrada123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos."));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeLogin("inexistente@exemplo.com", "qualquer123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos."));
    }

    @Test
    @DisplayName("refresh rotaciona o token e o antigo deixa de valer")
    void refreshRotacionaToken() throws Exception {
        MvcResult registro = mvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON).content(corpoDeRegistro()))
                .andExpect(status().isCreated()).andReturn();

        Cookie cookieOriginal = registro.getResponse().getCookie("refreshToken");
        assertThat(cookieOriginal).isNotNull();
        assertThat(cookieOriginal.isHttpOnly()).isTrue();

        MvcResult refresh = mvc.perform(post("/api/auth/refresh").cookie(cookieOriginal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        Cookie cookieNovo = refresh.getResponse().getCookie("refreshToken");
        assertThat(cookieNovo).isNotNull();
        assertThat(cookieNovo.getValue()).isNotEqualTo(cookieOriginal.getValue());

        // Reuso do token antigo: recusado (e, por politica, derruba a sessao toda).
        mvc.perform(post("/api/auth/refresh").cookie(cookieOriginal))
                .andExpect(status().isUnauthorized());

        // Como o reuso revoga a familia, nem o token novo continua valendo.
        mvc.perform(post("/api/auth/refresh").cookie(cookieNovo))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("refresh sem cookie devolve 401")
    void refreshSemCookie() throws Exception {
        mvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("logout revoga o refresh token e apaga o cookie")
    void logoutRevogaSessao() throws Exception {
        MvcResult registro = mvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON).content(corpoDeRegistro()))
                .andExpect(status().isCreated()).andReturn();
        Cookie cookie = registro.getResponse().getCookie("refreshToken");

        MvcResult logout = mvc.perform(post("/api/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(logout.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");

        mvc.perform(post("/api/auth/refresh").cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("cliente não acessa a agenda completa da barbearia")
    void clienteNaoAcessaAgendaDaEquipe() throws Exception {
        String token = registrarEObterToken();

        mvc.perform(get("/api/agendamentos").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("catálogo e disponibilidade ficam abertos para visitantes")
    void endpointsPublicos() throws Exception {
        mvc.perform(get("/api/servicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].preco").isNotEmpty());

        mvc.perform(get("/api/barbeiros")).andExpect(status().isOk());

        mvc.perform(get("/api/agendamentos/disponibilidade")
                        .param("data", br.com.projetofatec.barbeariaconde.suporte.DatasDeTeste
                                .proximaQuarta().toString())
                        .param("servico", "CABELO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aberto").exists());
    }

    private String registrarEObterToken() throws Exception {
        MvcResult resultado = mvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON).content(corpoDeRegistro()))
                .andExpect(status().isCreated()).andReturn();
        JsonNode corpo = json.readTree(resultado.getResponse().getContentAsString());
        return corpo.get("accessToken").asText();
    }
}
