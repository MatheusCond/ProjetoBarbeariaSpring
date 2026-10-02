package br.com.projetofatec.barbeariaconde.controller;

import br.com.projetofatec.barbeariaconde.dto.admin.BarbeiroAdminResponse;
import br.com.projetofatec.barbeariaconde.dto.admin.NovoBarbeiroRequest;
import br.com.projetofatec.barbeariaconde.dto.admin.RedefinirSenhaRequest;
import br.com.projetofatec.barbeariaconde.dto.admin.SituacaoBarbeiroRequest;
import br.com.projetofatec.barbeariaconde.service.AdministracaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Administracao da equipe, restrita ao perfil ADMIN.
 *
 * <p>O barbeiro tem acesso ao painel de agenda, mas nao a estas rotas: quem atende nao
 * decide quem faz parte da equipe. A restricao esta declarada aqui, por metodo de
 * seguranca, e tambem em {@code SecurityConfig} pelo caminho — se uma anotacao for
 * esquecida em um endpoint novo, a regra de caminho ainda barra.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administração", description = "Cadastro e situação dos profissionais (ADMIN)")
public class AdminController {

    private final AdministracaoService administracaoService;

    public AdminController(AdministracaoService administracaoService) {
        this.administracaoService = administracaoService;
    }

    @GetMapping("/barbeiros")
    @Operation(summary = "Profissionais cadastrados, ativos e inativos",
            description = """
                    Diferente de GET /api/barbeiros, que é público e devolve apenas id e nome
                    dos ativos. Aqui vêm também contato, situação e quantos atendimentos
                    seguem marcados para cada um.""")
    public List<BarbeiroAdminResponse> barbeiros() {
        return administracaoService.listarBarbeiros();
    }

    @PostMapping("/barbeiros")
    @Operation(summary = "Cadastra um profissional com senha inicial")
    public ResponseEntity<BarbeiroAdminResponse> cadastrar(@RequestBody @Valid NovoBarbeiroRequest dto,
                                                           UriComponentsBuilder uriBuilder) {
        BarbeiroAdminResponse criado = administracaoService.cadastrarBarbeiro(dto);
        return ResponseEntity
                .created(uriBuilder.path("/api/admin/barbeiros/{id}").buildAndExpand(criado.id()).toUri())
                .body(criado);
    }

    @PatchMapping("/barbeiros/{id}/situacao")
    @Operation(summary = "Ativa ou desativa o profissional",
            description = """
                    Desativar não apaga nem cancela nada: o profissional deixa de receber
                    novas reservas e perde o acesso, e as sessões abertas dele são revogadas.
                    O campo agendamentosFuturos da resposta diz o que continua marcado.""")
    public BarbeiroAdminResponse situacao(@PathVariable Long id,
                                          @RequestBody @Valid SituacaoBarbeiroRequest dto) {
        return administracaoService.definirSituacao(id, dto.ativo());
    }

    @PatchMapping("/barbeiros/{id}/senha")
    @Operation(summary = "Define uma senha provisória para o profissional",
            description = "Encerra as sessões abertas dele. Não serve para trocar a própria "
                    + "senha: para isso use PATCH /api/auth/senha.")
    public BarbeiroAdminResponse redefinirSenha(@PathVariable Long id,
                                                @RequestBody @Valid RedefinirSenhaRequest dto) {
        return administracaoService.redefinirSenha(id, dto.novaSenha());
    }
}
