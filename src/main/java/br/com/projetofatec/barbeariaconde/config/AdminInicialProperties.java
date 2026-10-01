package br.com.projetofatec.barbeariaconde.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Conta de administracao criada no primeiro boot (prefixo {@code barbearia.admin-inicial}).
 *
 * <p>Existe para resolver um problema concreto de implantacao: fora do ambiente de
 * demonstracao nao ha nenhuma tela que crie um administrador, entao uma instalacao nova
 * ficaria sem ninguem capaz de abrir o painel — e a unica saida seria um UPDATE na mao,
 * direto no banco.
 *
 * <p>A senha vem de variavel de ambiente de proposito. Nada aqui deve ter valor padrao:
 * um padrao em codigo publico seria uma porta destrancada.
 */
@ConfigurationProperties(prefix = "barbearia.admin-inicial")
@Getter
@Setter
public class AdminInicialProperties {

    /** E-mail do administrador. Em branco desliga a criacao automatica. */
    private String email = "";

    /** Senha inicial. Obrigatoria quando o e-mail e informado. */
    private String senha = "";

    private String nome = "Administração";

    private String telefone;

    public boolean configurado() {
        return email != null && !email.isBlank();
    }
}
