/*
 * Ajusta a navegação conforme a sessão: visitante vê "Login", usuário autenticado vê o
 * próprio nome, "Agendar", o painel (se for da equipe) e "Sair".
 *
 * Roda em todas as páginas e é o que dá ao site a sensação de continuidade entre elas,
 * já que a sessão é restaurada pelo cookie httpOnly e não por nada gravado no navegador.
 */
document.addEventListener('DOMContentLoaded', async () => {
    const menu = document.querySelector('[data-nav]');
    if (!menu) {
        return;
    }

    const usuario = await API.restaurarSessao();
    const itens = [{ texto: 'Home', href: 'index.html' }];

    if (usuario) {
        itens.push({ texto: 'Agendar', href: 'agendar.html' });
        if (API.ehEquipe()) {
            itens.push({ texto: 'Painel', href: 'painel.html' });
        }
        itens.push({ texto: `Sair (${primeiroNome(usuario.nome)})`, href: '#', acao: sair });
    } else {
        itens.push({ texto: 'Agendar', href: 'agendar.html' });
        itens.push({ texto: 'Login', href: 'login.html' });
    }

    menu.innerHTML = '';
    itens.forEach((item) => {
        const li = document.createElement('li');
        const a = document.createElement('a');
        a.textContent = item.texto;
        a.href = item.href;
        if (item.acao) {
            a.addEventListener('click', (evento) => {
                evento.preventDefault();
                item.acao();
            });
        }
        li.appendChild(a);
        menu.appendChild(li);
    });

    document.dispatchEvent(new CustomEvent('sessao-pronta', { detail: usuario }));
});

function primeiroNome(nome) {
    return (nome || '').split(' ')[0];
}

async function sair() {
    await API.logout();
    window.location.href = 'index.html';
}
