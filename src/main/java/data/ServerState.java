package data;

import chatServer.Room;
import chatServer.Server;
import chatServer.thredHandler.ClientThreadHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final ConcurrentHashMap<Integer, Integer> listOfHeartbeat = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> suspectedList = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> setOfVotes = new ConcurrentHashMap<>();

    private AtomicBoolean consensus_ongoing = new AtomicBoolean(false);

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
            System.out.println("[ERR] | Config file not found");
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

    public int getServerIdentity () {
        return  serverIdentity;
    }

    public ConcurrentHashMap<Integer, Server> getSetOfservers() {
        return setOfservers;
    }

    public void addClientThreadHandler(ClientThreadHandler clientThreadHandler) {
        setOfClientThreadHandlers.put(clientThreadHandler.getId(), clientThreadHandler);
    }

    public ClientThreadHandler getClientThreadHandler(Long threadIdentity) {
        return setOfClientThreadHandlers.get(threadIdentity);
    }

    public ConcurrentHashMap<Integer, Integer> getListOfHeartbeat() {
        return listOfHeartbeat;
    }

    public ConcurrentHashMap<Integer, String> getSuspectedList() {
        return suspectedList;
    }

    public AtomicBoolean consensus_ongoing() {
        return consensus_ongoing;
    }

    public ConcurrentHashMap<String, Integer> getSetOfVotes() {
        return setOfVotes;
    }

    public synchronized void removeServerFromListOfHeartbeat(Integer serverIdentity) {
        listOfHeartbeat.remove(serverIdentity);
    }

    public synchronized void removeServerFromSuspectedList(Integer serverIdentity) {
        suspectedList.remove(serverIdentity);
    }

    public int getServerValue() {
        return serverIdentity;
    }

    public int getNumberOfPriorServers() {
        return numOfPriorServers;
    }

    public List<String> getClientIdList() {
        List<String> clientIdList = new ArrayList<>();
        setOfRooms.forEach((roomID, room) -> {
            clientIdList.addAll(room.getSetOfClients().keySet());
        });
        return clientIdList;
    }

    public List<List<String>> getChatRoomList() {
        List<List<String>> chatRoomList = new ArrayList<>();
        for (Room room: setOfRooms.values()) {
            List<String> roomInfo = new ArrayList<>();
            roomInfo.add( room.getOwnerId() );
            roomInfo.add( room.getRoomID() );
            roomInfo.add( String.valueOf(room.getServerId()) );

            chatRoomList.add( roomInfo );
        }
        return chatRoomList;
    }
}
