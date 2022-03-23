package services.message;

import org.json.simple.JSONObject;

import java.util.List;

public class MessageClient {

    public static JSONObject getAvailableNewID(String available) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "newidentity");
        jsonObject.put("available", available);
        return jsonObject;
    }

    public static JSONObject getJoinRoom(String clientId, String formerRoomId, String roomId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomchange");
        jsonObject.put("identity", clientId);
        jsonObject.put("former", formerRoomId);
        jsonObject.put("roomid", roomId);
        return jsonObject;
    }

    public static JSONObject getCreateRoom(String roomId, String available) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "createroom");
        jsonObject.put("roomid", roomId);
        jsonObject.put("approved", available);
        return jsonObject;
    }

    public static JSONObject getCreateRoomChange(String clientId, String former, String roomId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomchange");
        jsonObject.put("identity", clientId);
        jsonObject.put("former", former);
        jsonObject.put("roomid", roomId);
        return jsonObject;
    }

    public static JSONObject getMessage(String id, String content) {
        JSONObject join = new JSONObject();
        join.put("type", "message");
        join.put("identity",id);
        join.put("content",content);
        return join;
    }

    public static JSONObject getWho(String roomId, List<String> members, String ownerId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomcontents");
        jsonObject.put("roomid", roomId);
        jsonObject.put("identities", members);
        jsonObject.put("owner", ownerId);
        return jsonObject;
    }

    public static JSONObject getList(List<String> rooms) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomlist");
        jsonObject.put("rooms", rooms);
        return jsonObject;
    }

    public static JSONObject getDeleteRoom(String roomId, String isAvailable) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "deleteroom");
        jsonObject.put("roomid", roomId);
        jsonObject.put("approved", isAvailable);
        return jsonObject;
    }

    public static JSONObject getRoute(String roomId, String host, String port) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "route");
        jsonObject.put("roomid", roomId);
        jsonObject.put("host", host);
        jsonObject.put("port", port);
        return jsonObject;
    }

    public static JSONObject getServerChange(String available, String serverId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "serverchange");
        jsonObject.put("available", available);
        jsonObject.put("serverid", serverId);
        return jsonObject;
    }


}
