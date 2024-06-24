package br.com.projetofatec.barbeariaconde.controller;

import br.com.projetofatec.barbeariaconde.model.Usuarios;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RestController;
import br.com.projetofatec.barbeariaconde.dto.AgendasDTO;
import br.com.projetofatec.barbeariaconde.service.AgendasService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/agendas")
@RequiredArgsConstructor
public class AgendasController {
    private final AgendasService agendaService;

    @PostMapping("/agendar-cabelo")
    public void agendarCabelo(@RequestBody AgendasDTO dto, Authentication authentication){
        agendaService.agendarCabelo(dto);
    }

    @PostMapping("/agendar-barba")
    public void agendarBarba(@RequestBody AgendasDTO dto) {
        agendaService.agendarCabelo(dto);
    }

    @PostMapping("/agendar-cabelo-barba")
    public void agendarCabeloBarba(@RequestBody AgendasDTO dto) {
        agendaService.agendarCabelo(dto);
    }
}