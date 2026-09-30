package br.com.projetofatec.barbeariaconde.controller;

import br.com.projetofatec.barbeariaconde.suporte.BaseDeIntegracao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O site é servido pela própria aplicação, então as páginas e os assets precisam
 * continuar públicos, e um caminho inexistente precisa responder 404.
 */
@AutoConfigureMockMvc
class RecursosEstaticosTest extends BaseDeIntegracao {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("páginas e assets do site são públicos")
    void paginasPublicas() throws Exception {
        for (String caminho : new String[]{
                "/", "/index.html", "/login.html", "/cadastro.html", "/agendar.html",
                "/painel.html", "/css/app.css", "/js/api.js", "/js/ui.js", "/js/nav.js",
                "/js/home.js", "/js/agendar.js", "/js/painel.js", "/js/login.js",
                "/js/cadastro.js", "/imgs/logo.jpg", "/imgs/on.jpg", "/imgs/cut.jpg",
                "/imgs/salao.jpg", "/imgs/fachada.jpg"}) {
            mvc.perform(get(caminho)).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("recurso inexistente responde 404, e não 500")
    void recursoInexistente() throws Exception {
        mvc.perform(get("/imgs/nao-existe.png")).andExpect(status().isNotFound());
        mvc.perform(get("/css/nao-existe.css")).andExpect(status().isNotFound());
    }
}
