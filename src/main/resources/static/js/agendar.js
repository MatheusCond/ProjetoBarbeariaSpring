/*
 * Tela de agendamento.
 *
 * A diferença central em relação à versão anterior: o horário não é digitado. A página
 * pergunta ao backend quais horários realmente cabem na agenda (GET
 * /api/agendamentos/disponibilidade) e oferece só esses. O cliente não descobre o
 * conflito depois de enviar — e o e-mail do dono da reserva não é mais um campo do
 * formulário, porque vem do token.
 */
const estado = {
    servicos: [],
    barbeiros: [],
    servicoSelecionado: null,
    horarioSelecionado: null,
    disponibilidade: null,
    reagendandoId: null,
    // Trocar serviço, data e profissional em sequência dispara consultas simultâneas.
    // Sem este contador, uma resposta atrasada pode sobrescrever a mais recente e a
    // grade mostra horários de outro serviço.
    consultaEmCurso: 0
};

document.addEventListener('DOMContentLoaded', async () => {
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
    estado.servicos = await API.servicos();
    const lista = document.getElementById('servicos');
    lista.innerHTML = '';

    estado.servicos.forEach((servico, indice) => {
        const item = document.createElement('li');
        item.className = 'card-servico';
        item.dataset.codigo = servico.codigo;
        item.innerHTML = `
            <h2>${servico.nome}</h2>
            <p class="product-description">${servico.descricao}</p>
            <p class="servico-duracao">${servico.duracaoMinutos} min</p>
            <p class="product-price">${formatarPreco(servico.preco)}</p>`;
        item.addEventListener('click', () => selecionarServico(servico.codigo));
        lista.appendChild(item);

        if (indice === 0) {
            estado.servicoSelecionado = servico.codigo;
        }
    });

    selecionarServico(estado.servicoSelecionado);
}

async function carregarBarbeiros() {
    estado.barbeiros = await API.barbeiros();
    const select = document.getElementById('barbeiro');
    select.innerHTML = '<option value="">Qualquer profissional disponível</option>';
    estado.barbeiros.forEach((barbeiro) => {
        const opcao = document.createElement('option');
        opcao.value = barbeiro.id;
        opcao.textContent = barbeiro.nome;
        select.appendChild(opcao);
    });
}

function selecionarServico(codigo) {
    estado.servicoSelecionado = codigo;
    estado.horarioSelecionado = null;
    document.querySelectorAll('.card-servico').forEach((card) => {
        card.classList.toggle('selecionado', card.dataset.codigo === codigo);
    });
    carregarDisponibilidade();
}

async function carregarDisponibilidade() {
    if (!estado.servicoSelecionado) {
        return;
    }
    const data = document.getElementById('data').value;
    const barbeiroId = document.getElementById('barbeiro').value || null;
    const painel = document.getElementById('horarios');

    if (!data) {
        painel.innerHTML = '<p class="aviso">Escolha uma data.</p>';
        return;
    }

    const consulta = ++estado.consultaEmCurso;
    painel.innerHTML = '<p class="aviso">Carregando horários...</p>';
    try {
        const resposta = await API.disponibilidade(data, estado.servicoSelecionado, barbeiroId);
        if (consulta !== estado.consultaEmCurso) {
            return; // Resposta de uma seleção que o usuário já trocou.
        }
        estado.disponibilidade = resposta;
        renderizarHorarios(resposta);
    } catch (e) {
        if (consulta === estado.consultaEmCurso) {
            painel.innerHTML = `<p class="aviso erro">${e.message}</p>`;
        }
    }
}

function renderizarHorarios(disponibilidade) {
    const painel = document.getElementById('horarios');
    painel.innerHTML = '';

    if (!disponibilidade.aberto) {
        painel.innerHTML = `<p class="aviso">${disponibilidade.motivoFechado}</p>`;
        return;
    }
    if (disponibilidade.horarios.length === 0) {
        painel.innerHTML = '<p class="aviso">Nenhum horário livre nesta data. '
            + 'Tente outro dia ou outro profissional.</p>';
        return;
    }

    disponibilidade.horarios.forEach((hora) => {
        const botao = document.createElement('button');
        botao.type = 'button';
        botao.className = 'slot';
        botao.textContent = hora.slice(0, 5);
        botao.addEventListener('click', () => confirmar(hora));
        painel.appendChild(botao);
    });
}

async function confirmar(hora) {
    if (!API.estaAutenticado()) {
        window.location.href = 'login.html';
        return;
    }

    const servico = estado.servicos.find((s) => s.codigo === estado.servicoSelecionado);
    const data = document.getElementById('data').value;
    const barbeiroId = document.getElementById('barbeiro').value || null;
    const acao = estado.reagendandoId ? 'Remarcar' : 'Confirmar';

    if (!window.confirm(`${acao} ${servico.nome} em ${formatarData(data)} às ${hora.slice(0, 5)}?`)) {
        return;
    }

    const painel = document.getElementById('horarios');
    painel.querySelectorAll('button').forEach((b) => (b.disabled = true));

    try {
        if (estado.reagendandoId) {
            await API.reagendar(estado.reagendandoId, {
                servico: servico.codigo, data, hora, barbeiroId: barbeiroId ? Number(barbeiroId) : null
            });
            encerrarReagendamento();
            avisar('Agendamento remarcado.', 'sucesso');
        } else {
            await API.agendar({
                servico: servico.codigo, data, hora,
                barbeiroId: barbeiroId ? Number(barbeiroId) : null
            });
            avisar('Horário reservado. Você recebe a confirmação no balcão.', 'sucesso');
        }
        await Promise.all([carregarDisponibilidade(), carregarMeusAgendamentos()]);
    } catch (e) {
        // 409 é o caso interessante: alguém reservou primeiro. Recarregar a grade mostra
        // o estado real da agenda em vez de deixar o usuário tentando o mesmo horário.
        avisar(e.message, 'erro');
        if (e.status === 409) {
            await carregarDisponibilidade();
        } else {
            painel.querySelectorAll('button').forEach((b) => (b.disabled = false));
        }
    }
}

async function carregarMeusAgendamentos() {
    const container = document.getElementById('agendas');
    try {
        const pagina = await API.meusAgendamentos();
        container.innerHTML = '';

        if (pagina.conteudo.length === 0) {
            container.innerHTML = '<p class="aviso">Você ainda não tem agendamentos.</p>';
            return;
        }

        pagina.conteudo.forEach((agendamento) => {
            container.appendChild(cartaoDeAgendamento(agendamento));
        });
    } catch (e) {
        container.innerHTML = `<p class="aviso erro">${e.message}</p>`;
    }
}

function cartaoDeAgendamento(agendamento) {
    const item = document.createElement('div');
    item.className = `agenda-item status-${agendamento.status.toLowerCase()}`;
    item.innerHTML = `
        <div class="agenda-cabecalho">
            <h3>${agendamento.servicoNome}</h3>
            <span class="etiqueta">${rotuloStatus(agendamento.status)}</span>
        </div>
        <p>${formatarData(agendamento.data)}, das ${agendamento.horaInicio.slice(0, 5)}
           às ${agendamento.horaFim.slice(0, 5)}</p>
        <p>Profissional: ${agendamento.barbeiro.nome}</p>
        <p>${formatarPreco(agendamento.preco)}</p>`;

    if (agendamento.status === 'AGENDADO') {
        const acoes = document.createElement('div');
        acoes.className = 'agenda-acoes';

        const remarcar = document.createElement('button');
        remarcar.type = 'button';
        remarcar.textContent = 'Remarcar';
        remarcar.addEventListener('click', () => iniciarReagendamento(agendamento));

        const cancelar = document.createElement('button');
        cancelar.type = 'button';
        cancelar.className = 'secundario';
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
        `Remarcando ${agendamento.servicoNome} de ${formatarData(agendamento.data)} `
        + `${agendamento.horaInicio.slice(0, 5)}. Escolha o novo horário.`;
    selecionarServico(agendamento.servico);
    document.getElementById('data').scrollIntoView({ behavior: 'smooth' });
}

function encerrarReagendamento() {
    estado.reagendandoId = null;
    document.getElementById('barraReagendamento').hidden = true;
}

async function cancelarAgendamento(agendamento) {
    if (!window.confirm(`Cancelar ${agendamento.servicoNome} de ${formatarData(agendamento.data)}?`)) {
        return;
    }
    try {
        await API.cancelar(agendamento.id);
        avisar('Agendamento cancelado. O horário voltou para a agenda.', 'sucesso');
        await Promise.all([carregarMeusAgendamentos(), carregarDisponibilidade()]);
    } catch (e) {
        avisar(e.message, 'erro');
    }
}

// --- Apoio ------------------------------------------------------------------

function avisar(mensagem, tipo) {
    const caixa = document.getElementById('aviso');
    caixa.textContent = mensagem;
    caixa.className = `caixa-aviso ${tipo}`;
    caixa.hidden = false;
    clearTimeout(avisar.temporizador);
    avisar.temporizador = setTimeout(() => (caixa.hidden = true), 6000);
}

function rotuloStatus(status) {
    return {
        AGENDADO: 'Agendado',
        CONCLUIDO: 'Concluído',
        CANCELADO: 'Cancelado',
        NAO_COMPARECEU: 'Não compareceu'
    }[status] || status;
}

function formatarPreco(valor) {
    return Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function formatarData(iso) {
    const [ano, mes, dia] = iso.split('-');
    const data = new Date(Number(ano), Number(mes) - 1, Number(dia));
    return data.toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: '2-digit' });
}
