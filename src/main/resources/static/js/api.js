/*
 * Camada de acesso à API e guarda da sessão.
 *
 * Como o token é guardado (e por que assim):
 *
 *  - O ACCESS TOKEN fica apenas nesta variável de módulo, em memória. Não vai para
 *    localStorage nem sessionStorage: qualquer script injetado na página (XSS, extensão,
 *    biblioteca de terceiros comprometida) consegue ler esses dois, e um token roubado de
 *    lá vale até expirar, em qualquer aba e em qualquer dispositivo.
 *
 *  - O REFRESH TOKEN nunca passa pelo JavaScript. Ele vem do backend em cookie
 *    HttpOnly + SameSite=Strict, com Path=/api/auth, e o navegador o envia sozinho
 *    somente para os endpoints de autenticação.
 *
 *  - Recarregar a página apaga o access token da memória. Em vez de persistir o token,
 *    restaurarSessao() chama POST /api/auth/refresh: o cookie prova quem é o usuário e a
 *    API devolve um access token novo. É o mesmo efeito prático de "continuar logado",
 *    sem deixar credencial legível no navegador.
 */
const API = (() => {
    const BASE = '';

    let accessToken = null;
    let usuario = null;
    let renovacaoAgendada = null;
    let renovacaoEmAndamento = null;

    /** Erro de API com a mensagem que o backend já devolve pronta para exibição. */
    class ErroDaApi extends Error {
        constructor(status, corpo) {
            super((corpo && corpo.mensagem) || 'Não foi possível completar a operação.');
            this.status = status;
            this.campos = (corpo && corpo.campos) || [];
        }
    }

    async function lerCorpo(resposta) {
        const texto = await resposta.text();
        if (!texto) {
            return null;
        }
        try {
            return JSON.parse(texto);
        } catch {
            return null;
        }
    }

    /**
     * Faz a requisição e, em caso de 401, tenta renovar a sessão uma única vez antes de
     * repetir. Assim a expiração do access token (15 min) é invisível para o usuário.
     */
    async function requisitar(caminho, opcoes = {}, permitirRenovacao = true) {
        const cabecalhos = Object.assign({}, opcoes.cabecalhos);
        if (opcoes.corpo !== undefined) {
            cabecalhos['Content-Type'] = 'application/json';
        }
        if (accessToken) {
            cabecalhos['Authorization'] = `Bearer ${accessToken}`;
        }

        const resposta = await fetch(BASE + caminho, {
            method: opcoes.metodo || 'GET',
            headers: cabecalhos,
            body: opcoes.corpo !== undefined ? JSON.stringify(opcoes.corpo) : undefined,
            credentials: 'same-origin'
        });

        if (resposta.status === 401 && permitirRenovacao && accessToken) {
            const renovou = await renovar();
            if (renovou) {
                return requisitar(caminho, opcoes, false);
            }
        }

        if (!resposta.ok) {
            throw new ErroDaApi(resposta.status, await lerCorpo(resposta));
        }
        return resposta.status === 204 ? null : lerCorpo(resposta);
    }

    function guardarSessao(dados) {
        accessToken = dados.accessToken;
        usuario = dados.usuario;
        agendarRenovacao(dados.expiraEmSegundos);
        return usuario;
    }

    function limparSessao() {
        accessToken = null;
        usuario = null;
        if (renovacaoAgendada) {
            clearTimeout(renovacaoAgendada);
            renovacaoAgendada = null;
        }
    }

    /** Renova um minuto antes de expirar, para o usuário não topar com o 401. */
    function agendarRenovacao(expiraEmSegundos) {
        if (renovacaoAgendada) {
            clearTimeout(renovacaoAgendada);
        }
        const antecedencia = Math.max(30, (expiraEmSegundos || 900) - 60);
        renovacaoAgendada = setTimeout(renovar, antecedencia * 1000);
    }

    /** Chamadas simultâneas compartilham a mesma renovação, evitando rotações em paralelo. */
    function renovar() {
        if (!renovacaoEmAndamento) {
            renovacaoEmAndamento = (async () => {
                try {
                    const dados = await requisitar('/api/auth/refresh', { metodo: 'POST' }, false);
                    guardarSessao(dados);
                    return true;
                } catch {
                    limparSessao();
                    return false;
                } finally {
                    renovacaoEmAndamento = null;
                }
            })();
        }
        return renovacaoEmAndamento;
    }

    return {
        ErroDaApi,

        estaAutenticado: () => accessToken !== null,
        usuarioAtual: () => usuario,
        ehEquipe: () => !!usuario && (usuario.role === 'BARBEIRO' || usuario.role === 'ADMIN'),
        ehAdmin: () => !!usuario && usuario.role === 'ADMIN',

        /**
         * Recupera a sessão no carregamento da página usando o cookie httpOnly.
         * Retorna o usuário ou null, sem lançar erro: visitante não autenticado é normal.
         */
        async restaurarSessao() {
            if (accessToken) {
                return usuario;
            }
            const renovou = await renovar();
            return renovou ? usuario : null;
        },

        async login(email, senha) {
            const dados = await requisitar('/api/auth/login', {
                metodo: 'POST',
                corpo: { email, senha }
            }, false);
            return guardarSessao(dados);
        },

        async registrar(dados) {
            const sessao = await requisitar('/api/auth/registrar', {
                metodo: 'POST',
                corpo: dados
            }, false);
            return guardarSessao(sessao);
        },

        async logout() {
            try {
                await requisitar('/api/auth/logout', { metodo: 'POST' }, false);
            } finally {
                limparSessao();
            }
        },

        servicos: () => requisitar('/api/servicos'),
        barbeiros: () => requisitar('/api/barbeiros'),

        disponibilidade(data, servico, barbeiroId) {
            const params = new URLSearchParams({ data, servico });
            if (barbeiroId) {
                params.set('barbeiroId', barbeiroId);
            }
            return requisitar(`/api/agendamentos/disponibilidade?${params}`);
        },

        agendar: (pedido) => requisitar('/api/agendamentos', { metodo: 'POST', corpo: pedido }),
        meusAgendamentos: (pagina = 0) => requisitar(`/api/agendamentos/meus?page=${pagina}&size=20`),
        cancelar: (id) => requisitar(`/api/agendamentos/${id}/cancelar`, { metodo: 'PATCH' }),
        reagendar: (id, pedido) => requisitar(`/api/agendamentos/${id}`, { metodo: 'PUT', corpo: pedido }),

        agendaDaEquipe(filtros = {}) {
            const params = new URLSearchParams({ page: filtros.pagina || 0, size: 20 });
            ['de', 'ate', 'status', 'barbeiroId'].forEach((chave) => {
                if (filtros[chave]) {
                    params.set(chave, filtros[chave]);
                }
            });
            return requisitar(`/api/agendamentos?${params}`);
        },

        concluir: (id) => requisitar(`/api/agendamentos/${id}/concluir`, { metodo: 'PATCH' }),
        naoCompareceu: (id) => requisitar(`/api/agendamentos/${id}/nao-compareceu`, { metodo: 'PATCH' }),

        /*
         * Administração da equipe (ADMIN). A tela também confere o perfil, mas é só
         * conveniência: quem manda é o token, e a API responde 403 de qualquer forma.
         */
        barbeirosDaAdministracao: () => requisitar('/api/admin/barbeiros'),

        cadastrarBarbeiro: (dados) =>
            requisitar('/api/admin/barbeiros', { metodo: 'POST', corpo: dados }),

        definirSituacaoDoBarbeiro: (id, ativo) =>
            requisitar(`/api/admin/barbeiros/${id}/situacao`, { metodo: 'PATCH', corpo: { ativo } }),

        redefinirSenhaDoBarbeiro: (id, novaSenha) =>
            requisitar(`/api/admin/barbeiros/${id}/senha`, { metodo: 'PATCH', corpo: { novaSenha } }),

        /**
         * Troca a própria senha. A resposta traz uma sessão nova — as outras são
         * revogadas no servidor —, então o token em memória precisa ser substituído,
         * senão a próxima requisição iria com o antigo e levaria 401.
         */
        async alterarSenha(senhaAtual, novaSenha) {
            const sessao = await requisitar('/api/auth/senha', {
                metodo: 'PATCH',
                corpo: { senhaAtual, novaSenha }
            });
            return guardarSessao(sessao);
        }
    };
})();
