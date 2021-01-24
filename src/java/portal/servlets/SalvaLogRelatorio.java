/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package portal.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import portal.dao.Conexao;

/**
 *
 * @author f9575449
 */
@WebServlet(name = "SalvaLog", urlPatterns = {"/salvalogrelatorio"})
public class SalvaLogRelatorio extends HttpServlet {

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
        
        // pega o parâmetro da requisição
        // request é um objeto que representa a requisição
        // System.out.println("==== Salva Log ====");
        // System.out.println(request.toString());

        String u = request.getParameter("relatorio[id_usuario]");
        String r = request.getParameter("relatorio[id_relatorio]");

        if (u != null && r != null) {
            int usuario = (int) Integer.parseInt(u);
            int rel = (int) Integer.parseInt(r);

            String sql;
            // System.out.println("Usuário: " + usuario + " | Relatório: " + rel);

            sql = "INSERT INTO portal_infor.tb_log (dtm_evento, id_usuario, id_relatorio) VALUES (NOW(), ?, ?);";

            // System.out.println("-------- SalvaLogRelatorio: Requisição ao servidor MysQL ------------");
            // System.out.println(sql);

            try (
                    Connection c = new Conexao().getConnection();
                    PreparedStatement s = c.prepareStatement(sql);)
                {
                    s.setInt(1, usuario);
                    s.setInt(2, rel);
                    s.execute();
                    Logger.getLogger(SalvaLogRelatorio.class.getName()).log(Level.INFO, "SQL: " + s.toString());
                    // System.out.println("SalvaLogRelatorio - Executado SQL: " + s.toString());
            } catch (SQLException ex) {
                Logger.getLogger(SalvaLogRelatorio.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            Logger.getLogger(SalvaLogRelatorio.class.getName()).log(Level.WARNING, "Chamada para gravar log, mas sem as informações necessárias.");
            // System.out.println("SalvaLogRelatorio: Chamada para gravar log, mas sem as informações necessárias.");
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
