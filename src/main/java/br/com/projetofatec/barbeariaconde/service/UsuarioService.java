package br.com.projetofatec.barbeariaconde.service;

import br.com.projetofatec.barbeariaconde.dto.auth.RegistroRequest;
import br.com.projetofatec.barbeariaconde.exception.ConflitoException;
import br.com.projetofatec.barbeariaconde.exception.RecursoNaoEncontradoException;
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
}
