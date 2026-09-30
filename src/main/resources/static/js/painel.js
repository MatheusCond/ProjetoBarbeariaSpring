/*
 * Painel da equipe: agenda do dia, com filtros, conclusão de atendimento e registro de
 * falta. Protegido nas duas pontas — a página redireciona quem não é da equipe e a API
 * responde 403 de qualquer forma, porque o perfil vem do token e não da tela.
 */
document.addEventListener('sessao-pronta', (evento) => {
    if (!evento.detail) {
        window.location.replace('login.html');
        return;
    }
    if (!API.ehEquipe()) {
        window.location.replace('agendar.html');
        return;
    }

    const hoje = new Date().toISOString().slice(0, 10);
    document.getElementById('de').value = hoje;
    document.getElementById('ate').value = hoje;

    document.getElementById('filtros').addEventListener('submit', (e) => {
        e.preventDefault();
        carregarAgenda();
    });

    carregarBarbeiros().then(carregarAgenda);
});

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
    corpo.innerHTML = '<tr><td colspan="7">Carregando...</td></tr>';

    try {
        const pagina = await API.agendaDaEquipe({
            de: document.getElementById('de').value,
            ate: document.getElementById('ate').value,
            status: document.getElementById('statusFiltro').value,
            barbeiroId: document.getElementById('barbeiroFiltro').value
        });

        document.getElementById('resumo').textContent =
            `${pagina.totalElementos} atendimento(s) no período`;

        corpo.innerHTML = '';
        if (pagina.conteudo.length === 0) {
            corpo.innerHTML = '<tr><td colspan="7">Nenhum atendimento no período.</td></tr>';
            return;
        }
        pagina.conteudo.forEach((a) => corpo.appendChild(linha(a)));
    } catch (e) {
        corpo.innerHTML = `<tr><td colspan="7" class="erro">${e.message}</td></tr>`;
    }
}

function linha(agendamento) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
        <td>${formatarData(agendamento.data)}</td>
        <td>${agendamento.horaInicio.slice(0, 5)} - ${agendamento.horaFim.slice(0, 5)}</td>
        <td>${agendamento.servicoNome}</td>
        <td>${agendamento.cliente.nome}<br><small>${agendamento.cliente.telefone || ''}</small></td>
        <td>${agendamento.barbeiro.nome}</td>
        <td><span class="etiqueta status-${agendamento.status.toLowerCase()}">
            ${rotuloStatus(agendamento.status)}</span></td>`;

    const acoes = document.createElement('td');
    if (agendamento.status === 'AGENDADO') {
        acoes.appendChild(botao('Concluir', () => executar(() => API.concluir(agendamento.id))));
        acoes.appendChild(botao('Faltou', () => executar(() => API.naoCompareceu(agendamento.id)), true));
        acoes.appendChild(botao('Cancelar', () => executar(() => API.cancelar(agendamento.id)), true));
    } else {
        acoes.textContent = '—';
    }
    tr.appendChild(acoes);
    return tr;
}

function botao(texto, acao, secundario = false) {
    const b = document.createElement('button');
    b.type = 'button';
    b.textContent = texto;
    if (secundario) {
        b.className = 'secundario';
    }
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

function rotuloStatus(status) {
    return {
        AGENDADO: 'Agendado',
        CONCLUIDO: 'Concluído',
        CANCELADO: 'Cancelado',
        NAO_COMPARECEU: 'Não compareceu'
    }[status] || status;
}

function formatarData(iso) {
    const [ano, mes, dia] = iso.split('-');
    return `${dia}/${mes}/${ano}`;
}
