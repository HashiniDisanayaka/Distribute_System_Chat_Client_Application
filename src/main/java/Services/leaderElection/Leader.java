package services.leaderElection;

import chatServer.Client;
import chatServer.Room;
import data.ServerState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Leader
{
    private Integer leaderID;

    private final List<String> activeClients = new ArrayList<>();
    private final HashMap<String,Room> activeChatRooms = new HashMap<>(); // <roomID, room obj>

    // singleton
    private static Leader leader;

    private Leader() {
    }

    public static Leader getLeader() {
        if (leader == null) {
            synchronized (Leader.class) {
                if (leader == null) {
                    leader = new Leader();
                }
            }
        }
        return leader;
    }

    public void setLeaderID( int leaderID )
    {
        this.leaderID = leaderID;
    }

    public Integer getLeaderID()
    {
        return leaderID;
    }

    public List<String> getClientIdList() {
        return this.activeClients;
    }

    public void resetLeaderInfo() {
        activeClients.clear();
        activeChatRooms.clear();
    }

    public void addClientUpdate(String clientId) {
        activeClients.add(clientId);
    }

    public void addClient(Client client) {
        activeClients.add(client.getClientID());
        activeChatRooms.get(client.getRoomID()).addMembers(client);
    }

    public boolean isRoomCreationAvailable( String roomID ) {
        return !(activeChatRooms.containsKey( roomID ));
    }

    public void addApprovedRoom(String clientID, String roomID, int serverID) {
        Room room = new Room(clientID, roomID, serverID);
        activeChatRooms.put(roomID, room);

        Client clientState = new Client(clientID, roomID, null);
        clientState.setRoomOwner(true);
        room.addMembers(clientState);
    }

    public void removeRoom(String roomId, String mainHallId, String ownerId) {
        HashMap<String, Client> formerSetOfClients = this.activeChatRooms.get(roomId).getSetOfClients();
        Room mainHall = this.activeChatRooms.get(mainHallId);

        formerSetOfClients.forEach((clientID, client) -> {
            client.setRoomID(mainHallId);
            mainHall.getSetOfClients().put(client.getClientID(), client);
        });
        formerSetOfClients.get(ownerId).setRoomOwner(false);
        this.activeChatRooms.remove(roomId);
    }

    public void removeRemoteChatRoom(Integer serverIdentity) {
        for (String entry : activeChatRooms.keySet()) {
            Room remoteRoom = activeChatRooms.get(entry);
            if(remoteRoom.getServerId() == serverIdentity){
                for(String client : remoteRoom.getSetOfClients().keySet()){
                    activeChatRooms.remove(client);
                }
                activeChatRooms.remove(entry);
            }
        }
    }

    public void localJoinRoomClient(Client client, String formerRoomId) {
        removeClient(client.getClientID(), formerRoomId);
        addClient(client);
    }

    public void removeClient(String clientId, String formerRoomId) {
        activeClients.remove(clientId);
        activeChatRooms.get(formerRoomId).removeMembers(clientId);
    }

    public boolean isLeader() {
        return ServerState.getInstance().getServerIdentity() == Leader.getLeader().getLeaderID();
    }

    public boolean isLeaderElected() {
        return ( FastBullyAlgorithm.leaderState && FastBullyAlgorithm.leaderUpdateComplete);
    }

    public boolean isLeaderElectedAndIamLeader() {
        return (FastBullyAlgorithm.leaderState && ServerState.getInstance().getServerIdentity() == Leader.getLeader().getLeaderID());
    }

    public boolean isLeaderElectedAndMessageFromLeader(int serverID) {
        return (FastBullyAlgorithm.leaderState && serverID == Leader.getLeader().getLeaderID());
    }

    public boolean isClientIdAlreadyTaken(String clientId) {
        return activeClients.contains(clientId);
    }

    public int getServerIdForExistingRooms(String roomId) {
        if (this.activeChatRooms.containsKey(roomId)) {
            Room targetRoom = activeChatRooms.get(roomId);
            return targetRoom.getServerId();
        } else {
            return -1;
        }
    }

    public ArrayList<String> getRoomIdList() {
        return new ArrayList<>(this.activeChatRooms.keySet());
    }

    public boolean isClientIdAvailable (String clientIdentity){
        return activeClients.contains(clientIdentity);
    }
    public Integer getLeaderIdentity() {
        return leaderID;
    }
}
