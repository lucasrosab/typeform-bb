package portal.autenticador;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.ResponseHandler;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import portal.dao.ConsultasDadosAutenticacao;

/**
 *
 * @author f9575449
 */
public class UtilSSO {
    private String servidorSSO = ConstantesSSO.SERVIDOR_SSO_PADRAO;
    private static final String PREFIXO_ACESSOS = "userdetails.role=id=";
    private static final String PREFIXO_ATRIBUTOS = "userdetails.attribute.name=";
    private static final String USERDETAILS_ATTRIBUTE_VALUE = "userdetails.attribute.value";

    /**
     * Efetua uma chamada GET para o endereço indicado
     *
     * @param uri
     * @return
     * @throws Exception
     */
    private String get(String uri) throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(uri);

        ResponseHandler<String> handler = new ResponseHandler<String>() {
            @Override
            public String handleResponse(HttpResponse proxyResponse) throws ClientProtocolException, IOException {
                final int statusCode = proxyResponse.getStatusLine().getStatusCode();

                if (statusCode == HttpServletResponse.SC_OK) {
                    HttpEntity entity = proxyResponse.getEntity();
                    if (entity != null) {
                        return EntityUtils.toString(entity);
                    } else {
                        return "";
                    }
                } else {
                    throw new ClientProtocolException("Autenticador devolveu código" + statusCode);
                }
            }
        };
        
