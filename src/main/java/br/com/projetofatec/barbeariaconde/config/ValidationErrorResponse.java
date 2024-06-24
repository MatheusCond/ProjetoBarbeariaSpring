package br.com.projetofatec.barbeariaconde.config;

public class ValidationErrorResponse {

    private String message;

    public ValidationErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}


