package services.heartbeat;

import chatServer.Server;
import data.ServerState;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.json.simple.JSONObject;
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
                MessagePassing.Sender(gossipingMessage, remoteServerArray.get(serverId));
                System.out.println("[LOG] | Gossip heatrbeat information to s" + remoteServerArray.get(serverId).getServerIdentity());
            } catch (IOException e){
                System.out.println("[WARN] | Server s" + remoteServerArray.get(serverId).getServerIdentity() + " has failed");
            }
        }

    }
}