        return httpclient.execute(httpGet, handler);
    }

    public String getCookieValue(Cookie[] cookies, String nomeCookie) {
        for (Cookie cookie : cookies) {
            if (cookie.getName().equalsIgnoreCase(nomeCookie)) {
                return (cookie.getValue().toString());
            }
        }
        return null;
    }

    public UsuarioSSO getAtributosUsuario(String tokenId) throws Exception {
        String resposta = null;
        boolean tokenValido = false;
        
        ConsultasDadosAutenticacao consultaDadosAutenticacao = new ConsultasDadosAutenticacao();
        
        // Verifica a existencia do token no dia
        try {
            resposta = consultaDadosAutenticacao.constulta_token(tokenId);
        } catch (Exception e) {
            Logger.getLogger(UtilSSO.class.getName()).log(Level.SEVERE, "Causa: {0}", e.getMessage());
        }

        /*
        Se o token não for valido, ou for o primeiro token obtido no dia,
        consulta dados no autenticador
        */
        tokenValido = verificaTokenValido(tokenId);
        
        if (!tokenValido || resposta == null) {
            try {
                resposta = consultaAutenticador(tokenId);
                consultaDadosAutenticacao.insereNovoToken(tokenId, resposta);
            } catch (Exception ex) {
                Logger.getLogger(UtilSSO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return getUsuario(tokenId, resposta);
    }

    public UsuarioSSO getUsuario(String tokenId, String resposta) throws Exception {
        String acessos = null;
        try {            
            // mapeia dados do usuario a partir da resposta 
            Map<String, String> map = getAtributos(resposta);
            acessos = getAcessos(resposta);
            return mapeiaDadosUsuario(tokenId, map, acessos);
        } catch (Exception e) {
            // ou retorna nulo
            Logger.getLogger(UtilSSO.class.getName()).log(Level.SEVERE, "Causa: {0}", e.getMessage());
            return null;
        }
    }
    
    protected UsuarioSSO mapeiaDadosUsuario(String token, Map<String, String> atributos, String acessos) {
        UsuarioSSO usuario = new UsuarioSSO();

        usuario.setTokenId(token);
        usuario.setChave(atributos.get("uid").toUpperCase());
        usuario.setNome(atributos.get("sn"));
        usuario.setEmail(atributos.get("mail"));
        
        // Campo endereco nao esta mais sendo retornado pelo autenticador
        //usuario.setEndereco(atributos.get("homepostaladdress"));        
        usuario.setEndereco("");        
        usuario.setTelefone(atributos.get("telephonenumber"));
        usuario.setAcessos(acessos);

        // outros campos retornados pelo autenticador que nao eram aproveitados
        // na versao do código disponibilizada pela Ditec
        usuario.setCodigoComissao(atributos.get("cd-cmss-usu"));
        usuario.setCpf(atributos.get("nr-cpf"));
        usuario.setCodigoPilar(atributos.get("codigopilar"));
        usuario.setNomeUF(atributos.get("nomeuf"));
        usuario.setPrefixoDependencia(atributos.get("prefixodependencia"));
        usuario.setDescricaoComissao(atributos.get("tx-cmss-usu"));
        usuario.setTipoDependencia(atributos.get("cd-tip-dep"));
        usuario.setPrefixoSuperEstadual(atributos.get("prefixosuperestadual"));
        usuario.setUorDependencia(atributos.get("cd-uor-dep"));
        usuario.setUorTrabalho(atributos.get("cd-eqp"));
        
        usuario.setPrefixoDiretoria(atributos.get("prefixodiretoria"));
        usuario.setNomeGuerra(atributos.get("nomeguerra"));
        usuario.setDisplayName(atributos.get("nomeguerra"));
        usuario.setRf(atributos.get("responsabilidadefuncional"));
        usuario.setNivelCargo(atributos.get("cd-ref-orgc"));
        usuario.setGrupamento(atributos.get("grupamento"));
        usuario.setCodigoInstituicao(atributos.get("cd-ior"));

        Logger.getLogger(UtilSSO.class.getName()).log(Level.FINE, "Consulta efetuada ao OpenAM para o usuário: {0}, {1}", new String[]{usuario.getChave(), usuario.getNome()});
           
        return usuario;
    }

    /*
     * Obtem os dados de um usuario a partir do tokenId do OpenAM
     */
    protected String consultaAutenticador(String tokenId) throws Exception {
        final String queryString = ConstantesSSO.PROTOCOLO + "://" + servidorSSO + "/sso/identity/attributes?subjectid=" + tokenId + "&refresh=true";
        return get(queryString);
    }

    /*
     * Obtem String de acessos, separados por vírgula, a partir da String de resposta do OpenAM
     * Alguns usuarios não tem nenhum acesso, e nesse caso devolvemos a String vazia
     */
    protected String getAcessos(String responseString) {
        String acessosUsuario = "";

        try {
            int inicioAcessos = responseString.indexOf(PREFIXO_ACESSOS);
            int fimAcessos = responseString.indexOf(PREFIXO_ATRIBUTOS);
            String acessos = responseString.substring(inicioAcessos, fimAcessos);
            String splitAcessos[] = acessos.split("\n");

            for (String splitAcesso : splitAcessos) {
                acessosUsuario += splitAcesso.substring(20, splitAcesso.indexOf(",")) + ",";
            }
        } catch (Exception e) {
            Logger.getLogger(UtilSSO.class.getName()).log(Level.WARNING, "Causa: {0}", e.getMessage());
            return null;
        }

        return acessosUsuario;
    }

    /*
     * Obtem um Map de atributos do usuario, a partir da String de resposta do OpenAM
     */
    protected Map<String, String> getAtributos(String responseString) {
        int inicioAtributos = responseString.indexOf(PREFIXO_ATRIBUTOS);
        String atributos = responseString.substring(inicioAtributos);

        String[] propriedades = atributos.split("\n");
        Map<String, String> map = new HashMap<>();

        // Transforma os atributos recebidos em um LinkedHashSet para prevenir itens duplicados
        int i = 0;
        while (i < propriedades.length) {
            if (propriedades[i].startsWith(USERDETAILS_ATTRIBUTE_VALUE)) {
                i++;
                continue;
            }

            String key = propriedades[i];
            String value = propriedades[i + 1].startsWith(USERDETAILS_ATTRIBUTE_VALUE) ? propriedades[i + 1] : "";

            map.put(key.substring(27, key.length()).toLowerCase().trim(), value.substring(28, value.length()).trim());
            i++;
        }

        return map;
    }
    
    public boolean verificaTokenValido(String tokenId) throws Exception {
            final String queryString = ConstantesSSO.PROTOCOLO + "://" + servidorSSO + "/sso/identity/isTokenValid?tokenid=" + tokenId;
            final String response = get(queryString);

            return Boolean.parseBoolean(response.substring(8, response.length()).trim());
    }

    public void logout(String tokenId) {
        try {
            final String queryString = ConstantesSSO.PROTOCOLO + "://" + servidorSSO + "/sso/identity/logout?subjectid=" + tokenId;
            get(queryString);
        } catch (Exception ex) {
            Logger.getLogger(UtilSSO.class.getName()).log(Level.SEVERE, "Causa: {0}", ex.getMessage());
        }
    }

    public boolean authorization(String uri, String tokenId) {
        try {
            final String queryString = ConstantesSSO.PROTOCOLO + "://" + servidorSSO + "/sso/identity/authorize?uri=" + uri + "subjectid=" + tokenId;
            final String response = get(queryString);

            return Boolean.parseBoolean(response.substring(8, response.length()).trim());
        } catch (Exception ex) {
            Logger.getLogger(UtilSSO.class.getName()).log(Level.SEVERE, "Causa: {0}", ex.getMessage());

            return false;
        }
    }

    public void refresh(String tokenId) {
        try {
            final String queryString = ConstantesSSO.PROTOCOLO + "://" + servidorSSO + "/sso/identity/isTokenValid?tokenId=" + tokenId + "&refresh=true";
            get(queryString);
        } catch (Exception ex) {
            Logger.getLogger(UtilSSO.class.getName()).log(Level.SEVERE, "Causa: {0}", ex.getMessage());
        }
    }

    public static String getURL_LOGIN(HttpServletRequest httpServletRequest) {
        String requestUrl = httpServletRequest.getRequestURL().toString();

        // Corrige eventual redirect para o portal da dipes
        requestUrl = requestUrl.replace("/dipes/APPS/", "");

        // Corrige o localhost
        requestUrl = requestUrl.replace("localhost", "localhost.bb.com.br");

        if (httpServletRequest.getQueryString() != null) {
            requestUrl += ("?" + httpServletRequest.getQueryString());
        }

        try {
            requestUrl = URLEncoder.encode(requestUrl, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(UtilSSO.class.getName()).log(Level.SEVERE, null, ex);
        }

        return ConstantesSSO.URL_LOGIN + requestUrl;
    }

    public void setServidorSSO(String servidorSSO) {
        this.servidorSSO = servidorSSO;
    }

    public String getServidorSSO() {
        return servidorSSO;
    }
}
