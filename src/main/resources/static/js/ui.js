/*
 * Peças de interface compartilhadas entre a home, o agendamento e o painel:
 * ícones, formatação e o cartão de serviço.
 *
 * Os ícones são SVG inline em vez das imagens PNG antigas. Eles herdam a cor do
 * contexto (`currentColor`), escalam sem perder nitidez e não custam requisição.
 */
const UI = (() => {

    const svg = (conteudo, tamanho = 24, preenchido = false) =>
        `<svg viewBox="0 0 24 24" width="${tamanho}" height="${tamanho}" aria-hidden="true"
              fill="${preenchido ? 'currentColor' : 'none'}" stroke="currentColor"
              stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">${conteudo}</svg>`;

    const icones = {
        tesoura: `<circle cx="6" cy="6" r="3"/><circle cx="6" cy="18" r="3"/>
                  <line x1="20" y1="4" x2="8.12" y2="15.88"/>
                  <line x1="14.47" y1="14.48" x2="20" y2="20"/>
                  <line x1="8.12" y1="8.12" x2="12" y2="12"/>`,

        navalha: `<path d="M4.5 4h15l-1.3 4.2H5.8L4.5 4Z"/><path d="M7 8.2h10"/>
                  <path d="M12 8.2V20"/><path d="M10 20h4"/>`,

        // Ferramentas cruzadas, ecoando a tesoura e as navalhas do brasão da marca.
        combo: `<path d="M4.5 3.5 17 17"/><path d="M19.5 3.5 7 17"/>
                <circle cx="5.5" cy="19" r="2.2"/><circle cx="18.5" cy="19" r="2.2"/>`,

        calendario: `<rect x="3" y="5" width="18" height="16" rx="2"/>
                     <path d="M3 10h18M8 3v4M16 3v4"/>`,

        relogio: `<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3.2 2"/>`,

        pessoa: `<circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 3.6-6.5 8-6.5s8 2.5 8 6.5"/>`,

        local: `<path d="M12 21s7-5.6 7-11a7 7 0 1 0-14 0c0 5.4 7 11 7 11Z"/>
                <circle cx="12" cy="10" r="2.6"/>`,

        telefone: `<path d="M21 16.9v2.5a2 2 0 0 1-2.2 2 19.5 19.5 0 0 1-8.5-3 19.2 19.2 0 0 1-5.9-5.9
                   19.5 19.5 0 0 1-3-8.6A2 2 0 0 1 3.4 2H6a2 2 0 0 1 2 1.7c.1 1 .3 1.9.6 2.8a2 2 0 0 1-.4 2.1L7 9.8a16 16 0 0 0 6 6l1.2-1.2a2 2 0 0 1 2.1-.5c.9.3 1.8.5 2.8.6a2 2 0 0 1 1.7 2Z"/>`,

        email: `<rect x="2.5" y="4.5" width="19" height="15" rx="2"/><path d="m3 7 9 6 9-6"/>`,

        instagram: `<rect x="3" y="3" width="18" height="18" rx="5"/><circle cx="12" cy="12" r="4"/>
                    <circle cx="17.2" cy="6.8" r="1" fill="currentColor" stroke="none"/>`,

        whatsapp: `<path d="M3.5 20.5 5 16.4A8.2 8.2 0 1 1 8 19.4l-4.5 1.1Z"/>
                   <path d="M9 9.3c.2 1 .7 2 1.5 2.8.8.8 1.7 1.3 2.7 1.6l.9-1.2 2 .9-.3 1.4c-1.8.4-3.8-.5-5.3-2-1.5-1.5-2.3-3.4-2-5.2l1.4-.3.9 2L9 9.3Z"/>`,

        check: `<path d="m4.5 12.5 5 5 10-11"/>`,

        estrela: `<path d="m12 3.5 2.6 5.6 6 .8-4.4 4.2 1.1 6-5.3-2.9-5.3 2.9 1.1-6L3.4 9.9l6-.8L12 3.5Z"/>`,

        escudo: `<path d="M12 3 5 6v5.5c0 4.3 2.9 8.1 7 9.5 4.1-1.4 7-5.2 7-9.5V6l-7-3Z"/>
                 <path d="m9.2 12 2 2 3.6-4"/>`,

        raio: `<path d="M13.5 3 5 14h6l-.5 7L19 10h-6l.5-7Z"/>`,

        voltar: `<path d="M19 12H5"/><path d="m11 18-6-6 6-6"/>`,

        menu: `<path d="M4 7h16M4 12h16M4 17h16"/>`
    };

    /** Ícone de cada serviço do catálogo, por código do enum do backend. */
    const iconePorServico = {
        CABELO: 'tesoura',
        BARBA: 'navalha',
        CABELO_E_BARBA: 'combo'
    };

    /*
     * Diálogo modal, base de UI.confirmar, UI.perguntar e UI.informar.
     *
     * Monta um <dialog> novo a cada chamada e o descarta ao fechar: não há estado de um
     * diálogo sobrando para o seguinte, e a página não carrega marcação de algo que
     * talvez nunca apareça.
     *
     * O <dialog> nativo resolve sozinho a parte chata — camada acima de tudo, foco preso
     * dentro enquanto estiver aberto e o resto da página inerte.
     *
     * Já o fechamento é conduzido aqui, em `finalizar`, e não deixado por conta do evento
     * `close`. A primeira versão resolvia a promessa dentro desse evento, e isso tinha uma
     * falha séria: há navegador que fecha o diálogo sem disparar `close` nenhum (o do
     * painel embutido deste projeto é um deles). Quando isso acontece, a promessa nunca
     * resolve, quem chamou fica esperando para sempre e a tela trava sem mensagem de erro.
     * Com cada caminho de saída chamando `finalizar`, o resultado não depende de o
     * navegador avisar coisa alguma; o ouvinte de `close` fica só como rede, para um
     * fechamento que não tenha partido daqui.
     */
    function abrirDialogo({
        titulo,
        mensagem = '',
        confirmar = 'Confirmar',
        cancelar = 'Cancelar',
        perigo = false,
        campo = null,
        somenteAviso = false
    }) {
        const paragrafos = (Array.isArray(mensagem) ? mensagem : [mensagem])
            .filter(Boolean)
            .map((texto) => `<p>${texto}</p>`)
            .join('');

        const entrada = campo ? `
            <div class="campo">
                <label for="dialogoCampo">${campo.rotulo}</label>
                <input type="${campo.tipo || 'text'}" id="dialogoCampo" autocomplete="off"
                       placeholder="${campo.dica || ''}">
            </div>
            <p class="dialogo-erro" hidden></p>` : '';

        const dialogo = document.createElement('dialog');
        dialogo.className = 'dialogo';

        // A confirmação vem primeiro no HTML para ser o botão padrão do formulário, e
        // assim Enter dentro do campo de texto confirmar. Na tela ela aparece à direita,
        // onde se espera encontrá-la, porque o CSS inverte a ordem visual.
        dialogo.innerHTML = `
            <form method="dialog">
                <div class="dialogo-corpo">
                    <h2>${titulo}</h2>
                    ${paragrafos}
                    ${entrada}
                </div>
                <div class="dialogo-acoes">
                    <button type="submit" value="confirmar" data-acao="confirmar"
                            class="btn ${perigo ? 'btn--perigo-solido' : 'btn--ouro'}">${confirmar}</button>
                    ${somenteAviso ? '' : `
                    <button type="button" data-acao="cancelar"
                            class="btn btn--contorno">${cancelar}</button>`}
                </div>
            </form>`;

        document.body.appendChild(dialogo);

        const entradaDeTexto = dialogo.querySelector('#dialogoCampo');
        const erro = dialogo.querySelector('.dialogo-erro');

        return new Promise((resolver) => {
            let encerrado = false;

            function finalizar(confirmado) {
                if (encerrado) {
                    return;
                }
                encerrado = true;

                const valor = entradaDeTexto ? entradaDeTexto.value : null;
                if (dialogo.open) {
                    dialogo.close();
                }
                dialogo.remove();

                if (somenteAviso) {
                    resolver();
                } else if (campo) {
                    resolver(confirmado ? valor : null);
                } else {
                    resolver(confirmado);
                }
            }

            // Sempre barra o envio padrão: quem fecha o diálogo é `finalizar`, para o
            // caminho ser o mesmo tenha o navegador o comportamento que tiver.
            dialogo.addEventListener('submit', (evento) => {
                evento.preventDefault();

                if (campo && campo.validar) {
                    const problema = campo.validar(entradaDeTexto.value);
                    if (problema) {
                        erro.textContent = problema;
                        erro.hidden = false;
                        entradaDeTexto.focus();
                        return;
                    }
                }
                finalizar(true);
            });

            const botaoCancelar = dialogo.querySelector('[data-acao="cancelar"]');
            if (botaoCancelar) {
                botaoCancelar.addEventListener('click', () => finalizar(false));
            }

            // Esc desiste. O tratamento é nosso, e não o nativo, pelo mesmo motivo do
            // envio: assim o fechamento e a resposta acontecem juntos, sempre.
            dialogo.addEventListener('keydown', (evento) => {
                if (evento.key === 'Escape') {
                    evento.preventDefault();
                    finalizar(false);
                }
            });

            // Clique no fundo escuro desiste, que é o que se espera de um modal.
            dialogo.addEventListener('click', (evento) => {
                if (evento.target === dialogo) {
                    finalizar(false);
                }
            });

            // Rede de segurança: fechamento vindo de fora daqui.
            dialogo.addEventListener('close', () => finalizar(false));

            dialogo.showModal();

            // Onde o foco começa: no campo quando há um; no cancelar quando a ação
            // destrói algo, para Enter de reflexo não apagar nada; na confirmação no
            // resto dos casos, que é o caminho comum.
            const inicial = entradaDeTexto
                || dialogo.querySelector(`[data-acao="${perigo ? 'cancelar' : 'confirmar'}"]`);
            if (inicial) {
                inicial.focus();
            }
        });
    }


    return {
        icone: (nome, tamanho) => svg(icones[nome] || '', tamanho),

        iconeDoServico: (codigo, tamanho = 30) =>
            svg(icones[iconePorServico[codigo] || 'tesoura'], tamanho),

        preco: (valor) =>
            Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }),

        /** "sábado, 03/10" — dia da semana ajuda o cliente a se situar. */
        data: (iso) => {
            const [ano, mes, dia] = iso.split('-');
            return new Date(Number(ano), Number(mes) - 1, Number(dia))
                .toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: '2-digit' });
        },

        dataCurta: (iso) => {
            const [ano, mes, dia] = iso.split('-');
            return `${dia}/${mes}/${ano}`;
        },

        hora: (iso) => (iso || '').slice(0, 5),

        rotuloStatus: (status) => ({
            AGENDADO: 'Agendado',
            CONCLUIDO: 'Concluído',
            CANCELADO: 'Cancelado',
            NAO_COMPARECEU: 'Não compareceu'
        }[status] || status),

        /**
         * Cartão de serviço, usado na vitrine da home e na etapa 1 do agendamento.
         * Na home é apenas informativo; no agendamento vira um botão de seleção.
         */
        cartaoServico(servico, { selecionavel = false } = {}) {
            const elemento = document.createElement(selecionavel ? 'button' : 'article');
            elemento.className = 'card-servico';
            elemento.dataset.codigo = servico.codigo;
            if (selecionavel) {
                elemento.type = 'button';
                elemento.setAttribute('aria-pressed', 'false');
            }
            elemento.innerHTML = `
                <span class="card-servico-icone">${this.iconeDoServico(servico.codigo)}</span>
                <h3>${servico.nome}</h3>
                <p class="card-servico-desc">${servico.descricao}</p>
                <div class="card-servico-rodape">
                    <span class="card-servico-preco">${this.preco(servico.preco)}</span>
                    <span class="card-servico-duracao">${servico.duracaoMinutos} min</span>
                </div>`;
            return elemento;
        },

        /** Mostra um alerta temporário e devolve o elemento usado. */
        avisar(id, mensagem, tipo = 'info', segundos = 6) {
            const caixa = document.getElementById(id);
            if (!caixa) {
                return null;
            }
            caixa.innerHTML = mensagem;
            caixa.className = `alerta alerta--${tipo}`;
            caixa.hidden = false;
            clearTimeout(caixa.dataset.temporizador);
            caixa.dataset.temporizador = setTimeout(() => (caixa.hidden = true), segundos * 1000);
            return caixa;
        },


        /**
         * Pergunta de sim ou não, no lugar de `confirm()`.
         *
         * @param titulo    uma linha, a pergunta em si
         * @param mensagem  texto ou lista de parágrafos; aceita <strong> para destacar
         *                  o dado que a pessoa precisa conferir antes de decidir
         * @param perigo    ação destrutiva: pinta a confirmação de vermelho e deixa o
         *                  foco no cancelar, para Enter não destruir nada sem querer
         * @returns Promise<boolean>
         */
        confirmar: (opcoes) => abrirDialogo(opcoes),

        /**
         * Pede um texto, no lugar de `prompt()`.
         *
         * @param campo  { rotulo, tipo, dica, validar } — `validar` recebe o valor e
         *               devolve a mensagem de erro, ou nada se estiver tudo certo
         * @returns Promise<string|null> — null quando a pessoa desiste
         */
        perguntar: (opcoes) => abrirDialogo(opcoes),

        /**
         * Recado que só precisa ser lido, no lugar de `alert()`. Sem botão de cancelar,
         * porque não há nada para cancelar.
         *
         * @returns Promise<void>
         */
        informar: (opcoes) => abrirDialogo(Object.assign({ confirmar: 'Entendi' }, opcoes,
            { somenteAviso: true })),

        estadoVazio: (mensagem, erro = false) =>
            `<p class="estado-vazio${erro ? ' erro' : ''}">${mensagem}</p>`
    };
})();
