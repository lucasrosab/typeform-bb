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
@WebServlet(name = "AlteraCliente", urlPatterns = {"/alteracliente"})
public class AlteraCliente extends HttpServlet {

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
        int iCodGrupoCliente = Tipos.inteiro(request.getParameter("CodGrupoCliente"));
        boolean bManual = Tipos.booleano(request.getParameter("manual"));
        String sNmGrupoCliente = Tipos.texto(request.getParameter("NmGrupoCliente"));
        JSONArray oOpcoes = getParameterJSONArray(request, "opcoes");
        
        // verificamos se há dados na sessão
        HttpSession httpSession = request.getSession();
        UsuarioSSO usuario = (UsuarioSSO) httpSession.getAttribute("usuarioSSO");

        JSONObject info = new JSONObject();
        info.put("comando", iCmd);
        info.put("CodGrupoCliente", iCodGrupoCliente);
        info.put("NmGrupoCliente", sNmGrupoCliente);
        info.put("manual", bManual);
        info.put("opcoes", oOpcoes);

        // Grava log de atualização de grupo de clientes
        Log.GravaLog(usuario.chave, 3, info);
        
        if (usuario != null && iCmd != 0) {
            // Se temos o usuário logado e o comando, podemos prosseguir
            // boolean PodeAbrir = usuario.getTemAcesso("PDC2");
            boolean PodeParametrizar  = usuario.getTemAcesso("PDC3");
            boolean PodeDespachar = usuario.getTemAcesso("PDC1");
            
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
                            Logger.getLogger(AlteraCliente.class.getName()).log(Level.SEVERE, null, ex);
                            break;
                        } catch (Exception ex) {
                            // Se não conseguiu conectar, já encerramos a tentativa e devolvemos o erro
                            retorno.put("titulo", "Erro!");
                            retorno.put("tipo", "error");
                            retorno.put("mensagem", "Erro Genérico: " + ex.toString());
                            Logger.getLogger(AlteraCliente.class.getName()).log(Level.SEVERE, null, ex);
                            break;
                        }
                        
