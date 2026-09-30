document.addEventListener('DOMContentLoaded', () => {
    const formulario = document.getElementById('formLogin');
    const erro = document.getElementById('mensagemErro');
    const botao = formulario.querySelector('button[type="submit"]');

    // Já autenticado (cookie de refresh válido): não faz sentido mostrar o login.
    API.restaurarSessao().then((usuario) => {
        if (usuario) {
            window.location.replace('agendar.html');
        }
    });

    formulario.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        esconder(erro);
        botao.disabled = true;
        botao.textContent = 'Entrando...';

        try {
            await API.login(
                document.getElementById('email').value.trim(),
                document.getElementById('senha').value
            );
            // O token fica em memória; a navegação seguinte o recupera pelo cookie.
            window.location.href = API.ehEquipe() ? 'painel.html' : 'agendar.html';
        } catch (e) {
            mostrar(erro, e.status === 401
                ? 'E-mail ou senha incorretos.'
                : e.message);
        } finally {
            botao.disabled = false;
            botao.textContent = 'Entrar';
        }
    });
});

function mostrar(elemento, mensagem) {
    elemento.textContent = mensagem;
    elemento.style.display = 'block';
}

function esconder(elemento) {
    elemento.style.display = 'none';
}
