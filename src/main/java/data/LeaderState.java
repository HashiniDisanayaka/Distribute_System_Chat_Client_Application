package data;

import chatServer.Room;
import services.leaderElection.FastBullyAlgorithm;

import java.util.HashMap;

public class LeaderState {

    private static LeaderState leaderState;
    private final HashMap<String, Room> chatRooms_active = new HashMap<>();

    Integer leaderIdentity;

    private LeaderState() {}

    public static LeaderState getInstance() {
        if (leaderState == null) {
            synchronized (LeaderState.class){
                if (leaderState == null) {
                    leaderState = new LeaderState();
                }
            }
        }
        return leaderState;
    }

    public boolean isLeaderElected() {
        return (FastBullyAlgorithm.flagLeader && FastBullyAlgorithm.leaderUpdated);
    }

    public Integer getLeaderIdentity() {
        return leaderIdentity;
    }

    public void removeRemoteChatRoom(Integer serverIdentity) {
        for (String entry : chatRooms_active.keySet()) {
            Room remoteRoom = chatRooms_active.get(entry);
            if(remoteRoom.getServerId() == serverIdentity){
                for(String client : remoteRoom.getSetOfClients().keySet()){
                    chatRooms_active.remove(client);
                }
                chatRooms_active.remove(entry);
            }
        }

    }

}
