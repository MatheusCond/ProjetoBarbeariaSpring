package br.com.projetofatec.barbeariaconde.suporte;

import br.com.projetofatec.barbeariaconde.model.Role;
import br.com.projetofatec.barbeariaconde.model.Usuario;
import br.com.projetofatec.barbeariaconde.repository.AgendamentoRepository;
import br.com.projetofatec.barbeariaconde.repository.AgendamentoSlotRepository;
import br.com.projetofatec.barbeariaconde.repository.RefreshTokenRepository;
import br.com.projetofatec.barbeariaconde.repository.UsuarioRepository;
import br.com.projetofatec.barbeariaconde.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base dos testes de integracao: sobe o contexto uma vez e limpa as tabelas antes de cada
 * teste. Nao usa {@code @Transactional} nos testes de proposito, porque o cenario de
 * concorrencia precisa de transacoes de verdade, commitadas.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseDeIntegracao {

    protected static final String SENHA_PADRAO = "senhaDeTeste123";

    private static final AtomicInteger SEQUENCIA = new AtomicInteger();

    @Autowired
    protected UsuarioRepository usuarioRepository;
    @Autowired
    protected AgendamentoRepository agendamentoRepository;
    @Autowired
    protected AgendamentoSlotRepository slotRepository;
    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;
    @Autowired
    protected UsuarioService usuarioService;

    @BeforeEach
    void limparBase() {
        slotRepository.deleteAll();
        agendamentoRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    protected Usuario criarCliente() {
        return criarCliente("cliente" + SEQUENCIA.incrementAndGet() + "@exemplo.com");
    }

    protected Usuario criarCliente(String email) {
        return usuarioService.criar("Cliente Teste", email, SENHA_PADRAO,
                "(17) 99999-1234", "Rua Teste, 1", Role.CLIENTE);
    }

    protected Usuario criarBarbeiro(String nome) {
        return usuarioService.criar(nome,
                nome.toLowerCase().replace(" ", ".") + "@barbearia.test", SENHA_PADRAO,
                "(17) 99999-0000", null, Role.BARBEIRO);
    }

    protected Usuario criarAdmin() {
        return usuarioService.criar("Admin", "admin@barbearia.test", SENHA_PADRAO,
                "(17) 99999-0001", null, Role.ADMIN);
    }
}
