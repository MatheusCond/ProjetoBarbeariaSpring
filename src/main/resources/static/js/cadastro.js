document.addEventListener('DOMContentLoaded', () => {
    const formulario = document.getElementById('formCadastro');
    const erro = document.getElementById('mensagemErro');
    const botao = formulario.querySelector('button[type="submit"]');

    formulario.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        erro.style.display = 'none';
        botao.disabled = true;
        botao.textContent = 'Cadastrando...';

        try {
            await API.registrar({
                nome: document.getElementById('nome').value.trim(),
                email: document.getElementById('email').value.trim(),
                senha: document.getElementById('senha').value,
                telefone: document.getElementById('telefone').value.trim(),
                endereco: document.getElementById('endereco').value.trim() || null
            });
            // O cadastro já devolve a sessão: nada de pedir login logo depois.
            window.location.href = 'agendar.html';
        } catch (e) {
            erro.innerHTML = montarMensagem(e);
            erro.style.display = 'block';
        } finally {
            botao.disabled = false;
            botao.textContent = 'Cadastrar';
        }
    });
});

/**
 * O backend devolve a lista de campos inválidos; mostrar campo por campo evita o
 * antigo "usuário ou e-mail já existem" genérico para qualquer erro.
 */
function montarMensagem(erro) {
    if (erro.campos && erro.campos.length > 0) {
        const itens = erro.campos
            .map((campo) => `<li>${rotulo(campo.campo)}: ${campo.mensagem}</li>`)
            .join('');
        return `<ul class="lista-erros">${itens}</ul>`;
    }
    return erro.message;
}

function rotulo(campo) {
    const rotulos = {
        nome: 'Nome',
        email: 'E-mail',
        senha: 'Senha',
        telefone: 'Telefone',
        endereco: 'Endereço'
    };
    return rotulos[campo] || campo;
}
