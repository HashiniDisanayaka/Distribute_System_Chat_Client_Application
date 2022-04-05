package chatServer.thredHandler;

import chatServer.Client;
import chatServer.Room;
import chatServer.Server;
import data.ServerState;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import services.MessageContext;
import services.leaderElection.Leader;
import services.message.MessageClient;
import services.message.MessagePassing;
import services.message.MessageServer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static services.message.MessagePassing.*;

public class ClientThreadHandler extends Thread{

    private final Socket socket;
    private String availableJoinRoomServerHostAddress;
    private String availableJoinRoomServerPort;
    final Object clientLock;
    private List<String> tempRoomsList;
    private int availableClientIdentity = -1;
    private int availableRoomCreation = -1;
    private int availableJoinRoom = -1;
    private Client client;
    private boolean quitState = false;

    public ClientThreadHandler(Socket socket) {
        this.socket = socket;
        this.clientLock = new Object();
    }

    public void setAvailableClientIdentity( int availableClientIdentity ) {
        this.availableClientIdentity = availableClientIdentity;
    }

    public void setAvailableRoomCreation( int availableRoomCreation ) {
        this.availableRoomCreation = availableRoomCreation;
    }

    public void setAvailableJoinRoom(int availableJoinRoom) {
        this.availableJoinRoom = availableJoinRoom;
    }

    public void setAvailableJoinRoomServerHostAddress(String availableJoinRoomServerHostAddress) {
        this.availableJoinRoomServerHostAddress = availableJoinRoomServerHostAddress;
    }

    public void setAvailableJoinRoomServerPort(String availableJoinRoomServerPort) {
        this.availableJoinRoomServerPort = availableJoinRoomServerPort;
    }

    public void setTempRoomsList(List<String> tempRoomsList) {
        this.tempRoomsList = tempRoomsList;
    }

    public Object getClientLock() {
        return clientLock;
    }

    public String getClientIdenetity () {
        return client.getClientID();
    }


    @Override
    public void run() {
        try {
            System.out.println("[LOG] | The client with ip address : " + socket.getInetAddress() + " , and port : " + socket.getPort() + " is connected ");

            BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            while (!quitState) {
                try {
                    String jsonFromClient = bufferedReader.readLine();

                    if (jsonFromClient==null){
                        continue;
                    }

                    //convert received message to json object
                    Object object = null;
                    JSONParser jsonParser = new JSONParser();
                    object = jsonParser.parse(jsonFromClient);
                    JSONObject jsonObject = (JSONObject) object;

                    if (isHasKey(jsonObject, "type")) {
                        if (jsonObject.get("type").equals("newidentity") && jsonObject.get("identity") != null) {
                            String newClientId = jsonObject.get("identity").toString();
                            newID(newClientId);
                        }

                        if (jsonObject.get("type").equals("createroom") && jsonObject.get("roomid") != null) {
                            String newRoomId = jsonObject.get("roomid").toString();
                            createRoom(newRoomId);
                        }

                        if (jsonObject.get("type").equals("who")) {
                            who();
                        }

                        if (jsonObject.get("type").equals("list")) {
                            list();
                        }

                        if (jsonObject.get("type").equals("joinroom")) {
                            String roomId = jsonObject.get("roomid").toString();
                            joinRoom(roomId);
                        }
                        if (jsonObject.get("type").equals("movejoin")) {
                            String roomID = jsonObject.get("roomid").toString();
                            String formerRoomID = jsonObject.get("former").toString();
                            String clientID = jsonObject.get("identity").toString();
                            moveJoin(roomID, formerRoomID, clientID);
                        }

                        if (jsonObject.get("type").equals("deleteroom")) {
                            String roomID = jsonObject.get("roomid").toString();
                            deleteRoom(roomID);
                        }

                        if (jsonObject.get("type").equals("message")) {
                            String content = jsonObject.get("content").toString();
                            message(content);
                        }

                        if (jsonObject.get("type").equals("quit")) {
                            quit();
                        }
                    } else {
                        System.out.println("[WARN] | Invalid JSON request");
                    }

                } catch (ParseException | InterruptedException | SocketException e) {
                    quit();
                    System.out.println("[WARN] : " + client.getClientID() + " forces quit : " + e.getMessage());
                }
            }

        } catch (IOException | InterruptedException e) {
            //e.printStackTrace();
            System.out.println("[WARN] : quit state exception : " + e.getMessage());
        }
        System.out.println("[INFO] : " + client.getClientID() + " Thread terminated");
    }

