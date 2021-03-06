package chatServer;

import java.util.HashMap;

public class Room {
    private final String clientId;
    private final String roomId;
    private final int serverId;

    private final HashMap<String,Client> setOfClients = new HashMap<>();

    public Room(String clientId, String roomId, int serverId) {
        this.clientId = clientId;
        this.roomId = roomId;
        this.serverId = serverId;
    }

    public synchronized int getServerId() {
        return serverId;
    }

    public synchronized HashMap<String, Client> getSetOfClients() {
        return setOfClients;
    }

    public String getOwnerId() {
        return clientId;
    }

    public synchronized String getRoomID() {
        return roomId;
    }

    public synchronized void addMembers(Client clientState) {
        this.setOfClients.put(clientState.getClientID(), clientState);
    }
    public synchronized void removeMembers(String clientId) {
        this.setOfClients.remove(clientId);
    }
}
