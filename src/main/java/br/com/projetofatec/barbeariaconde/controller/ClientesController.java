package br.com.projetofatec.barbeariaconde.controller;

import br.com.projetofatec.barbeariaconde.dto.ClientesDTO;
import br.com.projetofatec.barbeariaconde.service.ClientesService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clientes")
public class ClientesController {
    private final ClientesService clientesService;

    @PostMapping("/cadastrar-clientes")
    public ResponseEntity<ClientesDTO> cadastrarCliente(@RequestBody ClientesDTO dto, UriComponentsBuilder uriBuilder){
        ClientesDTO clientesDTO = clientesService.cadastrarCliente(dto);

        URI endereco = uriBuilder.path("/clientes/cadastrar-clientes/{id}").buildAndExpand(clientesDTO.getIdUsuario()).toUri();

        return ResponseEntity.created(endereco).body(clientesDTO);
    }

    @GetMapping("/buscar-clientes")
    public ResponseEntity<Page<ClientesDTO>> buscarClientes(@PageableDefault(size = 10) Pageable paginacao){
        Page<ClientesDTO> pageClientesDTO = clientesService.buscarClientes(paginacao);
        return ResponseEntity.ok(pageClientesDTO);
    }
}
