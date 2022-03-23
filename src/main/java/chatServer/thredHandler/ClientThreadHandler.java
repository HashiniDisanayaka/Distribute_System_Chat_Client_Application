package chatServer.thredHandler;

import java.net.Socket;
import java.util.List;

public class ClientThreadHandler extends Thread{

    private final Socket socket;
    private String availableJoinRoomServerHostAddress;
    private String availableJoinRoomServerPort;
    final Object clientLock;
    private List<String> tempRoomsList;
    private int availableClientIdentity = -1;
    private int availableRoomCreation = -1;
    private int availableJoinRoom = -1;

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

}
