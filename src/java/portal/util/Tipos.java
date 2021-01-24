package portal.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author f9575449
 */
public class Tipos {
    /*
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    
    public static String decodeUTF8(byte[] bytes) {
        return new String(bytes, UTF8_CHARSET);
    }   
    */
    
    public static boolean booleano(String valor) {
        try {
            return ("1".equalsIgnoreCase(valor) || "-1".equalsIgnoreCase(valor) || "yes".equalsIgnoreCase(valor) || "true".equalsIgnoreCase(valor) || "on".equalsIgnoreCase(valor));
        } catch (Exception e) {
            return false;
        }
    }
    public static boolean booleano(Object valor) {
        try {
            return booleano(valor.toString());
        } catch (Exception e) {
            return false;
        }
    }

    public static int inteiro(String valor) {
        try {
            // Tenta converter para um inteiro
            return Integer.parseInt(valor);
        } catch (Exception e) {
            // Em caso de erro, retorno 0
            return 0;
        }
    }

    public static int inteiro(Object valor) {
        try {
            // Tenta converter para um inteiro
            return Integer.parseInt(valor.toString());
        } catch (Exception e) {
            // Em caso de erro, retorno 0
            return 0;
        }
    }
    
    public static String texto(String Valor) {
        try {
            String sRet = new String( Valor.getBytes("ISO-8859-1"), "UTF-8" );
            // String sRet2 = decodeUTF8(Valor.getBytes("ISO-8859-1"));
            return sRet;
            /*
            Logger.getLogger(Tipos.class.getName()).log(Level.INFO, "Texto Original: {0}\nTexto Novo 1: {1}\nTexto Novo 2: {2}", new Object[]{Valor, sRet, sRet2});
            if (sRet.length()<=sRet2.length()) {
                return sRet;
            } else {
                return sRet2;
            }
            */
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Tipos.class.getName()).log(Level.SEVERE, null, ex);
            return Valor;
        }
    }
    
    public static String texto(Object Valor) {
        return texto(Valor.toString());
    }
    
    public static String trataParametroTexto(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "null";
        } else {
            return "'" + valor + "'";
        }
    }

    public static String trataParametroTexto(Object valor) {
        return trataParametroTexto(valor.toString());
    }

    public static String trataParametroNumero(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "null";
        } else {
            return valor;
        }
    }

    public static String trataParametroNumero(Object valor) {
        return trataParametroNumero(valor.toString());
    }
}
