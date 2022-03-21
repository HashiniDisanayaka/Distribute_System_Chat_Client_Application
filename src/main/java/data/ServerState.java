package data;

import ChatServer.Server;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;
import java.io.FileNotFoundException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ServerState {

    private String serverid;
    private String server_address;
    private int client_port;
    private int coordination_port;
    private int serverIdentity;

    private static ServerState stateInstance;

    private final ConcurrentHashMap<Integer, Server> setOfservers = new ConcurrentHashMap<>();

    private ServerState () {}

    public static ServerState getInstance() {
        if (stateInstance == null) {
            synchronized (ServerState.class) {
                if (stateInstance == null) {
                    stateInstance = new ServerState();
                }
            }
        }
        return (stateInstance);
    }

    public void initializeServer (String serverId, String path){
        this.serverid = serverId;

        try{
            File file = new File(path);
            Scanner readFile = new Scanner(file);
            while (readFile.hasNextLine()) {
                String line = readFile.nextLine();
                String[] dataArray = line.split(" ");
                String serverAdd = dataArray[1];
                int portClient = Integer.parseInt(dataArray[2]);
                int portCoordination = Integer.parseInt(dataArray[3]);
                int serverValue = Integer.parseInt(dataArray[0].substring(1, 2));
                if (dataArray[0].equals(serverId)) {
                    this.server_address = serverAdd;
                    this.client_port = portClient;
                    this.coordination_port = portCoordination;
                    this.serverIdentity = serverValue;
                    System.out.println(serverId);
                    System.out.println(coordination_port);
                }

                Server server = new Server(serverValue, serverAdd, portClient, portCoordination);
                setOfservers.put(server.getServerIdentity(), server);
                System.out.println(serverValue);
            }
            readFile.close();
        } catch (FileNotFoundException e) {
            System.out.println("ERR : Config file not found");
            log.error("Config file cannot find : " , e);
            e.printStackTrace();
        }



    }
}
