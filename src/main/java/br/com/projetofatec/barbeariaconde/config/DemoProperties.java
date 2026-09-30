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
}
