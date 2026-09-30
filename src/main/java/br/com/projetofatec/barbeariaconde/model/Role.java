package br.com.projetofatec.barbeariaconde.model;

/**
 * Perfis de acesso da aplicação. O prefixo {@code ROLE_} é o que o Spring Security
 * espera encontrar nas authorities quando usamos {@code hasRole(...)}.
 */
public enum Role {
    CLIENTE,
    BARBEIRO,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }

    public boolean isEquipe() {
        return this == BARBEIRO || this == ADMIN;
    }
}
