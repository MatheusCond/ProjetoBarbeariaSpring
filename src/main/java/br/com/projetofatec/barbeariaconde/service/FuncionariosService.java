package br.com.projetofatec.barbeariaconde.service;

import br.com.projetofatec.barbeariaconde.dto.FuncionariosDTO;
import br.com.projetofatec.barbeariaconde.model.Funcionarios;
import br.com.projetofatec.barbeariaconde.repository.FuncionariosRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FuncionariosService {
    private final FuncionariosRepository funcionariosRepository;

    private final ModelMapper modelMapper;


    public FuncionariosDTO cadastrarFuncionario(FuncionariosDTO dto) {
        Funcionarios funcionarioJaExiste = funcionariosRepository.findByNomeUsuario(dto.getNomeUsuario());

        if(funcionarioJaExiste != null) {
            throw new RuntimeException("Usuário já existe");
        }

        Funcionarios funcionarios = modelMapper.map(dto, Funcionarios.class);

        funcionariosRepository.save(funcionarios);

        return modelMapper.map(funcionarios, FuncionariosDTO.class);
    }
}
