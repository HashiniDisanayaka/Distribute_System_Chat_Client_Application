package services.heartbeat;

import chatServer.Server;
import data.ServerState;
import org.json.simple.JSONObject;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import services.leaderElection.Leader;
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
                    System.out.println("[ERR] | " + e.getMessage());
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

    public static void voteHandler (JSONObject jsonObject){

        ServerState serverState = ServerState.getInstance();
        MessageServer messageServer = MessageServer.getInstance();

        Integer suspectServerId = (int) (long)jsonObject.get("suspectServerIdentity");
        Integer serverIdentity = serverState.getServerIdentity();

        if (serverState.getSuspectedList().containsKey(suspectServerId)) {
            if (serverState.getSuspectedList().get(suspectServerId).equals("SUSPECTED")) {
                JSONObject answerMessage = messageServer.answerMessage(suspectServerId, "YES", serverIdentity);
                try {
                    MessagePassing.sender(answerMessage, serverState.getSetOfservers().get(Leader.getLeader().getLeaderID()));
                    System.out.println(String.format("[LOG] | Voting on suspected server: [%s] vote: YES", suspectServerId));
                } catch (Exception e) {
                    System.out.println("[ERR] | Voting on suspected server is failed");
                }

            } else {

                JSONObject answerVoteMessage = messageServer.answerMessage(suspectServerId, "NO", serverIdentity);
                try {
                    MessagePassing.sender(answerVoteMessage, serverState.getSetOfservers().get(Leader.getLeader().getLeaderID()));
                    System.out.println(String.format("INFO : Voting on suspected server: [%s] vote: NO", suspectServerId));
                } catch (Exception e) {
                    System.out.println("ERROR : Voting on suspected server is failed");
                }
            }
        }

    }

    public static void answerVoteHandler(JSONObject j_object){
        ServerState serverState = ServerState.getInstance();
        Integer suspectServerId = (int) (long)j_object.get("suspectServerIdentity");
        String vote = (String) j_object.get("vote");
        Integer votedId = (int) (long)j_object.get("votedId");
        Integer voteCount = serverState.getSetOfVotes().get(vote);

        System.out.println(String.format("[LOG] | Receiving voting to kick [%s]: [%s] voted by server: [%s]", suspectServerId, vote, votedId));

        if (voteCount == null) {
            serverState.getSetOfVotes().put(vote, 1);
        } else {
            serverState.getSetOfVotes().put(vote, voteCount + 1);
        }

    }

    public static void notifyServerDownHandler(JSONObject j_object){
        ServerState serverState = ServerState.getInstance();
        Leader leader = Leader.getLeader();
        Integer serverId = (int) (long)j_object.get("serverId");

        System.out.println("[LOG] | Server down notification received. Removing server: " + serverId);

        leader.removeRemoteChatRoom(serverId);
        serverState.removeServerFromListOfHeartbeat(serverId);
        serverState.removeServerFromSuspectedList(serverId);
    }

}
