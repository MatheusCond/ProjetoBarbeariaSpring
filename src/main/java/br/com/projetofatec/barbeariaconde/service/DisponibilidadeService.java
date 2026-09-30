package br.com.projetofatec.barbeariaconde.service;

import br.com.projetofatec.barbeariaconde.config.AgendaProperties;
import br.com.projetofatec.barbeariaconde.dto.agenda.DisponibilidadeResponse;
import br.com.projetofatec.barbeariaconde.exception.RegraNegocioException;
import br.com.projetofatec.barbeariaconde.model.Servico;
import br.com.projetofatec.barbeariaconde.model.Usuario;
import br.com.projetofatec.barbeariaconde.repository.AgendamentoSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Calcula a grade de horarios da barbearia e responde o que esta livre.
 *
 * <p>Toda a aritmetica e feita em minutos desde a meia-noite para evitar erros de
 * comparacao de {@link LocalTime} quando um atendimento se aproxima do fim do dia.
 *
 * <p>Um atendimento ocupa {@code ceil(duracao / duracaoSlot)} fatias consecutivas: um
 * corte de 30 min ocupa uma, cabelo + barba de 60 min ocupa duas. Um horario so aparece
 * como disponivel se TODAS as fatias necessarias estiverem livres e couberem antes do
 * fechamento, o que e a razao pela qual dois clientes nao conseguem mais se sobrepor.
 */
@Service
public class DisponibilidadeService {

    private final AgendaProperties props;
    private final AgendamentoSlotRepository slotRepository;
    private final UsuarioService usuarioService;

    public DisponibilidadeService(AgendaProperties props,
                                  AgendamentoSlotRepository slotRepository,
                                  UsuarioService usuarioService) {
        this.props = props;
        this.slotRepository = slotRepository;
        this.usuarioService = usuarioService;
    }

    // --- Consulta publica ----------------------------------------------------

    @Transactional(readOnly = true)
    public DisponibilidadeResponse consultar(LocalDate data, Servico servico, Long barbeiroId) {
        if (data == null) {
            throw new RegraNegocioException("Informe a data da consulta.");
        }
        int duracao = servico.getDuracaoMinutos();

        if (data.isBefore(hoje())) {
            return DisponibilidadeResponse.fechado(data, servico.name(), duracao,
                    "Data já passou.");
        }
        if (data.isAfter(hoje().plusDays(props.getJanelaMaximaDias()))) {
            return DisponibilidadeResponse.fechado(data, servico.name(), duracao,
                    "A agenda abre com até " + props.getJanelaMaximaDias() + " dias de antecedência.");
        }

        AgendaProperties.Expediente expediente = props.expedienteDe(data.getDayOfWeek());
        if (expediente == null) {
            return DisponibilidadeResponse.fechado(data, servico.name(), duracao,
                    "A barbearia não abre neste dia da semana.");
        }

        List<Usuario> barbeiros = barbeirosElegiveis(barbeiroId);
        if (barbeiros.isEmpty()) {
            return DisponibilidadeResponse.fechado(data, servico.name(), duracao,
                    "Nenhum barbeiro disponível nesta data.");
        }

        Map<Long, Set<LocalTime>> ocupacao = ocupacao(data, barbeiros.stream().map(Usuario::getId).toList());
        List<LocalTime> gradeBase = gradeDeInicios(expediente, servico, data);

        List<DisponibilidadeResponse.BarbeiroDisponibilidade> porBarbeiro = new ArrayList<>();
        Set<LocalTime> uniao = new TreeSet<>();

        for (Usuario barbeiro : barbeiros) {
            Set<LocalTime> ocupados = ocupacao.getOrDefault(barbeiro.getId(), Set.of());
            List<LocalTime> livres = gradeBase.stream()
                    .filter(inicio -> estaLivre(inicio, servico, ocupados))
                    .toList();
            porBarbeiro.add(new DisponibilidadeResponse.BarbeiroDisponibilidade(
                    barbeiro.getId(), barbeiro.getNome(), livres));
            uniao.addAll(livres);
        }

        return new DisponibilidadeResponse(data, servico.name(), duracao, true, null,
                List.copyOf(uniao), porBarbeiro);
    }

    // --- Regras reaproveitadas pelo agendamento ------------------------------

