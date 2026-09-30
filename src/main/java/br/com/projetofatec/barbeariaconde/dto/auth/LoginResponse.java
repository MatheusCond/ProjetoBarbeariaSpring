package br.com.projetofatec.barbeariaconde.dto.auth;

/**
 * Resposta de login e de refresh.
 *
 * <p>Somente o access token viaja no corpo: o refresh token vai em cookie httpOnly e
 * por isso nao aparece aqui, nem o JavaScript consegue le-lo.
 *
 * @param expiraEmSegundos vida util do access token, para o front agendar o refresh
 */
public record LoginResponse(
        String accessToken,
        String tipo,
        long expiraEmSegundos,
        UsuarioResponse usuario
) {
    public static LoginResponse de(String accessToken, long expiraEmSegundos, UsuarioResponse usuario) {
        return new LoginResponse(accessToken, "Bearer", expiraEmSegundos, usuario);
    }
}
