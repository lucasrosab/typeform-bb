/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package portal.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
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
import portal.dao.Conexao;
import static portal.dao.ResultsetConverter.getParameterJSONArray;
import portal.util.Log;
import portal.util.Tipos;

/**
 *
 * @author f9575449
 */
@WebServlet(name = "AlteraRelatorio", urlPatterns = {"/alterarelatorio"})
public class AlteraRelatorio extends HttpServlet {

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
        int iCmd =  Tipos.inteiro(request.getParameter("comando"));
        int iCodRelatorio = Tipos.inteiro(request.getParameter("CodRelatorio"));
        String sNmRelatorio = Tipos.texto(request.getParameter("NmRelatorio"));
        JSONArray oOpcoes = getParameterJSONArray(request, "opcoes");
        
        // verificamos se há dados na sessão
        HttpSession httpSession = request.getSession();
        UsuarioSSO usuario = (UsuarioSSO) httpSession.getAttribute("usuarioSSO");
        
        if (usuario != null && iCmd != 0) {
            // Se temos o usuário logado e o comando, podemos prosseguir
            // boolean PodeAbrir = usuario.getTemAcesso("PDC2");
            boolean PodeParametrizar  = usuario.getTemAcesso("PDC3");
            
            JSONObject info = new JSONObject();
            info.put("comando", iCmd);
            info.put("CodRelatorio", iCodRelatorio);
            info.put("NmRelatorio", sNmRelatorio);
            info.put("opcoes", oOpcoes);

            // Grava log de inclusão de pedido
            Log.GravaLog(usuario.chave, 4, info);
            
            // Aqui vamos montar nossa instrução SQL, de acordo com o comando, para afetar a tabela rdd.tb_grupo_cliente
            String sql, valores;
            
            // Agora, verificamos o comando solicitado
            switch (iCmd) {
                case 1:
                    // Comando 1: Ajuste fino dos filtros do cliente
                    if (PodeParametrizar) {
                        Connection c;
                        Savepoint original;
                        try {
                            c = new Conexao().getConnection();
                            
                            // Desabilita auto-commit para iniciar como transação
                            c.setAutoCommit(false);
                            
                            // Define um ponto seguro para restauração
                            original = c.setSavepoint("original");
                        } catch (SQLException ex) {
                            // Se não conseguiu salvar o ponto inicial, encerramos a tentativa e devolvemos o erro
                            retorno.put("titulo", "Erro!");
                            retorno.put("tipo", "error");
                            retorno.put("mensagem", "Erro de SQL: " + ex.toString());
                            Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.SEVERE, null, ex);
                            break;
                        } catch (Exception ex) {
                            // Se não conseguiu conectar, já encerramos a tentativa e devolvemos o erro
                            retorno.put("titulo", "Erro!");
                            retorno.put("tipo", "error");
                            retorno.put("mensagem", "Erro Genérico: " + ex.toString());
                            Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.SEVERE, null, ex);
                            break;
                        }
                        
                        try {
                            sql = "DELETE FROM rdd.tb_composicao_relatorio WHERE cod_relatorio = ?;";
                            PreparedStatement s = c.prepareStatement(sql);
                            s.setInt(1, iCodRelatorio);
                            s.executeUpdate();
                            Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.INFO, "SQL Limpeza (Comando 1): " + s.toString());

                            // Montamos a lista dos valores para tabela rdd.tb_filtro_grupo_cliente
                            valores = apuraValores(oOpcoes, iCodRelatorio);
                            sql = "INSERT INTO rdd.tb_composicao_relatorio VALUES " + valores + ";";
                            s = c.prepareStatement(sql);
                            s.executeUpdate();
                            Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.INFO, "SQL inclusão (Comando 1): " + s.toString());

