package br.com.projetofatec.barbeariaconde.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Contas criadas no primeiro boot quando {@code barbearia.demo.carregar=true}. */
@Component
@ConfigurationProperties(prefix = "barbearia.demo")
@Getter
@Setter
public class DemoProperties {

    private boolean carregar = false;

    private String emailAdmin = "admin@barbeariaconde.com.br";
    private String emailBarbeiroUm = "tiago@barbeariaconde.com.br";
    private String emailBarbeiroDois = "rafael@barbeariaconde.com.br";
    private String emailCliente = "cliente@exemplo.com";

    /** Senha comum das contas de demonstração. Vale apenas para o ambiente local. */
    private String senha = "barbearia123";

    /**
     * Senha apenas da conta de cliente. Em branco, usa a mesma das demais.
     *
     * <p>Existe para a demonstração publicada poder divulgar o acesso de cliente sem
     * entregar junto o da equipe. Os e-mails de todas as contas estão neste repositório,
     * que é público: com uma senha só, divulgar a do cliente equivale a dar acesso de
     * administração a quem ler o código.
     */
    private String senhaCliente = "";

    public String senhaDoCliente() {
        return senhaCliente == null || senhaCliente.isBlank() ? senha : senhaCliente;
    }
}
