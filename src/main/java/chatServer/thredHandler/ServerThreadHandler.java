package chatServer.thredHandler;

import chatServer.Client;
import chatServer.Server;
import data.ServerState;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import services.heartbeat.Consensus;
import services.heartbeat.Gossiping;
import services.leaderElection.FastBullyAlgorithm;
import services.leaderElection.Leader;
import services.leaderElection.LeaderUpdate;
import services.message.MessagePassing;
import services.message.MessageServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ServerThreadHandler extends Thread{

    private final ServerSocket serverSocket_coordination;

    private LeaderUpdate leaderUpdate = new LeaderUpdate();

    public ServerThreadHandler(ServerSocket serverSocket_coordination) {
        this.serverSocket_coordination = serverSocket_coordination;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Socket socket_server = serverSocket_coordination.accept();
                BufferedReader bufferedReader =new BufferedReader(new InputStreamReader(socket_server.getInputStream(), StandardCharsets.UTF_8));
                String serverJsonString = bufferedReader.readLine();
                JSONObject jsonObject = MessagePassing.obtainJSONobject(serverJsonString);

                if(MessagePassing.isHasKey(jsonObject, "option")){
                    FastBullyAlgorithm.receiveMessages(jsonObject);
                } else if(MessagePassing.isHasKey(jsonObject, "type")){
                    if(jsonObject.get("type").equals("approval_requset_to_cleintid") && jsonObject.get("clientid") != null && jsonObject.get("sender") != null && jsonObject.get("threadid") != null){
                        String clientId = jsonObject.get("clientid").toString();
                        String threadId = jsonObject.get("threadid").toString();
                        int sender = Integer.parseInt(jsonObject.get("sender").toString());
                        boolean available = !Leader.getLeader().isClientIdAvailable(clientId);

                        if(available){
                            Client client = new Client(clientId, ServerState.getMainHallIdbyServerInt(sender), null);
                            Leader.getLeader().addClient(client);
                        }
                        Server endpointServer = ServerState.getInstance().getSetOfservers().get(sender);

                        try {
                            MessagePassing.sender(MessageServer.getApprovalReplyToClientId(String.valueOf(available), threadId), endpointServer);
                            System.out.println("[LOG] | Client id " + clientId + " from s" + sender + " is" + (available ? " " : " not ") + "available");
                        } catch (IOException e) {
//                            e.printStackTrace();
                            System.out.println("[ERR] | " + e.getMessage());
                        }
                    } else if(jsonObject.get("type").equals("approval_reply_to_cleintid") && jsonObject.get("available") != null && jsonObject.get("threadid") != null) {
                        int available = Boolean.parseBoolean(jsonObject.get("approved").toString()) ? 1 : 0;
                        Long threadId = Long.parseLong(jsonObject.get("threadid").toString());

                        ClientThreadHandler clientHandlerThread = ServerState.getInstance().getClientThreadHandler(threadId);
                        clientHandlerThread.setAvailableClientIdentity(available);
                        Object clientLock = clientHandlerThread.getClientLock();
                        synchronized (clientLock) {
                            clientLock.notifyAll();
                        }
                    } else if (jsonObject.get("type").equals("approval_request_to_room_create")) {
                        String clientId = jsonObject.get("clientid").toString();
                        String roomId = jsonObject.get("roomid").toString();
                        int sender = Integer.parseInt(jsonObject.get("sender").toString());
                        String threadId = jsonObject.get("threadid").toString();
                        boolean available = Leader.getLeader().isRoomCreationAvailable(roomId);

                        if (available) {
                            Leader.getLeader().addApprovedRoom(clientId, roomId, sender);
                        }
                        Server endpointSerevr = ServerState.getInstance().getSetOfservers().get(sender);
                        try {
                            MessagePassing.sender(MessageServer.getApprovalReplyToRoomCreate(String.valueOf(available), threadId), endpointSerevr);
                            System.out.println("[LOG] | Room " + roomId + " creation request from client with id " + clientId + " is" + (available ? " " : " not ") + "available");
                        } catch (Exception e) {
                            System.out.println("[ERR] | " + e.getMessage());
                        }
                    } else if (jsonObject.get("type").equals("approval_reply_to_room_create")) {
                        int available = Boolean.parseBoolean(jsonObject.get("available").toString()) ? 1 : 0;
                        Long threadId = Long.parseLong(jsonObject.get("threadid").toString());

                        ClientThreadHandler clientThreadHandler = ServerState.getInstance().getClientThreadHandler(threadId);
                        clientThreadHandler.setAvailableRoomCreation(available);
                        Object clientLock = clientThreadHandler.getClientLock();
                        synchronized (clientLock) {
                            clientLock.notifyAll();
                        }
                    } else if (jsonObject.get("type").equals("approval_request_to_join_room")) {
                        String clientId = jsonObject.get("clientid").toString();
                        String roomId = jsonObject.get("roomid").toString();
                        String formerRoomId = jsonObject.get("former").toString();
                        int sender = Integer.parseInt(jsonObject.get("sender").toString());
                        String threadId = jsonObject.get("threadid").toString();
                        boolean isLocalRoomChange = Boolean.parseBoolean(jsonObject.get("isLocalRoomChange").toString());

                        if (isLocalRoomChange) {
                            Client client = new Client(clientId, roomId, null);
                            Leader.getLeader().localJoinRoomClient(client, formerRoomId);
                        } else {
                            int serverIDofTargetRoom = Leader.getLeader().getServerIdForExistingRooms(roomId);

                            Server destServer = ServerState.getInstance().getSetOfservers().get(sender);

                            try {
                                boolean available = serverIDofTargetRoom != -1;
                                if (available) {
                                    Leader.getLeader().removeClient(clientId, formerRoomId);
                                }
                                Server serverOfTargetRoom = ServerState.getInstance().getSetOfservers().get(serverIDofTargetRoom);

                                String host = (available) ? serverOfTargetRoom.getServer_address() : "";
                                String port = (available) ? String.valueOf(serverOfTargetRoom.getClients_port()) : "";

                                MessagePassing.sender(MessageServer.getApprovalReplyToJoinRoom(String.valueOf(available), threadId, host, port),destServer);
                                System.out.println("[LOG] : Joining Room from room [" + formerRoomId + "] to room [" + roomId + "] for client " + clientId + " is" + (serverIDofTargetRoom != -1 ? " " : " not ") + "available");
                            } catch (Exception e) {
                                System.out.println("[ERR] | " + e.getMessage());
                            }
                        }

                    } else if (jsonObject.get("type").equals("approval_reply_to_join_room")) {
                        int available = Boolean.parseBoolean(jsonObject.get("available").toString()) ? 1 : 0;
                        Long threadId = Long.parseLong(jsonObject.get("threadid").toString());
                        String host = jsonObject.get("host").toString();
                        String port = jsonObject.get("port").toString();

                        ClientThreadHandler clientThreadHandler = ServerState.getInstance().getClientThreadHandler(threadId);
                        Object clientLock = clientThreadHandler.getClientLock();

                        synchronized (clientLock) {
                            clientThreadHandler.setAvailableJoinRoom(available);
                            clientThreadHandler.setAvailableJoinRoomServerHostAddress(host);
                            clientThreadHandler.setAvailableJoinRoomServerPort(port);
                            clientLock.notifyAll();
                        }

                    } else if (jsonObject.get("type").equals("move_acknowledgement")) {
                        String clientId = jsonObject.get("clientid").toString();
                        String roomId = jsonObject.get("roomid").toString();
                        int sender = Integer.parseInt(jsonObject.get("sender").toString());

                        Client client = new Client(clientId, roomId, null);
                        Leader.getLeader().addClient(client);

                        System.out.println("INFO : Moved Client [" + clientId + "] to server s" + sender + " and join the room [" + roomId + "] ");

                    } else if (jsonObject.get("type").equals("request_list")) {
                        String threadId = jsonObject.get("threadid").toString();
                        int sender = Integer.parseInt(jsonObject.get("sender").toString());

                        Server endpointServer = ServerState.getInstance().getSetOfservers().get(sender);

                        MessagePassing.sender(MessageServer.getListResponses(Leader.getLeader().getRoomIdList(), threadId),endpointServer);

                    } else if (jsonObject.get("type").equals("response_list")) {
                        String threadId = jsonObject.get("threadid").toString();
                        JSONArray roomsJSONArray = (JSONArray) jsonObject.get("rooms");
                        ArrayList<String> roomIdList = new ArrayList(roomsJSONArray);

                        ClientThreadHandler clientThreadHandler = ServerState.getInstance().getClientThreadHandler(Long.parseLong(threadId));
                        Object clientLock = clientThreadHandler.getClientLock();

                        synchronized (clientLock) {
                            clientThreadHandler.setTempRoomsList(roomIdList);
                            clientLock.notifyAll();
                        }

                    } else if (jsonObject.get("type").equals("delete_request")) {
                        String ownerId = jsonObject.get("owner").toString();
                        String roomId = jsonObject.get("roomid").toString();
                        String mainHallId = jsonObject.get("mainhall").toString();

                        Leader.getLeader().removeRoom(roomId, mainHallId, ownerId);

                    } else if (jsonObject.get("type").equals("quit")) {
                        String clientId = jsonObject.get("clientid").toString();
                        String formerRoomId = jsonObject.get("former").toString();

                        Leader.getLeader().removeClient(clientId, formerRoomId);
                        System.out.println("[LOG] | Leader deletd the client '" + clientId + "'.");

                    } else if (jsonObject.get("type").equals("leader_state_update")) {
                        if( Leader.getLeader().isLeaderElectedAndIamLeader() )
                        {
                            if(!leaderUpdate.isAlive()) {
                                leaderUpdate = new LeaderUpdate();
                                leaderUpdate.start();
                            }
                            leaderUpdate.receiveUpdate(jsonObject);
                        }

                    } else if (jsonObject.get("type").equals("leader_state_update_complete")) {
                        int serverId = Integer.parseInt(jsonObject.get("serverid").toString());
                        if( Leader.getLeader().isLeaderElectedAndMessageFromLeader(serverId) )
                        {
                            System.out.println("[LOG] | Received leader updated message from s" + serverId);
                            FastBullyAlgorithm.leaderUpdateComplete = true;
                        }

                    } else if (jsonObject.get("type").equals("gossip")) {
                        Gossiping.messageReceive(jsonObject);

                    } else if (jsonObject.get("type").equals("startVote")) {
                        Consensus.voteHandler(jsonObject);

                    } else if (jsonObject.get("type").equals("answervote")) {
                        Consensus.answerVoteHandler(jsonObject);

                    } else if (jsonObject.get("type").equals("notifyserverdown")) {
                        Consensus.notifyServerDownHandler(jsonObject);

                    } else {
                        System.out.println("[WARN] | Command error, Corrupted JSON from Server");
                    }
                }

            }
        } catch (IOException e) {
            System.out.println("[ERR] | " + e.getMessage());
        }
    }
}
