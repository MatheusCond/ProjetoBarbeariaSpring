package br.com.projetofatec.barbeariaconde.service;

import br.com.projetofatec.barbeariaconde.config.CriptografiaSenha;
import br.com.projetofatec.barbeariaconde.dto.ClientesDTO;
import br.com.projetofatec.barbeariaconde.model.Clientes;
import br.com.projetofatec.barbeariaconde.repository.ClientesRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientesService {
    private final ClientesRepository clientesRepository;
    private final ModelMapper modelMapper;


    public ClientesDTO cadastrarCliente(ClientesDTO dto) {

        Clientes clientesJaExite = clientesRepository.findByNomeUsuario(dto.getNomeUsuario());

        if(clientesJaExite != null){
            throw new RuntimeException("Usuário já existe");
        }

        Clientes clientesComEmail = clientesRepository.findByEmailUsuario(dto.getEmailUsuario());
        if (clientesComEmail != null) {
            throw new RuntimeException("Email já existe");
        }
        Clientes cliente = modelMapper.map(dto, Clientes.class);

        String senhaCriptografada = CriptografiaSenha.criptografia(cliente.getSenhaUsuario());
        cliente.setSenhaUsuario(senhaCriptografada);

        clientesRepository.save(cliente);

        return modelMapper.map(cliente, ClientesDTO.class);
    }

    public Page<ClientesDTO> buscarClientes(Pageable paginacao) {
        return clientesRepository.findAll(paginacao).map(u -> modelMapper.map(u, ClientesDTO.class));
    }
}