    private void newID(String clientId) throws IOException, InterruptedException
    {
        if (checkId(clientId)) {
            while(!Leader.getLeader().isLeaderElected()) {
                Thread.sleep(1000);
            }

            if (Leader.getLeader().isLeader()) {
                boolean available = !Leader.getLeader().isClientIdAlreadyTaken(clientId);
                availableClientIdentity = available ? 1 : 0;
                System.out.println("[LOG] | Client id " + clientId + " is" + (available ? " " : " not ") + "approved");

            } else {
                try {
                    MessagePassing.sendToLeader(MessageServer.getApprovalRequestToClientId(clientId, String.valueOf(ServerState.getInstance().getServerIdentity()),String.valueOf(this.getId())));
                    System.out.println("[LOG] | Client Id " + clientId + " sent to leader for approval");
                } catch (IOException e) {
                    System.out.println("[ERR] | " + e.getMessage());
                }

                synchronized (clientLock) {
                    while (availableClientIdentity == -1) {
                        clientLock.wait(7000);
                    }
                }
            }

            if( availableClientIdentity == 1 ) {
                System.out.println( "[LOG] | Received correct id :" + clientId );
                this.client = new Client( clientId, ServerState.getInstance().getMainHall().getRoomID(), socket );
                ServerState.getInstance().getMainHall().addMembers(client);

                if (Leader.getLeader().isLeader()) {
                    Leader.getLeader().addClient(new Client(clientId, client.getRoomID(), null ));
                }

                String mainHallId = ServerState.getInstance().getMainHall().getRoomID();
                HashMap<String, Client> mainHallClients = ServerState.getInstance().getSetOfRooms().get( mainHallId ).getSetOfClients();

                ArrayList<Socket> setOfSockets = new ArrayList<>();
                for( String mainHallClient : mainHallClients.keySet() ){
                    setOfSockets.add( mainHallClients.get( mainHallClient ).getSocket() );
                }

                MessageContext messageContext = new MessageContext().setMessageType(MessageContext.MESSAGE_TYPE.NEW_ID).setClientId(clientId).setIsNewClientIdAvailable("true").setFormerRoomId("").setRoomId(mainHallId);

                synchronized(socket){
                    messageSend( null, messageContext );
                    messageSend( setOfSockets, messageContext.setMessageType(MessageContext.MESSAGE_TYPE.JOIN_ROOM) );
                }
            }  else if( availableClientIdentity == 0 ) {
                MessageContext messageContext = new MessageContext().setMessageType(MessageContext.MESSAGE_TYPE.NEW_ID).setClientId(clientId).setIsNewClientIdAvailable("false");
                System.out.println("[WARN] | This id is already in use");
                messageSend(null, messageContext);
            }
            availableClientIdentity = -1;
        } else {
            MessageContext messageContext = new MessageContext().setClientId(clientId).setIsNewClientIdAvailable("false");
            System.out.println("[LOG] | This id type is wrong");
            messageSend(null, messageContext.setMessageType(MessageContext.MESSAGE_TYPE.NEW_ID));
        }
    }

