package services.heartbeat;

import chatServer.Server;
import data.ServerState;
import org.json.simple.JSONObject;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import services.LeaderElection.Leader;
import services.message.MessagePassing;
import services.message.MessageServer;

import java.io.IOException;
import java.util.ArrayList;

public class Consensus implements Job {

    private ServerState serverState = ServerState.getInstance();
    private MessageServer messageServer = MessageServer.getInstance();
    private Leader leader = Leader.getLeader();
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (serverState.consensus_ongoing().get()) {
            System.out.println("[LOG] | There is an ongoing consensus.");
        } else {
            if (leader.isLeaderElected()) {
                serverState.consensus_ongoing().set(true);
                consensusPerform(jobExecutionContext);
                serverState.consensus_ongoing().set(false);
            }
        }
    }

    private void consensusPerform (JobExecutionContext jobExecutionContext){
        Integer serverIdSuspected =null;
        JobDataMap jobDataMap =jobExecutionContext.getJobDetail().getJobDataMap();
        String voteDuration = jobDataMap.get("voteDuration").toString();

        Integer leaderIdentity = leader.getLeaderIdentity();
        Integer serverIdentity = serverState.getServerIdentity();

        ArrayList<Server> setOfServers = new ArrayList<>();

        serverState.getSetOfVotes().put("YES", 0);
        serverState.getSetOfVotes().put("NO", 0);

        if (serverIdentity.equals(leaderIdentity)) {
            for (Integer serever : serverState.getSuspectedList().keySet()) {
                if (serverState.getSuspectedList().get(serever).equals("NON_SUSPECTED")) {
                    System.out.println("[LOG] | Serever is not suspected");
                } else if (serverState.getSuspectedList().get(serever).equals("SUSPECTED")) {
                    serverIdSuspected = serever;
                    break;
                }
            }

            for (Integer serverid : serverState.getSetOfservers().keySet()) {
                if (!serverid.equals(serverState.getServerIdentity()) && serverState.getSuspectedList().get(serverid).equals("NON_SUSPECTED")) {
                    setOfServers.add(serverState.getSetOfservers().get(serverid));
                }
            }

            if (serverIdSuspected != null) {
                serverState.getSetOfVotes().put("YES", 1);
                JSONObject startVoting = messageServer.startVoting(serverState.getServerIdentity(), serverIdSuspected);

                try {
                    MessagePassing.sendBroadcast(startVoting, setOfServers);
                    System.out.println("[LOG] | Leader calling for votes -- remove suspected server");
                } catch (IOException e) {
                    System.out.println("[ERR] | Leader calling for votes -- remove suspected server -- failed");
                }

                try {
                    Thread.sleep(Integer.parseInt(voteDuration) + 1000);
                } catch (InterruptedException e) {
                    System.out.println("[ERR] | " + e);
                }

                System.out.println((String.format("[LOG] | Consensus votes to kick server [%s]: %s", serverIdSuspected, serverState.getSetOfVotes())));

                if (serverState.getSetOfVotes().get("YES") > serverState.getSetOfVotes().get("NO")) {
                    JSONObject notifyServerDown = messageServer.notifyServerDown(serverIdSuspected);

                    try {
                        MessagePassing.sendBroadcast(notifyServerDown, setOfServers);
                        System.out.println("[LOG] | Notify server s" + serverIdSuspected + " down. Removing...");

                        leader.removeRemoteChatRoom(serverIdSuspected);
                        serverState.removeServerFromListOfHeartbeat(serverIdSuspected);
                        serverState.removeServerFromSuspectedList(serverIdSuspected);

                    } catch (IOException e) {
                        System.out.println("[ERR] | s" + serverIdSuspected + " removing is failed");
                    }

                    System.out.println("[LOG] | Number of servers in the group: " + serverState.getSetOfservers().size());
                }
            }
        }

    }
}
