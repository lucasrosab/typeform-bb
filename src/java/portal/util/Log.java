package portal.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.postgresql.util.PGobject;
import portal.dao.Conexao;
import portal.servlets.AlteraInformacao;

/**
 *
 * @author F9575449
 */
public class Log {
    public static void GravaLog(String Chave, int Evento, JSONObject Info) {
        String sql;

        sql = "INSERT INTO typeform.tb_log (id_usuario, tp_evento, info) VALUES ((SELECT id_usuario FROM typeform.tb_usuario WHERE matricula = ?), ?, ?);";
        PGobject jsonInfo = new PGobject();
        jsonInfo.setType("json");
        try {
            jsonInfo.setValue(Info.toJSONString());
        } catch (SQLException e) {
            Logger.getLogger(Log.class.getName()).log(Level.SEVERE, null, e);
        }

        try (
                Connection c = new Conexao().getConnection();
                PreparedStatement s = c.prepareStatement(sql);)
            {
                s.setString(1, Chave);
                s.setInt(2, Evento);
                s.setObject(3, jsonInfo);
                
                System.out.println(s.toString());
                s.execute();
                Logger.getLogger(Log.class.getName()).log(Level.INFO, "SQL: " + s.toString());
        } catch (SQLException ex) {
            Logger.getLogger(Log.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void GravaLog(int Usuario, int Evento, JSONObject Info) {
        String sql;

        sql = "INSERT INTO typeform.tb_log (id_usuario, tp_evento, info) VALUES (?, ?, ?);";
        PGobject jsonInfo = new PGobject();
        jsonInfo.setType("json");
        try {
            jsonInfo.setValue(Info.toJSONString());
        } catch (SQLException e) {
            Logger.getLogger(Log.class.getName()).log(Level.SEVERE, null, e);
        }

        try (
                Connection c = new Conexao().getConnection();
                PreparedStatement s = c.prepareStatement(sql);)
            {
                s.setInt(1, Usuario);
                s.setInt(2, Evento);
                s.setObject(3, jsonInfo);
                s.execute();
                Logger.getLogger(Log.class.getName()).log(Level.INFO, "SQL: " + s.toString());
        } catch (SQLException ex) {
            Logger.getLogger(Log.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static String getIPCliente(HttpServletRequest request) {
        // ListaHeaders(request);
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
             xForwardedForHeader = request.getHeader("X-FORWARDED-FOR");
        }
        if (xForwardedForHeader == null) {
             xForwardedForHeader = request.getHeader("x-forwarded-for");
        }
        if (xForwardedForHeader == null) {
             xForwardedForHeader = request.getHeader("Proxy-Client-IP");
        }
        if (xForwardedForHeader == null) {
             xForwardedForHeader = request.getHeader("WL-Proxy-Client-IP");
        }
        if (xForwardedForHeader == null) {
             xForwardedForHeader = request.getHeader("HTTP_CLIENT_IP");
        }
        if (xForwardedForHeader == null) {
             xForwardedForHeader = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (xForwardedForHeader == null) {
             xForwardedForHeader = request.getHeader("rlnclientipaddr");
        }
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        } else {
            // As of https://en.wikipedia.org/wiki/X-Forwarded-For
            // The general format of the field is: X-Forwarded-For: client, proxy1, proxy2 ...
            // we only want the client
            return new StringTokenizer(xForwardedForHeader, ",").nextToken().trim();
        }
    }
    
    public static void ListaHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        
        while(headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            System.out.println("Header Name - " + headerName + ", Value - " + request.getHeader(headerName));
        }        
    }

    public static void ListaParams(HttpServletRequest request) {
        Enumeration<String> params = request.getParameterNames();
        
        while(params.hasMoreElements()) {
            String paramName = params.nextElement();
            System.out.println("Parameter Name - " + paramName + ", Value - " + request.getParameter(paramName));
        }        
    }
}
