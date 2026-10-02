package br.com.projetofatec.barbeariaconde.service;

import br.com.projetofatec.barbeariaconde.config.AgendaProperties;
import br.com.projetofatec.barbeariaconde.dto.admin.BarbeiroAdminResponse;
import br.com.projetofatec.barbeariaconde.dto.admin.NovoBarbeiroRequest;
import br.com.projetofatec.barbeariaconde.model.Role;
import br.com.projetofatec.barbeariaconde.model.StatusAgendamento;
import br.com.projetofatec.barbeariaconde.model.Usuario;
import br.com.projetofatec.barbeariaconde.repository.AgendamentoRepository;
import br.com.projetofatec.barbeariaconde.security.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Operacoes do proprietario sobre a equipe.
 *
 * <p>Fecha a ultima lacuna de implantacao do projeto: o administrador criado no primeiro
 * boot ({@link br.com.projetofatec.barbeariaconde.config.AdminInicial}) ja conseguia
 * entrar, mas nao tinha por onde cadastrar quem vai atender — e sem barbeiro ativo a
 * agenda nao abre. Antes disso, so os dados de demonstracao criavam profissionais, o que
 * nao serve para uma instalacao real.
 *
 * <p>Desligar um profissional e sempre reversivel e nunca mexe em agendamento: o que muda
 * e o acesso dele e a presenca na lista de quem recebe novas reservas. Por isso cada
 * resposta leva {@code agendamentosFuturos} — a tela precisa dizer quantos atendimentos
 * continuam marcados no nome de quem acabou de ser desligado.
 */
@Service
public class AdministracaoService {

    private static final Logger log = LoggerFactory.getLogger(AdministracaoService.class);

    private final UsuarioService usuarioService;
    private final AgendamentoRepository agendamentoRepository;
    private final RefreshTokenService refreshTokenService;
    private final AgendaProperties props;

    public AdministracaoService(UsuarioService usuarioService,
                                AgendamentoRepository agendamentoRepository,
                                RefreshTokenService refreshTokenService,
                                AgendaProperties props) {
        this.usuarioService = usuarioService;
        this.agendamentoRepository = agendamentoRepository;
        this.refreshTokenService = refreshTokenService;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public List<BarbeiroAdminResponse> listarBarbeiros() {
        Map<Long, Long> futuros = futurosPorBarbeiro();
        return usuarioService.barbeirosParaAdministracao().stream()
                .map(barbeiro -> BarbeiroAdminResponse.de(
                        barbeiro, futuros.getOrDefault(barbeiro.getId(), 0L)))
                .toList();
    }

    /**
     * Cadastra um profissional com senha inicial definida pelo administrador.
     *
     * <p>Nao reaproveita o cadastro publico de propósito: aquele cria sempre
     * {@link Role#CLIENTE} e e aberto a visitantes. Perfil nunca vem da requisicao.
     */
    @Transactional
    public BarbeiroAdminResponse cadastrarBarbeiro(NovoBarbeiroRequest dto) {
        Usuario barbeiro = usuarioService.criar(dto.nome(), dto.email(), dto.senha(),
                textoOuNulo(dto.telefone()), null, Role.BARBEIRO);

        log.info("Profissional {} cadastrado pela administração.", barbeiro.getEmail());
        return BarbeiroAdminResponse.de(barbeiro, 0L);
    }

    /**
     * Liga ou desliga o profissional.
     *
     * <p>Ao desligar, as sessoes abertas dele sao revogadas: sem isso o refresh token no
     * navegador continuaria valendo por sete dias e o acesso so cairia na expiracao.
     */
    @Transactional
    public BarbeiroAdminResponse definirSituacao(Long id, boolean ativo) {
        Usuario barbeiro = usuarioService.definirSituacaoDoBarbeiro(id, ativo);

        if (!ativo) {
            refreshTokenService.revogarTodosDoUsuario(barbeiro.getId());
        }

        log.info("Profissional {} {} pela administração.",
                barbeiro.getEmail(), ativo ? "reativado" : "desativado");
        return comContagem(barbeiro);
    }

    /**
     * Define uma senha provisoria para o profissional e encerra as sessoes dele, para que
     * uma sessao antiga nao continue valendo com a credencial trocada.
     */
    @Transactional
    public BarbeiroAdminResponse redefinirSenha(Long id, String novaSenha) {
        Usuario barbeiro = usuarioService.barbeiroQualquer(id);
        usuarioService.redefinirSenha(barbeiro.getId(), novaSenha);
        refreshTokenService.revogarTodosDoUsuario(barbeiro.getId());

        log.info("Senha do profissional {} redefinida pela administração.", barbeiro.getEmail());
        return comContagem(barbeiro);
    }

    private BarbeiroAdminResponse comContagem(Usuario barbeiro) {
        return BarbeiroAdminResponse.de(barbeiro, agendamentoRepository
                .countByBarbeiroIdAndStatusAndDataGreaterThanEqual(
                        barbeiro.getId(),
                        StatusAgendamento.AGENDADO,
                        hoje()));
    }

    private Map<Long, Long> futurosPorBarbeiro() {
        Map<Long, Long> contagem = new HashMap<>();
        for (Object[] linha : agendamentoRepository.contarFuturosPorBarbeiro(hoje())) {
            contagem.put((Long) linha[0], (Long) linha[1]);
        }
        return contagem;
    }

    private LocalDate hoje() {
        return LocalDate.now(props.getFusoHorario());
    }

    /** Telefone em branco vira nulo: coluna opcional nao deve guardar string vazia. */
    private String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
