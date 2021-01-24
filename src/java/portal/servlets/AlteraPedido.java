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
import javax.servlet.http.HttpSession;
import org.json.simple.JSONObject;
import portal.autenticador.UsuarioSSO;
import portal.dao.Conexao;
import portal.util.Log;
import portal.util.Tipos;

/**
 *
 * @author f9575449
 */
@WebServlet(name = "AlteraPedido", urlPatterns = {"/alterapedido"})
public class AlteraPedido extends HttpServlet {

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
        JSONObject retorno = new JSONObject();

        // pega os parâmetros da requisição 
        int iCmd = Tipos.inteiro(request.getParameter("comando"));
        int iCodPedido = Tipos.inteiro(request.getParameter("pedido[cod_pedido]"));
        int iCodGrupoCliente = Tipos.inteiro(request.getParameter("pedido[cod_grupo_cliente]"));
        int iCodRelatorio = Tipos.inteiro(request.getParameter("pedido[cod_relatorio]"));
        String sNmPedido = Tipos.texto(request.getParameter("pedido[nm_pedido]"));
        String sDescJustificativa = Tipos.texto(request.getParameter("pedido[desc_justificativa]"));

        // verificamos se há dados na sessão
        HttpSession httpSession = request.getSession();
        UsuarioSSO usuario = (UsuarioSSO) httpSession.getAttribute("usuarioSSO");

        JSONObject info = new JSONObject();
        info.put("comando", iCmd);
        info.put("pedido", iCodPedido);
        info.put("cod_grupo_cliente", iCodGrupoCliente);
        info.put("cod_relatorio", iCodRelatorio);
        info.put("nm_pedido", sNmPedido);
        info.put("desc_justificativa", sDescJustificativa);

        // Grava log de inclusão de pedido
        Log.GravaLog(usuario.chave, 5, info);

