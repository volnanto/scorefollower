package at.ofai.music.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.websocket.EncodeException;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
        
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
 
/** 
 * @ServerEndpoint gives the relative name for the end point
 * This will be accessed via ws://localhost:8080/WebApp/echo
 * Where "localhost" is the address of the host,
 * "EchoChamber" is the name of the package
 * and "echo" is the address to access this class from the server
 */



@ServerEndpoint("/echo") 
public class EchoServer {



    /**
     * @OnOpen allows us to intercept the creation of a new session.
     * The session class allows us to send data to the user.
     * In the method onOpen, we'll let the user know that the handshake was 
     * successful.
     */
    @OnOpen
    public void onOpen(Session session) throws EncodeException{        
        System.out.println(session.getId() + " has opened a connection"); 
        try {
            session.getBasicRemote().sendText("Connection Established");

            session.getBasicRemote().sendObject(System.currentTimeMillis()/1000L); //Connection Starting Time

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
 
    /**
     * When a user sends a message to the server, this method will intercept the message
     * and allow us to react to it. For now the message is read as a String.
     */
    private static Set<Session> allSessions;
    
    @OnMessage
    public void onMessage(String message, Session session) throws EncodeException, IOException{
       
        System.out.println("Message from " + session.getId() + ": " + message);
        DataHandler x = new DataHandler();
        HashMap <Integer, String> map  = x.getHashmap();
        imageURL y = new imageURL();
        HashMap <Integer, String> url  = y.getURLmap();
        DataStorage z = new DataStorage();
        HashMap <Integer, Integer> refMap  = z.getHashmap();
        
        
        int refTime = Integer.parseInt(message);
        int bar = refMap.get(refTime);
        allSessions = session.getOpenSessions();
      
          for (Session sess: allSessions){
              JsonObject obj = Json.createObjectBuilder()
              .add("refTime", refTime)
              .add("bar", bar)
              .add("messageType", "setMusicScorePosition")
              .add("position", map.get(bar))
              .add("image", url.get(bar))
              .add("time", System.currentTimeMillis()/1000L)
              .build();
          sess.getBasicRemote().sendObject(obj);
          }
        
        
//        try {
//           JsonObject obj = Json.createObjectBuilder()
//              .add("time", message)
//              .add("messageType", "000000")
//              .add("bar", Json.createObjectBuilder()
//                     .add("position", "00 00 00 00"))
//              .add("image", "URL")
//              .build();
//            session.getBasicRemote().sendObject(obj);
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
    }
 
    /**
     * The user closes the connection.
     * 
     * Note: you can't send messages to the client from this method
     */
    @OnClose
    public void onClose(Session session){
        System.out.println("Session " +session.getId()+" has ended");
    }
    
}
    
    
   
