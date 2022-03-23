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

    public String getClientID() {
        return clientId;
    }

    public void setClientID(String clientId) {
        this.clientId = clientId;
    }

    public String getRoomID() {
        return roomId;
    }

    public void setRoomID(String roomId) {
        this.roomId = roomId;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public boolean isRoomOwner()
    {
        return isRoomOwner;
    }

    public void setRoomOwner( boolean roomOwner )
    {
        isRoomOwner = roomOwner;
    }
}
