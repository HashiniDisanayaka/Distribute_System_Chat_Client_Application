package chatServer.thredHandler;

import java.net.ServerSocket;

public class ServerThreadHandler extends Thread{

    private final ServerSocket serverSocket_coordination;


    public ServerThreadHandler(ServerSocket serverSocket_coordination) {
        this.serverSocket_coordination = serverSocket_coordination;
    }
}
