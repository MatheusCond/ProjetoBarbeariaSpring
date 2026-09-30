package br.com.projetofatec.barbeariaconde;

import br.com.projetofatec.barbeariaconde.config.AgendaProperties;
import br.com.projetofatec.barbeariaconde.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({JwtProperties.class, AgendaProperties.class})
public class BarbeariacondeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BarbeariacondeApplication.class, args);
    }
}
