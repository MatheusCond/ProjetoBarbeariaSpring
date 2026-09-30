package br.com.projetofatec.barbeariaconde.controller;

import br.com.projetofatec.barbeariaconde.dto.agenda.AgendamentoRequest;
import br.com.projetofatec.barbeariaconde.dto.agenda.AgendamentoResponse;
import br.com.projetofatec.barbeariaconde.dto.agenda.DisponibilidadeResponse;
import br.com.projetofatec.barbeariaconde.dto.agenda.ReagendarRequest;
import br.com.projetofatec.barbeariaconde.dto.comum.PaginaResposta;
import br.com.projetofatec.barbeariaconde.model.Servico;
import br.com.projetofatec.barbeariaconde.model.StatusAgendamento;
import br.com.projetofatec.barbeariaconde.model.Usuario;
import br.com.projetofatec.barbeariaconde.service.AgendamentoService;
import br.com.projetofatec.barbeariaconde.service.DisponibilidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;

/**
 * Endpoints de agendamento.
 *
 * <p>Substituem os tres endpoints quase identicos de antes
 * ({@code /agendar-cabelo}, {@code /agendar-barba}, {@code /agendar-cabelo-barba}) por um
 * unico {@code POST} que recebe o servico, alem de dar a cada operacao o verbo e o status
 * HTTP corretos.
 */
@RestController
@RequestMapping("/api/agendamentos")
@Tag(name = "Agendamentos", description = "Disponibilidade, reserva, remarcação e cancelamento")
public class AgendamentosController {

    private final AgendamentoService agendamentoService;
    private final DisponibilidadeService disponibilidadeService;

    public AgendamentosController(AgendamentoService agendamentoService,
                                  DisponibilidadeService disponibilidadeService) {
        this.agendamentoService = agendamentoService;
        this.disponibilidadeService = disponibilidadeService;
    }

    @GetMapping("/disponibilidade")
    @SecurityRequirements
    @Operation(summary = "Horários livres para um serviço em uma data",
            description = """
                    Aberto a visitantes, para que a agenda possa ser consultada antes do login.
                    Retorna somente horários em que o atendimento inteiro cabe: um serviço de
                    60 minutos exige dois slots consecutivos livres.""")
    public DisponibilidadeResponse disponibilidade(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(defaultValue = "CABELO") Servico servico,
            @RequestParam(required = false) Long barbeiroId) {
        return disponibilidadeService.consultar(data, servico, barbeiroId);
    }

    @PostMapping
    @Operation(summary = "Reserva um horário",
            description = "O cliente é sempre o usuário autenticado; a equipe pode informar clienteEmail.")
    public ResponseEntity<AgendamentoResponse> agendar(@AuthenticationPrincipal Usuario usuario,
                                                      @RequestBody @Valid AgendamentoRequest dto,
                                                      UriComponentsBuilder uriBuilder) {
        AgendamentoResponse criado = AgendamentoResponse.de(agendamentoService.criar(usuario, dto));
        return ResponseEntity
                .created(uriBuilder.path("/api/agendamentos/{id}").buildAndExpand(criado.id()).toUri())
                .body(criado);
    }

    @GetMapping("/meus")
    @Operation(summary = "Histórico de agendamentos do usuário autenticado")
    public PaginaResposta<AgendamentoResponse> meus(
            @AuthenticationPrincipal Usuario usuario,
            @PageableDefault(size = 10) Pageable paginacao) {
        return agendamentoService.meusAgendamentos(usuario, paginacao);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um agendamento (dono, barbeiro do atendimento ou equipe)")
    public AgendamentoResponse porId(@AuthenticationPrincipal Usuario usuario, @PathVariable Long id) {
        return AgendamentoResponse.de(agendamentoService.buscarVisivel(id, usuario));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    @Operation(summary = "Agenda completa da barbearia, com filtros (equipe)")
    public PaginaResposta<AgendamentoResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) StatusAgendamento status,
            @RequestParam(required = false) Long barbeiroId,
            @PageableDefault(size = 20, sort = {"data", "horaInicio"}, direction = Sort.Direction.ASC)
            Pageable paginacao) {
        return agendamentoService.listar(de, ate, status, barbeiroId, paginacao);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Remarca um agendamento específico")
    public AgendamentoResponse reagendar(@AuthenticationPrincipal Usuario usuario,
                                         @PathVariable Long id,
                                         @RequestBody @Valid ReagendarRequest dto) {
        return AgendamentoResponse.de(agendamentoService.reagendar(id, usuario, dto));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancela o agendamento e devolve o horário para a agenda")
    public AgendamentoResponse cancelar(@AuthenticationPrincipal Usuario usuario, @PathVariable Long id) {
        return AgendamentoResponse.de(agendamentoService.cancelar(id, usuario));
    }

    @PatchMapping("/{id}/concluir")
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    @Operation(summary = "Marca o atendimento como realizado (equipe)")
    public AgendamentoResponse concluir(@PathVariable Long id) {
        return AgendamentoResponse.de(agendamentoService.concluir(id));
    }

    @PatchMapping("/{id}/nao-compareceu")
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    @Operation(summary = "Registra que o cliente não compareceu (equipe)")
    public AgendamentoResponse naoCompareceu(@PathVariable Long id) {
        return AgendamentoResponse.de(agendamentoService.registrarNaoComparecimento(id));
    }
}
