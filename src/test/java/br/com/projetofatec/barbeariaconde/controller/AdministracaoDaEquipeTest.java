package br.com.projetofatec.barbeariaconde.controller;

import br.com.projetofatec.barbeariaconde.dto.agenda.AgendamentoRequest;
import br.com.projetofatec.barbeariaconde.model.Servico;
import br.com.projetofatec.barbeariaconde.model.Usuario;
import br.com.projetofatec.barbeariaconde.service.AgendamentoService;
import br.com.projetofatec.barbeariaconde.suporte.BaseDeIntegracao;
import br.com.projetofatec.barbeariaconde.suporte.DatasDeTeste;
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
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Administracao da equipe pelo proprietario.
 *
 * <p>Cobre a lacuna que faltava fechar: so os dados de demonstracao criavam
 * profissionais, entao numa instalacao real o administrador entrava e nao tinha como
 * cadastrar quem atende — e sem barbeiro ativo a agenda nao abre.
 */
@AutoConfigureMockMvc
class AdministracaoDaEquipeTest extends BaseDeIntegracao {

    private static final String SENHA_NOVA = "outraSenhaForte456";
    private static final String EMAIL_DO_ADMIN = "admin@barbearia.test";
    private static final String EMAIL_DO_TIAGO = "tiago.conde@barbearia.test";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private AgendamentoService agendamentoService;

    // --- Cadastro -------------------------------------------------------------

