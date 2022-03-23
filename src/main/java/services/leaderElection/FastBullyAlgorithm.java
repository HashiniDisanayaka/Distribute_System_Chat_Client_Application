package services.LeaderElection;

import chatServer.Server;
import data.ServerState;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.message.MessagePassing;
import services.message.MessageServer;

import java.io.IOException;
import java.util.List;

public class FastBullyAlgorithm implements Runnable{

    String operationType;
    String requestType;
    static int sourceID = -1;
    static volatile boolean receivedState = false;
    static volatile boolean leaderState = false;
    static volatile boolean electionState = false;

    public static volatile boolean leaderUpdateComplete = false;

//    private final Logger LOGGER = LoggerFactory.getLogger( FastBullyAlgorithm.class);

    public FastBullyAlgorithm( String operationType) {
        this.operationType = operationType;
    }

    public FastBullyAlgorithm( String operationType, String requestType ) {
        this.operationType = operationType;
        this.requestType = requestType;
    }

    public void run() {

        if(operationType.equals( "Timer" ))
        {
            System.out.println("[LOG] | Timer started...");
            try
            {
                // wait 7 seconds
                Thread.sleep( 7000 );
                if( !receivedState )
                {
                    // OK not received. Set self as leader
                    Leader.getLeader().setLeaderID( ServerState.getInstance().getServerValue() );
                    electionState = false; // allow another election request to come in
                    leaderState = true;
                    System.out.println( "[LOG] | Timeout" );
                    System.out.println( "[LOG] | Server s" + Leader.getLeader().getLeaderID() + " is the leader! " );
                    Leader.getLeader().resetLeaderInfo(); // reset leader lists when newly elected

                    Runnable sender = new FastBullyAlgorithm( "Sender", "coordinator" );
                    new Thread( sender ).start();
                }

                if( receivedState && !leaderState )
                {
                    System.out.println( "[LOG] | Timeout" );
                    System.out.println( "[LOG] | Coordinator message was not received" );
                    electionState = false;
                    receivedState = false;

                    Runnable sender = new FastBullyAlgorithm( "Sender", "election" );
                    new Thread( sender ).start();
                }
            }
            catch( Exception e )
            {
                System.out.println( "[ERROR] | Exception in timer thread!" );
            }
        }


        if(operationType.equals( "Heartbeat" ))
        {
            while( true )
            {
                try
                {
                    Thread.sleep( 10 );
                    if( leaderState && ServerState.getInstance().getServerValue() != Leader.getLeader().getLeaderID())
                    {
                        Thread.sleep( 1500 );
                        Server receiver = ServerState.getInstance().getSetOfservers().get( Leader.getLeader().getLeaderID());
                        MessagePassing.Sender( MessageServer.heartbeat( String.valueOf( ServerState.getInstance().getServerValue()) ), receiver );
                        System.out.println( "[LOG] | Sent heartbeat to leader s" + receiver.getServerIdentity() );
                    }
                }

                catch( Exception e )
                {
                    leaderState = false;
                    leaderUpdateComplete = false;
                    System.out.println( "[ERROR] | Leader has failed!" );

                    Runnable election = new FastBullyAlgorithm( "Sender", "election" );
                    new Thread( election ).start();
                }
            }
        }

        if(operationType.equals( "Sender" ))
        {
            if(requestType.equals( "election" ))
            {
                try
                {
                    sendElectionMessage();
                }
                catch( Exception e )
                {
                    System.out.println( "[ERROR] | Server has failed, election request cannot be processed" );
                }
            }

            if(requestType.equals( "ok" ))
            {
                try
                {
                    sendOkMessage();
                }
                catch( Exception e )
                {
                    e.printStackTrace();
                }
            }
            if(requestType.equals( "coordinator" ))
            {
                try
                {
                    sendCoordinatorMessage();
                }
                catch( Exception e )
                {
                    e.printStackTrace();
                }
            }
        }
    }


    public static void sendCoordinatorMessage() {
        int numberOfRequestsNotSent = 0;
        for ( int key : ServerState.getInstance().getSetOfservers().keySet() ) {
            if ( key != ServerState.getInstance().getServerValue() ){
                Server receiver = ServerState.getInstance().getSetOfservers().get(key);

                try {
                    MessagePassing.Sender(
                            MessageServer.coordinator( String.valueOf(ServerState.getInstance().getServerValue()) ),
                            receiver
                    );
                    System.out.println( "[LOG] | Sent leader ID to s"+receiver.getServerIdentity());
                }
                catch(Exception e) {
                    numberOfRequestsNotSent += 1;
                    System.out.println( "[ERROR] | Server s"+receiver.getServerIdentity()+" has failed, it will not receive the leader");
                }
            }
        }
        if( numberOfRequestsNotSent == ServerState.getInstance().getSetOfservers().size()-1 ) {
            // add self clients and chat rooms to leader state
            List<String> selfClients = ServerState.getInstance().getClientIdList();
            List<List<String>> selfRooms = ServerState.getInstance().getChatRoomList();

            for( String clientID : selfClients ) {
                Leader.getLeader().addClientUpdate( clientID );
            }

            for( List<String> chatRoom : selfRooms ) {
                Leader.getLeader().addApprovedRoom( chatRoom.get( 0 ),
                        chatRoom.get( 1 ), Integer.parseInt(chatRoom.get( 2 )) );
            }

            leaderUpdateComplete = true;
        }
    }

