package portal.autenticador;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import portal.util.Log;

/**
 *
 * @author f9575449
 */
public class FiltroSegurancaSSO implements Filter {
    public FiltroSegurancaSSO() {
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub
    }

    /*
    Faz o filtro verificando se o usuário está logado e se não estiver, redireciona para o autenticador. 
    Após autenticado, popula o objeto UsuarioSSO e o insere na sessão.
    */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        boolean loginRequested = false;

        if (response instanceof HttpServletResponse && request instanceof HttpServletRequest) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;

            /*
            No caso de uma requisição do tipo OPTIONS não redireciona a chamada para o Autenticador
            e apenas propaga diretamente a cadeia de filtros
            */
            if (!httpServletRequest.getMethod().equals("OPTIONS")) {
                HttpSession httpSession = httpServletRequest.getSession();
                UsuarioSSO usuario;

                usuario = (UsuarioSSO) httpSession.getAttribute("usuarioSSO");
                Long lastRequest = (Long) httpSession.getAttribute("LastRequestOpenAM");

                if (usuario == null || (lastRequest != null && System.currentTimeMillis() - lastRequest > 1000 * 60 * 2.9)) {
                    Cookie cookies[] = httpServletRequest.getCookies();
                    UtilSSO ssoUtil = new UtilSSO();

                    if (cookies != null) {
                        String tokenId = ssoUtil.getCookieValue(cookies, ConstantesSSO.NOME_COOKIE_SSO);

                        if (tokenId == null) {
                            enviaPaginaLogin((HttpServletResponse) response, httpServletRequest);

                            // Registro que a página de login foi requisitada para que a cadeia de filtros não
                            // seja propagada
                            loginRequested = true;
                        } else {
                            ssoUtil.setServidorSSO(ssoUtil.getCookieValue(cookies, ConstantesSSO.NOME_COOKIE_ACR));
                            try {
                                // Aqui onde são consultados os dados do usuario
                                usuario = ssoUtil.getAtributosUsuario(tokenId);
                            } catch (Exception ex) {
                                Logger.getLogger(FiltroSegurancaSSO.class.getName()).log(Level.SEVERE, "Não foi possível recuperar os atributos do usuário.", ex.getMessage());
                                enviaPaginaLogin((HttpServletResponse) response, httpServletRequest);                            
                                loginRequested = true;
                            }

                            if (usuario == null) {
                                enviaPaginaLogin((HttpServletResponse) response, httpServletRequest);

                                // Registro que a página de login foi requisitada para que a cadeia de filtros não
                                // seja propagada
                                loginRequested = true;
                            }

                            // InetAddress address;
                            String IPMaquina = Log.getIPCliente(httpServletRequest);
                            String NomeMaquina = "";
                            try {
                                NomeMaquina = InetAddress.getByName(IPMaquina).getHostName();
                                // NomeMaquina = address.getHostName();
                                Logger.getLogger(FiltroSegurancaSSO.class.getName()).log(Level.INFO, "Nome da máquina: {0} - IP: {1}", new Object[]{NomeMaquina, IPMaquina});
                            } catch (UnknownHostException e) {
                                Logger.getLogger(FiltroSegurancaSSO.class.getName()).log(Level.SEVERE, "Host desconhecido.", e.getMessage());
                            }

                            httpSession.setAttribute("usuarioSSO", usuario);
                            httpSession.setAttribute("LastRequestOpenAM", (Long) System.currentTimeMillis());
                            httpSession.setAttribute("NomeMaquina", NomeMaquina);
                            httpSession.setAttribute("IPMaquina", IPMaquina);
                        }
                    } else {

                        Logger.getLogger(FiltroSegurancaSSO.class.getName()).log(Level.WARNING, "Não existe Cookie SSO. (ln 103)");
                        enviaPaginaLogin((HttpServletResponse) response, httpServletRequest);

                        // Registro que a página de login foi requisitada para que a cadeia de filtros não
                        // seja propagada
                        loginRequested = true;
                    }
                }
            }
        }
        try {
            if (!loginRequested) {
                chain.doFilter(request, response);
            }
        } catch (NullPointerException e) {
            Logger.getLogger(FiltroSegurancaSSO.class.getName()).log(Level.SEVERE, "Não foi possível recuperar os atributos do usuário.", e.getMessage());
        } catch (Throwable t) {
            Logger.getLogger(FiltroSegurancaSSO.class.getName()).log(Level.SEVERE, "Não foi possível recuperar os atributos do usuário.", t);
        }
    }

    @Override
    public void init(FilterConfig fConfig)
            throws ServletException {// TODO Auto-generated method stub
    }

    /**
     * Direciona o browser para a página de login caso não se trate de uma
     * chamada Ajax ou retorna o erro 312 indicando para o cliente que é
     * necessário autenticação para completar uma chamada Ajax.
     *
     * @param response dados da resposta da requisição HTTP
     * @param request dados da requisição HTTP
     */
    public void enviaPaginaLogin(HttpServletResponse response, HttpServletRequest request) {
        // Verifica se há um cabeçalho HTTP indicando que a requisição foi efetuada via Ajax
        if (!isAjaxRequest(request)) {
            try {
                response.sendRedirect(UtilSSO.getURL_LOGIN(request));
            } catch (IOException ex) {
                Logger.getLogger(FiltroSegurancaSSO.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            // Retorna o erro 312 indicando que a autenticação foi requisitada em uma chamada Ajax
            try {
                response.sendError(312, "Autenticação requisitada em chamada AJAX");
            } catch (IOException ex) {
                Logger.getLogger(FiltroSegurancaSSO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Verifica se há o cabeçalho customizado Request-Method na requisição HTTP
     * indicando se a chamada foi efetuada via Ajax ou não.
     *
     * @param request dados da requisição HttpServletRequest
     *
     * @return true se foi indicado que a chamada foi efetuada via ajax, false
     * caso contrário.
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestMethod = request.getHeader("Request-Method");

        if (requestMethod != null && requestMethod.trim().equalsIgnoreCase("ajax")) {
            return true;
        } else {
            return false;
        }
    }
}
