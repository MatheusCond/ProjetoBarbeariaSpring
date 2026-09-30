package br.com.projetofatec.barbeariaconde.service;

import br.com.projetofatec.barbeariaconde.dto.agenda.AgendamentoRequest;
import br.com.projetofatec.barbeariaconde.dto.agenda.DisponibilidadeResponse;
import br.com.projetofatec.barbeariaconde.dto.agenda.ReagendarRequest;
import br.com.projetofatec.barbeariaconde.exception.ConflitoException;
import br.com.projetofatec.barbeariaconde.exception.RegraNegocioException;
import br.com.projetofatec.barbeariaconde.model.Agendamento;
import br.com.projetofatec.barbeariaconde.model.Servico;
import br.com.projetofatec.barbeariaconde.model.StatusAgendamento;
import br.com.projetofatec.barbeariaconde.model.Usuario;
import br.com.projetofatec.barbeariaconde.suporte.BaseDeIntegracao;
import br.com.projetofatec.barbeariaconde.suporte.DatasDeTeste;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regras de agenda: janela de atendimento, sobreposicao, limites e ciclo de vida. */
class AgendamentoRegrasTest extends BaseDeIntegracao {

    @Autowired
    private AgendamentoService agendamentoService;
    @Autowired
    private DisponibilidadeService disponibilidadeService;

    private AgendamentoRequest pedido(LocalDate data, LocalTime hora, Servico servico, Long barbeiroId) {
        return new AgendamentoRequest(servico, data, hora, barbeiroId, null, null);
    }

    @Test
    @DisplayName("reserva válida ocupa os slots correspondentes à duração do serviço")
    void reservaValidaOcupaSlots() {
        Usuario barbeiro = criarBarbeiro("Tiago Conde");
        Usuario cliente = criarCliente();
        LocalDate data = DatasDeTeste.proximaQuarta();

        Agendamento agendamento = agendamentoService.criar(cliente,
                pedido(data, LocalTime.of(10, 0), Servico.CABELO_E_BARBA, barbeiro.getId()));

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
        assertThat(agendamento.getHoraFim()).isEqualTo(LocalTime.of(11, 0));
        // 60 minutos = dois slots de 30
        assertThat(slotRepository.findByBarbeiroIdAndData(barbeiro.getId(), data))
                .extracting("hora")
                .containsExactlyInAnyOrder(LocalTime.of(10, 0), LocalTime.of(10, 30));
    }

