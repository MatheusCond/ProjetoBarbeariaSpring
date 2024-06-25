function openModal(service) {
    var modal = document.getElementById('modal' + service.replace(/ /g, ''));
    modal.style.display = "block";
}

function closeModal(service) {
    var modal = document.getElementById('modal' + service.replace(/ /g, ''));
    modal.style.display = "none";
}

function agendarCabelo() {
    var data = document.getElementById('dataCabelo').value;
    var horario = document.getElementById('horarioCabelo').value;
    var email = document.getElementById('emailCabelo').value;

    var dataHora = {
        dataAgenda: data,
        horaAgenda: horario,
        clienteEmail: email
    };

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "/agendas/agendar-cabelo");
    xhr.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
    xhr.onload = function () {
        if (xhr.readyState === XMLHttpRequest.DONE) {
            if (xhr.status === 200) {
                fetchAgendas();
                closeModal('Cabelo');
            } else {
                console.error('Erro ao enviar requisição: ' + xhr.status);
            }
        }
    };
    xhr.send(JSON.stringify(dataHora));
}

function agendarBarba() {
    var data = document.getElementById('dataBarba').value;
    var horario = document.getElementById('horarioBarba').value;
    var email = document.getElementById('emailBarba').value;

    var dataHora = {
        dataAgenda: data,
        horaAgenda: horario,
        clienteEmail: email
    };

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "/agendas/agendar-barba");
    xhr.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
    xhr.onload = function () {
        if (xhr.readyState === XMLHttpRequest.DONE) {
            if (xhr.status === 200) {
                fetchAgendas();
                closeModal('Barba');
            } else {
                console.error('Erro ao enviar requisição: ' + xhr.status);
            }
        }
    };
    xhr.send(JSON.stringify(dataHora));
}

function agendarCabeloBarba() {
    var data = document.getElementById('dataCabeloBarba').value;
    var horario = document.getElementById('horarioCabeloBarba').value;
    var email = document.getElementById('emailCabeloBarba').value;

    var dataHora = {
        dataAgenda: data,
        horaAgenda: horario,
        clienteEmail: email
    };

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "/agendas/agendar-cabelo-barba");
    xhr.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
    xhr.onload = function () {
        if (xhr.readyState === XMLHttpRequest.DONE) {
            if (xhr.status === 200) {
                fetchAgendas();
                closeModal('CabeloBarba');
            } else {
                console.error('Erro ao enviar requisição: ' + xhr.status);
            }
        }
    };
    xhr.send(JSON.stringify(dataHora));
}

document.addEventListener("DOMContentLoaded", function() {
    fetchAgendas();
});

function fetchAgendas() {
    var xhr = new XMLHttpRequest();
    xhr.open("GET", "/agendas/buscar-agendas");
    xhr.onload = function () {
        if (xhr.readyState === XMLHttpRequest.DONE && xhr.status === 200) {
            var agendas = JSON.parse(xhr.responseText);
            displayAgendas(agendas);
        }
    };
    xhr.send();
}

function displayAgendas(agendasResponse) {
    var agendas = agendasResponse.content;

    var agendasDiv = document.getElementById('agendas');
    agendasDiv.innerHTML = '';

    if (Array.isArray(agendas)) {
        agendas.forEach(function(agenda) {
            var agendaDiv = document.createElement('div');
            agendaDiv.classList.add('agenda-item');
            agendaDiv.innerHTML =
                `<h3>${agenda.servico}</h3>
                <p>Data: ${agenda.dataAgenda}</p>
                <p>Horário: ${agenda.horaAgenda}</p>
                <p data-type="email">${agenda.clienteEmail}</p>
                <button onclick="openEditarModal('${agenda.idAgenda}', '${agenda.servico}', '${agenda.dataAgenda}', '${agenda.horaAgenda}', '${agenda.clienteEmail}')">Editar</button>
                <button onclick="deletarAgenda(this)">Deletar</button>`;
            agendasDiv.appendChild(agendaDiv);

            agendaDiv.querySelector('h3').textContent = agenda.servico;
        });
    } else {
        console.error('O conteúdo de agendas não é um array:', agendas);
    }
}


function deletarAgenda(buttonElement) {
    var emailElement = buttonElement.parentElement.querySelector('p[data-type="email"]');
    var email = emailElement.textContent.trim();

    var xhr = new XMLHttpRequest();
    xhr.open("DELETE", "/agendas/deletar-agendas/" + encodeURIComponent(email));
    xhr.onload = function () {
        if (xhr.readyState === XMLHttpRequest.DONE) {
            if (xhr.status === 200) {
                fetchAgendas();
            } else {
                console.error('Erro ao deletar agenda: ' + xhr.status);
            }
        }
    };
    xhr.send();
}

function openEditarModal(idAgenda, servico, dataAgenda, horaAgenda, clienteEmail) {
    var modal = document.getElementById('modalEditarAgenda');
    var form = document.getElementById('formEditarAgenda');

    form.idAgenda.value = idAgenda;
    form.servico.value = servico;
    form.dataAgenda.value = dataAgenda;
    form.horaAgenda.value = horaAgenda;
    var emailElement = document.getElementById('emailAgendaEditar');
    emailElement.textContent = clienteEmail;

    modal.style.display = "block";
}

function salvarEdicao() {
    var form = document.getElementById('formEditarAgenda');
    var idAgenda = form.idAgenda.value;
    var servico = form.servico.value;
    var dataAgenda = form.dataAgenda.value;
    var horaAgenda = form.horaAgenda.value;
    var clienteEmail = document.getElementById('emailAgendaEditar').textContent.trim();

    var dataHora = {
        idAgenda: idAgenda,
        servico: servico,
        dataAgenda: dataAgenda,
        horaAgenda: horaAgenda,
        clienteEmail: clienteEmail
    };

    var xhr = new XMLHttpRequest();
    xhr.open("PUT", "/agendas/update-agendas/" + encodeURIComponent(clienteEmail));
    xhr.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
    xhr.onload = function () {
        if (xhr.readyState === XMLHttpRequest.DONE) {
            if (xhr.status === 200) {
                fetchAgendas();
                closeModal('EditarAgenda');
            } else {
                console.error('Erro ao enviar requisição: ' + xhr.status);
            }
        }
    };
    xhr.send(JSON.stringify(dataHora));
}