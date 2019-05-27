package slide.rest.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/* Por: slide 
 * ESTA CLASE NO REQUIERE DE AUMENTAR NI MODIFICAR CODIGO
 * PARA AGREGAR O MODIFICAR COMANDOS, VER CLASE Commands
 *
 *Caracteristicasdel server.-
 *cuando la Factura es manual solo envia:
 *-NIT emisor, Numero de factura, numero de autorizacion, fecha de emision
 * Los demas parametros no los necesita.
 *La pagina web se da cuenta pòr el numero de autorizacion, este codigo se da cuenta 
 *porque no se pone nada en el codigo de control (Tools.sendPost)
 *El campo "total" enviado a la pagina, es enviado sin redondear el numero.
 *
 */
public class Server {

    private static final String HOSTNAME = "0.0.0.0";
    private static final int PORT = 8001;
    private static final int BACKLOG = 1;

    private static final String HEADER_ALLOW = "Allow";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final int STATUS_OK = 200;
    private static final int STATUS_METHOD_NOT_ALLOWED = 405;

    private static final int NO_RESPONSE_LENGTH = -1;

    private static final String METHOD_GET = "GET";
    private static final String METHOD_OPTIONS = "OPTIONS";
    private static final String ALLOWED_METHODS = METHOD_GET + "," + METHOD_OPTIONS;
    
    Commands c;

    public Server() {
        c = new Commands();
        try {
            conectaConCache();
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void conectaConCache() throws SQLException, Exception {
        //String host = leeArchivoConfig.readConfig("host");//"localhost";
    }

    public void inicio() throws IOException {
        System.out.println("Server rest iniciado");
        
        final HttpServer server = HttpServer.create(new InetSocketAddress(HOSTNAME, PORT), BACKLOG);
        server.createContext("/", he -> {
            try {               
                    final Headers headers = he.getResponseHeaders();
                    final String requestMethod = he.getRequestMethod().toUpperCase();
                    switch (requestMethod) {
                        
                        case "POST":
                            setHeaders(headers);
                            //obtiene los datos json enviados desde la pagina mediante el metodo post
                            Object jSonObj = getRemoteGsonObj(he);
                            String jSonS = jSonObj.toString();
                            commandProcesor(jSonS, he);
                            //System.out.println("POST");
                            break;
                        case METHOD_OPTIONS:
                            //headers.set(HEADER_ALLOW, ALLOWED_METHODS);
                            setHeaders(headers);
                            he.sendResponseHeaders(STATUS_OK, NO_RESPONSE_LENGTH);
                            System.out.println("OPTIONS");
                            break;
                        default:
                            headers.set(HEADER_ALLOW, ALLOWED_METHODS);
                            he.sendResponseHeaders(STATUS_METHOD_NOT_ALLOWED, NO_RESPONSE_LENGTH);
                            break;
                    }
            } catch (Exception ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                he.close();
            }
        });
        server.start();
    }

    HashMap<String, Object> sessionMap = new HashMap<>();

    private void commandProcesor(String jSonS, HttpExchange he) throws SQLException, IOException {
        Gson gson = new Gson();
        HashMap jsonMap = gson.fromJson(jSonS, HashMap.class);
        
        String sessionId = (String) jsonMap.get("sessionId");
        if (sessionId == null) {
            sessionId = "0";
        }
        /*
        if (sessionId.equalsIgnoreCase("0")) {
            //generate a new session number
            int sessionNumber = (1 + (int) (Math.random() * 100000000));
            sessionId = "" + sessionNumber;
            c = new Commands();
            sessionMap.put(sessionId, c);
        } else {
            c = (Commands) sessionMap.get(sessionId);
        }
        */
        c.command(sessionId, jSonS, he);
    }

    private Object getRemoteGsonObj(HttpExchange he) throws UnsupportedEncodingException {
        InputStreamReader isr = new InputStreamReader(he.getRequestBody(), "utf-8");
        BufferedReader br = new BufferedReader(isr);
        JsonParser parser = new JsonParser();
        JsonObject obj = (JsonObject) parser.parse(br);
        return obj;
    }

    private void setHeaders(Headers headers) {
        headers.set("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        headers.add("Access-Control-Max-Age", "86400");//24 horas para CORS
        headers.add("Access-Control-Allow-Headers", "Origin,X-Requested-With,Content-Type,Accept");
        headers.add("Allow", "GET, HEAD, POST, TRACE, OPTIONS");
    }

    private static Map<String, List<String>> getRequestParameters(final URI requestUri) {
        final Map<String, List<String>> requestParameters = new LinkedHashMap<>();
        final String requestQuery = requestUri.getRawQuery();
        if (requestQuery != null) {
            final String[] rawRequestParameters = requestQuery.split("[&;]", -1);
            for (final String rawRequestParameter : rawRequestParameters) {
                final String[] requestParameter = rawRequestParameter.split("=", 2);
                final String requestParameterName = decodeUrlComponent(requestParameter[0]);
                requestParameters.putIfAbsent(requestParameterName, new ArrayList<>());
                final String requestParameterValue = requestParameter.length > 1 ? decodeUrlComponent(requestParameter[1]) : null;
                requestParameters.get(requestParameterName).add(requestParameterValue);
            }
        }
        return requestParameters;
    }

    private static String decodeUrlComponent(final String urlComponent) {
        try {
            return URLDecoder.decode(urlComponent, CHARSET.name());
        } catch (final UnsupportedEncodingException ex) {
            throw new InternalError(ex);
        }
    }
    
     public static void main(String[] args) {
        try {
            Server server = new Server();
            server.inicio(); 
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
