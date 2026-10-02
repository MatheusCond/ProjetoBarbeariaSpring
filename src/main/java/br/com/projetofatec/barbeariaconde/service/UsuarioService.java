package br.com.projetofatec.barbeariaconde.service;

import br.com.projetofatec.barbeariaconde.dto.auth.RegistroRequest;
import br.com.projetofatec.barbeariaconde.exception.ConflitoException;
import br.com.projetofatec.barbeariaconde.exception.RecursoNaoEncontradoException;
import br.com.projetofatec.barbeariaconde.exception.RegraNegocioException;
import br.com.projetofatec.barbeariaconde.model.Role;
import br.com.projetofatec.barbeariaconde.model.Usuario;
import br.com.projetofatec.barbeariaconde.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Carrega o usuario pelo e-mail.
     *
     * <p>A versao anterior devolvia {@code null} quando nao encontrava, o que fazia o
     * Spring Security estourar {@code InternalAuthenticationServiceException} (erro 500)
     * em vez de responder 401. Aqui a excecao correta e lancada.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
    }

    @Transactional
    public Usuario registrarCliente(RegistroRequest dto) {
        return criar(dto.nome(), dto.email(), dto.senha(), dto.telefone(), dto.endereco(), Role.CLIENTE);
    }

    @Transactional
    public Usuario criar(String nome, String email, String senha, String telefone, String endereco, Role role) {
        String emailNormalizado = email == null ? null : email.trim().toLowerCase();

        if (emailNormalizado != null && repository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new ConflitoException("Já existe uma conta com este e-mail.");
        }

        Usuario usuario = Usuario.builder()
                .nome(nome == null ? null : nome.trim())
                .email(emailNormalizado)
                .senhaHash(passwordEncoder.encode(senha))
                .telefone(telefone)
                .endereco(endereco)
                .role(role)
                .ativo(true)
                .build();

        try {
            return repository.save(usuario);
        } catch (DataIntegrityViolationException e) {
            // Duas requisicoes com o mesmo e-mail no mesmo instante: a restricao unica decide.
            throw new ConflitoException("Já existe uma conta com este e-mail.");
        }
    }

    @Transactional(readOnly = true)
    public Usuario porEmail(String email) {
        return repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + email));
    }

    @Transactional(readOnly = true)
    public List<Usuario> barbeirosAtivos() {
        return repository.findByRoleAndAtivoTrueOrderByNomeAsc(Role.BARBEIRO);
    }

    @Transactional(readOnly = true)
    public Usuario barbeiroAtivo(Long id) {
        return repository.findByIdAndRoleAndAtivoTrue(id, Role.BARBEIRO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbeiro não encontrado ou inativo."));
    }

    /** Inclui os inativos: a administracao precisa ver quem foi desligado para poder religar. */
    @Transactional(readOnly = true)
    public List<Usuario> barbeirosParaAdministracao() {
        return repository.findByRoleOrderByNomeAsc(Role.BARBEIRO);
    }

    /** Busca pelo id aceitando inativo, ao contrario de {@link #barbeiroAtivo(Long)}. */
    @Transactional(readOnly = true)
    public Usuario barbeiroQualquer(Long id) {
        return buscarBarbeiro(id);
    }

    /**
     * Liga ou desliga o acesso de um profissional.
     *
     * <p>Desativar nao apaga nada: os atendimentos ja marcados continuam no historico e na
     * agenda. O efeito e sobre o acesso ({@link Usuario#isEnabled()}) e sobre quem aparece
     * para receber novas reservas, porque a disponibilidade so considera barbeiros ativos.
     */
    @Transactional
    public Usuario definirSituacaoDoBarbeiro(Long id, boolean ativo) {
        Usuario barbeiro = buscarBarbeiro(id);
        barbeiro.setAtivo(ativo);
        return repository.save(barbeiro);
    }

    /**
     * Troca a senha do proprio usuario, conferindo a atual.
     *
     * <p>A conferencia acontece mesmo com a requisicao autenticada: so o access token nao
     * deve bastar para trocar a credencial e assumir a conta em definitivo.
     *
     * @throws RegraNegocioException se a senha atual nao confere ou se a nova e igual a ela
     */
    @Transactional
    public Usuario alterarSenhaPropria(Long usuarioId, String senhaAtual, String novaSenha) {
        Usuario usuario = porId(usuarioId);

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new RegraNegocioException("A senha atual não confere.");
        }
        if (passwordEncoder.matches(novaSenha, usuario.getSenhaHash())) {
            throw new RegraNegocioException("A nova senha precisa ser diferente da atual.");
        }

        return gravarSenha(usuario, novaSenha);
    }

    /**
     * Define a senha sem conferir a anterior: e a redefinicao feita pelo administrador
     * para quem perdeu o acesso, nao uma troca feita pelo dono da conta.
     */
    @Transactional
    public Usuario redefinirSenha(Long usuarioId, String novaSenha) {
        return gravarSenha(porId(usuarioId), novaSenha);
    }

    @Transactional(readOnly = true)
    public Usuario porId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private Usuario buscarBarbeiro(Long id) {
        return repository.findByIdAndRole(id, Role.BARBEIRO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional não encontrado."));
    }

    /** O hash e recalculado aqui; a senha em texto puro nunca chega ao banco. */
    private Usuario gravarSenha(Usuario usuario, String novaSenha) {
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        return repository.save(usuario);
    }
}
