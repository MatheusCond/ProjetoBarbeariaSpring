package br.com.projetofatec.barbeariaconde.exception;

/** Recurso inexistente ou invisivel para o usuario autenticado (HTTP 404). */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
