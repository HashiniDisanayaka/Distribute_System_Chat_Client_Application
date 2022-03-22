package chatServer;

import java.net.Socket;

public class Client {
    private String clientId;
    private String roomId;
    private Socket socket;
    private boolean isRoomOwner = false;

    public Client(String clientId, String roomId, Socket socket) {
        this.clientId = clientId;
        this.roomId = roomId;
        this.socket = socket;
    }
}
