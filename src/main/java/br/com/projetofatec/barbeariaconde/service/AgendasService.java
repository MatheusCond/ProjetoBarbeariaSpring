package br.com.projetofatec.barbeariaconde.service;

import br.com.projetofatec.barbeariaconde.model.Clientes;
import br.com.projetofatec.barbeariaconde.model.Usuarios;
import br.com.projetofatec.barbeariaconde.repository.ClientesRepository;
import br.com.projetofatec.barbeariaconde.repository.UsuariosRepository;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import br.com.projetofatec.barbeariaconde.dto.AgendasDTO;
import br.com.projetofatec.barbeariaconde.model.Agendas;
import br.com.projetofatec.barbeariaconde.repository.AgendasRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgendasService {
    private final AgendasRepository agendasRepository;
    private final UsuariosRepository usuariosRepository;
    private final ModelMapper modelMapper;



    public AgendasDTO agendarCabelo(AgendasDTO dto) {
        Usuarios cliente = usuariosRepository.findById(dto.getClienteEmail())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        Agendas agendas = modelMapper.map(dto, Agendas.class);

        agendas.setServico("Cabelo");
        agendas.setCliente(cliente);

        agendasRepository.save(agendas);

        return modelMapper.map(agendas, AgendasDTO.class);
    }

    public AgendasDTO agendarBarba(AgendasDTO dto) {
        Usuarios cliente = usuariosRepository.findById(dto.getClienteEmail())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        Agendas agendas = modelMapper.map(dto, Agendas.class);

        agendas.setServico("Barba");
        agendas.setCliente(cliente);

        agendasRepository.save(agendas);

        return modelMapper.map(agendas, AgendasDTO.class);
    }

    public AgendasDTO agendarCabeloBarba(AgendasDTO dto) {
        Usuarios cliente = usuariosRepository.findById(dto.getClienteEmail())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        Agendas agendas = modelMapper.map(dto, Agendas.class);

        agendas.setServico("Cabelo e barba");
        agendas.setCliente(cliente);

        agendasRepository.save(agendas);

        return modelMapper.map(agendas, AgendasDTO.class);
    }
}
