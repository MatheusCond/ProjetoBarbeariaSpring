package br.com.projetofatec.barbeariaconde.controller;

import br.com.projetofatec.barbeariaconde.dto.agenda.BarbeiroResponse;
import br.com.projetofatec.barbeariaconde.dto.agenda.ServicoResponse;
import br.com.projetofatec.barbeariaconde.model.Servico;
import br.com.projetofatec.barbeariaconde.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Dados publicos usados pela vitrine e pela tela de agendamento: catalogo de servicos
 * (com preco e duracao vindos do enum, nao chumbados no HTML) e lista de barbeiros.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Catálogo", description = "Serviços e profissionais da barbearia")
public class CatalogoController {

    private final UsuarioService usuarioService;

    public CatalogoController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/servicos")
    @SecurityRequirements
    @Operation(summary = "Serviços oferecidos, com duração e preço")
    public List<ServicoResponse> servicos() {
        return Arrays.stream(Servico.values()).map(ServicoResponse::de).toList();
    }

    @GetMapping("/barbeiros")
    @SecurityRequirements
    @Operation(summary = "Barbeiros ativos")
    public List<BarbeiroResponse> barbeiros() {
        return usuarioService.barbeirosAtivos().stream().map(BarbeiroResponse::de).toList();
    }
}
