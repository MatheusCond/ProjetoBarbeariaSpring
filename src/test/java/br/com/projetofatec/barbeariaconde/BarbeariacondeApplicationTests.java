package br.com.projetofatec.barbeariaconde;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BarbeariacondeApplicationTests {

    @Test
    void contextoSobe() {
        // Falha se algum bean, propriedade ou mapeamento JPA estiver inconsistente.
    }
}
