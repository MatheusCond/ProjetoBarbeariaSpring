document.getElementById('formLogin').addEventListener('submit', function(event) {
    event.preventDefault(); // Evita que o formulário seja enviado diretamente

    var formData = {
        nomeUsuario: document.getElementById('nomeUsuario').value,
        senhaUsuario: document.getElementById('senhaUsuario').value
    };

    fetch('/usuarios/login-usuarios', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(formData)
    })
        .then(response => {
            if (response.ok) {
                // Redirecionar para a página de agendamentos
                window.location.href = '/product.html'; // Substitua com o caminho correto para a página de agendamentos
            } else if (response.status === 401) {
                throw new Error('Usuário ou senha incorretos');
            } else {
                throw new Error('Erro ao fazer login');
            }
        })
        .catch(error => {
            console.error('Erro ao fazer login:', error);
            // Exibir mensagem de erro acima do formulário
            document.getElementById('mensagemErro').textContent = 'Nome ou senha incorretos';
            document.getElementById('mensagemErro').style.display = 'block';
        });
});
