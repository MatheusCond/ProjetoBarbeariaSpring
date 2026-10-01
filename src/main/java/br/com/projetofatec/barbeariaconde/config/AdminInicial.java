package br.com.projetofatec.barbeariaconde.config;

import br.com.projetofatec.barbeariaconde.model.Role;
import br.com.projetofatec.barbeariaconde.repository.UsuarioRepository;
import br.com.projetofatec.barbeariaconde.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Garante que exista pelo menos um administrador quando a aplicacao sobe.
 *
 * <p>Roda depois de {@link DadosDemonstracao} (ver {@link Order}), porque no ambiente de
 * demonstracao o administrador ja vem de la e este carregador so precisa constatar que
 * nao ha nada a fazer.
 */
@Configuration
public class AdminInicial {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    /** Mesmo piso exigido no cadastro de cliente. */
    private static final int TAMANHO_MINIMO_DA_SENHA = 8;

    @Bean
    @Order(AdminInicial.ORDEM)
    ApplicationRunner criarAdminInicial(UsuarioRepository repository,
                                        UsuarioService usuarioService,
                                        AdminInicialProperties props) {
        return args -> {
            boolean jaExisteAdmin =
                    !repository.findByRoleAndAtivoTrueOrderByNomeAsc(Role.ADMIN).isEmpty();

            if (jaExisteAdmin) {
                log.debug("Já existe administrador ativo: criação automática dispensada.");
                return;
            }

            if (!props.configurado()) {
                log.warn("""

                        ============================================================
                        NENHUM ADMINISTRADOR CADASTRADO.
                        Ninguém consegue abrir o painel da equipe nesta instalação.
                        Defina as variáveis de ambiente e reinicie:
                          BARBEARIA_ADMIN_EMAIL=voce@suaempresa.com
                          BARBEARIA_ADMIN_SENHA=<senha com ao menos 8 caracteres>
                        ============================================================""");
                return;
            }

            validarSenha(props);

            usuarioService.criar(props.getNome(), props.getEmail(), props.getSenha(),
                    props.getTelefone(), null, Role.ADMIN);

            log.info("Administrador inicial criado para {}. Troque a senha após o primeiro acesso.",
                    props.getEmail());
        };
    }

    /**
     * Senha fraca aqui e erro de configuracao, nao de uso: vale interromper o boot, que
     * acontece antes de a aplicacao atender qualquer requisicao.
     */
    private void validarSenha(AdminInicialProperties props) {
        String senha = props.getSenha();
        if (senha == null || senha.length() < TAMANHO_MINIMO_DA_SENHA) {
            throw new IllegalStateException(
                    "barbearia.admin-inicial.senha (BARBEARIA_ADMIN_SENHA) precisa ter ao menos "
                            + TAMANHO_MINIMO_DA_SENHA + " caracteres.");
        }
    }

    /** Depois de {@link DadosDemonstracao#ORDEM}. */
    static final int ORDEM = 20;
}
