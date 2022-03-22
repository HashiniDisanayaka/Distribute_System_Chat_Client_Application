package services.message;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.List;

public class MessageServer {
    private static MessageServer instance =null;

    private MessageServer() {}

    public static synchronized MessageServer getInstance() {
        if (instance == null){
            instance = new MessageServer();
        }
        return instance;
    }

    public static JSONObject gossipingMessage(Integer serverIdentity, HashMap<Integer, Integer> listOfHeartbeat) {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("type", "gossip");
        jsonObj.put("serverIdentity", serverIdentity);
        jsonObj.put("ListOfHeartbeat", listOfHeartbeat);
        return jsonObj;
    }

    public static JSONObject heartbeat( String sender) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "heartbeat");
        jsonObject.put("sender", sender);
        return jsonObject;
    }

    public static JSONObject election(String source) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "election");
        jsonObject.put("source", source);
        return jsonObject;
    }

    public static JSONObject sendOk(String sender) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "ok");
        jsonObject.put("sender", sender);
        return jsonObject;
    }

    public static JSONObject coordinator(String leader) {
        // {"option": "coordinator", "leader": "s3"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "coordinator");
        jsonObject.put("leader", leader);
        return jsonObject;
    }

    public static JSONObject LeaderUpdate( List<String> clientIdList, List<List<String>> chatRoomList) {
        JSONArray clients = new JSONArray();
        clients.addAll( clientIdList );

        JSONArray chatRooms = new JSONArray();
        for( List<String> chatRoomObj : chatRoomList ) {
            JSONObject chatRoom = new JSONObject();
            chatRoom.put( "clientid", chatRoomObj.get( 0 ) );
            chatRoom.put( "roomid", chatRoomObj.get( 1 ) );
            chatRoom.put( "serverid", chatRoomObj.get( 2 ) );
            chatRooms.add( chatRoom );
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "leaderstateupdate");
        jsonObject.put("clients", clients);
        jsonObject.put("chatrooms", chatRooms);
        return jsonObject;
    }
}
