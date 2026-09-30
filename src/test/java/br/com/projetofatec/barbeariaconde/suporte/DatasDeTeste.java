package br.com.projetofatec.barbeariaconde.suporte;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Datas relativas ao dia de execucao, para os testes nao quebrarem com o passar do tempo
 * nem depender do dia da semana em que a suite roda.
 */
public final class DatasDeTeste {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private DatasDeTeste() {
    }

    public static LocalDate hoje() {
        return LocalDate.now(FUSO);
    }

    /** Proxima ocorrencia do dia da semana, sempre a pelo menos 2 dias de distancia. */
    public static LocalDate proximo(DayOfWeek dia) {
        LocalDate data = hoje().plusDays(2);
        while (data.getDayOfWeek() != dia) {
            data = data.plusDays(1);
        }
        return data;
    }

    /** Dia util em que a barbearia abre (quarta-feira, 09:00 as 19:00). */
    public static LocalDate proximaQuarta() {
        return proximo(DayOfWeek.WEDNESDAY);
    }

    /** Dia em que a barbearia esta fechada. */
    public static LocalDate proximaSegunda() {
        return proximo(DayOfWeek.MONDAY);
    }
}
