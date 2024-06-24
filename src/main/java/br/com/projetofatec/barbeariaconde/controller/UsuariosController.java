package br.com.projetofatec.barbeariaconde.controller;

import br.com.projetofatec.barbeariaconde.config.ValidationErrorResponse;
import br.com.projetofatec.barbeariaconde.dto.DadosUsuariosDTO;
import br.com.projetofatec.barbeariaconde.dto.UsuariosDTO;
import br.com.projetofatec.barbeariaconde.service.UsuariosService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuariosController {
    private static final Logger logger = LoggerFactory.getLogger(UsuariosController.class);
    private final UsuariosService usuariosService;
    private final AuthenticationManager authenticationManager;


    @PostMapping("/cadastrar-usuarios")
    public ResponseEntity<?> cadastrar(@RequestBody @Valid DadosUsuariosDTO dto, UriComponentsBuilder uriBuilder){
        try {
            DadosUsuariosDTO dadosUsuariosDTO = usuariosService.cadastrarUsuario(dto);

            URI endereco = uriBuilder.path("/usuarios/cadastrar-usuario/{id}").buildAndExpand(dadosUsuariosDTO.getIdUsuario()).toUri();

            return ResponseEntity.created(endereco).body(dadosUsuariosDTO);
        } catch (DataIntegrityViolationException e) {
            String errorMessage = "Erro ao cadastrar usuário: Dados duplicados.";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ValidationErrorResponse(errorMessage));
        }
    }

    @PostMapping("/login-usuarios")
    public ResponseEntity<Void> validationCredentialUser(@RequestBody @Valid UsuariosDTO credential){
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(credential.getNomeUsuario(), credential.getSenhaUsuario());

        Authentication authentication = authenticationManager.authenticate(token);

        return ResponseEntity.ok().build();
    }
}
