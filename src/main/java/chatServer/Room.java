package chatServer;

import java.util.HashMap;

public class Room {
    private final String clientId;
    private final String roomId;
    private final int serverId;

    private final HashMap<String,Client> clientStateMap = new HashMap<>();

    public Room(String clientId, String roomId, int serverId) {
        this.clientId = clientId;
        this.roomId = roomId;
        this.serverId = serverId;
    }

    public synchronized HashMap<String, Client> getClientStateMap() {
        return clientStateMap;
    }

    public String getOwnerId() {
        return clientId;
    }

    public synchronized String getRoomID() {
        return roomId;
    }

    public synchronized int getServerID() {
        return serverId;
    }

    public synchronized void addMembers(Client clientState) {
        this.clientStateMap.put(clientState.getClientID(), clientState);
    }
}
