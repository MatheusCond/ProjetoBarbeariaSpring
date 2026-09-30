/*
 * Home: monta as partes que valem a pena vir de um lugar só.
 *
 * Os serviços são buscados na API para que preço e duração exibidos na vitrine
 * sejam os mesmos que o agendamento vai aplicar — antes estavam escritos à mão no
 * HTML e podiam divergir do backend sem ninguém perceber.
 */
const DIFERENCIAIS = [
    {
        icone: 'estrela',
        titulo: 'Atendimento ao cliente',
        texto: 'Cada horário é de uma pessoa só. Você entra na hora marcada.'
    },
    {
        icone: 'tesoura',
        titulo: 'Profissional qualificado',
        texto: 'Corte na tesoura ou na máquina, desenho de barba e acabamento caprichado.'
    },
    {
        icone: 'local',
        titulo: 'Bem localizado',
        texto: 'No centro de Bady Bassitt, com fácil acesso e estacionamento por perto.'
    },
    {
        icone: 'escudo',
        titulo: 'Ambiente diferenciado',
        texto: 'Espaço pensado para o cliente esperar bem e sair satisfeito.'
    }
];

const CONTATOS = [
    { icone: 'local', rotulo: 'Endereço', texto: 'Bady Bassitt — São Paulo' },
    { icone: 'whatsapp', rotulo: 'WhatsApp', texto: '(17) 99242-6593', href: 'https://wa.me/5517992426593' },
    { icone: 'email', rotulo: 'E-mail', texto: 'tcondesiq299@hotmail.com', href: 'mailto:tcondesiq299@hotmail.com' },
    { icone: 'instagram', rotulo: 'Instagram', texto: '@barbeariaconde_', href: 'https://instagram.com/barbeariaconde_' },
    { icone: 'relogio', rotulo: 'Horário', texto: 'Ter a sex 09h–19h · Sáb 08h–18h' }
];

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('ano').textContent = new Date().getFullYear();
    montarDiferenciais();
    montarContatos();
    carregarServicos();
});

async function carregarServicos() {
    const grade = document.getElementById('servicosHome');
    try {
        const servicos = await API.servicos();
        grade.innerHTML = '';
        servicos.forEach((servico) => grade.appendChild(UI.cartaoServico(servico)));
    } catch {
        grade.innerHTML = UI.estadoVazio('Não foi possível carregar os serviços agora.', true);
    }
}

function montarDiferenciais() {
    const grade = document.getElementById('diferenciais');
    grade.innerHTML = DIFERENCIAIS.map((item) => `
        <article class="diferencial">
            ${UI.icone(item.icone, 30)}
            <h3>${item.titulo}</h3>
            <p>${item.texto}</p>
        </article>`).join('');
}

function montarContatos() {
    const lista = document.getElementById('contatoLista');
    lista.innerHTML = CONTATOS.map((item) => {
        const valor = item.href
            ? `<a href="${item.href}" target="_blank" rel="noopener">${item.texto}</a>`
            : `<span>${item.texto}</span>`;
        return `<li>${UI.icone(item.icone, 22)}<div><strong>${item.rotulo}</strong>${valor}</div></li>`;
    }).join('');
}
