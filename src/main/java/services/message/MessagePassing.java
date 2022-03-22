package services.message;

import chatServer.Server;
import org.json.simple.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MessagePassing {

    public static void Sender(JSONObject objct, Server endpoint) throws IOException
    {
        Socket socket = new Socket(endpoint.getServer_address(), endpoint.getCoordination_port());
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.write((objct.toJSONString() + "\n").getBytes( StandardCharsets.UTF_8));
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
}