    private void messageSend(ArrayList<Socket> socketList, MessageContext messageContext) throws IOException {
        JSONObject sendToClient = new JSONObject();
        if (messageContext.messageType.equals(MessageContext.MESSAGE_TYPE.NEW_ID)) {
            sendToClient = MessageClient.getApprovalNewID(messageContext.isNewClientIdAvailable);
            sendClient(sendToClient,socket);

        } else if (messageContext.messageType.equals(MessageContext.MESSAGE_TYPE.JOIN_ROOM)) {
            sendToClient = MessageClient.getJoinRoom(messageContext.clientId, messageContext.formerRoomId, messageContext.roomId);
            if (socketList != null) {
                sendToBroadcast(sendToClient, socketList);
            }

        } else if (messageContext.messageType.equals(MessageContext.MESSAGE_TYPE.ROUTE)) {
            sendToClient = MessageClient.getRoute(messageContext.roomId, messageContext.targetHost, messageContext.targetPort);
            sendClient(sendToClient, socket);

        } else if (messageContext.messageType.equals(MessageContext.MESSAGE_TYPE.SERVER_CHANGE)) {
            sendToClient = MessageClient.getServerChange(messageContext.isServerChangeAvailable, messageContext.availableServerId);
            sendClient(sendToClient, socket);

        } else if (messageContext.messageType.equals(MessageContext.MESSAGE_TYPE.CREATE_ROOM)) {
            sendToClient = MessageClient.getCreateRoom(messageContext.roomId, messageContext.isNewRoomIdAvailable);
            sendClient(sendToClient,socket);

        } else if (messageContext.messageType.equals(MessageContext.MESSAGE_TYPE.BROADCAST_JOIN_ROOM)) {
            sendToClient = MessageClient.getCreateRoomChange(messageContext.clientId, messageContext.formerRoomId, messageContext.roomId);
            sendToBroadcast(sendToClient, socketList);
        } else if (messageContext.messageType.equals(MessageContext.MESSAGE_TYPE.WHO)) {
            sendToClient = MessageClient.getWho(messageContext.roomId, messageContext.memberList, messageContext.clientId);//owner
            sendClient(sendToClient,socket);
        } else if (messageContext.messageType.equals(MessageContext.MESSAGE_TYPE.LIST)) {
            sendToClient = MessageClient.getList(messageContext.roomList);
            sendClient(sendToClient,socket);
        } else if (messageContext.messageType.equals(MessageContext.MESSAGE_TYPE.DELETE_ROOM)) {
            sendToClient = MessageClient.getDeleteRoom(messageContext.roomId, messageContext.isDeleteRoomAvailable);
            sendClient(sendToClient,socket);
        } else if (messageContext.messageType.equals(MessageContext.MESSAGE_TYPE.MESSAGE)) {
            sendToClient = MessageClient.getMessage(messageContext.clientId,messageContext.body);
            sendToBroadcast(sendToClient, socketList);
        }
    }

