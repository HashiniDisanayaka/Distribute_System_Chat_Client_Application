package services.message;

import chatServer.Server;
import data.ServerState;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import services.leaderElection.Leader;
import services.leaderElection.Leader;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MessagePassing {

    public static void sender(JSONObject objct, Server endpoint) throws IOException
    {
        Socket socket = new Socket(endpoint.getServer_address(), endpoint.getCoordination_port());
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.write((objct.toJSONString() + "\n").getBytes( StandardCharsets.UTF_8));
        dataOutputStream.flush();
    }

    public static void sendToLeader(JSONObject obj) throws IOException
    {
        Server destServer = ServerState.getInstance().getSetOfservers().get( Leader.getLeader().getLeaderID() );
        Socket socket = new Socket(destServer.getServer_address(), destServer.getCoordination_port());
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.write((obj.toJSONString() + "\n").getBytes( StandardCharsets.UTF_8));
        dataOutputStream.flush();
    }

    public static void sendBroadcast(JSONObject obj, ArrayList<Server> serverList) throws IOException {
        for (Server server : serverList) {
            Socket socket = new Socket(server.getServer_address(), server.getCoordination_port());
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.write((obj.toJSONString() + "\n").getBytes( StandardCharsets.UTF_8));
            dataOutputStream.flush();
        }
    }

    public static void sendToBroadcast(JSONObject obj, ArrayList<Socket> socketList) throws IOException {
        for (Socket each : socketList) {
            Socket TEMP_SOCK = (Socket) each;
            PrintWriter TEMP_OUT = new PrintWriter(TEMP_SOCK.getOutputStream());
            TEMP_OUT.println(obj);
            TEMP_OUT.flush();
        }
    }

    public static JSONObject obtainJSONobject (String jStr) {
        JSONObject jsonObject = null;
        try {
            JSONParser jsonParser = new JSONParser();
            Object obj = jsonParser.parse(jStr);
            jsonObject = (JSONObject) obj;
        } catch (ParseException e) {
            System.out.println("[ERR] | " + e.getMessage());
        }
        return  jsonObject;
    }

    public static boolean isHasKey(JSONObject jObj, String key) {
        return (jObj != null && jObj.get(key) != null);
    }

    public static boolean checkId(String id) {
        return (Character.toString(id.charAt(0)).matches("[a-zA-Z]+")
                && id.matches("[a-zA-Z0-9]+") && id.length() >= 3 && id.length() <= 16);
    }

    public static void sendClient(JSONObject jsonObject, Socket socket) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.write((jsonObject.toJSONString() + "\n").getBytes(StandardCharsets.UTF_8));
        dataOutputStream.flush();
    }
}