    @Test
    @DisplayName("admin cadastra profissional, que passa a aparecer na lista pública")
    void cadastroCriaProfissionalDisponivel() throws Exception {
        criarAdmin();

        mvc.perform(post("/api/admin/barbeiros")
                        .header(HttpHeaders.AUTHORIZATION, autorizacaoDoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Rafael Souza",
                                  "email": "rafael@barbeariaconde.com.br",
                                  "senha": "%s",
                                  "telefone": "(17) 99999-0002"
                                }
                                """.formatted(SENHA_NOVA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Rafael Souza"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.agendamentosFuturos").value(0))
                // O hash nunca sai da API, nem na area administrativa.
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        mvc.perform(get("/api/barbeiros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Rafael Souza"));

        // E o profissional recem-criado entra com a senha provisoria.
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeLogin("rafael@barbeariaconde.com.br", SENHA_NOVA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.role").value("BARBEIRO"));
    }

    @Test
    @DisplayName("e-mail já cadastrado é recusado com 409")
    void emailRepetido() throws Exception {
        criarAdmin();
        criarBarbeiro("Tiago Conde");

        mvc.perform(post("/api/admin/barbeiros")
                        .header(HttpHeaders.AUTHORIZATION, autorizacaoDoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Outro Profissional",
                                  "email": "%s",
                                  "senha": "%s"
                                }
                                """.formatted(EMAIL_DO_TIAGO, SENHA_NOVA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Já existe uma conta com este e-mail."));
    }

    @Test
    @DisplayName("cadastro inválido aponta campo por campo")
    void cadastroInvalido() throws Exception {
        criarAdmin();

        mvc.perform(post("/api/admin/barbeiros")
                        .header(HttpHeaders.AUTHORIZATION, autorizacaoDoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "A", "email": "x", "senha": "123", "telefone": "abc" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.length()").value(4));
    }

    // --- Situacao -------------------------------------------------------------

    @Test
    @DisplayName("listagem mostra ativos e inativos, com o que segue marcado para cada um")
    void listagemMostraTudo() throws Exception {
        criarAdmin();
        Usuario tiago = criarBarbeiro("Tiago Conde");
        criarBarbeiro("Rafael Souza");
        agendarCom(tiago);

        desativar(tiago.getId());

        mvc.perform(get("/api/admin/barbeiros")
                        .header(HttpHeaders.AUTHORIZATION, autorizacaoDoAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // Ordenado por nome: Rafael antes de Tiago.
                .andExpect(jsonPath("$[0].nome").value("Rafael Souza"))
                .andExpect(jsonPath("$[0].agendamentosFuturos").value(0))
                .andExpect(jsonPath("$[1].nome").value("Tiago Conde"))
                .andExpect(jsonPath("$[1].ativo").value(false))
                .andExpect(jsonPath("$[1].email").value(EMAIL_DO_TIAGO))
                .andExpect(jsonPath("$[1].agendamentosFuturos").value(1));

        // A lista publica continua mostrando apenas quem pode receber reserva.
        mvc.perform(get("/api/barbeiros"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Rafael Souza"));
    }

    @Test
    @DisplayName("desativar derruba o acesso e informa o que continua agendado")
    void desativarDerrubaAcesso() throws Exception {
        criarAdmin();
        Usuario tiago = criarBarbeiro("Tiago Conde");
        agendarCom(tiago);

        MvcResult login = logar(EMAIL_DO_TIAGO, SENHA_PADRAO);
        String tokenDoBarbeiro = acessoDe(login);
        Cookie refreshDoBarbeiro = login.getResponse().getCookie("refreshToken");

        desativar(tiago.getId())
                .andExpect(jsonPath("$.ativo").value(false))
                // Desativar nao cancela nada: o atendimento marcado continua valendo.
                .andExpect(jsonPath("$.agendamentosFuturos").value(1));

        // Token ainda nao expirado, mas a conta desativada ja nao autentica.
        mvc.perform(get("/api/agendamentos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDoBarbeiro))
                .andExpect(status().isUnauthorized());

        // E o refresh token que ficou no navegador dele tambem foi revogado.
        mvc.perform(post("/api/auth/refresh").cookie(refreshDoBarbeiro))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeLogin(EMAIL_DO_TIAGO, SENHA_PADRAO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Esta conta está desativada."));
    }

    @Test
    @DisplayName("reativar devolve o acesso e a vaga na agenda")
    void reativarDevolveAcesso() throws Exception {
        criarAdmin();
        Usuario tiago = criarBarbeiro("Tiago Conde");

        desativar(tiago.getId());

        mvc.perform(patch("/api/admin/barbeiros/{id}/situacao", tiago.getId())
                        .header(HttpHeaders.AUTHORIZATION, autorizacaoDoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"ativo\": true }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));

        mvc.perform(get("/api/barbeiros")).andExpect(jsonPath("$.length()").value(1));
        logar(EMAIL_DO_TIAGO, SENHA_PADRAO);
    }

    @Test
    @DisplayName("profissional inexistente responde 404")
    void profissionalInexistente() throws Exception {
        criarAdmin();

        mvc.perform(patch("/api/admin/barbeiros/{id}/situacao", 9999)
                        .header(HttpHeaders.AUTHORIZATION, autorizacaoDoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"ativo\": false }"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a administração só alcança barbeiros: nem cliente, nem a própria conta")
    void administracaoSoAlcancaBarbeiros() throws Exception {
        criarAdmin();
        Usuario cliente = criarCliente("cliente.alvo@exemplo.com");
        Long idDoAdmin = usuarioRepository.findByEmailIgnoreCase(EMAIL_DO_ADMIN).orElseThrow().getId();

        // A propria conta inclusive: ninguem se desativa por aqui e deixa a instalacao
        // sem administrador nenhum.
        for (Long alvo : new Long[]{cliente.getId(), idDoAdmin}) {
            mvc.perform(patch("/api/admin/barbeiros/{id}/situacao", alvo)
                            .header(HttpHeaders.AUTHORIZATION, autorizacaoDoAdmin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"ativo\": false }"))
                    .andExpect(status().isNotFound());
        }
    }

    // --- Permissao ------------------------------------------------------------

    @Test
    @DisplayName("barbeiro, cliente e visitante não entram na administração")
    void somenteAdmin() throws Exception {
        criarAdmin();
        criarBarbeiro("Tiago Conde");
        criarCliente("cliente.comum@exemplo.com");

        mvc.perform(get("/api/admin/barbeiros")).andExpect(status().isUnauthorized());

        for (String email : new String[]{EMAIL_DO_TIAGO, "cliente.comum@exemplo.com"}) {
            mvc.perform(get("/api/admin/barbeiros")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(email)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        // O barbeiro continua com o painel de agenda, que e o acesso dele.
        mvc.perform(get("/api/agendamentos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(EMAIL_DO_TIAGO)))
                .andExpect(status().isOk());
    }

    // --- Senhas ---------------------------------------------------------------

    @Test
    @DisplayName("senha redefinida pelo admin invalida a anterior e encerra as sessões")
    void redefinicaoDeSenhaPeloAdmin() throws Exception {
        criarAdmin();
        Usuario tiago = criarBarbeiro("Tiago Conde");

        Cookie refreshAntigo = logar(EMAIL_DO_TIAGO, SENHA_PADRAO)
                .getResponse().getCookie("refreshToken");

        mvc.perform(patch("/api/admin/barbeiros/{id}/senha", tiago.getId())
                        .header(HttpHeaders.AUTHORIZATION, autorizacaoDoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"novaSenha\": \"%s\" }".formatted(SENHA_NOVA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Tiago Conde"));

        mvc.perform(post("/api/auth/refresh").cookie(refreshAntigo))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeLogin(EMAIL_DO_TIAGO, SENHA_PADRAO)))
                .andExpect(status().isUnauthorized());

        logar(EMAIL_DO_TIAGO, SENHA_NOVA);
    }

    @Test
    @DisplayName("senha curta é recusada na redefinição")
    void redefinicaoComSenhaCurta() throws Exception {
        criarAdmin();
        Usuario tiago = criarBarbeiro("Tiago Conde");

        mvc.perform(patch("/api/admin/barbeiros/{id}/senha", tiago.getId())
                        .header(HttpHeaders.AUTHORIZATION, autorizacaoDoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"novaSenha\": \"curta\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("novaSenha"));
    }

    @Test
    @DisplayName("troca da própria senha exige a atual e mantém esta sessão de pé")
    void trocaDaPropriaSenha() throws Exception {
        criarAdmin();

        MvcResult login = logar(EMAIL_DO_ADMIN, SENHA_PADRAO);
        String token = acessoDe(login);
        Cookie refreshAntigo = login.getResponse().getCookie("refreshToken");

        trocarSenha(token, "errada123", SENHA_NOVA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A senha atual não confere."));

        trocarSenha(token, SENHA_PADRAO, SENHA_PADRAO)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value("A nova senha precisa ser diferente da atual."));

        MvcResult troca = trocarSenha(token, SENHA_PADRAO, SENHA_NOVA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        // A sessao de quem trocou segue em pe, com o par de tokens novo.
        mvc.perform(get("/api/auth/eu")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + acessoDe(troca)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL_DO_ADMIN));
        mvc.perform(post("/api/auth/refresh").cookie(troca.getResponse().getCookie("refreshToken")))
                .andExpect(status().isOk());

        // As outras, nao.
        mvc.perform(post("/api/auth/refresh").cookie(refreshAntigo))
                .andExpect(status().isUnauthorized());

        logar(EMAIL_DO_ADMIN, SENHA_NOVA);
    }

    @Test
    @DisplayName("visitante não troca senha nenhuma")
    void trocaDeSenhaExigeAutenticacao() throws Exception {
        mvc.perform(patch("/api/auth/senha").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"senhaAtual\": \"a\", \"novaSenha\": \"%s\" }"
                                .formatted(SENHA_NOVA)))
                .andExpect(status().isUnauthorized());
    }

    // --- Apoio ----------------------------------------------------------------

    private void agendarCom(Usuario barbeiro) {
        agendamentoService.criar(criarCliente(), new AgendamentoRequest(
                Servico.CABELO, DatasDeTeste.proximaQuarta(), LocalTime.of(10, 0),
                barbeiro.getId(), null, null));
    }

    private ResultActions desativar(Long id) throws Exception {
        return mvc.perform(patch("/api/admin/barbeiros/{id}/situacao", id)
                        .header(HttpHeaders.AUTHORIZATION, autorizacaoDoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"ativo\": false }"))
                .andExpect(status().isOk());
    }

    private ResultActions trocarSenha(String token, String atual, String nova) throws Exception {
        return mvc.perform(patch("/api/auth/senha")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"senhaAtual\": \"%s\", \"novaSenha\": \"%s\" }".formatted(atual, nova)));
    }

    private MvcResult logar(String email, String senha) throws Exception {
        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeLogin(email, senha)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String autorizacaoDoAdmin() throws Exception {
        return "Bearer " + tokenDe(EMAIL_DO_ADMIN);
    }

    private String tokenDe(String email) throws Exception {
        return acessoDe(logar(email, SENHA_PADRAO));
    }

    private String acessoDe(MvcResult resultado) throws Exception {
        return json.readTree(resultado.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String corpoDeLogin(String email, String senha) {
        return """
                { "email": "%s", "senha": "%s" }
                """.formatted(email, senha);
    }
}