    private void createRoom(String newRoomId) throws IOException, InterruptedException
    {
        if (checkId(newRoomId) && !client.isRoomOwner()) {
            while(!Leader.getLeader().isLeaderElected()) {
                Thread.sleep(1000);
            }

            if (Leader.getLeader().isLeader()) {
                boolean available = Leader.getLeader().isRoomCreationAvailable(newRoomId);
                availableRoomCreation = available ? 1 : 0;
                System.out.println("[LOG] | Room " + newRoomId + " creation request from client " + client.getClientID() + " is " + (available ? " " : " not ") + "approved");

            } else {
                try {
                    MessagePassing.sendToLeader(MessageServer.getApprovalRequestToRoomCreate(client.getClientID(), newRoomId, String.valueOf(ServerState.getInstance().getServerIdentity()), String.valueOf(this.getId())));
                    System.out.println("[LOG] | Room " + newRoomId + " create request by " + client.getClientID() + " sent to leader for approval");
                } catch (Exception e) {
                    System.out.println("[ERR] | " + e.getMessage());
                }

                synchronized (clientLock) {
                    while (availableRoomCreation == -1) {
                        clientLock.wait(7000);
                    }
                }
            }

            if( availableRoomCreation == 1) {
                System.out.println( "[LOG] | Received room id " + newRoomId + "is coorct" );
                String formerRoomId = client.getRoomID();
                HashMap<String,Client> clientSet = ServerState.getInstance().getSetOfRooms().get(formerRoomId).getSetOfClients();

                ArrayList<Socket> socketFormer = new ArrayList<>();
                for( String each : clientSet.keySet() ){
                    socketFormer.add( clientSet.get( each ).getSocket() );
                }

                ServerState.getInstance().getSetOfRooms().get( formerRoomId ).removeMembers( client.getClientID() );

                Room newRoom = new Room( client.getClientID(), newRoomId, ServerState.getInstance().getServerIdentity() );
                ServerState.getInstance().getSetOfRooms().put(newRoomId, newRoom);

                client.setRoomID(newRoomId);
                client.setRoomOwner(true);
                newRoom.addMembers(client);

                if (Leader.getLeader().isLeader()) {
                    Leader.getLeader().addApprovedRoom(client.getClientID(), newRoomId, ServerState.getInstance().getServerIdentity());
                }

                synchronized (socket) {
                    MessageContext messageContext = new MessageContext().setClientId(client.getClientID()).setRoomId(newRoomId).setFormerRoomId(formerRoomId).setIsNewRoomIdAvailable("true");

                    messageSend(null, messageContext.setMessageType(MessageContext.MESSAGE_TYPE.CREATE_ROOM));
                    messageSend(socketFormer, messageContext.setMessageType(MessageContext.MESSAGE_TYPE.BROADCAST_JOIN_ROOM));
                }

            } else if ( availableRoomCreation == 0 ) {
                MessageContext messageContext = new MessageContext().setRoomId(newRoomId).setIsNewRoomIdAvailable("false");

                System.out.println("[WARN] | Room id [" + newRoomId + "] is already in use");
                messageSend(null, messageContext.setMessageType(MessageContext.MESSAGE_TYPE.CREATE_ROOM));
            }
            availableRoomCreation = -1;

        } else {
            MessageContext messageContext = new MessageContext().setRoomId(newRoomId).setIsNewRoomIdAvailable("false");
            System.out.println("[WARN] : Received wrong room id type (or client already owns a room " + newRoomId + " )");
            messageSend(null, messageContext.setMessageType(MessageContext.MESSAGE_TYPE.CREATE_ROOM));
        }
    }

    private void list() throws IOException, InterruptedException {
        tempRoomsList = null;
        while (!Leader.getLeader().isLeaderElected()) {
            Thread.sleep(1000);
        }

        if (Leader.getLeader().isLeader()) {
            tempRoomsList = Leader.getLeader().getRoomIdList();

        } else {
            MessagePassing.sendToLeader(
                    MessageServer.getListRequests(client.getClientID(), String.valueOf(this.getId()), String.valueOf(ServerState.getInstance().getServerIdentity())));
            synchronized (clientLock) {
                while (tempRoomsList == null) {
                    clientLock.wait(7000);
                }
            }
        }

        if (tempRoomsList != null) {
            MessageContext messageContext = new MessageContext().setRoomList(tempRoomsList);

            System.out.println("[LOG] | Room received :" + tempRoomsList);
            messageSend(null, messageContext.setMessageType(MessageContext.MESSAGE_TYPE.LIST));
        }
    }

    private void who() throws IOException {
        String roomId = client.getRoomID();
        Room room = ServerState.getInstance().getSetOfRooms().get(roomId);
        HashMap<String, Client> clientStateMap = room.getSetOfClients();
        List<String> participantsList = new ArrayList<>(clientStateMap.keySet());
        String ownerId = room.getOwnerId();

        MessageContext messageContext = new MessageContext().setClientId(ownerId).setRoomId(client.getRoomID()).setMemberList(participantsList);

        System.out.println("[LOG] | Members are in the room " + roomId + " : " + participantsList);
        messageSend(null, messageContext.setMessageType(MessageContext.MESSAGE_TYPE.WHO));
    }

