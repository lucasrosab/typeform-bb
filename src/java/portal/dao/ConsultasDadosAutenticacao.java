package portal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import portal.autenticador.UsuarioSSO;
import portal.autenticador.UtilSSO;
import portal.util.Log;

/**
 *
 * @author f9575449
 */
public class ConsultasDadosAutenticacao {
    public boolean insereNovoToken(String tokenId, String respostaAutenticador) throws SQLException {
        UtilSSO c;
        UsuarioSSO u;
        
        try {
            c = new UtilSSO();
            u = c.getUsuario(tokenId, respostaAutenticador);
        } catch (Exception e) {
            Logger.getLogger(ConsultasDadosAutenticacao.class.getName()).log(Level.SEVERE, "Causa: {0}", e.getMessage());
            u = null;
        }
        
        boolean resposta = false;
        String consulta = null;

        if (u == null) {
            // Verificar esses usuários... é estranho não haver dados sobre eles.
            // consulta = "INSERT INTO rdd.tb_usuario(dt_token, token, resposta) VALUES (CURRENT_DATE, ?, ?);";
            Logger.getLogger(ConsultasDadosAutenticacao.class.getName()).log(Level.WARNING, "IMPORTANTE: Verificar esta tentativa de acesso:");
            System.out.printf("================================================================================");
            System.out.printf("dt_token: %s     token: %s      resposta: %s", LocalDate.now().toString(), tokenId, respostaAutenticador);
            System.out.printf("================================================================================");
        } else {
            /* Como o postgres 9.4 não suporta ON CONFLICT, vamos usar uma function por enquanto...
            consulta = "INSERT INTO rdd.tb_usuario (dt_token, token, resposta, matricula, nome, nm_reduzido, uor_posicao, uor_localizacao) " +
                       "VALUES (CURRENT_DATE, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (matricula) DO UPDATE " +
                       "SET nome = EXCLUDED.nome, nm_reduzido = EXCLUDED.nm_reduzido, uor_posicao = EXCLUDED.uor_posicao, uor_localizacao = EXCLUDED.uor_localizacao, token = EXCLUDED.token, dt_token = EXCLUDED.dt_token, resposta = EXCLUDED.resposta;";
            */
            consulta = "SELECT typeform.proc_usuario(?, ?, ?, ?, ?, ?, ?) AS retorno;";
            
            Connection con = null;
            PreparedStatement stmt = null;

            try {
                con = new Conexao().getConnection();
                stmt = con.prepareStatement(consulta);
                stmt.setString(1, tokenId);
                stmt.setString(2, respostaAutenticador);
                stmt.setString(3, u.chave);
                stmt.setString(4, u.nome);
                stmt.setString(5, u.nomeGuerra);
                stmt.setInt(6, Integer.parseInt(u.uorTrabalho));
                stmt.setInt(7, Integer.parseInt(u.uorDependencia));

                // System.out.println(stmt.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    resposta = (rs.getInt("retorno") == 0);
                } 

                JSONObject ret = new JSONObject();
                ret.put("acessos", u.acessos);

                // Grava log de login
                Log.GravaLog(u.chave, 1, ret);
                
                // Como esta instrução é muito grande, vou desativar o log dela
                // Logger.getLogger(ConsultasDadosAutenticacao.class.getName()).log(Level.INFO, "Executado SQL: " + stmt.toString());
            } catch (SQLException ex) {
                Logger.getLogger(ConsultasDadosAutenticacao.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                stmt.close();
                con.close();
            }
        }


        return resposta;
    }

    public String constulta_token(String tokenId) throws SQLException {
        String resposta = null;

        // Garante que somente seja usado a informação do MySQL quando houve acesso no dia
        String consulta = "SELECT resposta FROM typeform.tb_usuario WHERE token = ? AND dtm_token >= CURRENT_DATE LIMIT 1; ";

        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = new Conexao().getConnection();

            stmt = con.prepareStatement(consulta);
            stmt.setString(1, tokenId);

            rs = stmt.executeQuery();
            Logger.getLogger(ConsultasDadosAutenticacao.class.getName()).log(Level.INFO, "Executado SQL: " + stmt.toString());
            
            if (rs.next()) {
                resposta = rs.getString("resposta");
            } 

        } catch (SQLException ex) {
            Logger.getLogger(ConsultasDadosAutenticacao.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            rs.close();
            stmt.close();
            con.close();
        }

        return resposta;
    }
}
