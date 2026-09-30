package br.com.projetofatec.barbeariaconde.exception;

/** Horario ja ocupado ou estado incompativel com a operacao (HTTP 409). */
public class ConflitoException extends RuntimeException {
    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
