package br.com.projetofatec.barbeariaconde.model;

import java.math.BigDecimal;

/**
 * Catálogo de serviços da barbearia. A duração fica junto do serviço porque é ela
 * que define quantos slots da agenda o atendimento ocupa.
 */
public enum Servico {
    CABELO("Cabelo", "Na tesoura ou na máquina, como o cliente preferir", 30, new BigDecimal("25.00")),
    BARBA("Barba", "Corte e desenho de barba profissional", 30, new BigDecimal("25.00")),
    CABELO_E_BARBA("Cabelo e barba", "Pacote completo de cabelo e barba", 60, new BigDecimal("45.00"));

    private final String nome;
    private final String descricao;
    private final int duracaoMinutos;
    private final BigDecimal preco;

    Servico(String nome, String descricao, int duracaoMinutos, BigDecimal preco) {
        this.nome = nome;
        this.descricao = descricao;
        this.duracaoMinutos = duracaoMinutos;
        this.preco = preco;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public BigDecimal getPreco() {
        return preco;
    }
}
