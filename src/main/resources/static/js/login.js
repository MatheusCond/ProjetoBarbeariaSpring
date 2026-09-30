const VANTAGENS = [
    'Horários livres em tempo real',
    'Remarque ou cancele sozinho',
    'Histórico dos seus atendimentos'
];

document.addEventListener('DOMContentLoaded', () => {
    decorar();

    const formulario = document.getElementById('formLogin');
    const erro = document.getElementById('mensagemErro');
    const botao = formulario.querySelector('button[type="submit"]');

    // Já autenticado (cookie de refresh válido): não faz sentido mostrar o login.
    API.restaurarSessao().then((usuario) => {
        if (usuario) {
            window.location.replace(API.ehEquipe() ? 'painel.html' : 'agendar.html');
        }
    });

    formulario.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        erro.hidden = true;
        botao.disabled = true;
        botao.textContent = 'Entrando...';

        try {
            await API.login(
                document.getElementById('email').value.trim(),
                document.getElementById('senha').value
            );
            // O token fica em memória; a próxima página o recupera pelo cookie.
            window.location.href = API.ehEquipe() ? 'painel.html' : 'agendar.html';
        } catch (e) {
            erro.textContent = e.status === 401 ? 'E-mail ou senha incorretos.' : e.message;
            erro.hidden = false;
            botao.disabled = false;
            botao.textContent = 'Entrar';
        }
    });
});

function decorar() {
    document.getElementById('voltar').insertAdjacentHTML('afterbegin', UI.icone('voltar', 16));
    document.getElementById('vantagens').innerHTML = VANTAGENS
        .map((texto) => `<li>${UI.icone('check', 18)}${texto}</li>`)
        .join('');
}