    /**
     * Verifica se o par data/hora e aceitavel para o servico, independente de quem
     * esta livre: dia de funcionamento, grade, expediente, pausa, antecedencia e janela.
     *
     * @throws RegraNegocioException com mensagem pronta para exibir ao usuario
     */
    public void validarJanelaDeAtendimento(LocalDate data, LocalTime hora, Servico servico) {
        LocalDate hoje = hoje();

        if (data.isBefore(hoje)) {
            throw new RegraNegocioException("Não é possível agendar em uma data que já passou.");
        }
        if (data.isAfter(hoje.plusDays(props.getJanelaMaximaDias()))) {
            throw new RegraNegocioException(
                    "A agenda aceita reservas com no máximo " + props.getJanelaMaximaDias() + " dias de antecedência.");
        }

        AgendaProperties.Expediente expediente = props.expedienteDe(data.getDayOfWeek());
        if (expediente == null) {
            throw new RegraNegocioException("A barbearia não abre em " + diaEmPortugues(data) + ".");
        }

        int abertura = minutos(expediente.getAbertura());
        int fechamento = minutos(expediente.getFechamento());
        int inicio = minutos(hora);
        int fim = inicio + servico.getDuracaoMinutos();

        if (inicio < abertura || fim > fechamento) {
            throw new RegraNegocioException(("Fora do horário de funcionamento (%s às %s). "
                    + "O serviço escolhido leva %d minutos.")
                    .formatted(expediente.getAbertura(), expediente.getFechamento(), servico.getDuracaoMinutos()));
        }
        if ((inicio - abertura) % props.getDuracaoSlotMinutos() != 0) {
            throw new RegraNegocioException("Os horários são de %d em %d minutos a partir de %s."
                    .formatted(props.getDuracaoSlotMinutos(), props.getDuracaoSlotMinutos(),
                            expediente.getAbertura()));
        }
        if (expediente.colideComPausa(hora, hora.plusMinutes(servico.getDuracaoMinutos()))) {
            throw new RegraNegocioException("Este horário cai no intervalo da equipe (%s às %s)."
                    .formatted(expediente.getPausaInicio(), expediente.getPausaFim()));
        }
        if (LocalDateTime.of(data, hora).isBefore(agora().plus(props.getAntecedenciaMinima()))) {
            throw new RegraNegocioException("Reserve com pelo menos %d minutos de antecedência."
                    .formatted(props.getAntecedenciaMinima().toMinutes()));
        }
    }

    /** Fatias da agenda que o atendimento ocupa a partir de {@code inicio}. */
    public List<LocalTime> slotsDoAtendimento(LocalTime inicio, Servico servico) {
        int quantidade = slotsNecessarios(servico);
        List<LocalTime> slots = new ArrayList<>(quantidade);
        for (int i = 0; i < quantidade; i++) {
            slots.add(inicio.plusMinutes((long) i * props.getDuracaoSlotMinutos()));
        }
        return slots;
    }

    /** Ocupacao atual do dia, por barbeiro, em uma unica consulta. */
    @Transactional(readOnly = true)
    public Map<Long, Set<LocalTime>> ocupacao(LocalDate data, Collection<Long> barbeiroIds) {
        if (barbeiroIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Set<LocalTime>> mapa = new HashMap<>();
        for (Object[] linha : slotRepository.ocupacaoDoDia(data, barbeiroIds)) {
            Long barbeiroId = (Long) linha[0];
            LocalTime hora = (LocalTime) linha[1];
            mapa.computeIfAbsent(barbeiroId, k -> new HashSet<>()).add(hora);
        }
        return mapa;
    }

    public boolean estaLivre(LocalTime inicio, Servico servico, Set<LocalTime> ocupados) {
        return slotsDoAtendimento(inicio, servico).stream().noneMatch(ocupados::contains);
    }

    /** Barbeiros candidatos: o informado, ou todos os ativos, sempre em ordem estavel. */
    public List<Usuario> barbeirosElegiveis(Long barbeiroId) {
        if (barbeiroId != null) {
            return List.of(usuarioService.barbeiroAtivo(barbeiroId));
        }
        return usuarioService.barbeirosAtivos().stream()
                .sorted(Comparator.comparing(Usuario::getId))
                .toList();
    }

    // --- Apoio ---------------------------------------------------------------

    private List<LocalTime> gradeDeInicios(AgendaProperties.Expediente expediente, Servico servico, LocalDate data) {
        int passo = props.getDuracaoSlotMinutos();
        int abertura = minutos(expediente.getAbertura());
        int fechamento = minutos(expediente.getFechamento());
        int duracao = servico.getDuracaoMinutos();
        LocalDateTime limiteMinimo = agora().plus(props.getAntecedenciaMinima());

        List<LocalTime> grade = new ArrayList<>();
        for (int inicio = abertura; inicio + duracao <= fechamento; inicio += passo) {
            LocalTime hora = LocalTime.ofSecondOfDay(inicio * 60L);
            if (expediente.colideComPausa(hora, hora.plusMinutes(duracao))) {
                continue;
            }
            if (LocalDateTime.of(data, hora).isBefore(limiteMinimo)) {
                continue;
            }
            grade.add(hora);
        }
        return grade;
    }

    private int slotsNecessarios(Servico servico) {
        int passo = props.getDuracaoSlotMinutos();
        return (servico.getDuracaoMinutos() + passo - 1) / passo;
    }

    private static int minutos(LocalTime hora) {
        return hora.getHour() * 60 + hora.getMinute();
    }

    private LocalDate hoje() {
        return LocalDate.now(props.getFusoHorario());
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(props.getFusoHorario());
    }

    private static String diaEmPortugues(LocalDate data) {
        return switch (data.getDayOfWeek()) {
            case MONDAY -> "segunda-feira";
            case TUESDAY -> "terça-feira";
            case WEDNESDAY -> "quarta-feira";
            case THURSDAY -> "quinta-feira";
            case FRIDAY -> "sexta-feira";
            case SATURDAY -> "sábado";
            case SUNDAY -> "domingo";
        };
    }
}
