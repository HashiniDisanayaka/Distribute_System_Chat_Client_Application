import chatServer.thredHandler.ClientThreadHandler;
import chatServer.thredHandler.ServerThreadHandler;
import data.ServerState;
import exceptions.InvalidServerIdentifierException;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.heartbeat.Consensus;
import services.heartbeat.Gossiping;
import services.LeaderElection.FastBullyAlgorithm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

@Slf4j
public class ServerMain {

    private final Logger LOGGER = LoggerFactory.getLogger(ServerMain.class);

    private static Boolean isGossiping = true;

    public static void main(String[] args){
        System.out.println("[LOG] | ARG[0] : " + args[0] + " ARG[1] : " + args[1]  );
        log.info("args [0] : ", args[0]);
        log.info("args [1] : ", args[1]);
        String severInitialized = "<<<<<<< Server " + args[0] + " initialized >>>>>>>";
        ServerState.getInstance().initializeServer(args[0], args[1]);

        System.out.println(severInitialized);

        try{
            if(ServerState.getInstance().getServer_address() == null){
                System.out.println("[ERR] | Server address cannot be null");
                throw new InvalidServerIdentifierException("Server address cannot be null");
            }

            ServerSocket serverSocket_coordianation = new ServerSocket();
            ServerSocket serverSocket_client = new ServerSocket();

            SocketAddress socketAddress_coordination = new InetSocketAddress("0.0.0.0", ServerState.getInstance().getCoordination_port());
            SocketAddress socketAddress_client = new InetSocketAddress("0.0.0.0", ServerState.getInstance().getClient_port());

            serverSocket_coordianation.bind(socketAddress_coordination);
            System.out.println("[LOG] | Waiting for coordination port " + serverSocket_coordianation.getLocalPort());

            serverSocket_client.bind(socketAddress_client);
            System.out.println("[LOG] | Waiting for client port " + serverSocket_client.getLocalPort());

            ServerThreadHandler serverThreadHandler = new ServerThreadHandler(serverSocket_coordianation);
            serverThreadHandler.start();

            //leader election
            FastBullyAlgorithm.initialize();

            Runnable heartbeat = new FastBullyAlgorithm("Heartbeat");
            new Thread(heartbeat).start();

            //heartbeat
            if(isGossiping){
                System.out.println("[LOG] | Starting gossiping");
                gossipingStart();
                consensusStart();
            }
            while (true){
                Socket socket_client = serverSocket_client.accept();
                ClientThreadHandler clientThreadHandler = new ClientThreadHandler(socket_client);
                ServerState.getInstance().addClientThreadHandler(clientThreadHandler);
                clientThreadHandler.start();
            }

        }
        catch (IllegalArgumentException e ) {
            System.out.println("[ERR] | Invalid Server id");
            log.error("Invalid Server id : ", e);
        }
        catch (IOException e){
            System.out.println("[ERR] | Error in Server sockets");
            log.error("Error in Server sockets : ", e);
        }
    }

    private static void consensusStart() {
        try {
            JobDetail jobDetail_consensus = JobBuilder.newJob(Consensus.class).withIdentity(Global.CONSENSUS, "group1").build();
            jobDetail_consensus.getJobDataMap().put("voteDuration", 5);

            Trigger trigger_consensus = TriggerBuilder.newTrigger().withIdentity(Global.CONSENSUS_TRIGGER, "group1").withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(10).repeatForever()).build();
            Scheduler scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
            scheduler.scheduleJob(jobDetail_consensus, trigger_consensus);
        } catch (SchedulerException e) {
            System.out.println("[ERR] | Error in starting consensus : " + e);
        }
    }

    private static void gossipingStart() {
        try{
            JobDetail jobDetail_gossiping = JobBuilder.newJob(Gossiping.class).withIdentity(Global.GOSSIPING, "group1").build();
            jobDetail_gossiping.getJobDataMap().put("error_factor", 5);

            Trigger trigger_gossiping = TriggerBuilder.newTrigger().withIdentity(Global.GOSSIPING_TRIGGER, "group1").withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(3).repeatForever()).build();

            Scheduler scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
            scheduler.scheduleJob(jobDetail_gossiping, trigger_gossiping);

        } catch (Exception e){
            System.out.println("[ERR] | Error in starting gossiping process");
            log.error("Error in starting gossiping process : ", e);
        }
    }
}