    private void joinRoom(String roomId) throws IOException, InterruptedException {
        String roomIdFormer = client.getRoomID();

        if (client.isRoomOwner()) {
            MessageContext messageContext = new MessageContext().setClientId(client.getClientID()).setRoomId(roomIdFormer) .setFormerRoomId(roomIdFormer);

            System.out.println("[WARN] | Join room denied, Client " + client.getClientID() + " Owns another room");
            messageSend(null, messageContext.setMessageType(MessageContext.MESSAGE_TYPE.JOIN_ROOM));

        } else if (ServerState.getInstance().getSetOfRooms().containsKey(roomId)) {
            client.setRoomID(roomId);
            ServerState.getInstance().getSetOfRooms().get(roomIdFormer).removeMembers(client.getClientID());
            ServerState.getInstance().getSetOfRooms().get(roomId).addMembers(client);

            System.out.println("INFO : client [" + client.getClientID() + "] joined room :" + roomId);

            //create broadcast list
            HashMap<String, Client> clientListNew = ServerState.getInstance().getSetOfRooms().get(roomId).getSetOfClients();
            HashMap<String, Client> clientListOld = ServerState.getInstance().getSetOfRooms().get(roomIdFormer).getSetOfClients();
            HashMap<String, Client> clientList = new HashMap<>();
            clientList.putAll(clientListOld);
            clientList.putAll(clientListNew);

            ArrayList<Socket> SocketList = new ArrayList<>();
            for (String each : clientList.keySet()) {
                SocketList.add(clientList.get(each).getSocket());
            }

            MessageContext messageContext = new MessageContext().setClientId(client.getClientID()).setRoomId(roomId).setFormerRoomId(roomIdFormer);
            messageSend(SocketList, messageContext.setMessageType(MessageContext.MESSAGE_TYPE.BROADCAST_JOIN_ROOM));

            while (!Leader.getLeader().isLeaderElected()) {
                Thread.sleep(1000);
            }

            if (Leader.getLeader().isLeader()) {
                Leader.getLeader().localJoinRoomClient(client, roomIdFormer);
            } else {
                MessagePassing.sendToLeader(
                        MessageServer.getApprovalRequestToJoinRoom(
                                client.getClientID(),
                                roomId,
                                roomIdFormer,
                                String.valueOf(ServerState.getInstance().getServerIdentity()),
                                String.valueOf(this.getId()),
                                String.valueOf(true)
                        )
                );
            }

        } else {

            while (!Leader.getLeader().isLeaderElected()) {
                Thread.sleep(1000);
            }
            availableJoinRoom = -1;

            if (Leader.getLeader().isLeader()) {
                int serverIDofTargetRoom = Leader.getLeader().getServerIdForExistingRooms(roomId);
                availableJoinRoom = serverIDofTargetRoom != -1 ? 1 : 0;

                if (availableJoinRoom == 1) {
                    Server serverOfTargetRoom = ServerState.getInstance().getSetOfservers().get(serverIDofTargetRoom);
                    availableJoinRoomServerHostAddress = serverOfTargetRoom.getServer_address();
                    availableJoinRoomServerPort = String.valueOf(serverOfTargetRoom.getClients_port());
                }

                System.out.println("[LOG] | Responsed receievd : for route request for join room (server itself is the Leader)");

            } else {
                MessagePassing.sendToLeader(
                        MessageServer.getApprovalRequestToJoinRoom(
                                client.getClientID(),
                                roomId,
                                roomIdFormer,
                                String.valueOf(ServerState.getInstance().getServerIdentity()),
                                String.valueOf(this.getId()),
                                String.valueOf(false)
                        )
                );

                synchronized (clientLock) {
                    while (availableJoinRoom == -1) {
                        System.out.println("[LOG] | Waiting -------- server approving route on Join room request");
                        clientLock.wait(7000);
                    }
                }

                System.out.println("[LOG] : Received response to join room for route request");
            }

            if (availableJoinRoom == 1) {

                //broadcast to former room
                ServerState.getInstance().removeClient(client.getClientID(), roomIdFormer, getId());
                System.out.println("INFO : client [" + client.getClientID() + "] left room :" + roomIdFormer);

                //create broadcast list
                HashMap<String, Client> clientListOld = ServerState.getInstance().getSetOfRooms().get(roomIdFormer).getSetOfClients();
                System.out.println("INFO : Send broadcast to former room in local server");

                ArrayList<Socket> SocketList = new ArrayList<>();
                for (String each : clientListOld.keySet()) {
                    SocketList.add(clientListOld.get(each).getSocket());
                }

                MessageContext messageContext = new MessageContext()
                        .setClientId(client.getClientID())
                        .setRoomId(roomId)
                        .setFormerRoomId(roomIdFormer)
                        .setTargetHost(availableJoinRoomServerHostAddress)
                        .setTargetPort(availableJoinRoomServerPort);

                messageSend(SocketList,  messageContext.setMessageType(MessageContext.MESSAGE_TYPE.BROADCAST_JOIN_ROOM));

                //server change : route
                messageSend(SocketList,  messageContext.setMessageType(MessageContext.MESSAGE_TYPE.ROUTE));
                System.out.println("[LOG] | Route Message --> Client");
                quitState = true;

            } else if (availableJoinRoom == 0) {
                MessageContext messageContext = new MessageContext()
                        .setClientId(client.getClientID())
                        .setRoomId(roomIdFormer)
                        .setFormerRoomId(roomIdFormer);

                System.out.println("[WARN] | Received room id "+roomId + " does not exist");
                messageSend(null, messageContext.setMessageType(MessageContext.MESSAGE_TYPE.JOIN_ROOM));
            }

            availableJoinRoom = -1;
        }
    }

