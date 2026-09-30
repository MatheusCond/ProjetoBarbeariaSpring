package br.com.projetofatec.barbeariaconde.service;

import br.com.projetofatec.barbeariaconde.dto.agenda.AgendamentoRequest;
import br.com.projetofatec.barbeariaconde.model.Servico;
import br.com.projetofatec.barbeariaconde.model.Usuario;
import br.com.projetofatec.barbeariaconde.suporte.BaseDeIntegracao;
import br.com.projetofatec.barbeariaconde.suporte.DatasDeTeste;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O cenario que o projeto original nao tratava: varias pessoas disputando o mesmo horario
 * ao mesmo tempo.
 *
 * <p>Checar disponibilidade e depois gravar nao basta, porque duas requisicoes podem ler
 * "livre" antes de qualquer uma gravar. A garantia vem da restricao unica de
 * {@code agendamento_slots}: o banco aceita apenas a primeira gravacao.
 */
class AgendamentoConcorrenciaTest extends BaseDeIntegracao {

    private static final int CONCORRENTES = 8;

    @Autowired
    private AgendamentoService agendamentoService;

    @Test
    @DisplayName("com 8 clientes disputando o mesmo horário, apenas 1 reserva é gravada")
    void apenasUmaReservaVenceADisputa() throws Exception {
        Usuario barbeiro = criarBarbeiro("Tiago Conde");
        LocalDate data = DatasDeTeste.proximaQuarta();
        LocalTime hora = LocalTime.of(10, 0);

        List<Usuario> clientes = new java.util.ArrayList<>();
        for (int i = 0; i < CONCORRENTES; i++) {
            clientes.add(criarCliente("disputa" + i + "@exemplo.com"));
        }

        AtomicInteger sucessos = new AtomicInteger();
        AtomicInteger recusas = new AtomicInteger();
        CountDownLatch largada = new CountDownLatch(1);
        CountDownLatch chegada = new CountDownLatch(CONCORRENTES);

        try (ExecutorService pool = Executors.newFixedThreadPool(CONCORRENTES)) {
            for (Usuario cliente : clientes) {
                pool.submit(() -> {
                    try {
                        largada.await();
                        agendamentoService.criar(cliente, new AgendamentoRequest(
                                Servico.CABELO, data, hora, barbeiro.getId(), null, null));
                        sucessos.incrementAndGet();
                    } catch (Exception e) {
                        recusas.incrementAndGet();
                    } finally {
                        chegada.countDown();
                    }
                });
            }

            largada.countDown();
            assertThat(chegada.await(30, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(sucessos.get()).isEqualTo(1);
        assertThat(recusas.get()).isEqualTo(CONCORRENTES - 1);

        // A verificacao que importa: a agenda ficou consistente no banco.
        assertThat(slotRepository.findByBarbeiroIdAndData(barbeiro.getId(), data)).hasSize(1);
        assertThat(agendamentoRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("com 2 barbeiros e 8 clientes no mesmo horário, exatamente 2 reservas passam")
    void capacidadeLimitadaPelaEquipe() throws Exception {
        criarBarbeiro("Tiago Conde");
        criarBarbeiro("Rafael Souza");
        LocalDate data = DatasDeTeste.proximaQuarta();
        LocalTime hora = LocalTime.of(14, 0);

        List<Usuario> clientes = new java.util.ArrayList<>();
        for (int i = 0; i < CONCORRENTES; i++) {
            clientes.add(criarCliente("fila" + i + "@exemplo.com"));
        }

        AtomicInteger sucessos = new AtomicInteger();
        CountDownLatch largada = new CountDownLatch(1);
        CountDownLatch chegada = new CountDownLatch(CONCORRENTES);

        try (ExecutorService pool = Executors.newFixedThreadPool(CONCORRENTES)) {
            for (Usuario cliente : clientes) {
                pool.submit(() -> {
                    try {
                        largada.await();
                        // Sem barbeiro definido: o servico escolhe quem estiver livre.
                        agendamentoService.criar(cliente, new AgendamentoRequest(
                                Servico.CABELO, data, hora, null, null, null));
                        sucessos.incrementAndGet();
                    } catch (Exception ignorado) {
                        // Conflito esperado para quem chegou depois.
                    } finally {
                        chegada.countDown();
                    }
                });
            }

            largada.countDown();
            assertThat(chegada.await(30, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(sucessos.get()).isEqualTo(2);
        assertThat(agendamentoRepository.count()).isEqualTo(2);
        assertThat(slotRepository.count()).isEqualTo(2);
    }
}
