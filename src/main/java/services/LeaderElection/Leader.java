package services.LeaderElection;

import chatServer.Client;
import chatServer.Room;

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
                    leader = new Leader(); //instance will be created at request time
//                    leader.addServerDefaultMainHalls();
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

    public void resetLeaderInfo() {
        activeClients.clear();
        activeChatRooms.clear();
    }

    public void addClientUpdate(String clientID) {
        activeClients.add(clientID);
    }

    public void addApprovedRoom(String clientID, String roomID, int serverID) {
        Room room = new Room(clientID, roomID, serverID);
        activeChatRooms.put(roomID, room);

        //add client to the new room
        Client clientState = new Client(clientID, roomID, null);
        clientState.setRoomOwner(true);
        room.addMembers(clientState);
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

    public boolean isLeaderElected() {
        return (FastBullyAlgorithm.leaderState && FastBullyAlgorithm.leaderUpdateComplete);
    }

    public Integer getLeaderIdentity() {
        return leaderID;
    }
}
