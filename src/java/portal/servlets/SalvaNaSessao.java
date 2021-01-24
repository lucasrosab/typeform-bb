/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import org.json.simple.JSONObject;
import portal.autenticador.UsuarioSSO;
import portal.util.Log;
import portal.util.Tipos;

/**
 *
 * @author f9575449
 */
@WebServlet(name = "SalvaNaSessao", urlPatterns = {"/salvanasessao"})
public class SalvaNaSessao extends HttpServlet {

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
        
        response.setContentType("text/html;charset=UTF-8");
       
        boolean debug = false;
        /*
        if (debug) {
            Logger.getLogger(SalvaNaSessao.class.getName()).log(Level.INFO, request.toString());
        }
        */

        // pega o parâmetro da requisição
        // request é um objeto que representa a requisição
        String n = Tipos.texto(request.getParameter("nome"));
        String v = Tipos.texto(request.getParameter("valor"));
        
        // verificamos se há dados na sessão
        HttpSession httpSession = request.getSession();
        UsuarioSSO usuario = (UsuarioSSO) httpSession.getAttribute("usuarioSSO");
        
        if (debug) {
            Logger.getLogger(SalvaNaSessao.class.getName()).log(Level.INFO, "Nome: {0}\t Valor: {1}", new Object[]{n, v});
        }
        
        if (n != null && v != null && usuario != null) {
            // Se temos o usuário logado e o comando, podemos prosseguir
            boolean PodeAbrir = usuario.getTemAcesso("PDC2");
            
            JSONObject info = new JSONObject();
            info.put("nome", n);
            info.put("valor", v);

            // Grava log de visualização do pedido
            Log.GravaLog(usuario.chave, 8, info);
            
            // Grava o pedido na sessão
            if (!request.getMethod().equals("OPTIONS")) {
                if (PodeAbrir) {
                    httpSession.setAttribute(n, v);
                    if (debug) {
                        Logger.getLogger(SalvaNaSessao.class.getName()).log(Level.INFO, "Nome e valor gravados na sessão...  Nome: {0}\t Valor: {1}", new Object[]{n, v});
                    }
                }
            }
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
