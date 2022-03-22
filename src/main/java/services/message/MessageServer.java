package services.message;

import org.json.simple.JSONObject;

import java.util.HashMap;

public class MessageServer {
    private static MessageServer instance =null;

    private MessageServer() {}

    public static synchronized MessageServer getInstance() {
        if (instance == null){
            instance = new MessageServer();
        }
        return instance;
    }

    public static JSONObject gossipingMessage (Integer serverIdentity, HashMap<Integer, Integer> listOfHeartbeat) {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("type", "gossip");
        jsonObj.put("serverIdentity", serverIdentity);
        jsonObj.put("ListOfHeartbeat", listOfHeartbeat);
        return jsonObj;
    }

    public static JSONObject startVoting (Integer serverIdentity, Integer suspectServerIdentity) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "startVote");
        jsonObject.put("serverIdentity", serverIdentity);
        jsonObject.put("suspectServerIdentity", suspectServerIdentity);
        return jsonObject;
    }

    public static JSONObject notifyServerDown(Integer serverIdentity) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "notifyserverdown");
        jsonObject.put("serverIdentity", serverIdentity);
        return jsonObject;
    }
}
