/*
 * Painel da equipe: agenda do período, com filtros, conclusão de atendimento e
 * registro de falta.
 *
 * Protegido nas duas pontas — a página redireciona quem não é da equipe e a API
 * responde 403 de qualquer forma, porque o perfil vem do token e não da tela.
 */
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('ano').textContent = new Date().getFullYear();
});

document.addEventListener('sessao-pronta', (evento) => {
    if (!evento.detail) {
        window.location.replace('login.html');
        return;
    }
    if (!API.ehEquipe()) {
        window.location.replace('agendar.html');
        return;
    }

    definirPeriodo(0);

    document.getElementById('filtros').addEventListener('submit', (e) => {
        e.preventDefault();
        carregarAgenda();
    });
    document.getElementById('hoje').addEventListener('click', () => definirPeriodo(0, true));
    document.getElementById('semana').addEventListener('click', () => definirPeriodo(7, true));

    carregarBarbeiros().then(carregarAgenda);
});

function definirPeriodo(diasAdiante, recarregar = false) {
    const hoje = new Date();
    const fim = new Date();
    fim.setDate(fim.getDate() + diasAdiante);

    document.getElementById('de').value = hoje.toISOString().slice(0, 10);
    document.getElementById('ate').value = fim.toISOString().slice(0, 10);

    if (recarregar) {
        carregarAgenda();
    }
}

async function carregarBarbeiros() {
    const select = document.getElementById('barbeiroFiltro');
    select.innerHTML = '<option value="">Todos os profissionais</option>';
    (await API.barbeiros()).forEach((barbeiro) => {
        const opcao = document.createElement('option');
        opcao.value = barbeiro.id;
        opcao.textContent = barbeiro.nome;
        select.appendChild(opcao);
    });
}

async function carregarAgenda() {
    const corpo = document.getElementById('corpoAgenda');
    corpo.innerHTML = '<tr><td colspan="7" class="vazio">Carregando...</td></tr>';

    try {
        const pagina = await API.agendaDaEquipe({
            de: document.getElementById('de').value,
            ate: document.getElementById('ate').value,
            status: document.getElementById('statusFiltro').value,
            barbeiroId: document.getElementById('barbeiroFiltro').value
        });

        document.getElementById('resumo').textContent =
            `${pagina.totalElementos} atendimento(s) no período selecionado`;
        montarMetricas(pagina.conteudo);

        corpo.innerHTML = '';
        if (pagina.conteudo.length === 0) {
            corpo.innerHTML =
                '<tr><td colspan="7" class="vazio">Nenhum atendimento no período.</td></tr>';
            return;
        }
        pagina.conteudo.forEach((a) => corpo.appendChild(linha(a)));
    } catch (e) {
        corpo.innerHTML = `<tr><td colspan="7" class="vazio erro">${e.message}</td></tr>`;
    }
}

/** Resumo do que está na tela: a contagem acompanha os filtros aplicados. */
function montarMetricas(agendamentos) {
    const conta = (status) => agendamentos.filter((a) => a.status === status).length;
    const faturamento = agendamentos
        .filter((a) => a.status === 'CONCLUIDO')
        .reduce((total, a) => total + Number(a.preco), 0);

    const cartoes = [
        { valor: conta('AGENDADO'), rotulo: 'Agendados', destaque: true },
        { valor: conta('CONCLUIDO'), rotulo: 'Concluídos' },
        { valor: conta('CANCELADO') + conta('NAO_COMPARECEU'), rotulo: 'Cancelados / faltas' },
        { valor: UI.preco(faturamento), rotulo: 'Concluído no período' }
    ];

    document.getElementById('metricas').innerHTML = cartoes.map((c) => `
        <div class="metrica${c.destaque ? ' destaque' : ''}">
            <strong>${c.valor}</strong>
            <span>${c.rotulo}</span>
        </div>`).join('');
}

function linha(agendamento) {
    const tr = document.createElement('tr');
    const status = agendamento.status.toLowerCase();

    tr.innerHTML = `
        <td>${UI.dataCurta(agendamento.data)}</td>
        <td class="horario">${UI.hora(agendamento.horaInicio)} — ${UI.hora(agendamento.horaFim)}</td>
        <td>${agendamento.servicoNome}<small>${UI.preco(agendamento.preco)}</small></td>
        <td>${agendamento.cliente.nome}<small>${agendamento.cliente.telefone || ''}</small></td>
        <td>${agendamento.barbeiro.nome}</td>
        <td><span class="etiqueta status-${status}">${UI.rotuloStatus(agendamento.status)}</span></td>`;

    const celulaAcoes = document.createElement('td');
    if (agendamento.status === 'AGENDADO') {
        const acoes = document.createElement('div');
        acoes.className = 'acoes';
        acoes.append(
            botao('Concluir', 'btn--ouro', () => executar(() => API.concluir(agendamento.id))),
            botao('Faltou', 'btn--contorno', () => executar(() => API.naoCompareceu(agendamento.id))),
            botao('Cancelar', 'btn--fantasma btn--perigo',
                () => executar(() => API.cancelar(agendamento.id)))
        );
        celulaAcoes.appendChild(acoes);
    } else {
        celulaAcoes.innerHTML = '<small>—</small>';
    }
    tr.appendChild(celulaAcoes);

    return tr;
}

function botao(texto, variante, acao) {
    const b = document.createElement('button');
    b.type = 'button';
    b.className = `btn btn--p ${variante}`;
    b.textContent = texto;
    b.addEventListener('click', acao);
    return b;
}

async function executar(acao) {
    try {
        await acao();
        await carregarAgenda();
    } catch (e) {
        window.alert(e.message);
    }
}
