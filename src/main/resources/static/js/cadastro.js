const VANTAGENS = [
    'Sem taxa e sem compromisso',
    'Agende com até 60 dias de antecedência',
    'Cancele até 2 horas antes'
];

const ROTULOS = {
    nome: 'Nome',
    email: 'E-mail',
    senha: 'Senha',
    telefone: 'Telefone',
    endereco: 'Endereço'
};

document.addEventListener('DOMContentLoaded', () => {
    decorar();

    const formulario = document.getElementById('formCadastro');
    const erro = document.getElementById('mensagemErro');
    const botao = formulario.querySelector('button[type="submit"]');

    formulario.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        erro.hidden = true;
        botao.disabled = true;
        botao.textContent = 'Criando conta...';

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
            erro.hidden = false;
            botao.disabled = false;
            botao.textContent = 'Criar conta';
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
            .map((campo) => `<li>${ROTULOS[campo.campo] || campo.campo}: ${campo.mensagem}</li>`)
            .join('');
        return `<ul>${itens}</ul>`;
    }
    return erro.message;
}

function decorar() {
    document.getElementById('voltar').insertAdjacentHTML('afterbegin', UI.icone('voltar', 16));
    document.getElementById('vantagens').innerHTML = VANTAGENS
        .map((texto) => `<li>${UI.icone('check', 18)}${texto}</li>`)
        .join('');
}
