/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package slide.rest.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author slide
 * 
 */
public class Commands {
    
    private static final int STATUS_OK = 200;
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    
    HashMap<Integer, String> columnMap = new HashMap<>();
    
    int count = 0;
    
    public Commands() {
    }
    
    public void command(String sessionId, String jSonS, HttpExchange he) throws IOException {
        
        System.out.println("Received json:" + jSonS+" sessionId="+sessionId);
        //Convierte el json a hash map 
        Gson gson = new Gson();
        HashMap fromJsonHM = gson.fromJson(jSonS, HashMap.class);

        //obtiene el comando enviado desde el cliente nn
        String comando = (String) fromJsonHM.get("comando");
        if (comando == null) {
            comando = "";
        }

        //hash map que sera enviado al cliente
        HashMap<String, String> responseMap = new HashMap<>();
        String toJson = "";

        // comandos posibles enviados desde el cliente
        switch (comando) {
            case "test":
                count=count+1;
                //responde al cliente
                responseMap.put("comando", "test");
                responseMap.put("estado", "recibido:"+count);
                 {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Commands.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                toJson = gson.toJson(responseMap);
                break;            
            
            default:
                System.out.println("Instruccion no encontrada '" + comando + "'");
                //responde con error al cliente
                responseMap.put("comando", "error");
                responseMap.put("mensaje", "Instruccion no encontrada '" + comando + "'");
                break;
        }

        // envia la respuesta al cliente
        System.out.println("JsonretornadoAlaVista:" + toJson);
        byte[] rawResponseBody = toJson.getBytes(CHARSET);
        he.sendResponseHeaders(STATUS_OK, rawResponseBody.length);
        he.getResponseBody().write(rawResponseBody);
        
    }
    
}
