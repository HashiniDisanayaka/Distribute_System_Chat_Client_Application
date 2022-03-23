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

    public static JSONObject obtainJSONobject (String jStr) {
        JSONObject jsonObject = null;
        try {
            JSONParser jsonParser = new JSONParser();
            Object obj = jsonParser.parse(jStr);
            jsonObject = (JSONObject) obj;
        } catch (ParseException e) {
            System.out.println("[ERR] | " + e);
        }
        return  jsonObject;
    }

    public static boolean isHasKey(JSONObject jObj, String key) {
        return (jObj != null && jObj.get(key) != null);
    }
}
