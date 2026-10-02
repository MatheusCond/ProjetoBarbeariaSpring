/*
 * Tela de agendamento.
 *
 * A diferença central em relação à versão original: o horário não é digitado. A
 * página pergunta ao backend quais horários realmente cabem na agenda (GET
 * /api/agendamentos/disponibilidade) e oferece só esses. O cliente não descobre o
 * conflito depois de enviar — e o e-mail do dono da reserva não é mais um campo do
 * formulário, porque vem do token.
 */
const estado = {
    servicos: [],
    servicoSelecionado: null,
    disponibilidade: null,
    reagendandoId: null,
    // Trocar serviço, data e profissional em sequência dispara consultas simultâneas.
    // Sem este contador, uma resposta atrasada pode sobrescrever a mais recente e a
    // grade passa a mostrar horários de outro serviço.
    consultaEmCurso: 0
};

document.addEventListener('DOMContentLoaded', async () => {
    document.getElementById('ano').textContent = new Date().getFullYear();

    const hoje = new Date().toISOString().slice(0, 10);
    const campoData = document.getElementById('data');
    campoData.value = hoje;
    campoData.min = hoje;

    campoData.addEventListener('change', carregarDisponibilidade);
    document.getElementById('barbeiro').addEventListener('change', carregarDisponibilidade);
    document.getElementById('cancelarReagendamento')
        .addEventListener('click', encerrarReagendamento);

    await Promise.all([carregarServicos(), carregarBarbeiros()]);
});

// A sessão é resolvida pelo nav.js; esperamos por ela para saber o que exibir.
document.addEventListener('sessao-pronta', (evento) => {
    const autenticado = Boolean(evento.detail);
    document.getElementById('avisoLogin').hidden = autenticado;
    document.getElementById('secaoMeusAgendamentos').hidden = !autenticado;
    if (autenticado) {
        carregarMeusAgendamentos();
    }
});

async function carregarServicos() {
    const lista = document.getElementById('servicos');
    estado.servicos = await API.servicos();
    lista.innerHTML = '';

    estado.servicos.forEach((servico) => {
        const cartao = UI.cartaoServico(servico, { selecionavel: true });
        cartao.addEventListener('click', () => selecionarServico(servico.codigo));
        lista.appendChild(cartao);
    });

    selecionarServico(estado.servicos[0].codigo);
}

async function carregarBarbeiros() {
    const select = document.getElementById('barbeiro');
    const barbeiros = await API.barbeiros();
    select.innerHTML = '<option value="">Qualquer profissional disponível</option>';
    barbeiros.forEach((barbeiro) => {
        const opcao = document.createElement('option');
        opcao.value = barbeiro.id;
        opcao.textContent = barbeiro.nome;
        select.appendChild(opcao);
    });
}

function selecionarServico(codigo) {
    estado.servicoSelecionado = codigo;
    document.querySelectorAll('.card-servico').forEach((card) => {
        const ativo = card.dataset.codigo === codigo;
        card.classList.toggle('selecionado', ativo);
        card.setAttribute('aria-pressed', String(ativo));
    });
    carregarDisponibilidade();
}

function servicoAtual() {
    return estado.servicos.find((s) => s.codigo === estado.servicoSelecionado);
}

async function carregarDisponibilidade() {
    if (!estado.servicoSelecionado) {
        return;
    }
    const data = document.getElementById('data').value;
    const barbeiroId = document.getElementById('barbeiro').value || null;
    const painel = document.getElementById('horarios');

    atualizarResumo(data, barbeiroId);

    if (!data) {
        painel.innerHTML = UI.estadoVazio('Escolha uma data para ver os horários.');
        return;
    }

    const consulta = ++estado.consultaEmCurso;
    painel.innerHTML = UI.estadoVazio('Carregando horários...');

    try {
        const resposta = await API.disponibilidade(data, estado.servicoSelecionado, barbeiroId);
        if (consulta !== estado.consultaEmCurso) {
            return; // Resposta de uma seleção que o usuário já trocou.
        }
        estado.disponibilidade = resposta;
        renderizarHorarios(resposta);
    } catch (e) {
        if (consulta === estado.consultaEmCurso) {
            painel.innerHTML = UI.estadoVazio(e.message, true);
        }
    }
}

function atualizarResumo(data, barbeiroId) {
    const servico = servicoAtual();
    const select = document.getElementById('barbeiro');
    const profissional = barbeiroId
        ? select.options[select.selectedIndex].textContent
        : 'qualquer profissional';

    document.getElementById('resumoEscolha').textContent = servico && data
        ? `${servico.nome} · ${servico.duracaoMinutos} min · ${UI.data(data)} · ${profissional}`
        : '';
}

function renderizarHorarios(disponibilidade) {
    const painel = document.getElementById('horarios');
    painel.innerHTML = '';

    if (!disponibilidade.aberto) {
        painel.innerHTML = UI.estadoVazio(disponibilidade.motivoFechado);
        return;
    }
    if (disponibilidade.horarios.length === 0) {
        painel.innerHTML = UI.estadoVazio(
            'Nenhum horário livre nesta data. Tente outro dia ou outro profissional.');
        return;
    }

    disponibilidade.horarios.forEach((hora) => {
        const botao = document.createElement('button');
        botao.type = 'button';
        botao.className = 'slot';
        botao.textContent = UI.hora(hora);
        botao.addEventListener('click', () => confirmar(hora));
        painel.appendChild(botao);
    });
}

