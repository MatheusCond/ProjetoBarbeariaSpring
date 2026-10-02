/*
 * Cabeçalho: monta o menu conforme a sessão e cuida do comportamento no mobile.
 *
 * Visitante vê "Entrar" e o botão de agendar. Autenticado vê o próprio nome, o
 * painel (se for da equipe), a administração da equipe (se for ADMIN) e "Sair".
 * A sessão é restaurada pelo cookie httpOnly,
 * então a montagem espera o refresh terminar antes de decidir o que mostrar.
 */
document.addEventListener('DOMContentLoaded', async () => {
    const menu = document.querySelector('[data-nav]');
    const botaoMenu = document.querySelector('[data-menu-botao]');

    if (botaoMenu && menu) {
        botaoMenu.innerHTML = UI.icone('menu', 22);
        botaoMenu.addEventListener('click', () => {
            const aberto = menu.dataset.aberto === 'true';
            menu.dataset.aberto = String(!aberto);
            botaoMenu.setAttribute('aria-expanded', String(!aberto));
        });
    }

    if (!menu) {
        return;
    }

    const usuario = await API.restaurarSessao();
    const paginaAtual = location.pathname.split('/').pop() || 'index.html';

    const itens = [{ texto: 'Início', href: 'index.html' }];

    if (usuario) {
        itens.push({ texto: 'Agendar', href: 'agendar.html' });
        if (API.ehEquipe()) {
            itens.push({ texto: 'Painel', href: 'painel.html' });
        }
        if (API.ehAdmin()) {
            itens.push({ texto: 'Equipe', href: 'admin.html' });
        }
        itens.push({ chip: usuario });
        itens.push({ texto: 'Sair', acao: sair, classe: 'btn btn--contorno btn--p' });
    } else {
        itens.push({ texto: 'Serviços', href: 'index.html#servicos' });
        itens.push({ texto: 'Contato', href: 'index.html#contato' });
        itens.push({ texto: 'Entrar', href: 'login.html', classe: 'btn btn--contorno btn--p' });
        itens.push({ texto: 'Agendar', href: 'agendar.html', classe: 'btn btn--ouro btn--p' });
    }

    menu.innerHTML = '';
    itens.forEach((item) => menu.appendChild(montarItem(item, paginaAtual)));

    // As páginas esperam por este evento para saber se podem carregar dados do usuário.
    document.dispatchEvent(new CustomEvent('sessao-pronta', { detail: usuario }));
});

function montarItem(item, paginaAtual) {
    const li = document.createElement('li');

    if (item.chip) {
        li.innerHTML = `
            <span class="usuario-chip">
                <span class="usuario-avatar">${inicial(item.chip.nome)}</span>
                ${primeiroNome(item.chip.nome)}
            </span>`;
        return li;
    }

    const link = document.createElement('a');
    link.textContent = item.texto;
    link.href = item.href || '#';
    if (item.classe) {
        link.className = item.classe;
    }
    if (item.href === paginaAtual && !item.classe) {
        link.classList.add('ativo');
    }
    if (item.acao) {
        link.addEventListener('click', (evento) => {
            evento.preventDefault();
            item.acao();
        });
    }

    li.appendChild(link);
    return li;
}

function primeiroNome(nome) {
    return (nome || '').trim().split(' ')[0];
}

function inicial(nome) {
    return (nome || '?').trim().charAt(0).toUpperCase();
}

async function sair() {
    await API.logout();
    window.location.href = 'index.html';
}