                            c.commit();
                            retorno.put("titulo", "Comando aceito!");
                            retorno.put("tipo", "success");
                            retorno.put("mensagem", "Suas definições foram gravadas!");
                        } catch (SQLException ex) {
                            try {
                                c.rollback(original);
                            } catch (SQLException ex1) {
                                Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.SEVERE, null, ex1);
                            }
                            
                            // Em caso de erro, vamos reconstruir a mensagem de resposta
                            retorno.put("titulo", "Erro!");
                            retorno.put("tipo", "error");
                            retorno.put("mensagem", ex.toString());
                            Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        retorno.put("titulo", "Acesso Negado!");
                        retorno.put("tipo", "warning");
                        retorno.put("mensagem", "Para acessar esta funcionalidade, solicite acesso ao papel PDC3 do aplicativo COC, canal INTR.");
                    }   
                    break;
                case 2:
                    // Comando 2: Edição ou inclusão de relatório
                    if (PodeParametrizar) {
                        if (iCodRelatorio==0) {
                            sql = "INSERT INTO rdd.tb_relatorio (nm_relatorio) VALUES (?) RETURNING cod_relatorio;";
                        } else {
                            sql = "UPDATE rdd.tb_relatorio SET nm_relatorio=? WHERE cod_relatorio=?;";
                        }
                        
                        Connection c;
                        Savepoint original;
                        try {
                            c = new Conexao().getConnection();
                            
                            // Desabilita auto-commit para iniciar como transação
                            c.setAutoCommit(false);
                            
                            // Define um ponto seguro para restauração
                            original = c.setSavepoint("original");
                        } catch (SQLException ex) {
                            // Se não conseguiu salvar o ponto inicial, encerramos a tentativa e devolvemos o erro
                            retorno.put("titulo", "Erro!");
                            retorno.put("tipo", "error");
                            retorno.put("mensagem", "Erro de SQL: " + ex.toString());
                            Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.SEVERE, null, ex);
                            break;
                        } catch (Exception ex) {
                            // Se não conseguiu conectar, já encerramos a tentativa e devolvemos o erro
                            retorno.put("titulo", "Erro!");
                            retorno.put("tipo", "error");
                            retorno.put("mensagem", "Erro Genérico: " + ex.toString());
                            Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.SEVERE, null, ex);
                            break;
                        }
                        
                        try {
                            PreparedStatement s = c.prepareStatement(sql);
                            
                            // Esse comando pode precisar de diferentes parâmetros
                            s.setString(1, sNmRelatorio);

                            if (iCodRelatorio==0) {
                                ResultSet rs = s.executeQuery();
                                if (rs.next()) {
                                    iCodRelatorio = rs.getInt("cod_relatorio");
                                }
                            } else {
                                // Por fim, parâmetros adicionais quando o comando é de alteração
                                s.setInt(2, iCodRelatorio);
                                s.executeUpdate();
                            }
                            Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.INFO, "SQL (Comando 2): " + s.toString());
                            
                            // Montamos a lista dos valores para tabela rdd.tb_filtro_grupo_cliente
                            valores = apuraValores(oOpcoes, iCodRelatorio);
                            sql = "DELETE FROM rdd.tb_composicao_relatorio WHERE cod_relatorio = ?;";
                            s = c.prepareStatement(sql);
                            s.setInt(1, iCodRelatorio);
                            s.executeUpdate();
                            Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.INFO, "SQL Limpeza: " + s.toString());

                            sql = "INSERT INTO rdd.tb_composicao_relatorio VALUES " + valores + ";";
                            s = c.prepareStatement(sql);
                            s.executeUpdate();
                            Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.INFO, "SQL inclusão: " + s.toString());
                            
                            c.commit();
                            retorno.put("titulo", "Comando aceito!");
                            retorno.put("tipo", "success");
                            retorno.put("mensagem", "Suas definições foram gravadas!");
                        } catch (SQLException ex) {
                            try {
                                c.rollback(original);
                            } catch (SQLException ex1) {
                                Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.SEVERE, null, ex1);
                            }
                            
                            // Em caso de erro, vamos reconstruir a mensagem de resposta
                            retorno.put("titulo", "Erro!");
                            retorno.put("tipo", "error");
                            retorno.put("mensagem", ex.toString());
                            Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        retorno.put("titulo", "Acesso Negado!");
                        retorno.put("tipo", "warning");
                        retorno.put("mensagem", "Para acessar esta funcionalidade, solicite acesso ao papel PDC3 do aplicativo COC, canal INTR.");
                    }   
                    break;
                case 3:
                    // Comando 3: Apaga relatório
                    if (PodeParametrizar) {
                        sql = "DELETE FROM rdd.tb_relatorio WHERE cod_relatorio = ?;";
                        
                        Connection c;
                        Savepoint original;
                        try {
                            c = new Conexao().getConnection();
                            
                            // Desabilita auto-commit para iniciar como transação
                            c.setAutoCommit(false);
                            
                            // Define um ponto seguro para restauração
                            original = c.setSavepoint("original");
                        } catch (SQLException ex) {
                            // Se não conseguiu salvar o ponto inicial, encerramos a tentativa e devolvemos o erro
                            retorno.put("titulo", "Erro!");
                            retorno.put("tipo", "error");
                            retorno.put("mensagem", "Erro de SQL: " + ex.toString());
                            Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.SEVERE, null, ex);
                            break;
                        } catch (Exception ex) {
                            // Se não conseguiu conectar, já encerramos a tentativa e devolvemos o erro
                            retorno.put("titulo", "Erro!");
                            retorno.put("tipo", "error");
                            retorno.put("mensagem", "Erro Genérico: " + ex.toString());
                            Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.SEVERE, null, ex);
                            break;
                        }
                        
                        try {
                            PreparedStatement s = c.prepareStatement(sql);
                            
                            // Esse comando precisa apenas de cod_pedido
                            s.setInt(1, iCodRelatorio);
                            s.executeUpdate();

                            sql = "DELETE FROM rdd.tb_composicao_relatorio WHERE cod_relatorio = ?;";
                            s = c.prepareStatement(sql);
                            s.setInt(1, iCodRelatorio);
                            s.executeUpdate();
                            Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.INFO, "SQL Limpeza: " + s.toString());
                            
                            c.commit();
                            retorno.put("titulo", "Comando aceito!");
                            retorno.put("tipo", "success");
                            retorno.put("mensagem", "As definições desse relatório foram apagadas!");
                            Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.INFO, "SQL (Comando 3): " + s.toString());
                        } catch (SQLException ex) {
                            try {
                                c.rollback(original);
                            } catch (SQLException ex1) {
                                Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.SEVERE, null, ex1);
                            }
                            
                            // Em caso de erro, vamos reconstruir a mensagem de resposta
                            retorno.put("titulo", "Erro!");
                            retorno.put("tipo", "error");
                            retorno.put("mensagem", ex.toString());
                            Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        retorno.put("titulo", "Acesso Negado!");
                        retorno.put("tipo", "warning");
                        retorno.put("mensagem", "Para acessar esta funcionalidade, solicite acesso ao papel PDC3 do aplicativo COC, canal INTR.");
                    }   
                    break;
                default:
                    break;
            }
        } else {
            // Caso contrário, armazena no log da aplicação para verificação posterior
            Logger.getLogger(AlteraRelatorio.class.getName()).log(Level.WARNING, "Chamada para alterar o cliente, mas sem as informações necessárias.");
            retorno.put("titulo", "Erro!");
            retorno.put("tipo", "error");
            retorno.put("mensagem", "Chamada para alterar o conjunto de clientes, mas sem as informações necessárias... Nada foi executado.");
        }
        response.getWriter().print(retorno);
    }
    
    protected String apuraValores(JSONArray Opcoes, int CodRelatorio) {
        // Montamos a lista dos valores para tabela rdd.tb_filtro_grupo_cliente
        String valores = "";

        int i = 1;
        for (Object o: Opcoes ) {
            JSONObject jo = (JSONObject) o;
            boolean bMarcado = Tipos.booleano(jo.get("marcado"));
            String sCodInformacao = Tipos.trataParametroNumero(jo.get("cod_informacao"));
            String sOrdem = Tipos.trataParametroNumero(jo.get("ordem"));

            if (bMarcado) {
                if (!valores.equals("")){
                    valores += ", ";
                }
                valores += "(" + CodRelatorio + ", " + sOrdem + ", " + sCodInformacao + ")";

                i++;
            }
        }
        return valores;
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
