package br.com.projetofatec.barbeariaconde.controller;

import br.com.projetofatec.barbeariaconde.dto.FuncionariosDTO;
import br.com.projetofatec.barbeariaconde.service.FuncionariosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/funcionarios")
public class FuncionariosController {
    private final FuncionariosService funcionariosService;

    @PostMapping("/cadastrar-funcionarios")
    public ResponseEntity<FuncionariosDTO> cadastrarFuncionario(@RequestBody FuncionariosDTO dto, UriComponentsBuilder uriBuilder){
        FuncionariosDTO funcionariosDTO = funcionariosService.cadastrarFuncionario(dto);

        URI endereco = uriBuilder.path("/funcionarios/cadastrar-funcionarios/{id}").buildAndExpand(funcionariosDTO.getIdUsuario()).toUri();

        return ResponseEntity.created(endereco).body(funcionariosDTO);
    }
}
