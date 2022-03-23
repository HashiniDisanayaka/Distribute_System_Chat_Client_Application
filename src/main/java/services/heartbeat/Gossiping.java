package services.heartbeat;

import chatServer.Server;
import data.ServerState;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.json.simple.JSONObject;
import services.leaderElection.Leader;
import services.message.MessagePassing;
import services.message.MessageServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Gossiping implements Job{

    private ServerState serverState = ServerState.getInstance();
    private MessageServer messageServer = MessageServer.getInstance();
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        String errorFactor = jobDataMap.get("error_factor").toString();

        for (Server server : serverState.getSetOfservers().values()) {
//            System.out.println(server);
            Integer serverIdentity = server.getServerIdentity();
            Integer serverValue = serverState.getServerIdentity();
            Integer numOfHeartbeat = serverState.getListOfHeartbeat().get(serverIdentity);
//            System.out.println(numOfHeartbeat);
            if(serverIdentity.equals(serverValue)){
                serverState.getListOfHeartbeat().put(serverIdentity, 0);
            } else {
                if (numOfHeartbeat != null){
                    serverState.getListOfHeartbeat().put(serverIdentity, (numOfHeartbeat+1));
                } else {
                    serverState.getListOfHeartbeat().put(serverIdentity, 1);
                }
            }
            numOfHeartbeat = serverState.getListOfHeartbeat().get(serverIdentity);
//            System.out.println("LOG | heartbeat count : " + numOfHeartbeat);

            if (numOfHeartbeat != null) {
                if(numOfHeartbeat < Integer.parseInt(errorFactor)){
                    serverState.getSuspectedList().put(serverIdentity, "NON_SUSPECTED");
                } else {
                    serverState.getSuspectedList().put(serverIdentity, "SUSPECTED");
                }
            }
        }

//        System.out.println(serverState.getServers().size());

        if(serverState.getSetOfservers().size() > 1) {
            Integer serverId = ThreadLocalRandom.current().nextInt( serverState.getSetOfservers().size() - 1);
            ArrayList<Server> remoteServerArray = new ArrayList<>();

            for (Server server : serverState.getSetOfservers().values()) {
                Integer serverIdentity = server.getServerIdentity();
                Integer serverValue = serverState.getServerIdentity();
                if (!serverIdentity.equals(serverValue)) {
                    remoteServerArray.add(server);
                }
            }

            HashMap<Integer, Integer> listOfHeartbeat = new HashMap<>(serverState.getListOfHeartbeat());
            JSONObject gossipingMessage = messageServer.gossipingMessage(serverState.getServerIdentity(), listOfHeartbeat);

            try{
                MessagePassing.sender(gossipingMessage, remoteServerArray.get(serverId));
                System.out.println("[LOG] | Gossip heatrbeat information to s" + remoteServerArray.get(serverId).getServerIdentity());
            } catch (IOException e){
                System.out.println("[ERR] | Server s" + remoteServerArray.get(serverId).getServerIdentity() + " has failed");
            }
        }

    }

    public static void messageReceive(JSONObject jsonObject) {

        ServerState serverState = ServerState.getInstance();
        HashMap<String, Long> gossipsFromOthers = (HashMap<String, Long>) jsonObject.get("ListOfHeartbeat");
        Integer serverValue = (int) (long)jsonObject.get("serverId");

        System.out.println(("Receiving gossip from server: [" + serverValue.toString() + "] gossipping: " + gossipsFromOthers));

        for (String serverId : gossipsFromOthers.keySet()) {
            Integer numOfLocalHeartbeat = serverState.getListOfHeartbeat().get(Integer.parseInt(serverId));
            Integer numOfRemoteHeartbeat = (int) (long)gossipsFromOthers.get(serverId);
            if (numOfLocalHeartbeat != null && numOfRemoteHeartbeat < numOfLocalHeartbeat) {
                serverState.getListOfHeartbeat().put(Integer.parseInt(serverId), numOfRemoteHeartbeat);
            }
        }

        System.out.println(("Current cluster heart beat state is: " + serverState.getListOfHeartbeat()));

        if (Leader.getLeader().isLeaderElected() && Leader.getLeader().getLeaderID().equals(serverState.getServerIdentity())) {
            if (serverState.getListOfHeartbeat().size() < gossipsFromOthers.size()) {
                for (String serverId : gossipsFromOthers.keySet()) {
                    if (!serverState.getListOfHeartbeat().containsKey(serverId)) {
                        serverState.getSuspectedList().put(Integer.parseInt(serverId), "SUSPECTED");
                    }
                }
            }
        }

    }

}
