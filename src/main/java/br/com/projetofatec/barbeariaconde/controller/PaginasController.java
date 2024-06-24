package br.com.projetofatec.barbeariaconde.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

@RestController
public class PaginasController {

    @GetMapping(value = "/cadastro", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> paginaCadastro() throws IOException {
        Resource resource = new ClassPathResource("static/cadastro.html");
        InputStream inputStream = resource.getInputStream();
        return ResponseEntity.ok().body(new InputStreamResource(inputStream));
    }
}
