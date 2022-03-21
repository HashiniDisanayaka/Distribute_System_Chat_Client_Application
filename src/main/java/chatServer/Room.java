package chatServer;

public class Room {
    private final String clientId;
    private final String roomId;
    private final int serverId;


    public Room(String clientId, String roomId, int serverId) {
        this.clientId = clientId;
        this.roomId = roomId;
        this.serverId = serverId;
    }
}
