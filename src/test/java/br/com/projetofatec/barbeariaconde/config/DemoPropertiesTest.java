package br.com.projetofatec.barbeariaconde.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A conta de cliente da demonstracao publicada tem senha divulgada no README; as da
 * equipe, nao. Como os e-mails de todas elas estao neste repositorio, que e publico, as
 * duas senhas precisam ser mesmo independentes: se voltarem a ser a mesma, divulgar o
 * acesso de cliente passa a entregar a administracao junto.
 */
class DemoPropertiesTest {

    @Test
    @DisplayName("sem senha própria, o cliente usa a mesma senha das demais contas")
    void semSenhaPropriaUsaAComum() {
        DemoProperties props = new DemoProperties();
        props.setSenha("senhaComum123");

        assertThat(props.senhaDoCliente()).isEqualTo("senhaComum123");
    }

    @Test
    @DisplayName("senha em branco conta como não definida")
    void brancoContaComoNaoDefinida() {
        DemoProperties props = new DemoProperties();
        props.setSenha("senhaComum123");
        props.setSenhaCliente("   ");

        assertThat(props.senhaDoCliente()).isEqualTo("senhaComum123");
    }

    @Test
    @DisplayName("com senha própria, o cliente não compartilha a senha da equipe")
    void senhaPropriaIsolaOClienteDaEquipe() {
        DemoProperties props = new DemoProperties();
        props.setSenha("senhaDaEquipe456");
        props.setSenhaCliente("senhaPublicaDoCliente");

        assertThat(props.senhaDoCliente()).isEqualTo("senhaPublicaDoCliente");
        assertThat(props.senhaDoCliente()).isNotEqualTo(props.getSenha());
    }
}