    private void moveJoin(String roomId, String roomIdFormer, String clientId) throws IOException, InterruptedException {
        roomId = (ServerState.getInstance().getSetOfRooms().containsKey(roomId))? roomId:ServerState.getInstance().getMainHallId();
        this.client = new Client(clientId, roomId, socket);
        ServerState.getInstance().getSetOfRooms().get(roomId).addMembers(client);

        HashMap<String, Client> clientListNew = ServerState.getInstance().getSetOfRooms().get(roomId).getSetOfClients();

        ArrayList<Socket> SocketList = new ArrayList<>();
        for (String each : clientListNew.keySet()) {
            SocketList.add(clientListNew.get(each).getSocket());
        }

        MessageContext messageContext = new MessageContext()
                .setClientId(client.getClientID())
                .setRoomId(roomId)
                .setFormerRoomId(roomIdFormer)
                .setIsServerChangeAvailable("true")
                .setAvailableServerId(ServerState.getInstance().getServerid());

        messageSend(null, messageContext.setMessageType(MessageContext.MESSAGE_TYPE.SERVER_CHANGE));
        messageSend(SocketList, messageContext.setMessageType(MessageContext.MESSAGE_TYPE.BROADCAST_JOIN_ROOM));

        //TODO : check sync
        while (!Leader.getLeader().isLeaderElected()) {
            Thread.sleep(1000);
        }

        if (Leader.getLeader().isLeader()) {
            Client client = new Client(clientId, roomId, null);
            Leader.getLeader().addClient(client);

        } else {
            MessagePassing.sendToLeader(
                    MessageServer.getMoveRequest(
                            client.getClientID(),
                            roomId,
                            roomIdFormer,
                            String.valueOf(ServerState.getInstance().getServerIdentity()),
                            String.valueOf(this.getId())
                    )
            );
        }
    }

