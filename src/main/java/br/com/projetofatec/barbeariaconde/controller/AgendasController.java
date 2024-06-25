package br.com.projetofatec.barbeariaconde.controller;

import br.com.projetofatec.barbeariaconde.dto.AgendaDTOUp;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import br.com.projetofatec.barbeariaconde.dto.AgendasDTO;
import br.com.projetofatec.barbeariaconde.service.AgendasService;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/agendas")
@RequiredArgsConstructor
public class AgendasController {
    private final AgendasService agendaService;
    private final AgendasService agendasService;

    @PostMapping("/agendar-cabelo")
    public void agendarCabelo(@RequestBody AgendasDTO dto, Authentication authentication){
        agendaService.agendarCabelo(dto);
    }

    @PostMapping("/agendar-barba")
    public void agendarBarba(@RequestBody AgendasDTO dto) {
        agendaService.agendarBarba(dto);
    }

    @PostMapping("/agendar-cabelo-barba")
    public void agendarCabeloBarba(@RequestBody AgendasDTO dto) {
        agendaService.agendarCabeloBarba(dto);
    }

    @GetMapping("/buscar-agendas")
    public Page<AgendasDTO> buscarAgendas(@PageableDefault(size = 10)Pageable paginacao) {
        return agendaService.buscarAgendas(paginacao);
    }

    @DeleteMapping("/deletar-agendas/{email}")
    public void deletarAgendas(@PathVariable @NotNull String email){
        agendaService.deletarAgenda(email);
    }

    @PutMapping("/update-agendas/{email}")
    public List<AgendaDTOUp> updateAgendas(@PathVariable @NotNull String email, @RequestBody @Valid AgendaDTOUp dto) {
        List<AgendaDTOUp> agendasAtualizada = agendasService.updateAgenda(email, dto);

        return agendasAtualizada;
    }
}