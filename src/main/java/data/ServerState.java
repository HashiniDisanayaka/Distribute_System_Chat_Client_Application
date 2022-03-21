package data;

import chatServer.Room;
import chatServer.Server;
import chatServer.thredHandler.ClientThreadHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ServerState {

    private String serverid;
    private String server_address;
    private int client_port;
    private int coordination_port;
    private int serverIdentity;
    private int numOfPriorServers;

    private static ServerState stateInstance;
    private Room mainHall;

    private final ConcurrentHashMap<Integer, Server> setOfservers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Room> setOfRooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ClientThreadHandler> setOfClientThreadHandlers = new ConcurrentHashMap<>();

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

    public void initializeServer (String serverid, String path){
        this.serverid = serverid;

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
                if (dataArray[0].equals(serverid)) {
                    this.server_address = serverAdd;
                    this.client_port = portClient;
                    this.coordination_port = portCoordination;
                    this.serverIdentity = serverValue;
                    System.out.println(serverIdentity);
                    System.out.println(server_address);
                    System.out.println(coordination_port);
                    System.out.println(client_port);
                }

                Server server = new Server(serverValue, serverAdd, portClient, portCoordination);
                setOfservers.put(server.getServerIdentity(), server);
                System.out.println(serverValue);
            }
            readFile.close();
        } catch (FileNotFoundException e) {
            System.out.println("ERR | Config file not found");
            log.error("Config file cannot find : " , e);
            e.printStackTrace();
        }
        numOfPriorServers = setOfservers.size() - serverIdentity;
//        System.out.println(numOfPriorServers);
        this.mainHall = new Room("", getMainHallId(), serverIdentity);
        this.setOfRooms.put(getMainHallId(), mainHall);
    }

    public String getMainHallId() {
        return getMainHallIdbyServerInt(this.serverIdentity);
    }

    public static String getMainHallIdbyServerInt(int serverIdentity) {
        return "MainHall-s" + serverIdentity;
    }

    public String getServer_address() {
        return server_address;
    }

    public int getClient_port() {
        return client_port;
    }

    public int getCoordination_port () {
        return coordination_port;
    }

    public void addClientThreadHandler(ClientThreadHandler clientThreadHandler) {
        setOfClientThreadHandlers.put(clientThreadHandler.getId(), clientThreadHandler);
    }
}
