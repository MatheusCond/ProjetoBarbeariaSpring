package br.com.projetofatec.barbeariaconde.exception;

/** Regra de negocio violada pela requisicao do cliente (HTTP 400). */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
