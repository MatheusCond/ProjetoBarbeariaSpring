package br.com.projetofatec.barbeariaconde.controller;

import br.com.projetofatec.barbeariaconde.dto.auth.AlterarSenhaRequest;
import br.com.projetofatec.barbeariaconde.dto.auth.LoginRequest;
import br.com.projetofatec.barbeariaconde.dto.auth.LoginResponse;
import br.com.projetofatec.barbeariaconde.dto.auth.RegistroRequest;
import br.com.projetofatec.barbeariaconde.dto.auth.UsuarioResponse;
import br.com.projetofatec.barbeariaconde.model.Usuario;
import br.com.projetofatec.barbeariaconde.security.CookieRefresh;
import br.com.projetofatec.barbeariaconde.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Cadastro, login, renovação e encerramento de sessão")
public class AuthController {

    private final AuthService authService;
    private final CookieRefresh cookieRefresh;

    public AuthController(AuthService authService, CookieRefresh cookieRefresh) {
        this.authService = authService;
        this.cookieRefresh = cookieRefresh;
    }

    @PostMapping("/registrar")
    @SecurityRequirements
    @Operation(summary = "Cria uma conta de cliente e já devolve a sessão")
    public ResponseEntity<LoginResponse> registrar(@RequestBody @Valid RegistroRequest dto) {
        return responder(authService.registrar(dto), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Autentica por e-mail e senha",
            description = """
                    Devolve o access token no corpo e o refresh token em cookie httpOnly.
                    O front deve manter o access token em memória e chamar /api/auth/refresh
                    quando ele expirar.""")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest dto) {
        return responder(authService.login(dto), HttpStatus.OK);
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Troca o refresh token do cookie por um novo access token",
            description = "O refresh token é rotacionado: o anterior deixa de valer imediatamente.")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
        String recebido = cookieRefresh.ler(request)
                .orElseThrow(() -> new BadCredentialsException("Sessão não encontrada."));
        return responder(authService.renovar(recebido), HttpStatus.OK);
    }

    @PostMapping("/logout")
    @SecurityRequirements
    @Operation(summary = "Revoga o refresh token e apaga o cookie")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        cookieRefresh.ler(request).ifPresent(authService::logout);
        return ResponseEntity.noContent()
                .header(cookieRefresh.nomeDoCabecalho(), cookieRefresh.cabecalhoDeRemocao())
                .build();
    }

    @PostMapping("/logout-global")
    @Operation(summary = "Encerra a sessão em todos os dispositivos")
    public ResponseEntity<Void> logoutGlobal(@AuthenticationPrincipal Usuario usuario) {
        authService.logoutGlobal(usuario);
        return ResponseEntity.noContent()
                .header(cookieRefresh.nomeDoCabecalho(), cookieRefresh.cabecalhoDeRemocao())
                .build();
    }

    @PatchMapping("/senha")
    @Operation(summary = "Troca a própria senha",
            description = """
                    Exige a senha atual, mesmo com a requisição autenticada. As demais sessões
                    do usuário são encerradas e esta recebe um par de tokens novo, então quem
                    está trocando a senha não precisa entrar de novo.""")
    public ResponseEntity<LoginResponse> alterarSenha(@AuthenticationPrincipal Usuario usuario,
                                                      @RequestBody @Valid AlterarSenhaRequest dto) {
        return responder(authService.alterarSenha(usuario, dto), HttpStatus.OK);
    }

    @GetMapping("/eu")
    @Operation(summary = "Dados do usuário autenticado")
    public UsuarioResponse eu(@AuthenticationPrincipal Usuario usuario) {
        return UsuarioResponse.de(usuario);
    }

    private ResponseEntity<LoginResponse> responder(AuthService.Sessao sessao, HttpStatus status) {
        return ResponseEntity.status(status)
                .header(cookieRefresh.nomeDoCabecalho(), cookieRefresh.cabecalhoDeCriacao(sessao.refreshToken()))
                .body(sessao.corpo());
    }
}
