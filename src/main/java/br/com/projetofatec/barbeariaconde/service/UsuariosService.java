package br.com.projetofatec.barbeariaconde.service;

import br.com.projetofatec.barbeariaconde.config.CriptografiaSenha;
import br.com.projetofatec.barbeariaconde.dto.DadosUsuariosDTO;
import br.com.projetofatec.barbeariaconde.dto.UsuariosDTO;
import br.com.projetofatec.barbeariaconde.model.Usuarios;
import br.com.projetofatec.barbeariaconde.repository.UsuariosRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuariosService implements UserDetailsService {

    private final UsuariosRepository usuariosRepository;

    private final ModelMapper modelMapper;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        return usuariosRepository.findByNomeUsuario(login);
    }

    public DadosUsuariosDTO cadastrarUsuario(DadosUsuariosDTO dto) {
        if (usuariosRepository.existsByEmailUsuario(dto.getEmailUsuario())) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }

        if (usuariosRepository.existsByNomeUsuario(dto.getNomeUsuario())) {
            throw new IllegalArgumentException("Nome de usuário já cadastrado");
        }
        Usuarios usuarios = modelMapper.map(dto, Usuarios.class);

        String senhaCriptografada = CriptografiaSenha.criptografia(usuarios.getSenhaUsuario());
        usuarios.setSenhaUsuario(senhaCriptografada);

        usuariosRepository.save(usuarios);

        return modelMapper.map(usuarios, DadosUsuariosDTO.class);
    }

    public Page<UsuariosDTO> buscarUsuarios(Pageable paginacao) {
        return usuariosRepository.findAll(paginacao).map(u -> modelMapper.map(u, UsuariosDTO.class));
    }

}