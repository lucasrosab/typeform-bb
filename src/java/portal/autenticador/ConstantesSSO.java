package portal.autenticador;

/**
 *
 * @author f9575449
 */
public class ConstantesSSO {
    public static final String PROTOCOLO = "http";
    public static final String DOMINIO_LOGIN = "login.intranet.bb.com.br";
    public static final String NOME_COOKIE_SSO = "BBSSOToken";
    public static final String NOME_COOKIE_ACR = "ssoacr";
    public static final String URL_LOGIN = PROTOCOLO + "://" + DOMINIO_LOGIN + "/distAuth/UI/Login?goto=";
    public static final String SERVIDOR_SSO_PADRAO = "sso.intranet.bb.com.br";
    public static final String URL_VALIDACAO_TOKEN = PROTOCOLO + "://" + SERVIDOR_SSO_PADRAO + "/sso/identity/logout?subjectid=";

    private ConstantesSSO() {
        throw new AssertionError();
    }
}
