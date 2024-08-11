package br.com.projetofatec.barbeariaconde.config;

import br.com.projetofatec.barbeariaconde.model.Usuarios;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    public String criarToken(Usuarios usuarios){
        try {
            Algorithm algorithm = Algorithm.HMAC256("1234"); //Criando token
            LocalDateTime dataExpiracao = LocalDateTime.now().plusHours(1); // Expiração do token
            return JWT.create()
                    .withIssuer("Barbearia Conde")  //Para quem é este token
                    .withSubject(usuarios.getNomeUsuario()) //Buscando Nome de Usuário para ter o token
                    .withExpiresAt(dataExpiracao.toInstant(ZoneOffset.of("-03:00"))) /*Padrão para definir tipo da expiração,
sempre colocar assim*/
                    .sign(algorithm);

        } catch (JWTCreationException e){
            throw new RuntimeException("Erro ao criar token", e);
        }
    }
}
