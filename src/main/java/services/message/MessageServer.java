package services.message;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
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

    public static JSONObject iamup(Integer serverIdentity) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "iamup");
        jsonObject.put("source", serverIdentity);
        return jsonObject;
    }
    public static JSONObject sendNomination(Integer serverIdentity) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "nomination");
        jsonObject.put("source", serverIdentity);
        return jsonObject;
    }
    public static JSONObject sendView(Integer serverIdentity) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "view");
        jsonObject.put("source", serverIdentity);
        return jsonObject;
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
        jsonObject.put("type", "leader_state_update");
        jsonObject.put("clients", clients);
        jsonObject.put("chatrooms", chatRooms);
        return jsonObject;
    }

    public static JSONObject getLeaderStateUpdateComplete(String serverId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "leader_state_update_complete");
        jsonObject.put("serverid", serverId);
        return jsonObject;
    }

    public static JSONObject getApprovalReplyToClientId(String available, String threadID) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "approval_reply_to_cleintid");
        jsonObject.put("available", available);
        jsonObject.put("threadid", threadID);
        return jsonObject;
    }

    public static JSONObject getApprovalRequestToClientId(String clientId, String sender, String threadId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "approval_requset_to_cleintid");
        jsonObject.put("clientid", clientId);
        jsonObject.put("sender", sender);
        jsonObject.put("threadid", threadId);
        return jsonObject;
    }

    public static JSONObject getApprovalRequestToRoomCreate(String clientId, String roomId, String sender, String threadId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "approval_request_to_room_create");
        jsonObject.put("clientid", clientId);
        jsonObject.put("roomid", roomId);
        jsonObject.put("sender", sender);
        jsonObject.put("threadid", threadId);
        return jsonObject;
    }

    public static JSONObject getApprovalReplyToRoomCreate(String available, String threadId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "approval_reply_to_room_create");
        jsonObject.put("available", available);
        jsonObject.put("threadid", threadId);
        return jsonObject;
    }

    public static JSONObject getApprovalRequestToJoinRoom(String clientId, String roomId, String formerRoomId, String sender, String threadId, String isLocalRoomChange) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "approval_request_to_join_room");
        jsonObject.put("sender", sender);
        jsonObject.put("roomid", roomId);
        jsonObject.put("former", formerRoomId);
        jsonObject.put("clientid", clientId);
        jsonObject.put("threadid", threadId);
        jsonObject.put("isLocalRoomChange", isLocalRoomChange);
        return jsonObject;
    }

    public static JSONObject getApprovalReplyToJoinRoom(String available, String threadId, String host, String port) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "approval_reply_to_join_room");
        jsonObject.put("available", available);
        jsonObject.put("host", host);
        jsonObject.put("port", port);
        jsonObject.put("threadid", threadId);
        return jsonObject;
    }

    public static JSONObject getMoveRequest(String clientId, String roomId, String formerRoomId, String sender, String threadId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "move_acknowledgement");
        jsonObject.put("sender", sender);
        jsonObject.put("roomid", roomId);
        jsonObject.put("former", formerRoomId);
        jsonObject.put("clientid", clientId);
        jsonObject.put("threadid", threadId);
        return jsonObject;
    }

    public static JSONObject getListRequests(String clientID, String threadID, String sender) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "request_list");
        jsonObject.put("sender", sender);
        jsonObject.put("clientid", clientID);
        jsonObject.put("threadid", threadID);
        return jsonObject;
    }

    public static JSONObject getListResponses(ArrayList<String> roomIdList, String threadId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "response_list");
        jsonObject.put("threadid", threadId);
        jsonObject.put("rooms", roomIdList);
        return jsonObject;
    }

    public static JSONObject getDeleteRoomRequest(String ownerId, String roomId, String mainHallId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "delete_request");
        jsonObject.put("owner", ownerId);
        jsonObject.put("roomid", roomId);
        jsonObject.put("mainhall", mainHallId);
        return jsonObject;
    }

    public static JSONObject getQuit(String clientId, String formerRoomId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "quit");
        jsonObject.put("clientid", clientId);
        jsonObject.put("former", formerRoomId);
        return jsonObject;
    }

    public static JSONObject answerMessage(Integer suspectServerId, String vote, Integer votedId){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "answervote");
        jsonObject.put("suspectServerId", suspectServerId);
        jsonObject.put("votedId", votedId);
        jsonObject.put("vote", vote);
        return jsonObject;
    }

}
