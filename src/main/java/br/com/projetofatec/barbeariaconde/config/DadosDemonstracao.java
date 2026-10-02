package br.com.projetofatec.barbeariaconde.config;

import br.com.projetofatec.barbeariaconde.model.Role;
import br.com.projetofatec.barbeariaconde.repository.UsuarioRepository;
import br.com.projetofatec.barbeariaconde.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Popula contas de demonstracao para a aplicacao ser utilizavel no primeiro boot.
 *
 * <p>Sem pelo menos um barbeiro cadastrado nao existe agenda, e o projeto nao tem tela de
 * administracao para criar o primeiro. As credenciais e o proprio carregamento sao
 * controlados por configuracao ({@code barbearia.demo.*}) e devem ficar desligados em
 * producao — e o que o perfil {@code prod} faz.
 */
@Configuration
@ConditionalOnProperty(prefix = "barbearia.demo", name = "carregar", havingValue = "true")
public class DadosDemonstracao {

    private static final Logger log = LoggerFactory.getLogger(DadosDemonstracao.class);

    /**
     * Roda antes de {@link AdminInicial}: assim, quando a demonstração está ligada, o
     * administrador já existe e a criação automática não tem o que fazer. Na ordem
     * inversa, este carregador veria a base populada e pularia os barbeiros, deixando a
     * agenda sem ninguém para atender.
     */
    static final int ORDEM = 10;

    @Bean
    @Order(DadosDemonstracao.ORDEM)
    ApplicationRunner carregarDadosDemo(UsuarioRepository repository,
                                        UsuarioService usuarioService,
                                        DemoProperties props) {
        return args -> {
            if (repository.count() > 0) {
                log.info("Base já populada: dados de demonstração não foram recriados.");
                return;
            }

            usuarioService.criar("Administração", props.getEmailAdmin(), props.getSenha(),
                    "(17) 99999-0000", null, Role.ADMIN);
            usuarioService.criar("Tiago Conde", props.getEmailBarbeiroUm(), props.getSenha(),
                    "(17) 99999-0001", null, Role.BARBEIRO);
            usuarioService.criar("Rafael Souza", props.getEmailBarbeiroDois(), props.getSenha(),
                    "(17) 99999-0002", null, Role.BARBEIRO);
            usuarioService.criar("Cliente Demo", props.getEmailCliente(), props.senhaDoCliente(),
                    "(17) 98888-1234", "Rua das Flores, 100 - Bady Bassitt", Role.CLIENTE);

            log.info("Dados de demonstração criados: 1 admin, 2 barbeiros e 1 cliente. "
                    + "As credenciais estão no README e em application.yml.");
        };
    }
}