        if (usuario != null && iCmd != 0) {
            // Se temos o usuário logado e o comando, podemos prosseguir
            boolean PodeAbrir = usuario.getTemAcesso("PDC2");
            boolean PodeDespachar = usuario.getTemAcesso("PDC1");
            // boolean PodeParametrizar  = usuario.getTemAcesso("PDC3");

            // Aqui vamos montar nossa instrução SQL, de acordo com o comando
            String sql = "";

            // Agora, verificamos o comando solicitado
            switch (iCmd) {
                case 1:
                    // Comando 1: Reprocessamento
                    if (PodeDespachar) {
                        sql = "UPDATE rdd.tb_pedido SET cod_status=2, desc_status='Em fila de processamento...', perc_atg=0, mat_solicitante = ?, mat_deferimento = ? WHERE cod_pedido = ?;";
                        retorno.put("titulo", "Comando aceito!");
                        retorno.put("tipo", "success");
                        retorno.put("mensagem", "Seu pedido foi colocado em fila de processamento!");

                        // Grava log de reprocessamento do pedido
                        Log.GravaLog(usuario.chave, 7, info);

                        // Grava log de deferimento do pedido
                        Log.GravaLog(usuario.chave, 6, info);
                    } else {
                        if (PodeAbrir) {
                            sql = "UPDATE rdd.tb_pedido SET cod_status=1, desc_status='Aguardando deferimento...', perc_atg=0, mat_solicitante = ? WHERE cod_pedido = ?;";
                            retorno.put("titulo", "Comando aceito!");
                            retorno.put("tipo", "info");
                            retorno.put("mensagem", "Seu pedido está aguardando deferimento por outro usuário...");

                            // Grava log de reprocessamento do pedido
                            Log.GravaLog(usuario.chave, 7, info);
                        } else {
                            retorno.put("titulo", "Acesso Negado!");
                            retorno.put("tipo", "warning");
                            retorno.put("mensagem", "Para acessar esta funcionalidade, solicite acesso ao papel PDC1 ou PDC2 do aplicativo COC, canal INTR.");
                        }
                    }
                    break;
                case 2:
                    // Comando 2: Edição ou inclusão de pedido
                    if (iCodPedido == 0) {
                        if (PodeDespachar) {
                            sql = "INSERT INTO rdd.tb_pedido (nm_pedido, desc_justificativa, mat_solicitante, mat_deferimento, cod_grupo_cliente, cod_relatorio, cod_status, desc_status, perc_atg) VALUES (?, ?, ?, ?, ?, ?, 2, 'Em fila de processamento...', 0);";
                            retorno.put("titulo", "Comando aceito!");
                            retorno.put("tipo", "success");
                            retorno.put("mensagem", "Seu pedido foi colocado em fila de processamento!");

                            // Grava log de deferimento do pedido
                            Log.GravaLog(usuario.chave, 6, info);
                        } else {
                            if (PodeAbrir) {
                                sql = "INSERT INTO rdd.tb_pedido (nm_pedido, desc_justificativa, mat_solicitante, mat_deferimento, cod_grupo_cliente, cod_relatorio, cod_status, desc_status, perc_atg) VALUES (?, ?, ?, ?, ?, ?, 1, 'Aguardando deferimento...', 0);";
                                retorno.put("titulo", "Comando aceito!");
                                retorno.put("tipo", "info");
                                retorno.put("mensagem", "Seu pedido está aguardando deferimento por outro usuário...");
                            } else {
                                retorno.put("titulo", "Acesso Negado!");
                                retorno.put("tipo", "warning");
                                retorno.put("mensagem", "Para acessar esta funcionalidade, solicite acesso ao papel PDC1 ou PDC2 do aplicativo COC, canal INTR.");
                            }
                        }
                    } else {
                        if (PodeDespachar) {
                            sql = "UPDATE rdd.tb_pedido SET nm_pedido=?, desc_justificativa=?, mat_solicitante=?, mat_deferimento=?, cod_grupo_cliente=?, cod_relatorio=?, cod_status=2, desc_status='Em fila de processamento...', perc_atg=0 WHERE cod_pedido=?;";
                            retorno.put("titulo", "Comando aceito!");
                            retorno.put("tipo", "success");
                            retorno.put("mensagem", "Seu pedido foi colocado em fila de processamento!");

                            // Grava log de deferimento do pedido
                            Log.GravaLog(usuario.chave, 6, info);
                        } else {
                            if (PodeAbrir) {
                                sql = "UPDATE rdd.tb_pedido SET nm_pedido=?, desc_justificativa=?, mat_solicitante=?, mat_deferimento=?, cod_grupo_cliente=?, cod_relatorio=?, cod_status=1, desc_status='Aguardando deferimento...', perc_atg=0 WHERE cod_pedido=?;";
                                retorno.put("titulo", "Comando aceito!");
                                retorno.put("tipo", "info");
                                retorno.put("mensagem", "Seu pedido está aguardando deferimento por outro usuário...");
                            } else {
                                retorno.put("titulo", "Acesso Negado!");
                                retorno.put("tipo", "warning");
                                retorno.put("mensagem", "Para acessar esta funcionalidade, solicite acesso ao papel PDC1 ou PDC2 do aplicativo COC, canal INTR.");
                            }
                        }
                    }
                    break;
                default:
                    break;
            }

            if (!sql.equalsIgnoreCase("")) {
                try (
                        Connection c = new Conexao().getConnection();
                        PreparedStatement s = c.prepareStatement(sql);) {
                    // Verificamos o comando novamente, para inseri os parâmetros corretos
                    if (iCmd == 3) {
                        // Esse comando precisa apenas de cod_pedido
                        s.setInt(1, iCodPedido);
                    } else if (iCmd == 1) {
                        s.setString(1, usuario.chave);
                        if (PodeDespachar) {
                            s.setString(2, usuario.chave);
                            s.setInt(3, iCodPedido);
                        } else {
                            s.setInt(2, iCodPedido);
                        }
                    } else if (iCmd == 2) {
                        // Esse comando pode precisar de diferentes parâmetros
                        s.setString(1, sNmPedido);
                        s.setString(2, sDescJustificativa);
                        s.setString(3, usuario.chave);
                        s.setInt(5, iCodGrupoCliente);
                        s.setInt(6, iCodRelatorio);

                        // Aqui deverá ficar o controle de acessos
                        if (PodeDespachar) {
                            s.setString(4, usuario.chave);
                        } else {
                            s.setNull(4, java.sql.Types.CHAR);
                        }

                        // Por fim, parâmetros adicionais quando o comando é de alteração
                        if (iCodPedido > 0) {
                            s.setInt(7, iCodPedido);
                        }
                    }

                    s.execute();
                    Logger.getLogger(AlteraPedido.class.getName()).log(Level.INFO, "SQL: " + s.toString());
                } catch (SQLException ex) {
                    // Em caso de erro, vamos reconstruir a mensagem de resposta
                    retorno = new JSONObject();
                    retorno.put("titulo", "Erro!");
                    retorno.put("tipo", "error");
                    retorno.put("mensagem", ex.toString());
                    Logger.getLogger(AlteraPedido.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            // Caso contrário, armazena no log da aplicação para verificação posterior
            Logger.getLogger(AlteraPedido.class.getName()).log(Level.WARNING, "Chamada para alterar o pedido, mas sem as informações necessárias.");
            retorno.put("titulo", "Erro!");
            retorno.put("tipo", "error");
            retorno.put("mensagem", "Chamada para alterar o pedido, mas sem as informações necessárias... Nada foi executado.");
        }
        // Logger.getLogger(AlteraPedido.class.getName()).log(Level.INFO, "Retorno: " + retorno.toString());
        response.getWriter().print(retorno);
        // Logger.getLogger(AlteraPedido.class.getName()).log(Level.INFO, "Resposta: " + response.toString());
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
