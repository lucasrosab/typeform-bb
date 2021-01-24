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
import org.postgresql.util.PGobject;
import portal.autenticador.UsuarioSSO;
import portal.dao.Conexao;
import static portal.dao.ResultsetConverter.getParameterJSONArray;
import portal.util.Log;
import portal.util.Tipos;

/**
 *
 * @author f9575449
 */
@WebServlet(name = "AlteraInformacao", urlPatterns = {"/alterainformacao"})
public class AlteraInformacao extends HttpServlet {

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
        
        // pega o comando da requisição 
        int iCmd =  Tipos.inteiro(request.getParameter("comando"));
        
        // verificamos se há dados na sessão
        HttpSession httpSession = request.getSession();
        UsuarioSSO usuario = (UsuarioSSO) httpSession.getAttribute("usuarioSSO");
        
        if (usuario != null && iCmd != 0) {
            // Se temos o usuário logado e o comando, podemos prosseguir
            // boolean PodeAbrir = usuario.getTemAcesso("PDC2");
            boolean PodeParametrizar  = usuario.getTemAcesso("PDC3");

            // Aqui vamos montar nossa instrução SQL, de acordo com o comando
            String sql; 
            
            // Agora, verificamos o comando solicitado
            switch (iCmd) {
                case 1:
                    // Comando 1: Edição ou inclusão de grupos
                    if (PodeParametrizar) {
                        // parâmetros para este comando
                        int iCodCampo = Tipos.inteiro(request.getParameter("CodCampo"));
                        String sNmCampo = Tipos.texto(request.getParameter("NmCampo"));
                        String sDescCampo = Tipos.texto(request.getParameter("DescCampo"));
                        int iCodTabela = Tipos.inteiro(request.getParameter("CodTabela"));
                        int iCodInformacao = Tipos.inteiro(request.getParameter("CodInformacao"));
                        JSONArray oGrupos = getParameterJSONArray(request, "listaGrupos");
                        JSONArray oDetalhamentos = getParameterJSONArray(request, "listaDetalhamentos");
                        
                        JSONObject info = new JSONObject();
                        info.put("comando", iCmd);
                        info.put("CodCampo", iCodCampo);
                        info.put("NmCampo", sNmCampo);
                        info.put("DescCampo", sDescCampo);
                        info.put("CodTabela", iCodTabela);
                        info.put("CodInformacao", iCodInformacao);
                        info.put("listaGrupos", oGrupos);
                        info.put("listaDetalhamentos", oDetalhamentos);

                        // Grava log de inclusão de pedido
                        Log.GravaLog(usuario.chave, 9, info);
                        
                        Connection c;
                        Savepoint original;
                        try {
                            c = new Conexao().getConnection();
                        } catch (Exception ex) {
                            // Se não conseguiu conectar, já encerramos a tentativa e devolvemos o erro
                            retorno.put("titulo", "Erro!");
                            retorno.put("tipo", "error");
                            retorno.put("mensagem", ex.toString());
                            Logger.getLogger(AlteraInformacao.class.getName()).log(Level.SEVERE, null, ex);
                            break;
                        }
                        
                        try {
                            // Desabilita auto-commit para iniciar como transação
                            c.setAutoCommit(false);
                            
                            // Define um ponto seguro para restauração
                            original = c.setSavepoint("original");
                        } catch (SQLException ex) {
                            // Se não conseguiu salvar o ponto inicial, encerramos a tentativa e devolvemos o erro
                            retorno.put("titulo", "Erro!");
                            retorno.put("tipo", "error");
                            retorno.put("mensagem", ex.toString());
                            Logger.getLogger(AlteraInformacao.class.getName()).log(Level.SEVERE, null, ex);
                            break;
                        }
                            
                        try {
                            // Se não há código de campo, adicionaremos
                            if (iCodCampo==0) {
                                sql = "INSERT INTO rdd.tb_campo (nm_campo, desc_campo) VALUES (?, ?) RETURNING cod_campo;";
                            } else {
                                // Como ainda pode ser um campo novo, tentaremos inserir de outra forma, ou atualizar se for um já existente
                                // sql = "UPDATE rdd.tb_campo SET nm_campo = ?, desc_campo = ? WHERE cod_campo=?;";
                                sql = "INSERT INTO rdd.tb_campo (nm_campo, desc_campo) VALUES (?, ?) ON CONFLICT (nm_campo) DO UPDATE SET desc_campo = EXCLUDED.desc_campo RETURNING cod_campo;";
                            }

                            PreparedStatement tbCampo = c.prepareStatement(sql);
                            
                            tbCampo.setString(1, sNmCampo);
                            tbCampo.setString(2, sDescCampo);

                            ResultSet rs = tbCampo.executeQuery();
                            if (rs.next()) {
                                iCodCampo = rs.getInt("cod_campo");
                            }
                            /*
                            if (iCodCampo==0) {
                                ResultSet rs = tbCampo.executeQuery();
                                if (rs.next()) {
                                    iCodCampo = rs.getInt("cod_campo");
                                }
                            } else {
                                // Por fim, parâmetros adicionais quando o comando é de alteração
                                tbCampo.setInt(3, iCodCampo);
                                tbCampo.executeUpdate();
                            }
                            */
                            Logger.getLogger(AlteraInformacao.class.getName()).log(Level.INFO, "SQL (Comando 1): " + tbCampo.toString());
                            
                            // Agora que a tabela tb_campo já foi ajustada, vamos limpar a tabela tb_composicao_informacao e seguir para as próximas
                            int i = 1;
                            PreparedStatement tbComposicao = c.prepareStatement("DELETE FROM rdd.tb_composicao_informacao WHERE cod_informacao = ?");
                            tbComposicao.setInt(1, iCodInformacao);
                            tbComposicao.executeUpdate();
                                        
                            for (Object o: oGrupos ) {
                                JSONObject jo = (JSONObject) o;
                                boolean bMarcado = Tipos.booleano(jo.get("marcado"));
                                boolean bIncluiGrupo = Tipos.booleano(jo.get("adiciona"));
                                boolean bAlteraGrupo = Tipos.booleano(jo.get("atualiza"));
                                boolean bExcluiGrupo = Tipos.booleano(jo.get("apaga"));
                                int iCodGrupo = Tipos.inteiro(jo.get("cod_grupo"));
                                String sNmGrupo = jo.get("nm_grupo").toString();
                                int iCodTratamento = Tipos.inteiro(jo.get("cod_tratamento"));
                                String sFiltro = jo.get("filtro").toString();

                                if (bIncluiGrupo && !bExcluiGrupo && !bAlteraGrupo) {
                                    sql = "INSERT INTO rdd.tb_grupo(nm_grupo, nm_campo, cod_tratamento, filtro) VALUES (?, ?, ?, ?) RETURNING cod_grupo;";
                                } else if (bAlteraGrupo && !bExcluiGrupo && !bIncluiGrupo) {
                                    sql = "UPDATE rdd.tb_grupo SET nm_grupo=?, nm_campo=?, cod_tratamento=?, filtro=? WHERE cod_grupo=?;";
                                } else if (bExcluiGrupo && !bIncluiGrupo && !bAlteraGrupo) {
                                    sql = "DELETE FROM rdd.tb_grupo WHERE cod_grupo=?;";
                                } else {
                                    sql = "";
                                }

                                if (!sql.equals("")) {
                                    // Aqui, será necessário considerar os detalhamentos também
                                    PreparedStatement tbGrupo = c.prepareStatement(sql);

                                    if (bExcluiGrupo) {
                                        tbGrupo.setInt(1, iCodGrupo);
                                    } else {
                                        tbGrupo.setString(1, sNmGrupo);
                                        tbGrupo.setString(2, sNmCampo);
                                        tbGrupo.setInt(3, iCodTratamento);
                                        tbGrupo.setString(4, sFiltro);
                                        if (bAlteraGrupo) {
                                            tbGrupo.setInt(5, iCodGrupo);
                                        }
                                    }
                                    
                                    if (bIncluiGrupo) {
                                        /* ResultSet rs = tbGrupo.executeQuery(); */
                                        rs = tbGrupo.executeQuery();
                                        if (rs.next()) {
                                            iCodGrupo = rs.getInt("cod_grupo");
                                        }
                                    } else {
                                        // Por fim, parâmetros adicionais quando o comando é de alteração
                                        tbGrupo.executeUpdate();
                                    }
                                    Logger.getLogger(AlteraInformacao.class.getName()).log(Level.INFO, "SQL (Comando 1): " + tbGrupo.toString());
                                }

                                // Por fim, a última tabela
                                // int iCodOrdem = Tipos.inteiro(jo.get("ordem_grupo"));
                                int iCodDetalhamento = 0;

                                if (bMarcado && !bExcluiGrupo){
                                    sql = "INSERT INTO rdd.tb_composicao_informacao(cod_tabela, cod_grupo, cod_detalhamento, cod_informacao, cod_ordem) VALUES (?, ?, ?, ?, ?);";
                                    tbComposicao = c.prepareStatement(sql);

                                    tbComposicao.setInt(1, iCodTabela);
                                    tbComposicao.setInt(2, iCodGrupo);
                                    tbComposicao.setInt(3, iCodDetalhamento);
                                    tbComposicao.setInt(4, iCodInformacao);
                                    tbComposicao.setInt(5, i);
                                    tbComposicao.executeUpdate();
                                    Logger.getLogger(AlteraInformacao.class.getName()).log(Level.INFO, "SQL (Comando 1): " + tbComposicao.toString());
                                    i++;
                                }
                            }
                            c.commit();
                            retorno.put("titulo", "Comando aceito!");
                            retorno.put("tipo", "success");
                            retorno.put("mensagem", "Informação gravada com sucesso!");
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
                            Logger.getLogger(AlteraInformacao.class.getName()).log(Level.SEVERE, null, ex);
                        } finally {
                            try {
                                // c.releaseSavepoint(original);
                                c.close();
                            } catch (SQLException ex) {
                                Logger.getLogger(AlteraInformacao.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }                       
                    } else {
                        retorno.put("titulo", "Acesso Negado!");
                        retorno.put("tipo", "warning");
                        retorno.put("mensagem", "Para acessar esta funcionalidade, solicite acesso ao papel PDC3 do aplicativo COC, canal INTR.");
                    }   
                    break;
                case 2:
                    // Comando 2: Edição ou inclusão de informacao
                    if (PodeParametrizar) {
                        // parâmetros para este comando
                        int iCodInformacao = Tipos.inteiro(request.getParameter("CodInformacao"));
                        String sNmInformacao = Tipos.texto(request.getParameter("NmInformacao"));
                        boolean bExibeQtd = Tipos.booleano(request.getParameter("ExibeQtd"));
                        boolean bExibeValor = Tipos.booleano(request.getParameter("ExibeValor"));
                        boolean bExibePartQtd = Tipos.booleano(request.getParameter("ExibePartQtd"));
                        boolean bExibePartValor = Tipos.booleano(request.getParameter("ExibePartValor"));
                        int iCodTemplate = Tipos.inteiro(request.getParameter("CodTemplate"));
                        String sNmFonte = Tipos.texto(request.getParameter("Nmfonte"));
                        int iCodAgrupador = Tipos.inteiro(request.getParameter("CodAgrupador"));
                        int iCodContador = Tipos.inteiro(request.getParameter("CodContador"));
                        String sFiltroInformacao = Tipos.texto(request.getParameter("FiltroInformacao"));
                        
                        // Recebe o json como string
                        String sCabecalho = Tipos.texto(request.getParameter("Cabecalho"));
                        PGobject jsonCabecalho = new PGobject();
                        jsonCabecalho.setType("json");
                        try {
                            jsonCabecalho.setValue(sCabecalho);
                        } catch (SQLException e) {
                            Logger.getLogger(AlteraInformacao.class.getName()).log(Level.SEVERE, null, e);
                        }
                        
                        // Parametros Recebidos
                        Logger.getLogger(AlteraInformacao.class.getName()).log(Level.INFO, "Parâmetros Recebidos:\nNome da Informação: {0}\nCabeçalho: {1}", new Object[]{sNmInformacao, sCabecalho});
                        
                        JSONObject info = new JSONObject();
                        info.put("comando", iCmd);
                        info.put("CodInformacao", iCodInformacao);
                        info.put("NmInformacao", sNmInformacao);
                        info.put("ExibeQtd", bExibeQtd);
                        info.put("ExibeValor", bExibeValor);
                        info.put("ExibePartQtd", bExibePartQtd);
                        info.put("ExibePartValor", bExibePartValor);
                        info.put("CodTemplate", iCodTemplate);
                        info.put("Nmfonte", sNmFonte);
                        info.put("CodAgrupador", iCodAgrupador);
                        info.put("CodContador", iCodContador);
                        info.put("FiltroInformacao", sFiltroInformacao);
                        info.put("Cabecalho", sCabecalho);

                        // Grava log de inclusão de pedido
                        Log.GravaLog(usuario.chave, 10, info);
                        
                        if (iCodInformacao==0) {
                            sql = "INSERT INTO rdd.tb_informacao (nm_informacao, exibe_qtd, exibe_valor, exibe_part_qtd, exibe_part_valor, cod_template, cabecalho, nm_fonte, cod_agrupador, cod_contador, filtro_informacao) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING cod_informacao;";
                        } else {
                            sql = "UPDATE rdd.tb_informacao SET nm_informacao = ?, exibe_qtd = ?, exibe_valor = ?, exibe_part_qtd = ?, exibe_part_valor = ?, cod_template = ?, cabecalho = ?, nm_fonte = ?, cod_agrupador = ?, cod_contador = ?, filtro_informacao = ? WHERE cod_informacao=?;";
                        }
                        
                        try ( Connection c = new Conexao().getConnection(); ){
                            PreparedStatement s = c.prepareStatement(sql);
                            
                            // Esse comando pode precisar de diferentes parâmetros
                            s.setString(1, sNmInformacao);
                            s.setBoolean(2, bExibeQtd);
                            s.setBoolean(3, bExibeValor);
                            s.setBoolean(4, bExibePartQtd);
                            s.setBoolean(5, bExibePartValor);
                            s.setInt(6, iCodTemplate);
                            s.setObject(7, jsonCabecalho );
                            s.setString(8, sNmFonte);
                            s.setInt(9, iCodAgrupador);
                            s.setInt(10, iCodContador);
                            s.setString(11, sFiltroInformacao);

                            if (iCodInformacao==0) {
                                ResultSet rs = s.executeQuery();
                                if (rs.next()) {
                                    iCodInformacao = rs.getInt("cod_informacao");
                                }
                            } else {
                                // Por fim, parâmetros adicionais quando o comando é de alteração
                                s.setInt(12, iCodInformacao);
                                s.execute();
                            }
                            Logger.getLogger(AlteraInformacao.class.getName()).log(Level.INFO, "SQL (Comando 2): " + s.toString());
                            
                            retorno.put("titulo", "Comando aceito!");
                            retorno.put("tipo", "success");
                            retorno.put("mensagem", "Informação gravada com sucesso!");
                        } catch (SQLException ex) {
                            // Em caso de erro, vamos reconstruir a mensagem de resposta
                            retorno.put("titulo", "Erro!");
                            retorno.put("tipo", "error");
                            retorno.put("mensagem", ex.toString());
                            Logger.getLogger(AlteraInformacao.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        retorno.put("titulo", "Acesso Negado!");
                        retorno.put("tipo", "warning");
                        retorno.put("mensagem", "Para acessar esta funcionalidade, solicite acesso ao papel PDC3 do aplicativo COC, canal INTR.");
                    }   
                    break;
                case 3:
                    // Comando 3: Apaga informação
                    if (PodeParametrizar) {
                        /*
                        sql = "DELETE FROM rdd.tb_relatorio WHERE cod_relatorio = ?;";
                        
                        try ( Connection c = new Conexao().getConnection(); ) {
                            PreparedStatement s = c.prepareStatement(sql);
                            
                            // Esse comando precisa apenas de cod_pedido
                            s.setInt(1, iCodRelatorio);
                            s.execute();

                            sql = "DELETE FROM rdd.tb_composicao_relatorio WHERE cod_relatorio = ?;";
                            s = c.prepareStatement(sql);
                            s.setInt(1, iCodRelatorio);
                            s.execute();
                            Logger.getLogger(AlteraInformacao.class.getName()).log(Level.INFO, "SQL Limpeza: " + s.toString());
                            
                            retorno.put("titulo", "Comando aceito!");
                            retorno.put("tipo", "success");
                            retorno.put("mensagem", "As definições desse relatório foram apagadas!");
                            Logger.getLogger(AlteraInformacao.class.getName()).log(Level.INFO, "SQL (Comando 3): " + s.toString());
                        } catch (SQLException ex) {
                            // Em caso de erro, vamos reconstruir a mensagem de resposta
                            retorno.put("titulo", "Erro!");
                            retorno.put("tipo", "error");
                            retorno.put("mensagem", ex.toString());
                            Logger.getLogger(AlteraInformacao.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        */
                    } else {
                        retorno.put("titulo", "Acesso Negado!");
                        retorno.put("tipo", "warning");
                        retorno.put("mensagem", "Para acessar esta funcionalidade, solicite acesso ao papel PDC3 do aplicativo COC, canal INTR.");
                    }   
                    break;
                case 4:
                    // Comando 2: Edição de faixas
                    if (PodeParametrizar) {
                        // parâmetros para este comando
                        int iCodGrupo = Tipos.inteiro(request.getParameter("CodGrupo"));
                        String sNmGrupo = Tipos.texto(request.getParameter("NmGrupo"));
                        String sFiltro = Tipos.texto(request.getParameter("DescFiltro"));
                        int iCodTratamento = Tipos.inteiro(request.getParameter("CodTratamento"));
                        
                        JSONObject info = new JSONObject();
                        info.put("comando", iCmd);
                        info.put("CodGrupo", iCodGrupo);
                        info.put("NmGrupo", sNmGrupo);
                        info.put("Filtro", sFiltro);
                        info.put("CodTratamento", iCodTratamento);

                        // Grava log de edição de grupo
                        Log.GravaLog(usuario.chave, 9, info);
                        
                        sql = "UPDATE rdd.tb_grupo SET nm_grupo = ?, cod_tratamento = ?, filtro = ? WHERE cod_grupo=?;";
                        
                        try ( Connection c = new Conexao().getConnection(); ){
                            PreparedStatement s = c.prepareStatement(sql);
                            
                            s.setString(1, sNmGrupo);
                            s.setInt(2, iCodTratamento);
                            s.setString(3, sFiltro);
                            s.setInt(4, iCodGrupo);
                            s.execute();
                            Logger.getLogger(AlteraInformacao.class.getName()).log(Level.INFO, "SQL (Comando 4): " + s.toString());
                            
                            retorno.put("titulo", "Comando aceito!");
                            retorno.put("tipo", "success");
                            retorno.put("mensagem", "Informação gravada com sucesso!");
                        } catch (SQLException ex) {
                            // Em caso de erro, vamos reconstruir a mensagem de resposta
                            retorno.put("titulo", "Erro!");
                            retorno.put("tipo", "error");
                            retorno.put("mensagem", ex.toString());
                            Logger.getLogger(AlteraInformacao.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        retorno.put("titulo", "Acesso Negado!");
                        retorno.put("tipo", "warning");
                        retorno.put("mensagem", "Para acessar esta funcionalidade, solicite acesso ao papel PDC3 do aplicativo COC, canal INTR.");
                    }   
                    break;
                case 5:
                    // Comando 1: Alterações na ordem dos grupos e detalhamentos
                    if (PodeParametrizar) {
                        // parâmetros para este comando
                        int iCodTabela = Tipos.inteiro(request.getParameter("CodTabela"));
                        int iCodInformacao = Tipos.inteiro(request.getParameter("CodInformacao"));
                        JSONArray oGrupos = getParameterJSONArray(request, "listaGrupos");
                        JSONArray oDetalhamentos = getParameterJSONArray(request, "listaDetalhamentos");
                        
                        JSONObject info = new JSONObject();
                        info.put("comando", iCmd);
                        info.put("CodTabela", iCodTabela);
                        info.put("CodInformacao", iCodInformacao);
                        info.put("listaGrupos", oGrupos);
                        info.put("listaDetalhamentos", oDetalhamentos);

                        // Grava log de inclusão/alteração de grupo
                        Log.GravaLog(usuario.chave, 9, info);
                        
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
                            Logger.getLogger(AlteraInformacao.class.getName()).log(Level.SEVERE, null, ex);
                            break;
                        } catch (Exception ex) {
                            // Se não conseguiu conectar, já encerramos a tentativa e devolvemos o erro
                            retorno.put("titulo", "Erro!");
                            retorno.put("tipo", "error");
                            retorno.put("mensagem", "Erro Genérico: " + ex.toString());
                            Logger.getLogger(AlteraInformacao.class.getName()).log(Level.SEVERE, null, ex);
                            break;
                        }
                            
                        try {
                            PreparedStatement tbComposicao = c.prepareStatement("DELETE FROM rdd.tb_composicao_informacao WHERE cod_informacao = ?");
                            tbComposicao.setInt(1, iCodInformacao);
                            tbComposicao.executeUpdate();
                                        
                            for (Object o: oGrupos ) {
                                JSONObject jo = (JSONObject) o;
                                boolean bMarcado = Tipos.booleano(jo.get("marcado"));
                                boolean bExcluiGrupo = Tipos.booleano(jo.get("apaga"));
                                int iCodGrupo = Tipos.inteiro(jo.get("cod_grupo"));
                                int iCodOrdem = Tipos.inteiro(jo.get("ordem_grupo"));

                                // Alterar depois para suportar detalhamentos
                                int iCodDetalhamento = 0;

                                sql = "INSERT INTO rdd.tb_composicao_informacao(cod_tabela, cod_grupo, cod_detalhamento, cod_informacao, cod_ordem) VALUES (?, ?, ?, ?, ?);";
                                tbComposicao = c.prepareStatement(sql);

                                tbComposicao.setInt(1, iCodTabela);
                                tbComposicao.setInt(2, iCodGrupo);
                                tbComposicao.setInt(3, iCodDetalhamento);
                                tbComposicao.setInt(4, iCodInformacao);
                                tbComposicao.setInt(5, iCodOrdem);
                                tbComposicao.executeUpdate();
                                Logger.getLogger(AlteraInformacao.class.getName()).log(Level.INFO, "SQL (Comando 1): " + tbComposicao.toString());
                            }
                            c.commit();
                            retorno.put("titulo", "Comando aceito!");
                            retorno.put("tipo", "success");
                            retorno.put("mensagem", "Informação gravada com sucesso!");
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
                            Logger.getLogger(AlteraInformacao.class.getName()).log(Level.SEVERE, null, ex);
                        } finally {
                            try {
                                // c.releaseSavepoint(original);
                                c.close();
                            } catch (SQLException ex) {
                                Logger.getLogger(AlteraInformacao.class.getName()).log(Level.SEVERE, null, ex);
                            }
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
            Logger.getLogger(AlteraInformacao.class.getName()).log(Level.WARNING, "Chamada para alterar a informação, mas sem os dados necessários.");
            retorno.put("titulo", "Erro!");
            retorno.put("tipo", "error");
            retorno.put("mensagem", "Chamada para alterar a informação, mas sem os dados necessários... Nada foi executado.");
        }
        response.getWriter().print(retorno);
    }
    
    /*
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
    */

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
