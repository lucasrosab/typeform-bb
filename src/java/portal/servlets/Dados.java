package portal.servlets;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import portal.autenticador.UsuarioSSO;
import portal.dao.Consultas;
import portal.util.Log;
// import java.net.InetAddress;
// import java.net.UnknownHostException;
// import portal.autenticador.FiltroSegurancaSSO;
// import portal.util.Log;

/**
 *
 * @author f9575449
 */
@WebServlet(name = "Dados", urlPatterns = {"/dados"})
public class Dados extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        
        // pega o parâmetro da requisição
        // request é um objeto que representa a requisição
        String tipo = request.getParameter("tipo");
        String sql = "";
        Logger.getLogger(Dados.class.getName()).log(Level.INFO, "tipo = {0}", tipo);
        
        // verificamos se há dados na sessão
        HttpSession httpSession = request.getSession();
        UsuarioSSO usuario = (UsuarioSSO) httpSession.getAttribute("usuarioSSO");
        // String NomeMaquina = (String) httpSession.getAttribute("NomeMaquina");
        // String IPMaquina = (String) httpSession.getAttribute("IPMaquina");

        JSONObject info = new JSONObject();
        info.put("tipo", tipo);

        // Grava log de atualização de dados
        Log.GravaLog(usuario.chave, 2, info);
        
        // InetAddress address;
        String IPMaquina = Log.getIPCliente(request);
        Logger.getLogger(Dados.class.getName()).log(Level.INFO, "Nome da máquina: {0}", IPMaquina);
        // String NomeMaquina = "";
        /*
        try {
            IPMaquina = request.getRemoteHost();
            address= InetAddress.getByName(IPMaquina);
            NomeMaquina = address.getHostName();
            Logger.getLogger(Dados.class.getName()).log(Level.INFO, "Nome da máquina: {0} - IP: {1}", new Object[]{NomeMaquina, IPMaquina});
        } catch (UnknownHostException e) {
            Logger.getLogger(Dados.class.getName()).log(Level.SEVERE, "Host desconhecido.", e.getMessage());
        }
        System.out.println("Dados.java: NomeMaquina (" + NomeMaquina + ") - IPMaquina (" + IPMaquina + ")");
        */
        if (tipo.equals("usuarioLogado")) {
            int usuariosLogados = new Consultas().usuariosLogados();
            Logger.getLogger(Dados.class.getName()).log(Level.INFO, "Usuários logados: {0}", usuariosLogados);
            
            boolean Local = IPMaquina.equals("0:0:0:0:0:0:0:1");
            JSONObject ret = new JSONObject();
            if (Local) {
                ret.put("chave", "F9575449");
                ret.put("nomeGuerra", "WAGNER");
                ret.put("nome", "WAGNER DA PAZ OLIVEIRA");
                ret.put("PDC1", true);
                ret.put("PDC2", true);
                ret.put("PDC3", true);
                ret.put("usuariosLogados", usuariosLogados);
            } else {
                ret.put("chave", usuario.chave);
                ret.put("nomeGuerra", usuario.nomeGuerra);
                ret.put("nome", usuario.nome);
                ret.put("PDC1", usuario.getTemAcesso("PDC1"));
                ret.put("PDC2", usuario.getTemAcesso("PDC2"));
                ret.put("PDC3", usuario.getTemAcesso("PDC3"));
                ret.put("usuariosLogados", usuariosLogados);
            }
            
            // Grava log de login
            // Log.GravaLog(usuario.chave, 1, ret);
            Logger.getLogger(Dados.class.getName()).log(Level.INFO, "Usuário logado: {0}", ret.toString());
            response.getWriter().print(ret);
        } else if (tipo.equals("pedidos")) {
            sql = "SELECT * FROM rdd.vw_pedidos;";
        } else if (tipo.equals("lista_ativos")) {
            /* Em teoria, já sabemos do usuário aqui, então basta apenas buscar na sessão o número do pedido */
            String p = (String) httpSession.getAttribute("pedido");
            Logger.getLogger(Dados.class.getName()).log(Level.INFO, "lista_ativos: Pedido = {0}", p);
            if ( !p.isEmpty() ) {
                sql = "SELECT * FROM rdd.vw_resumo WHERE cod_pedido = " + p;
            }
            Logger.getLogger(Dados.class.getName()).log(Level.INFO, "lista_ativos: SQL Gerado = {0}", sql);
        } else if (tipo.equals("lista_inativos")) {
            sql = "";
        } else if (tipo.equals("listaclientes")) {
            sql = "SELECT * FROM rdd.tb_grupo_cliente;";
        } else if (tipo.equals("listarelatorios")) {
            sql = "SELECT * FROM rdd.tb_relatorio;";
        } else if (tipo.equals("clientes")) {
            sql = "SELECT * FROM rdd.vw_lista_clientes";
        } else if (tipo.equals("clientenovo")) {
            sql = "SELECT * FROM rdd.vw_cliente_padrao";
        } else if (tipo.equals("relatorios")) {
            sql = "SELECT * FROM rdd.vw_lista_relatorios";
        } else if (tipo.equals("relatorionovo")) {
            sql = "SELECT * FROM rdd.vw_relatorio_padrao";
        } else if (tipo.equals("informacoes")) {
            sql = "SELECT * FROM rdd.vw_lista_informacoes";
        } else if (tipo.equals("listas")) {
            sql = "SELECT * FROM rdd.vw_listas";
        } else if (tipo.equals("informacaonova")) {
            sql = "SELECT * FROM rdd.vw_informacao_padrao";
        } else if (tipo.equals("relacoes")) {
            sql = "SELECT * FROM rdd.tb_relacao";
        } else {
            Logger.getLogger(Dados.class.getName()).log(Level.SEVERE, "ATENÇÃO: tipo = '" + tipo + "'");
        }
        
        if (!sql.equals("")) {
            JSONArray retorno;
            retorno = new Consultas().retornaSQL(sql);
            Logger.getLogger(Dados.class.getName()).log(Level.INFO, "\nSQL enviado: \n" + sql + "\nRetorno desse SQL: \n" + retorno.toJSONString());
            response.getWriter().print(retorno);
        }
    }
    
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Logger.getLogger(Dados.class.getName()).log(Level.INFO, "doGet: tipo = {0}", request.getParameter("tipo"));
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Logger.getLogger(Dados.class.getName()).log(Level.INFO, "doPost: tipo = {0}", request.getParameter("tipo"));
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