async function confirmar(hora) {
    if (!API.estaAutenticado()) {
        window.location.href = 'login.html';
        return;
    }

    const servico = servicoAtual();
    const data = document.getElementById('data').value;
    const barbeiroId = document.getElementById('barbeiro').value || null;

    const confirmado = await UI.confirmar({
        titulo: estado.reagendandoId ? 'Remarcar atendimento' : 'Confirmar agendamento',
        mensagem: `<strong>${servico.nome}</strong> em <strong>${UI.data(data)}</strong>,`
            + ` às <strong>${UI.hora(hora)}</strong>.`,
        confirmar: estado.reagendandoId ? 'Remarcar' : 'Confirmar'
    });
    if (!confirmado) {
        return;
    }

    const painel = document.getElementById('horarios');
    painel.querySelectorAll('button').forEach((b) => (b.disabled = true));

    try {
        const pedido = {
            servico: servico.codigo,
            data,
            hora,
            barbeiroId: barbeiroId ? Number(barbeiroId) : null
        };

        if (estado.reagendandoId) {
            await API.reagendar(estado.reagendandoId, pedido);
            encerrarReagendamento();
            UI.avisar('aviso', 'Agendamento remarcado.', 'ok');
        } else {
            await API.agendar(pedido);
            UI.avisar('aviso', 'Horário reservado. Até logo!', 'ok');
        }
        await Promise.all([carregarDisponibilidade(), carregarMeusAgendamentos()]);
    } catch (e) {
        // 409 é o caso interessante: alguém reservou primeiro. Recarregar a grade mostra
        // o estado real da agenda em vez de deixar o usuário tentando o mesmo horário.
        UI.avisar('aviso', e.message, 'erro', 8);
        if (e.status === 409) {
            await carregarDisponibilidade();
        } else {
            painel.querySelectorAll('button').forEach((b) => (b.disabled = false));
        }
    }
}

async function carregarMeusAgendamentos() {
    const container = document.getElementById('agendas');
    if (!API.estaAutenticado()) {
        return;
    }
    try {
        const pagina = await API.meusAgendamentos();
        container.innerHTML = '';

        if (pagina.conteudo.length === 0) {
            container.innerHTML = UI.estadoVazio('Você ainda não tem agendamentos.');
            return;
        }
        pagina.conteudo.forEach((a) => container.appendChild(cartaoDeAgendamento(a)));
    } catch (e) {
        container.innerHTML = UI.estadoVazio(e.message, true);
    }
}

function cartaoDeAgendamento(agendamento) {
    const item = document.createElement('article');
    item.className = `agenda-item status-${agendamento.status.toLowerCase()}`;
    item.innerHTML = `
        <div class="agenda-cabecalho">
            <h3>${agendamento.servicoNome}</h3>
            <span class="etiqueta status-${agendamento.status.toLowerCase()}">
                ${UI.rotuloStatus(agendamento.status)}</span>
        </div>
        <div class="agenda-dados">
            <div>${UI.icone('calendario', 18)}<span>${UI.data(agendamento.data)}</span></div>
            <div>${UI.icone('relogio', 18)}<span>${UI.hora(agendamento.horaInicio)} às
                ${UI.hora(agendamento.horaFim)}</span></div>
            <div>${UI.icone('pessoa', 18)}<span>${agendamento.barbeiro.nome}</span>
                <span class="agenda-preco">${UI.preco(agendamento.preco)}</span></div>
        </div>`;

    if (agendamento.status === 'AGENDADO') {
        const acoes = document.createElement('div');
        acoes.className = 'agenda-acoes';

        const remarcar = document.createElement('button');
        remarcar.type = 'button';
        remarcar.className = 'btn btn--contorno btn--p';
        remarcar.textContent = 'Remarcar';
        remarcar.addEventListener('click', () => iniciarReagendamento(agendamento));

        const cancelar = document.createElement('button');
        cancelar.type = 'button';
        cancelar.className = 'btn btn--fantasma btn--p btn--perigo';
        cancelar.textContent = 'Cancelar';
        cancelar.addEventListener('click', () => cancelarAgendamento(agendamento));

        acoes.append(remarcar, cancelar);
        item.appendChild(acoes);
    }

    return item;
}

function iniciarReagendamento(agendamento) {
    estado.reagendandoId = agendamento.id;
    document.getElementById('barraReagendamento').hidden = false;
    document.getElementById('textoReagendamento').textContent =
        `Remarcando ${agendamento.servicoNome} de ${UI.data(agendamento.data)}, `
        + `${UI.hora(agendamento.horaInicio)}. Escolha o novo horário abaixo.`;
    selecionarServico(agendamento.servico);
    document.getElementById('barraReagendamento').scrollIntoView({ behavior: 'smooth', block: 'center' });
}

function encerrarReagendamento() {
    estado.reagendandoId = null;
    document.getElementById('barraReagendamento').hidden = true;
}

async function cancelarAgendamento(agendamento) {
    const confirmado = await UI.confirmar({
        titulo: 'Cancelar agendamento',
        mensagem: [
            `<strong>${agendamento.servicoNome}</strong> em`
                + ` <strong>${UI.data(agendamento.data)}</strong>,`
                + ` às <strong>${UI.hora(agendamento.horaInicio)}</strong>.`,
            'O horário volta para a agenda e fica livre para outra pessoa.'
        ],
        confirmar: 'Cancelar atendimento',
        cancelar: 'Manter',
        perigo: true
    });
    if (!confirmado) {
        return;
    }
    try {
        await API.cancelar(agendamento.id);
        UI.avisar('aviso', 'Agendamento cancelado. O horário voltou para a agenda.', 'ok');
        await Promise.all([carregarMeusAgendamentos(), carregarDisponibilidade()]);
    } catch (e) {
        UI.avisar('aviso', e.message, 'erro', 8);
    }
}