    private void deleteRoom(String roomId) throws IOException, InterruptedException {

        String mainHallId = ServerState.getInstance().getMainHall().getRoomID();

        if (ServerState.getInstance().getSetOfRooms().containsKey(roomId)) {
            Room room = ServerState.getInstance().getSetOfRooms().get(roomId);
            if (room.getOwnerId().equals(client.getClientID())) {

                HashMap<String,Client> formerClientList = ServerState.getInstance().getSetOfRooms()
                        .get(roomId).getSetOfClients();

                HashMap<String,Client> mainHallClientList = ServerState.getInstance().getSetOfRooms()
                        .get(mainHallId).getSetOfClients();
                mainHallClientList.putAll(formerClientList);

                ArrayList<Socket> socketList = new ArrayList<>();
                for (String each : mainHallClientList.keySet()){
                    socketList.add(mainHallClientList.get(each).getSocket());
                }

                ServerState.getInstance().getSetOfRooms().remove(roomId);
                client.setRoomOwner( false );

                for(String client:formerClientList.keySet()){
                    String clientId = formerClientList.get(client).getClientID();
                    formerClientList.get(client).setRoomID(mainHallId);
                    ServerState.getInstance().getSetOfRooms().get(mainHallId).addMembers(formerClientList.get(client));

                    MessageContext messageContext = new MessageContext()
                            .setClientId(clientId)
                            .setRoomId(mainHallId)
                            .setFormerRoomId(roomId);

                    messageSend(socketList, messageContext.setMessageType(MessageContext.MESSAGE_TYPE.BROADCAST_JOIN_ROOM));
                }

                MessageContext messageContext = new MessageContext()
                        .setRoomId(roomId)
                        .setIsDeleteRoomAvailable("true");

                //TODO : check sync
                while (!Leader.getLeader().isLeaderElected()) {
                    Thread.sleep(1000);
                }

                if (Leader.getLeader().isLeader()) {
                    Leader.getLeader().removeRoom(roomId, mainHallId, client.getClientID());

                } else {
                    MessagePassing.sendToLeader(MessageServer.getDeleteRoomRequest(client.getClientID(), roomId, mainHallId)
                    );
                }

                System.out.println("[LOG] | " + client.getClientID() + " deletes room " + roomId  );

            } else {
                MessageContext messageContext = new MessageContext()
                        .setRoomId(roomId)
                        .setIsDeleteRoomAvailable("false");

                messageSend(null, messageContext.setMessageType(MessageContext.MESSAGE_TYPE.DELETE_ROOM));
                System.out.println("[WARN] | " + client.getClientID() + " is not the owner of " + roomId );
            }

        } else {
            MessageContext messageContext = new MessageContext()
                    .setRoomId(roomId)
                    .setIsDeleteRoomAvailable("false");

            messageSend(null, messageContext.setMessageType(MessageContext.MESSAGE_TYPE.DELETE_ROOM));
            System.out.println("[WARN] | Received room id " + roomId + " does not exist");
        }
    }

    //quit room
    private void quit() throws IOException, InterruptedException {

        //delete room if room owner
        if (client.isRoomOwner()){
            deleteRoom(client.getRoomID());
            System.out.println("INFO : Deleted room before " + client.getClientID() + " quit");
        }

        //send broadcast with empty target room for quit
        HashMap<String,Client> formerClientList = ServerState.getInstance().getSetOfRooms().get(client.getRoomID()).getSetOfClients();

        ArrayList<Socket> socketList = new ArrayList<>();
        for (String each:formerClientList.keySet()){
            socketList.add(formerClientList.get(each).getSocket());
        }
        MessageContext messageContext = new MessageContext()
                .setClientId(client.getClientID())
                .setRoomId("")
                .setFormerRoomId(client.getRoomID());
        messageSend(socketList, messageContext.setMessageType(MessageContext.MESSAGE_TYPE.BROADCAST_JOIN_ROOM));

        ServerState.getInstance().removeClient(client.getClientID(), client.getRoomID(), getId());

        if( !Leader.getLeader().isLeader() ) {
            MessagePassing.sendToLeader(MessageServer.getQuit(client.getClientID(), client.getRoomID()));

        } else {
            // Leader is self , removes client from global list
            Leader.getLeader().removeClient(client.getClientID(),client.getRoomID() );
        }

        if (!socket.isClosed()) socket.close();
        quitState = true;
        System.out.println("INFO : " + client.getClientID() + " quit");
    }

    private void message(String content) throws IOException {
        String clientId = client.getClientID();
        String roomId = client.getRoomID();
        HashMap<String, Client> clientList = ServerState.getInstance().getSetOfRooms().get(roomId).getSetOfClients();

        ArrayList<Socket> socketList = new ArrayList<>();
        for (String each:clientList.keySet()){
            if (!clientList.get(each).getClientID().equals(clientId)){
                socketList.add(clientList.get(each).getSocket());
            }
        }
        MessageContext messageContext = new MessageContext()
                .setClientId(clientId)
                .setBody(content);

        messageSend(socketList, messageContext.setMessageType(MessageContext.MESSAGE_TYPE.MESSAGE));
    }

}