                        try {
                            sql = "DELETE FROM typeform.tb_filtro_grupo_cliente WHERE cod_grupo_cliente = ?;";
                            PreparedStatement s = c.prepareStatement(sql);
                            s.setInt(1, iCodGrupoCliente);
                            s.executeUpdate();
                            Logger.getLogger(AlteraCliente.class.getName()).log(Level.INFO, "SQL Limpeza (Comando 1): " + s.toString());

                            // Montamos a lista dos valores para tabela rdd.tb_filtro_grupo_cliente
                            valores = apuraValores(oOpcoes, iCodGrupoCliente);
                            sql = "INSERT INTO typeform.tb_filtro_grupo_cliente VALUES " + valores + ";";
                            s = c.prepareStatement(sql);
                            s.executeUpdate();
                            Logger.getLogger(AlteraCliente.class.getName()).log(Level.INFO, "SQL inclusão (Comando 1): " + s.toString());

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
                            Logger.getLogger(AlteraCliente.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        retorno.put("titulo", "Acesso Negado!");
                        retorno.put("tipo", "warning");
                        retorno.put("mensagem", "Para acessar esta funcionalidade, solicite acesso ao papel PDC3 do aplicativo COC, canal INTR.");
                    }   
                    break;
                case 2:
                    // Comando 2: Edição ou inclusão de pedido
                    if (PodeDespachar) {
                        if (iCodGrupoCliente==0) {
                            sql = "INSERT INTO typeform.tb_grupo_cliente (nm_grupo_cliente, manual) VALUES (?, ?) RETURNING cod_grupo_cliente;";
                        } else {
                            sql = "UPDATE typeform.tb_grupo_cliente SET nm_grupo_cliente=?, manual=? WHERE cod_grupo_cliente=?;";
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
                            Logger.getLogger(AlteraCliente.class.getName()).log(Level.SEVERE, null, ex);
                            break;
                        } catch (Exception ex) {
                            // Se não conseguiu conectar, já encerramos a tentativa e devolvemos o erro
                            retorno.put("titulo", "Erro!");
                            retorno.put("tipo", "error");
                            retorno.put("mensagem", "Erro Genérico: " + ex.toString());
                            Logger.getLogger(AlteraCliente.class.getName()).log(Level.SEVERE, null, ex);
                            break;
                        }
                        
                        try {
                            PreparedStatement s = c.prepareStatement(sql);
                            
                            // Esse comando pode precisar de diferentes parâmetros
                            s.setString(1, sNmGrupoCliente);
                            s.setBoolean(2, bManual);

                            if (iCodGrupoCliente==0) {
                                ResultSet rs = s.executeQuery();
                                if (rs.next()) {
                                    iCodGrupoCliente = rs.getInt("cod_grupo_cliente");
                                }
                            } else {
                                // Por fim, parâmetros adicionais quando o comando é de alteração
                                s.setInt(3, iCodGrupoCliente);
                                s.executeUpdate();
                            }
                            Logger.getLogger(AlteraCliente.class.getName()).log(Level.INFO, "SQL (Comando 2): " + s.toString());
                            
                            // Montamos a lista dos valores para tabela rdd.tb_filtro_grupo_cliente
                            valores = apuraValores(oOpcoes, iCodGrupoCliente);
                            sql = "DELETE FROM typeform.tb_filtro_grupo_cliente WHERE cod_grupo_cliente = ?;";
                            s = c.prepareStatement(sql);
                            s.setInt(1, iCodGrupoCliente);
                            s.executeUpdate();
                            Logger.getLogger(AlteraCliente.class.getName()).log(Level.INFO, "SQL Limpeza: " + s.toString());

                            sql = "INSERT INTO typeform.tb_filtro_grupo_cliente VALUES " + valores + ";";
                            s = c.prepareStatement(sql);
                            s.executeUpdate();
                            Logger.getLogger(AlteraCliente.class.getName()).log(Level.INFO, "SQL inclusão: " + s.toString());
                            
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
                            Logger.getLogger(AlteraCliente.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        retorno.put("titulo", "Acesso Negado!");
                        retorno.put("tipo", "warning");
                        retorno.put("mensagem", "Para acessar esta funcionalidade, solicite acesso ao papel PDC1 do aplicativo COC, canal INTR.");
                    }   
                    break;
                case 3:
                    // Comando 3: Apaga pedido
                    if (PodeParametrizar) {
                        // Vamos fazer isso como uma transação, pois podemos ter que alterar mais de uma tabela
                        Savepoint original;
                        Connection c;
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
                            retorno.put("mensagem", ex.toString());
                            Logger.getLogger(AlteraCliente.class.getName()).log(Level.SEVERE, null, ex);
                            break;
                        }
                            
                        try {
                            sql = "DELETE FROM typeform.tb_grupo_cliente WHERE cod_grupo_cliente = ?;";
                            PreparedStatement tbGrupoCliente = c.prepareStatement(sql);
                            tbGrupoCliente.setInt(1, iCodGrupoCliente);
                            tbGrupoCliente.executeUpdate();
                            Logger.getLogger(AlteraCliente.class.getName()).log(Level.INFO, "SQL Limpeza 1: " + tbGrupoCliente.toString());

                            sql = "DELETE FROM typeform.tb_filtro_grupo_cliente WHERE cod_grupo_cliente = ?;";
                            PreparedStatement tbFiltroGrupoCliente = c.prepareStatement(sql);
                            tbFiltroGrupoCliente.setInt(1, iCodGrupoCliente);
                            tbFiltroGrupoCliente.executeUpdate();
                            Logger.getLogger(AlteraCliente.class.getName()).log(Level.INFO, "SQL Limpeza 2: " + tbFiltroGrupoCliente.toString());
                            
                            sql = "DELETE FROM typeform.tb_pedido WHERE cod_grupo_cliente = ?;";
                            PreparedStatement tbPedido = c.prepareStatement(sql);
                            tbPedido.setInt(1, iCodGrupoCliente);
                            tbPedido.executeUpdate();
                            Logger.getLogger(AlteraCliente.class.getName()).log(Level.INFO, "SQL Limpeza 3: " + tbPedido.toString());
                            
                            c.commit();
                            retorno.put("titulo", "Comando aceito!");
                            retorno.put("tipo", "success");
                            retorno.put("mensagem", "As definições desse conjunto de clientes foram apagadas!");
                        } catch (SQLException ex) {
                            try {
                                c.rollback(original);
                            } catch (SQLException ex1) {
                                Logger.getLogger(AlteraInformacao.class.getName()).log(Level.SEVERE, null, ex1);
                            }
                            // Em caso de erro, vamos reconstruir a mensagem de resposta
                            retorno.put("titulo", "Erro!");
                            retorno.put("tipo", "error");
                            retorno.put("mensagem", ex.toString());
                            Logger.getLogger(AlteraCliente.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(AlteraCliente.class.getName()).log(Level.WARNING, "Chamada para alterar o cliente, mas sem as informações necessárias.");
            retorno.put("titulo", "Erro!");
            retorno.put("tipo", "error");
            retorno.put("mensagem", "Chamada para alterar o conjunto de clientes, mas sem as informações necessárias... Nada foi executado.");
        }
        response.getWriter().print(retorno);
    }
    
    protected String apuraValores(JSONArray Opcoes, int CodGrupoCliente) {
        // Montamos a lista dos valores para tabela rdd.tb_filtro_grupo_cliente
        String valores = "";

        int i = 1;
        for (Object o: Opcoes ) {
            JSONObject jo = (JSONObject) o;
            boolean bMarcado = Tipos.booleano(jo.get("marcado"));
            String sCodInformacao = Tipos.trataParametroNumero(jo.get("cod_informacao"));
            String sCodGrupo = Tipos.trataParametroNumero(jo.get("cod_grupo")) ;
            String sCodDetalhamento = Tipos.trataParametroNumero(jo.get("cod_detalhamento"));
            String sCodFiltroAdicional = Tipos.trataParametroNumero(jo.get("cod_filtro_adicional")) ;
            String sCodRelacao = Tipos.trataParametroNumero(jo.get("cod_relacao"));

            if (bMarcado) {
                if (!valores.equals("")){
                    valores += ", ";
                }
                valores += "(" + CodGrupoCliente + ", " + i + ", " + sCodInformacao + ", " + sCodGrupo + ", " + sCodDetalhamento + ", " + sCodFiltroAdicional + ", " + sCodRelacao + ")";

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
