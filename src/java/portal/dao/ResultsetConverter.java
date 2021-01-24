package portal.dao;

import java.io.UnsupportedEncodingException;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import portal.util.Tipos;

/**
 *
 * @author f9575449
 */
public class ResultsetConverter {
    public static JSONArray converte( ResultSet rs ) throws SQLException {
        JSONArray json = new JSONArray();
        ResultSetMetaData rsmd = rs.getMetaData();

        while(rs.next()) {
            int numColumns = rsmd.getColumnCount();
            /* Logger.getLogger(ResultsetConverter.class.getName()).log(Level.INFO, "Resultset com {0} coluna(s)", numColumns); */
            JSONObject obj = new JSONObject();

            for (int i=1; i<numColumns+1; i++) {
                String column_name = rsmd.getColumnLabel(i);
                int column_type = rsmd.getColumnType(i);
                String column_type_name = rsmd.getColumnTypeName(i);
                /* Logger.getLogger(ResultsetConverter.class.getName()).log(Level.INFO, "Coluna {0} ({1}): {2} / {3}", new Object[]{i, column_name, column_type, column_type_name}); */

                switch (column_type) {
                    case java.sql.Types.NULL:
                        obj.put(column_name, null);
                        break;
                    case java.sql.Types.ARRAY:
                        obj.put(column_name, rs.getArray(column_name));
                        break;
                    case java.sql.Types.BIGINT:
                        obj.put(column_name, rs.getLong(column_name));
                        break;
                    case java.sql.Types.BOOLEAN: case java.sql.Types.BIT:
                        obj.put(column_name, rs.getBoolean(column_name));
                        break;
                    case java.sql.Types.BLOB:
                        obj.put(column_name, rs.getBlob(column_name));
                        break;
                    case java.sql.Types.DOUBLE: case java.sql.Types.NUMERIC:
                        obj.put(column_name, rs.getDouble(column_name));
                        break;
                    case java.sql.Types.FLOAT:
                        obj.put(column_name, rs.getFloat(column_name));
                        break;
                    case java.sql.Types.INTEGER: case java.sql.Types.TINYINT: case java.sql.Types.SMALLINT:
                        obj.put(column_name, rs.getInt(column_name));
                        break;
                    case java.sql.Types.NVARCHAR:
                        obj.put(column_name, rs.getNString(column_name));
                        break;
                    case java.sql.Types.VARCHAR:
                        obj.put(column_name, rs.getString(column_name));
                        break;
                    case java.sql.Types.DATE:
                        try {
                            Date d = rs.getDate(column_name);
                            SimpleDateFormat dt = new SimpleDateFormat("dd/MM/yyyy");
                            obj.put(column_name, dt.format(d));
                        } catch (NullPointerException nex) {
                            obj.put(column_name, null);
                        }
                        break;
                    case java.sql.Types.TIMESTAMP:
                        obj.put(column_name, rs.getString(column_name));
                        /* obj.put(column_name, rs.getTimestamp(column_name)); */
                        break;
                    case java.sql.Types.CHAR:
                        // No MySQL, esse é o tipo de dados dos campos json
                        if(column_type_name.equalsIgnoreCase("JSON")) {
                            JSONParser pa = new JSONParser();
                            try {
                                String s = rs.getString(column_name);
                                Object o = pa.parse(s);
                                obj.put(column_name, o);
                            } catch (ParseException pex) {
                                obj.put(column_name, rs.getObject(column_name));
                                Logger.getLogger(ResultsetConverter.class.getName()).log(Level.WARNING, null, pex);
                            } catch (NullPointerException nex) {
                                obj.put(column_name, rs.getObject(column_name));
                            } catch (Exception ex) {
                                obj.put(column_name, rs.getObject(column_name));
                                Logger.getLogger(ResultsetConverter.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        } else {
                            obj.put(column_name, rs.getString(column_name));
                        }
                        break;
                    default:
                        // No Postgres, o tipo de dados JSON sai como java.sql.Types.OTHER
                        obj.put(column_name, rs.getObject(column_name));
                        break;
                }
                // Logger.getLogger(ResultsetConverter.class.getName()).log(Level.INFO, "Coluna {0} ({1}): {2} / {3} > {4}", new Object[]{i, column_name, column_type, column_type_name, obj.toString()});
            }

            json.add(obj);
        }

        return json;
    }
    
    public static JSONArray getParameterJSONArray( HttpServletRequest req, String NomeDesejado) {
        // Variável de retorno
        JSONArray json = new JSONArray();
        
        // Pra começar, vamos apurar os nomes das colunas
        List Colunas = new ArrayList();
        Enumeration<String> Nomes = req.getParameterNames();
        int Tam = 0;
        while (Nomes.hasMoreElements()) {
            String Nome = (String) Tipos.texto(Nomes.nextElement());
            Nome = Nome.replace("[", "]");
            String[] Elementos = Nome.split("]");
            if (Elementos[0].equals(NomeDesejado)) {
                int lTam = Integer.parseInt(Elementos[1]);
                if (lTam > Tam) {
                    Tam = lTam;
                }
                if (Tam == 0 && !Elementos[3].equals("$$hashKey")) {
                    Colunas.add(Elementos[3]);
                }
            }
        }
        Tam = Tam +1;
        
        // Com o nome das colunas definido, vamos montar o JSONObject de acordo
        for (int i = 0; i < Tam; i++) {
            JSONObject obj = new JSONObject();
            for (Object column_name : Colunas ) {
                String sValor = Tipos.texto( req.getParameter(NomeDesejado + "[" + i + "][" + column_name + "]") );
                obj.put(column_name, sValor);
                
                /*
                if (sValor.isEmpty()) {
                    obj.put(column_name, null);
                } else {
                    obj.put(column_name, sValor);
                }
                */
            }
            json.add(obj);
        }

        return json;
    }
}
