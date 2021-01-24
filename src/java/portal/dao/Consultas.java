/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package portal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
// import portal.autenticador.UsuarioSSO;

/**
 *
 * @author f9575449
 */
public class Consultas {
    public JSONArray retornaSQL(String sql) {
        JSONArray resposta = new JSONArray();
        /* Logger.getLogger(Consultas.class.getName()).log(Level.INFO, "SQL recebido = " + sql); */
        
        try (
                Connection c = new Conexao().getConnection();
                PreparedStatement s = c.prepareCall(sql);
                ResultSet rs = s.executeQuery();
            ){
                /* Logger.getLogger(Consultas.class.getName()).log(Level.INFO, "SQL executado = " + s.toString()); */
                resposta = ResultsetConverter.converte(rs);
        } catch (SQLException ex) {
            Logger.getLogger(Consultas.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return resposta;
    }
    
    public int usuariosLogados() {
        int ret = 0;
        String sql = "SELECT COUNT(DISTINCT l.id_usuario) AS usuarios_logados FROM typeform.tb_log l WHERE l.dtm BETWEEN CURRENT_DATE AND CURRENT_DATE+1;";
        
        try (
                Connection c = new Conexao().getConnection();
                PreparedStatement s = c.prepareCall(sql.toString());
                ResultSet rs = s.executeQuery();
            ){
                if (rs.next()) {
                    ret = rs.getInt("usuarios_logados");
                } 
        } catch (SQLException ex) {
            Logger.getLogger(Consultas.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return ret;
    }
}
