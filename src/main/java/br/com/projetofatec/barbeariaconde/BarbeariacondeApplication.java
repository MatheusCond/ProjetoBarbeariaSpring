package br.com.projetofatec.barbeariaconde;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class BarbeariacondeApplication {

	public static void main(String[] args) {
		SpringApplication.run(BarbeariacondeApplication.class, args);
	}
}
