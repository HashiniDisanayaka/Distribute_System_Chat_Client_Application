package chatServer.thredHandler;

import java.net.Socket;

public class ClientThreadHandler extends Thread{

    private final Socket socket;
    final Object clientLock;

    public ClientThreadHandler(Socket socket) {
        this.socket = socket;
        this.clientLock = new Object();
    }
}