    public static void sendOkMessage() {
        try {
            Server receiver = ServerState.getInstance().getSetOfservers().get(sourceID);
            MessagePassing.Sender( MessageServer.sendOk( String.valueOf(ServerState.getInstance().getServerValue()) ), receiver );

            System.out.println( "[LOG] | Sent OK to s"+ receiver.getServerIdentity());
        }
        catch(Exception e) {
            System.out.println( "[ERROR] | Server s"+sourceID+" has failed. OK message cannot be sent");
        }
    }

    public static void sendElectionMessage()
    {
        System.out.println( "[LOG] | Election started!" );
        int numberOfFailedRequests = 0;
        for ( int key : ServerState.getInstance().getSetOfservers().keySet() ) {
            if( key > ServerState.getInstance().getServerValue() ){
                Server receiver = ServerState.getInstance().getSetOfservers().get(key);
                try {
                    MessagePassing.Sender( MessageServer.election( String.valueOf(ServerState.getInstance().getServerValue()) ), receiver);
                    System.out.println( "[LOG] | Sent election request to s" + receiver.getServerIdentity());
                }
                catch(Exception e){
                    System.out.println( "[ERROR] | Server s"+receiver.getServerIdentity()+" has failed, cannot send election request");
                    numberOfFailedRequests++;
                }
            }

        }
        if (numberOfFailedRequests == ServerState.getInstance().getNumberOfPriorServers()) {
            if(!electionState){
                //startTime=System.currentTimeMillis();
                electionState = true;
                receivedState = false;
                Runnable timer = new FastBullyAlgorithm("Timer");
                new Thread(timer).start();
            }
        }
    }

    public static void receiveMessages(JSONObject j_object) {
        String option = j_object.get( "option" ).toString();
        switch( option ) {
            case "election":
                // {"option": "election", "source": 1}
                sourceID = Integer.parseInt(j_object.get( "source" ).toString());
                System.out.println( "INFO : Received election request from s" + sourceID );

                if( ServerState.getInstance().getServerValue() > sourceID ) {
                    Runnable sender = new FastBullyAlgorithm( "Sender", "ok" );
                    new Thread( sender ).start();
                }
                if( !electionState ) {
                    Runnable sender = new FastBullyAlgorithm( "Sender", "election" );
                    new Thread( sender ).start();
                    //startTime = System.currentTimeMillis();
                    electionState = true;

                    Runnable timer = new FastBullyAlgorithm( "Timer" );
                    new Thread( timer ).start();
                    System.out.println( "INFO : Election started");
                }
                break;
            case "ok": {
                // {"option": "ok", "sender": 1}
                receivedState = true;
                int senderID = Integer.parseInt(j_object.get( "sender" ).toString());
                System.out.println( "INFO : Received OK from s" + senderID );
                break;
            }
            case "coordinator":
                // {"option": "coordinator", "leader": 1}
                Leader.getLeader().setLeaderID(
                        Integer.parseInt(j_object.get( "leader" ).toString()) );
                leaderState = true;
                leaderUpdateComplete = false;
                electionState = false;
                receivedState = false;
                System.out.println( "INFO : Server s" + Leader.getLeader().getLeaderID()
                                            + " is selected as leader! " );

                // send local client list and chat room list to leader
                try
                {
                    MessagePassing.sendToLeader(
                            MessageServer.LeaderUpdate(
                                    ServerState.getInstance().getClientIdList(),
                                    ServerState.getInstance().getChatRoomList()
                            )
                    );
                } catch( IOException e ) {
                    System.out.println("WARN : Leader state update message could not be sent");
                }
                break;
            case "heartbeat": {
                // {"option": "heartbeat", "sender": 1}
                int senderID = Integer.parseInt(j_object.get( "sender" ).toString());
                //System.out.println( "INFO : Heartbeat received from s" + senderID );
                break;
            }
        }
    }

    public static void initialize()
    {
        // Initiate election
        System.out.println("[LOG] | Initialize fast bully algorithm ");
        Runnable sender = new FastBullyAlgorithm("Sender","election");
        new Thread(sender).start();
    }
}
