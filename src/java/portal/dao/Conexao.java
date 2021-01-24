package portal.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author f9575449
 */
public class Conexao {
    private final String usuario = "webpdc";
    private final String senha = "80088008"; 
    private final String bancodados = "pdc";
    private String servidor;

    public Conexao() {
        boolean Local = false;
        
        if (Local) {
            servidor = "jdbc:postgresql://localhost:5432/";
        } else {
            // servidor = "jdbc:postgresql://172.20.125.1:5432/";
            servidor = "jdbc:postgresql://172.17.207.146:5432/";
        }
    }
    
    /**
     * Metodo que tenta conectar no banco de dados
     * 
     * @return      Retorna um objeto Connection caso a conexao seja bem-sucedida.
     *              Null caso a conexao nao seja bem-sucedida
     */
    public Connection getConnection() {
        try { 
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Cadê o driver do postgres?");
            Logger.getLogger(Conexao.class.getName()).log(Level.SEVERE, null, e);
            return null;
        }
        Connection c = null;
        
        try {
            c = DriverManager.getConnection(servidor + bancodados, usuario, senha); 
        } catch (SQLException e) {
            Logger.getLogger(Conexao.class.getName()).log(Level.SEVERE, null, e);
	}
        
        return c;
    }
}
