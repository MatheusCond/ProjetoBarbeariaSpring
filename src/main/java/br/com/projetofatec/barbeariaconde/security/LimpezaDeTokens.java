package br.com.projetofatec.barbeariaconde.security;

import br.com.projetofatec.barbeariaconde.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Remove refresh tokens vencidos.
 *
 * <p>Sem isso a tabela cresce para sempre, ja que cada login e cada rotacao inserem uma
 * linha nova. Os tokens sao mantidos por mais um dia depois de expirarem, o que preserva a
 * deteccao de reuso por uma janela curta.
 */
@Component
public class LimpezaDeTokens {

    private static final Logger log = LoggerFactory.getLogger(LimpezaDeTokens.class);

    private final RefreshTokenRepository repository;

    public LimpezaDeTokens(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 15 4 * * *")
    @Transactional
    public void removerExpirados() {
        int removidos = repository.removerExpiradosAntesDe(Instant.now().minus(1, ChronoUnit.DAYS));
        if (removidos > 0) {
            log.info("Limpeza de refresh tokens: {} registro(s) removido(s).", removidos);
        }
    }
}
