package br.com.projetofatec.barbeariaconde.service;


import br.com.projetofatec.barbeariaconde.dto.AgendaDTOUp;
import br.com.projetofatec.barbeariaconde.dto.UsuariosDTO;
import br.com.projetofatec.barbeariaconde.model.Usuarios;
import br.com.projetofatec.barbeariaconde.repository.UsuariosRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import br.com.projetofatec.barbeariaconde.dto.AgendasDTO;
import br.com.projetofatec.barbeariaconde.model.Agendas;
import br.com.projetofatec.barbeariaconde.repository.AgendasRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

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
        agendas.setStatus(1);
        agendas.setCliente(cliente);

        agendasRepository.save(agendas);

        return modelMapper.map(agendas, AgendasDTO.class);
    }

    public AgendasDTO agendarBarba(AgendasDTO dto) {
        Usuarios cliente = usuariosRepository.findById(dto.getClienteEmail())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        Agendas agendas = modelMapper.map(dto, Agendas.class);

        agendas.setServico("Barba");
        agendas.setStatus(1);
        agendas.setCliente(cliente);

        agendasRepository.save(agendas);

        return modelMapper.map(agendas, AgendasDTO.class);
    }

    public AgendasDTO agendarCabeloBarba(AgendasDTO dto) {
        Usuarios cliente = usuariosRepository.findById(dto.getClienteEmail())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        Agendas agendas = modelMapper.map(dto, Agendas.class);

        agendas.setServico("Cabelo e barba");
        agendas.setStatus(1);
        agendas.setCliente(cliente);

        agendasRepository.save(agendas);

        return modelMapper.map(agendas, AgendasDTO.class);
    }

    public Page<AgendasDTO> buscarAgendas(Pageable paginacao) {
        return agendasRepository.findAll(paginacao).map(u -> modelMapper.map(u, AgendasDTO.class));
    }

    @Transactional
    public void deletarAgenda(String email) {
        agendasRepository.deleteByClienteEmail(email);
    }

    @Transactional
    public List<AgendaDTOUp> updateAgenda(String email, AgendaDTOUp dto) {
        List<Agendas> existingAgendas = agendasRepository.findByClienteEmail(email);

        if (existingAgendas.isEmpty()) {
            throw new RuntimeException("Nenhuma agenda encontrada para o email fornecido");
        }

        List<Agendas> updatedAgendas = existingAgendas.stream().map(agenda -> {
            agenda.setServico(dto.getServico());
            agenda.setDataAgenda(dto.getDataAgenda());
            agenda.setHoraAgenda(dto.getHoraAgenda());
            return agendasRepository.save(agenda);
        }).collect(Collectors.toList());

        return updatedAgendas.stream().map(agenda -> modelMapper.map(agenda, AgendaDTOUp.class)).collect(Collectors.toList());
    }
}
