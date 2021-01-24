package portal.autenticador;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author f9575449
 */
public class LogoutSSO extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public LogoutSSO() {
        super(); // TODO Auto-generated constructor stub
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getSession().removeAttribute("usuarioSSO");
        request.getSession().removeAttribute("tokenId");
        try {
            response.sendRedirect("http://login.intranet.bb.com.br/distAuth/UI/Logout?goto="
                    + ConstantesSSO.PROTOCOLO + "://"
                    + request.getServerName() + ":" + request.getServerPort() + request.getContextPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    }}
