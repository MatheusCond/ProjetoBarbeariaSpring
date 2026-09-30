package br.com.projetofatec.barbeariaconde.dto.comum;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envelope proprio de paginacao.
 *
 * <p>Serializar {@code PageImpl} direto no corpo da resposta gera aviso do Spring Data
 * (o formato do JSON nao e estavel entre versoes). Este record fixa o contrato da API.
 */
public record PaginaResposta<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        boolean primeira,
        boolean ultima
) {

    public static <E, T> PaginaResposta<T> de(Page<E> page, Function<E, T> mapeador) {
        return new PaginaResposta<>(
                page.getContent().stream().map(mapeador).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
