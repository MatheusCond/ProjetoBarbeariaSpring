/*
 * Administração da equipe: cadastro de profissionais, ativação e senha provisória.
 *
 * Fecha o ciclo de implantação do projeto. O administrador do primeiro boot vem de
 * variável de ambiente e, até aqui, não tinha por onde cadastrar quem atende — e sem
 * barbeiro ativo a agenda simplesmente não abre.
 *
 * Como no painel, a verificação de perfil desta tela é conveniência: ela evita mostrar
 * uma página inútil a quem não é administrador, mas é a API que decide, porque o perfil
 * vem do token.
 */
const MINIMO_DA_SENHA = 8;

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('ano').textContent = new Date().getFullYear();
});

document.addEventListener('sessao-pronta', (evento) => {
    if (!evento.detail) {
        window.location.replace('login.html');
        return;
    }
    if (!API.ehAdmin()) {
        window.location.replace(API.ehEquipe() ? 'painel.html' : 'agendar.html');
        return;
    }

    document.getElementById('novoBarbeiro').addEventListener('submit', cadastrar);
    carregarEquipe();
});

async function carregarEquipe() {
    const corpo = document.getElementById('corpoEquipe');
    corpo.innerHTML = '<tr><td colspan="5" class="vazio">Carregando...</td></tr>';

    try {
        const barbeiros = await API.barbeirosDaAdministracao();
        resumir(barbeiros);

        corpo.innerHTML = '';
        if (barbeiros.length === 0) {
            corpo.innerHTML = '<tr><td colspan="5" class="vazio">'
                + 'Nenhum profissional cadastrado. Cadastre o primeiro no formulário acima — '
                + 'sem ninguém ativo a agenda não abre.</td></tr>';
            return;
        }
        barbeiros.forEach((barbeiro) => corpo.appendChild(linha(barbeiro)));
    } catch (e) {
        corpo.innerHTML = `<tr><td colspan="5" class="vazio erro">${e.message}</td></tr>`;
    }
}

function resumir(barbeiros) {
    const ativos = barbeiros.filter((b) => b.ativo).length;
    const inativos = barbeiros.length - ativos;

    const partes = [`${ativos} profissional(is) ativo(s)`];
    if (inativos > 0) {
        partes.push(`${inativos} desativado(s)`);
    }
    if (ativos === 0) {
        partes.push('a agenda fica fechada enquanto não houver ninguém ativo');
    }
    document.getElementById('resumo').textContent = partes.join(' · ');
}

function linha(barbeiro) {
    const tr = document.createElement('tr');
    const situacao = barbeiro.ativo
        ? '<span class="etiqueta status-concluido">Ativo</span>'
        : '<span class="etiqueta status-cancelado">Desativado</span>';

    tr.innerHTML = `
        <td>${barbeiro.nome}</td>
        <td>${barbeiro.email}<small>${barbeiro.telefone || 'sem telefone'}</small></td>
        <td>${situacao}</td>
        <td>${barbeiro.agendamentosFuturos || '—'}</td>`;

    const celulaAcoes = document.createElement('td');
    const acoes = document.createElement('div');
    acoes.className = 'acoes';
    acoes.append(
        barbeiro.ativo
            ? botao('Desativar', 'btn--fantasma btn--perigo', () => alternar(barbeiro, false))
            : botao('Reativar', 'btn--ouro', () => alternar(barbeiro, true)),
        botao('Nova senha', 'btn--contorno', () => redefinirSenha(barbeiro))
    );
    celulaAcoes.appendChild(acoes);
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

async function cadastrar(evento) {
    evento.preventDefault();

    const botaoEnviar = evento.target.querySelector('button[type="submit"]');
    botaoEnviar.disabled = true;

    try {
        const criado = await API.cadastrarBarbeiro({
            nome: document.getElementById('nome').value.trim(),
            email: document.getElementById('email').value.trim(),
            senha: document.getElementById('senha').value,
            telefone: document.getElementById('telefone').value.trim()
        });

        evento.target.reset();
        avisar(`${criado.nome} foi cadastrado e já pode receber agendamentos.`, 'ok');
        await carregarEquipe();
    } catch (e) {
        avisar(mensagemDeErro(e), 'erro');
    } finally {
        botaoEnviar.disabled = false;
    }
}

/**
 * Desativar não cancela nada, então o que já está marcado continua valendo: por isso o
 * aviso traz a quantidade antes de confirmar.
 */
async function alternar(barbeiro, ativo) {
    if (!ativo && !await confirmarDesativacao(barbeiro)) {
        return;
    }

    try {
        const atualizado = await API.definirSituacaoDoBarbeiro(barbeiro.id, ativo);
        avisar(ativo
            ? `${atualizado.nome} voltou a receber agendamentos.`
            : `${atualizado.nome} foi desativado. ${textoDoQueSobra(atualizado)}`,
            ativo ? 'ok' : 'info');
        await carregarEquipe();
    } catch (e) {
        avisar(mensagemDeErro(e), 'erro');
    }
}

function confirmarDesativacao(barbeiro) {
    const paragrafos = [
        'Ele perde o acesso e sai da lista de quem recebe novas reservas.'
            + ' Nada é apagado, e a ação pode ser desfeita.'
    ];

    if (barbeiro.agendamentosFuturos > 0) {
        paragrafos.push(`Atenção: <strong>${barbeiro.agendamentosFuturos} atendimento(s)</strong>`
            + ' seguem marcados no nome dele e continuarão na agenda.'
            + ' Remarque ou cancele pelo painel.');
    }

    return UI.confirmar({
        titulo: `Desativar ${barbeiro.nome}?`,
        mensagem: paragrafos,
        confirmar: 'Desativar',
        cancelar: 'Manter ativo',
        perigo: true
    });
}

function textoDoQueSobra(barbeiro) {
    return barbeiro.agendamentosFuturos > 0
        ? `${barbeiro.agendamentosFuturos} atendimento(s) seguem marcados na agenda.`
        : 'Não havia nada marcado para ele.';
}

async function redefinirSenha(barbeiro) {
    const nova = await UI.perguntar({
        titulo: `Nova senha para ${barbeiro.nome}`,
        mensagem: 'As sessões abertas dele serão encerradas, e ele entrará com esta senha'
            + ' até trocá-la pelo painel.',
        campo: {
            rotulo: 'Senha provisória',
            tipo: 'password',
            dica: `mínimo ${MINIMO_DA_SENHA} caracteres`,
            validar: (valor) => valor.length < MINIMO_DA_SENHA
                ? `A senha precisa ter ao menos ${MINIMO_DA_SENHA} caracteres.`
                : null
        },
        confirmar: 'Redefinir'
    });

    if (nova === null) {
        return;
    }

    try {
        await API.redefinirSenhaDoBarbeiro(barbeiro.id, nova);
        avisar(`Senha de ${barbeiro.nome} redefinida. Entregue a ele e peça a troca no `
            + 'primeiro acesso.', 'ok');
    } catch (e) {
        avisar(mensagemDeErro(e), 'erro');
    }
}

/** Erro de validação vem com a lista de campos; sem ela, só a mensagem. */
function mensagemDeErro(erro) {
    if (erro.campos && erro.campos.length > 0) {
        return `${erro.message}<ul>${erro.campos
            .map((campo) => `<li>${campo.mensagem}</li>`).join('')}</ul>`;
    }
    return erro.message;
}

function avisar(mensagem, tipo) {
    UI.avisar('aviso', mensagem, tipo, 8);
}
