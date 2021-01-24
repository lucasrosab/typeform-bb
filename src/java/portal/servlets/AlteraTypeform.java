/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package portal.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import portal.util.Log;
import portal.util.Tipos;

/**
 *
 * @author t1077070
 */
@WebServlet(name = "AlteraTypeform", urlPatterns = {"/typeform"})
public class AlteraTypeform extends HttpServlet {

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

        String sResposta = Tipos.texto(request.getParameter("resposta[]"));

        if (PodeAbrir) {
            sql = "INSERT INTO typeform.tb_resposta (id_pergunta, id_funcionario, dtm, resposta) VALUES (?, ?, ?, ?);";
            retorno.put("titulo", "Comando aceito!");
            retorno.put("tipo", "info");
            retorno.put("mensagem", "Sua resposta foi registrada com sucesso...");
        } else {
            retorno.put("titulo", "Acesso Negado!");
            retorno.put("tipo", "warning");
            retorno.put("mensagem", "Para acessar esta funcionalidade, solicite acesso ao papel PDC1 ou PDC2 do aplicativo COC, canal INTR.");
        }

        try (PrintWriter out = response.getWriter()) {

            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet AlteraTypeform</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet AlteraTypeform at " + request.getContextPath() + "</h1>");
            out.println("</body>");
            out.println("</html>");
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
