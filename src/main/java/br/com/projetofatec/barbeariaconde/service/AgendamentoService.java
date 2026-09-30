package br.com.projetofatec.barbeariaconde.service;

import br.com.projetofatec.barbeariaconde.config.AgendaProperties;
import br.com.projetofatec.barbeariaconde.dto.agenda.AgendamentoRequest;
import br.com.projetofatec.barbeariaconde.dto.agenda.AgendamentoResponse;
import br.com.projetofatec.barbeariaconde.dto.agenda.ReagendarRequest;
import br.com.projetofatec.barbeariaconde.dto.comum.PaginaResposta;
import br.com.projetofatec.barbeariaconde.exception.ConflitoException;
import br.com.projetofatec.barbeariaconde.exception.RecursoNaoEncontradoException;
import br.com.projetofatec.barbeariaconde.exception.RegraNegocioException;
import br.com.projetofatec.barbeariaconde.model.Agendamento;
import br.com.projetofatec.barbeariaconde.model.Servico;
import br.com.projetofatec.barbeariaconde.model.StatusAgendamento;
import br.com.projetofatec.barbeariaconde.model.Usuario;
import br.com.projetofatec.barbeariaconde.repository.AgendamentoRepository;
import br.com.projetofatec.barbeariaconde.repository.AgendamentoSpecs;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Regras de agendamento.
 *
 * <p>O que mudou em relacao a versao anterior, em que cada servico tinha um endpoint que
 * gravava qualquer data e hora recebida:
 * <ul>
 *   <li>o cliente vem do token, nunca do corpo da requisicao;</li>
 *   <li>data e hora passam por {@link DisponibilidadeService#validarJanelaDeAtendimento};</li>
 *   <li>a sobreposicao e barrada em duas camadas: consulta de disponibilidade e, contra
 *       corrida entre requisicoes simultaneas, a restricao unica de
 *       {@code agendamento_slots};</li>
 *   <li>cancelar e reagendar atuam em um agendamento identificado por id, e nao em todos
 *       os agendamentos de um e-mail.</li>
 * </ul>
 */
@Service
public class AgendamentoService {

    /** Sentinela para "nao ignorar nenhum agendamento" nas consultas de conflito. */
    private static final long NENHUM = -1L;

    /** Teto de tentativas, para uma disputa muito grande nao virar laco longo. */
    private static final int TETO_DE_TENTATIVAS = 6;

    private final AgendamentoRepository repository;
    private final DisponibilidadeService disponibilidade;
    private final UsuarioService usuarioService;
    private final AgendaProperties props;

    /**
     * Transacoes controladas na mao para que cada tentativa de reserva seja uma transacao
     * nova. Depois de uma violacao de restricao unica a sessao do Hibernate nao pode ser
     * reaproveitada, e por isso a retentativa precisa comecar do zero.
     */
    private final TransactionTemplate transacao;

    public AgendamentoService(AgendamentoRepository repository,
                              DisponibilidadeService disponibilidade,
                              UsuarioService usuarioService,
                              AgendaProperties props,
                              PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.disponibilidade = disponibilidade;
        this.usuarioService = usuarioService;
        this.props = props;
        this.transacao = new TransactionTemplate(transactionManager);
    }

    // --- Escrita -------------------------------------------------------------

    /**
     * Reserva um horario, com retentativa quando o cliente nao escolheu o profissional.
     *
     * <p>Sob concorrencia, varias requisicoes leem "livre" para o mesmo barbeiro e apenas
     * uma consegue gravar. Se o cliente nao tinha preferencia, faz sentido tentar de novo:
     * a nova tentativa ja enxerga o slot recem-ocupado e passa para o proximo barbeiro
     * livre. Com barbeiro escolhido nao ha alternativa, e a resposta e 409 na primeira
     * tentativa.
     */
    public Agendamento criar(Usuario solicitante, AgendamentoRequest req) {
        return comRetentativa(req.barbeiroId(), () -> criarEmTransacao(solicitante, req));
    }

    public Agendamento reagendar(Long id, Usuario solicitante, ReagendarRequest req) {
        return comRetentativa(req.barbeiroId(), () -> reagendarEmTransacao(id, solicitante, req));
    }

    /**
     * Executa a operacao em transacoes independentes ate conseguir gravar.
     *
     * <p>So repete quando a falha foi colisao de gravacao ({@link HorarioTomadoException}) e
     * quando ha outro profissional para tentar. Conflito de regra de negocio (agenda cheia,
     * limite do cliente, horario invalido) sobe na primeira tentativa.
     */
    private Agendamento comRetentativa(Long barbeiroEscolhido, Supplier<Agendamento> operacao) {
        int tentativas = barbeiroEscolhido != null ? 1 : Math.min(TETO_DE_TENTATIVAS, quantidadeDeBarbeiros() + 1);

        HorarioTomadoException ultima = null;
        for (int tentativa = 1; tentativa <= tentativas; tentativa++) {
            try {
                return transacao.execute(status -> operacao.get());
            } catch (HorarioTomadoException e) {
                ultima = e;
            }
        }
        throw new ConflitoException(ultima.getMessage());
    }

    private int quantidadeDeBarbeiros() {
        return usuarioService.barbeirosAtivos().size();
    }

    private Agendamento criarEmTransacao(Usuario solicitante, AgendamentoRequest req) {
        Usuario cliente = resolverCliente(solicitante, req.clienteEmail());
        Servico servico = req.servico();

        disponibilidade.validarJanelaDeAtendimento(req.data(), req.hora(), servico);
        validarLimiteDeReservasAtivas(cliente);
        validarConflitoDoProprioCliente(cliente, req.data(), req.hora(), servico, NENHUM);

        Usuario barbeiro = escolherBarbeiroLivre(req.data(), req.hora(), servico, req.barbeiroId());

        Agendamento agendamento = Agendamento.builder()
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servico(servico)
                .data(req.data())
                .horaInicio(req.hora())
                .horaFim(req.hora().plusMinutes(servico.getDuracaoMinutos()))
                .status(StatusAgendamento.AGENDADO)
                .preco(servico.getPreco())
                .observacao(normalizar(req.observacao()))
                .build();
        agendamento.ocuparSlots(disponibilidade.slotsDoAtendimento(req.hora(), servico));

        return salvarTratandoCorrida(agendamento);
    }

    private Agendamento reagendarEmTransacao(Long id, Usuario solicitante, ReagendarRequest req) {
        Agendamento agendamento = buscarVisivel(id, solicitante);
        exigirStatusAgendado(agendamento);
        if (!solicitante.isEquipe()) {
            validarAntecedenciaParaAlterar(agendamento);
        }

        Servico servico = req.servico();
        disponibilidade.validarJanelaDeAtendimento(req.data(), req.hora(), servico);
        validarConflitoDoProprioCliente(agendamento.getCliente(), req.data(), req.hora(), servico,
                agendamento.getId());

        // Libera os slots atuais antes de medir a disponibilidade: sem isso o proprio
        // agendamento apareceria como obstaculo ao ser remarcado para um horario vizinho.
        agendamento.liberarSlots();
        repository.saveAndFlush(agendamento);

        Long barbeiroDesejado = req.barbeiroId() != null ? req.barbeiroId() : agendamento.getBarbeiro().getId();
        Usuario barbeiro = escolherBarbeiroLivre(req.data(), req.hora(), servico, barbeiroDesejado);

        agendamento.setServico(servico);
        agendamento.setData(req.data());
        agendamento.setHoraInicio(req.hora());
        agendamento.setHoraFim(req.hora().plusMinutes(servico.getDuracaoMinutos()));
        agendamento.setPreco(servico.getPreco());
        agendamento.setBarbeiro(barbeiro);
        agendamento.ocuparSlots(disponibilidade.slotsDoAtendimento(req.hora(), servico));

        return salvarTratandoCorrida(agendamento);
    }

    @Transactional
    public Agendamento cancelar(Long id, Usuario solicitante) {
        Agendamento agendamento = buscarVisivel(id, solicitante);
        exigirStatusAgendado(agendamento);
        if (!solicitante.isEquipe()) {
            validarAntecedenciaParaAlterar(agendamento);
        }

        // Liberar os slots devolve o horario para a agenda; o agendamento fica no
        // historico com status CANCELADO.
        agendamento.liberarSlots();
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        return repository.save(agendamento);
    }

    @Transactional
    public Agendamento concluir(Long id) {
        Agendamento agendamento = buscar(id);
        exigirStatusAgendado(agendamento);
        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        return repository.save(agendamento);
    }

    @Transactional
    public Agendamento registrarNaoComparecimento(Long id) {
        Agendamento agendamento = buscar(id);
        exigirStatusAgendado(agendamento);
        agendamento.setStatus(StatusAgendamento.NAO_COMPARECEU);
        return repository.save(agendamento);
    }

    // --- Leitura -------------------------------------------------------------

    /**
     * O mapeamento para DTO acontece dentro da transacao de proposito: a aplicacao roda
     * com {@code open-in-view=false}, entao associacoes preguicosas precisam ser
     * resolvidas aqui, e nao no controller.
     */
    @Transactional(readOnly = true)
    public PaginaResposta<AgendamentoResponse> meusAgendamentos(Usuario cliente, Pageable pageable) {
        return PaginaResposta.de(
                repository.findByClienteIdOrderByDataDescHoraInicioDesc(cliente.getId(), pageable),
                AgendamentoResponse::de);
    }

    @Transactional(readOnly = true)
    public PaginaResposta<AgendamentoResponse> listar(LocalDate de, LocalDate ate, StatusAgendamento status,
                                                     Long barbeiroId, Pageable pageable) {
        Page<Agendamento> pagina = repository.findAll(AgendamentoSpecs.todos(
                AgendamentoSpecs.apartirDe(de),
                AgendamentoSpecs.ate(ate),
                AgendamentoSpecs.status(status),
                AgendamentoSpecs.barbeiro(barbeiroId)), pageable);
        return PaginaResposta.de(pagina, AgendamentoResponse::de);
    }

    @Transactional(readOnly = true)
    public Agendamento buscarVisivel(Long id, Usuario solicitante) {
        Agendamento agendamento = buscar(id);
        boolean dono = agendamento.getCliente().getId().equals(solicitante.getId());
        boolean barbeiroDoAtendimento = agendamento.getBarbeiro().getId().equals(solicitante.getId());
        if (!dono && !barbeiroDoAtendimento && !solicitante.isEquipe()) {
            // 404 em vez de 403: nao confirma a existencia do agendamento de outra pessoa.
            throw new RecursoNaoEncontradoException("Agendamento não encontrado.");
        }
        return agendamento;
    }

    // --- Regras --------------------------------------------------------------

    private Agendamento buscar(Long id) {
        return repository.buscarComRelacionamentos(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado."));
    }

    private Usuario resolverCliente(Usuario solicitante, String clienteEmail) {
        if (clienteEmail == null || clienteEmail.isBlank()) {
            return solicitante;
        }
        if (!solicitante.isEquipe()) {
            if (clienteEmail.trim().equalsIgnoreCase(solicitante.getEmail())) {
                return solicitante;
            }
            throw new RegraNegocioException("Você só pode agendar para a sua própria conta.");
        }
        return usuarioService.porEmail(clienteEmail.trim().toLowerCase());
    }

    private void validarLimiteDeReservasAtivas(Usuario cliente) {
        long ativos = repository.countByClienteIdAndStatus(cliente.getId(), StatusAgendamento.AGENDADO);
        if (ativos >= props.getMaxAgendamentosAtivosPorCliente()) {
            throw new ConflitoException(("Você já tem %d agendamentos em aberto, que é o limite. "
                    + "Cancele ou conclua um deles para marcar outro.")
                    .formatted(props.getMaxAgendamentosAtivosPorCliente()));
        }
    }

    private void validarConflitoDoProprioCliente(Usuario cliente, LocalDate data, LocalTime hora,
                                                 Servico servico, long idIgnorado) {
        LocalTime fim = hora.plusMinutes(servico.getDuracaoMinutos());
        if (!repository.conflitosDoCliente(cliente.getId(), data, hora, fim, idIgnorado).isEmpty()) {
            throw new ConflitoException("Você já tem um atendimento marcado que ocupa esse horário.");
        }
    }

    /**
     * Devolve um barbeiro sem sobreposicao no intervalo pedido.
     *
     * <p>Com {@code barbeiroId} informado, valida aquele barbeiro. Sem ele, pega o
     * primeiro livre — assim o cliente que nao tem preferencia nao precisa descobrir
     * sozinho quem esta disponivel.
     */
    private Usuario escolherBarbeiroLivre(LocalDate data, LocalTime hora, Servico servico, Long barbeiroId) {
        List<Usuario> candidatos = disponibilidade.barbeirosElegiveis(barbeiroId);
        if (candidatos.isEmpty()) {
            throw new ConflitoException("Nenhum barbeiro cadastrado para atender.");
        }

        Map<Long, Set<LocalTime>> ocupacao =
                disponibilidade.ocupacao(data, candidatos.stream().map(Usuario::getId).toList());

        return candidatos.stream()
                .filter(b -> disponibilidade.estaLivre(hora, servico, ocupacao.getOrDefault(b.getId(), Set.of())))
                .findFirst()
                .orElseThrow(() -> new ConflitoException(barbeiroId != null
                        ? "Este barbeiro já tem atendimento nesse horário. Escolha outro horário ou outro profissional."
                        : "Este horário acabou de ser ocupado. Escolha outro horário."));
    }

    private void exigirStatusAgendado(Agendamento agendamento) {
        if (!agendamento.getStatus().isAtivo()) {
            throw new ConflitoException("Este agendamento está %s e não pode mais ser alterado."
                    .formatted(agendamento.getStatus().name().toLowerCase().replace('_', ' ')));
        }
    }

    private void validarAntecedenciaParaAlterar(Agendamento agendamento) {
        LocalDateTime inicio = LocalDateTime.of(agendamento.getData(), agendamento.getHoraInicio());
        LocalDateTime limite = LocalDateTime.now(props.getFusoHorario())
                .plus(props.getAntecedenciaMinimaCancelamento());
        if (inicio.isBefore(limite)) {
            throw new ConflitoException(("Alterações são aceitas até %d horas antes do atendimento. "
                    + "Entre em contato com a barbearia.")
                    .formatted(props.getAntecedenciaMinimaCancelamento().toHours()));
        }
    }

    /**
     * Grava o agendamento com {@code flush} imediato para que uma eventual violacao da
     * restricao unica de slot aconteca aqui, e nao no fim da transacao. E a defesa contra
     * duas reservas simultaneas que passaram pela mesma checagem de disponibilidade.
     */
    private Agendamento salvarTratandoCorrida(Agendamento agendamento) {
        try {
            return repository.saveAndFlush(agendamento);
        } catch (DataIntegrityViolationException e) {
            throw new HorarioTomadoException(
                    "Este horário acabou de ser reservado por outro cliente. Escolha outro horário.");
        } catch (ConcurrencyFailureException e) {
            // O banco nao conseguiu serializar as duas gravacoes (deadlock ou timeout de
            // lock). Nada foi gravado, e repetir resolve.
            throw new HorarioTomadoException(
                    "Muitas reservas para este horário ao mesmo tempo. Tente novamente.");
        }
    }

    /**
     * Colisao de gravacao detectada pelo banco. Interna ao servico: nunca chega ao
     * controller, porque {@link #comRetentativa} a converte em nova tentativa ou em
     * {@link ConflitoException}.
     */
    private static class HorarioTomadoException extends RuntimeException {
        HorarioTomadoException(String mensagem) {
            super(mensagem);
        }
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
