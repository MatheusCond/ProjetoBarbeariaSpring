document.getElementById('formCadastro').addEventListener('submit', function(event) {
    event.preventDefault(); // Evita que o formulário seja enviado diretamente

    var formData = {
        nomeUsuario: document.getElementById('nomeUsuario').value,
        emailUsuario: document.getElementById('emailUsuario').value,
        senhaUsuario: document.getElementById('senhaUsuario').value,
        endereco: document.getElementById('endereco').value,
        telefone: document.getElementById('telefone').value
    };

    fetch('/clientes/cadastrar-clientes', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(formData)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Erro ao cadastrar cliente');
            }
            return response.json();
        })
        .then(data => {
            console.log('Cliente cadastrado com sucesso:', data);
            // Limpar campos do formulário ou redirecionar para outra página, se necessário
            window.location.href = '/login.html'; // Substitua com o caminho correto para a tela de login
        })
        .catch(error => {
            console.error('Erro ao cadastrar cliente:', error);
            // Exibir mensagem de erro acima do formulário
            document.getElementById('mensagemErro').textContent = 'Usuário ou e-mail já existem';
            document.getElementById('mensagemErro').style.display = 'block';
        });
});