    @Test
    @DisplayName("segundo cliente não consegue o mesmo horário do mesmo barbeiro")
    void naoPermiteMesmoHorarioDoMesmoBarbeiro() {
        Usuario barbeiro = criarBarbeiro("Tiago Conde");
        LocalDate data = DatasDeTeste.proximaQuarta();

        agendamentoService.criar(criarCliente(), pedido(data, LocalTime.of(14, 0), Servico.CABELO, barbeiro.getId()));

        Usuario outro = criarCliente();
        assertThatThrownBy(() -> agendamentoService.criar(outro,
                pedido(data, LocalTime.of(14, 0), Servico.CABELO, barbeiro.getId())))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    @DisplayName("serviço longo bloqueia o slot seguinte, não só o horário de início")
    void servicoLongoBloqueiaSlotSeguinte() {
        Usuario barbeiro = criarBarbeiro("Tiago Conde");
        LocalDate data = DatasDeTeste.proximaQuarta();

        // 10:00 as 11:00
        agendamentoService.criar(criarCliente(),
                pedido(data, LocalTime.of(10, 0), Servico.CABELO_E_BARBA, barbeiro.getId()));

        Usuario outro = criarCliente();
        // 10:30 comecaria no meio do atendimento anterior
        assertThatThrownBy(() -> agendamentoService.criar(outro,
                pedido(data, LocalTime.of(10, 30), Servico.CABELO, barbeiro.getId())))
                .isInstanceOf(ConflitoException.class);

        // 11:00 esta livre
        assertThat(agendamentoService.criar(outro,
                pedido(data, LocalTime.of(11, 0), Servico.CABELO, barbeiro.getId())).getId()).isNotNull();
    }

    @Test
    @DisplayName("sem barbeiro informado, o sistema escolhe outro profissional livre")
    void escolheBarbeiroLivreAutomaticamente() {
        Usuario primeiro = criarBarbeiro("Tiago Conde");
        Usuario segundo = criarBarbeiro("Rafael Souza");
        LocalDate data = DatasDeTeste.proximaQuarta();

        Agendamento a = agendamentoService.criar(criarCliente(),
                pedido(data, LocalTime.of(15, 0), Servico.CABELO, null));
        Agendamento b = agendamentoService.criar(criarCliente(),
                pedido(data, LocalTime.of(15, 0), Servico.CABELO, null));

        assertThat(a.getBarbeiro().getId()).isEqualTo(primeiro.getId());
        assertThat(b.getBarbeiro().getId()).isEqualTo(segundo.getId());

        // Esgotada a equipe, o terceiro pedido no mesmo horario e recusado.
        Usuario terceiro = criarCliente();
        assertThatThrownBy(() -> agendamentoService.criar(terceiro,
                pedido(data, LocalTime.of(15, 0), Servico.CABELO, null)))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    @DisplayName("mesmo cliente não pode ocupar o mesmo horário com dois barbeiros")
    void clienteNaoSeDuplicaNoMesmoHorario() {
        criarBarbeiro("Tiago Conde");
        criarBarbeiro("Rafael Souza");
        Usuario cliente = criarCliente();
        LocalDate data = DatasDeTeste.proximaQuarta();

        agendamentoService.criar(cliente, pedido(data, LocalTime.of(16, 0), Servico.CABELO, null));

        assertThatThrownBy(() -> agendamentoService.criar(cliente,
                pedido(data, LocalTime.of(16, 0), Servico.BARBA, null)))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("já tem um atendimento");
    }

    @Test
    @DisplayName("rejeita data passada, dia fechado, horário fora da grade e intervalo da equipe")
    void rejeitaHorariosInvalidos() {
        Usuario barbeiro = criarBarbeiro("Tiago Conde");
        Usuario cliente = criarCliente();
        LocalDate quarta = DatasDeTeste.proximaQuarta();

        assertThatThrownBy(() -> agendamentoService.criar(cliente,
                pedido(DatasDeTeste.hoje().minusDays(1), LocalTime.of(10, 0), Servico.CABELO, barbeiro.getId())))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já passou");

        assertThatThrownBy(() -> agendamentoService.criar(cliente,
                pedido(DatasDeTeste.proximaSegunda(), LocalTime.of(10, 0), Servico.CABELO, barbeiro.getId())))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não abre");

        assertThatThrownBy(() -> agendamentoService.criar(cliente,
                pedido(quarta, LocalTime.of(10, 17), Servico.CABELO, barbeiro.getId())))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("de 30 em 30");

        assertThatThrownBy(() -> agendamentoService.criar(cliente,
                pedido(quarta, LocalTime.of(7, 0), Servico.CABELO, barbeiro.getId())))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Fora do horário");

        // 18:30 + 60 min passaria das 19:00
        assertThatThrownBy(() -> agendamentoService.criar(cliente,
                pedido(quarta, LocalTime.of(18, 30), Servico.CABELO_E_BARBA, barbeiro.getId())))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Fora do horário");

        assertThatThrownBy(() -> agendamentoService.criar(cliente,
                pedido(quarta, LocalTime.of(12, 0), Servico.CABELO, barbeiro.getId())))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("intervalo da equipe");

        assertThatThrownBy(() -> agendamentoService.criar(cliente,
                pedido(DatasDeTeste.hoje().plusDays(400), LocalTime.of(10, 0), Servico.CABELO, barbeiro.getId())))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("antecedência");
    }

    @Test
    @DisplayName("limite de reservas ativas por cliente é respeitado")
    void respeitaLimiteDeReservasAtivas() {
        Usuario barbeiro = criarBarbeiro("Tiago Conde");
        Usuario cliente = criarCliente();
        LocalDate data = DatasDeTeste.proximaQuarta();

        agendamentoService.criar(cliente, pedido(data, LocalTime.of(9, 0), Servico.CABELO, barbeiro.getId()));
        agendamentoService.criar(cliente, pedido(data, LocalTime.of(9, 30), Servico.CABELO, barbeiro.getId()));
        agendamentoService.criar(cliente, pedido(data, LocalTime.of(10, 0), Servico.CABELO, barbeiro.getId()));

        assertThatThrownBy(() -> agendamentoService.criar(cliente,
                pedido(data, LocalTime.of(10, 30), Servico.CABELO, barbeiro.getId())))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("limite");
    }

    @Test
    @DisplayName("cancelar devolve o horário para a agenda e mantém o histórico")
    void cancelarLiberaHorario() {
        Usuario barbeiro = criarBarbeiro("Tiago Conde");
        Usuario cliente = criarCliente();
        LocalDate data = DatasDeTeste.proximaQuarta();

        Agendamento agendamento = agendamentoService.criar(cliente,
                pedido(data, LocalTime.of(17, 0), Servico.CABELO, barbeiro.getId()));

        Agendamento cancelado = agendamentoService.cancelar(agendamento.getId(), cliente);

        assertThat(cancelado.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        assertThat(slotRepository.findByBarbeiroIdAndData(barbeiro.getId(), data)).isEmpty();
        assertThat(agendamentoRepository.findById(agendamento.getId())).isPresent();

        // O horario volta a ser oferecido e pode ser reservado por outra pessoa.
        assertThat(agendamentoService.criar(criarCliente(),
                pedido(data, LocalTime.of(17, 0), Servico.CABELO, barbeiro.getId())).getId()).isNotNull();
    }

    @Test
    @DisplayName("cancelar duas vezes é recusado")
    void naoCancelaDuasVezes() {
        Usuario barbeiro = criarBarbeiro("Tiago Conde");
        Usuario cliente = criarCliente();
        Agendamento agendamento = agendamentoService.criar(cliente,
                pedido(DatasDeTeste.proximaQuarta(), LocalTime.of(17, 30), Servico.CABELO, barbeiro.getId()));

        agendamentoService.cancelar(agendamento.getId(), cliente);

        assertThatThrownBy(() -> agendamentoService.cancelar(agendamento.getId(), cliente))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    @DisplayName("reagendar move apenas o agendamento indicado e libera o horário antigo")
    void reagendarMoveApenasUm() {
        Usuario barbeiro = criarBarbeiro("Tiago Conde");
        Usuario cliente = criarCliente();
        LocalDate data = DatasDeTeste.proximaQuarta();

        Agendamento primeiro = agendamentoService.criar(cliente,
                pedido(data, LocalTime.of(9, 0), Servico.CABELO, barbeiro.getId()));
        Agendamento segundo = agendamentoService.criar(cliente,
                pedido(data, LocalTime.of(9, 30), Servico.CABELO, barbeiro.getId()));

        Agendamento remarcado = agendamentoService.reagendar(primeiro.getId(), cliente,
                new ReagendarRequest(Servico.CABELO, data, LocalTime.of(15, 0), barbeiro.getId()));

        assertThat(remarcado.getHoraInicio()).isEqualTo(LocalTime.of(15, 0));
        // O outro agendamento do mesmo cliente nao foi tocado: era exatamente o bug do PUT antigo.
        assertThat(agendamentoRepository.findById(segundo.getId()).orElseThrow().getHoraInicio())
                .isEqualTo(LocalTime.of(9, 30));
        assertThat(slotRepository.findByBarbeiroIdAndData(barbeiro.getId(), data))
                .extracting("hora")
                .containsExactlyInAnyOrder(LocalTime.of(9, 30), LocalTime.of(15, 0));
    }

    @Test
    @DisplayName("reagendar para horário vizinho não conflita com o próprio agendamento")
    void reagendarParaHorarioVizinho() {
        Usuario barbeiro = criarBarbeiro("Tiago Conde");
        Usuario cliente = criarCliente();
        LocalDate data = DatasDeTeste.proximaQuarta();

        Agendamento agendamento = agendamentoService.criar(cliente,
                pedido(data, LocalTime.of(10, 0), Servico.CABELO, barbeiro.getId()));

        Agendamento remarcado = agendamentoService.reagendar(agendamento.getId(), cliente,
                new ReagendarRequest(Servico.CABELO_E_BARBA, data, LocalTime.of(10, 0), barbeiro.getId()));

        assertThat(remarcado.getHoraFim()).isEqualTo(LocalTime.of(11, 0));
        assertThat(slotRepository.findByBarbeiroIdAndData(barbeiro.getId(), data)).hasSize(2);
    }

    @Test
    @DisplayName("disponibilidade não oferece horário ocupado nem dia fechado")
    void disponibilidadeRefleteAgenda() {
        Usuario barbeiro = criarBarbeiro("Tiago Conde");
        LocalDate data = DatasDeTeste.proximaQuarta();

        DisponibilidadeResponse antes = disponibilidadeService.consultar(data, Servico.CABELO, barbeiro.getId());
        assertThat(antes.aberto()).isTrue();
        assertThat(antes.horarios()).contains(LocalTime.of(14, 0));
        // A pausa das 12:00 as 13:00 nao aparece na grade.
        assertThat(antes.horarios()).doesNotContain(LocalTime.of(12, 0), LocalTime.of(12, 30));

        agendamentoService.criar(criarCliente(), pedido(data, LocalTime.of(14, 0), Servico.CABELO, barbeiro.getId()));

        DisponibilidadeResponse depois = disponibilidadeService.consultar(data, Servico.CABELO, barbeiro.getId());
        assertThat(depois.horarios()).doesNotContain(LocalTime.of(14, 0));

        DisponibilidadeResponse fechado =
                disponibilidadeService.consultar(DatasDeTeste.proximaSegunda(), Servico.CABELO, barbeiro.getId());
        assertThat(fechado.aberto()).isFalse();
        assertThat(fechado.horarios()).isEmpty();
    }

    @Test
    @DisplayName("serviço de 60 minutos não é oferecido quando só resta um slot livre")
    void disponibilidadeConsideraDuracao() {
        Usuario barbeiro = criarBarbeiro("Tiago Conde");
        LocalDate data = DatasDeTeste.proximaQuarta();

        // Ocupa 11:00, deixando somente 10:30 livre entre 10:00 (livre) e 11:00.
        agendamentoService.criar(criarCliente(), pedido(data, LocalTime.of(11, 0), Servico.CABELO, barbeiro.getId()));

        DisponibilidadeResponse curto =
                disponibilidadeService.consultar(data, Servico.CABELO, barbeiro.getId());
        DisponibilidadeResponse longo =
                disponibilidadeService.consultar(data, Servico.CABELO_E_BARBA, barbeiro.getId());

        assertThat(curto.horarios()).contains(LocalTime.of(10, 30));
        assertThat(longo.horarios()).doesNotContain(LocalTime.of(10, 30));
    }

    @Test
    @DisplayName("cliente não consegue ver nem cancelar agendamento de outra pessoa")
    void isolamentoEntreClientes() {
        Usuario barbeiro = criarBarbeiro("Tiago Conde");
        Usuario dono = criarCliente();
        Usuario intruso = criarCliente();

        Agendamento agendamento = agendamentoService.criar(dono,
                pedido(DatasDeTeste.proximaQuarta(), LocalTime.of(16, 30), Servico.CABELO, barbeiro.getId()));

        assertThatThrownBy(() -> agendamentoService.cancelar(agendamento.getId(), intruso))
                .isInstanceOf(br.com.projetofatec.barbeariaconde.exception.RecursoNaoEncontradoException.class);

        // A equipe, por outro lado, tem acesso.
        Usuario admin = criarAdmin();
        assertThat(agendamentoService.buscarVisivel(agendamento.getId(), admin).getId())
                .isEqualTo(agendamento.getId());
    }

    @Test
    @DisplayName("cliente não pode agendar em nome de outra pessoa; a equipe pode")
    void agendamentoEmNomeDeOutro() {
        Usuario barbeiro = criarBarbeiro("Tiago Conde");
        Usuario cliente = criarCliente("dono@exemplo.com");
        Usuario intruso = criarCliente("intruso@exemplo.com");
        LocalDate data = DatasDeTeste.proximaQuarta();

        AgendamentoRequest emNomeDeOutro = new AgendamentoRequest(
                Servico.CABELO, data, LocalTime.of(13, 0), barbeiro.getId(), cliente.getEmail(), null);

        assertThatThrownBy(() -> agendamentoService.criar(intruso, emNomeDeOutro))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("própria conta");

        Usuario admin = criarAdmin();
        Agendamento pelaEquipe = agendamentoService.criar(admin, emNomeDeOutro);
        assertThat(pelaEquipe.getCliente().getId()).isEqualTo(cliente.getId());
    }

    @Test
    @DisplayName("concluir encerra o atendimento e bloqueia alterações posteriores")
    void concluirEncerraAtendimento() {
        Usuario barbeiro = criarBarbeiro("Tiago Conde");
        Usuario cliente = criarCliente();
        Agendamento agendamento = agendamentoService.criar(cliente,
                pedido(DatasDeTeste.proximaQuarta(), LocalTime.of(11, 30), Servico.CABELO, barbeiro.getId()));

        assertThat(agendamentoService.concluir(agendamento.getId()).getStatus())
                .isEqualTo(StatusAgendamento.CONCLUIDO);

        assertThatThrownBy(() -> agendamentoService.cancelar(agendamento.getId(), cliente))
                .isInstanceOf(ConflitoException.class);
    }
}
