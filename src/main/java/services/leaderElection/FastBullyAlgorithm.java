package services.leaderElection;

import chatServer.Server;
import data.ServerState;
import org.json.simple.JSONObject;
import services.message.MessagePassing;
import services.message.MessageServer;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class FastBullyAlgorithm implements Runnable{

    String operationType;
    String requestType;
    static int sourceID = -1;
    static int highestPrior = -1;
    static volatile boolean receivedState = false;
    static volatile boolean viewReceivedState = false;
    static volatile boolean nominationRecieved = false;
    static volatile boolean leaderState = false;
    static volatile boolean electionState = false;

    public static volatile boolean leaderUpdateComplete = false;
    static List<Integer> view = new ArrayList<>();
    static List<Integer> answer = new ArrayList<>();

    public FastBullyAlgorithm( String operationType) {
        this.operationType = operationType;
    }

    public FastBullyAlgorithm( String operationType, String requestType ) {
        this.operationType = operationType;
        this.requestType = requestType;
    }

    public void run() {

        if(operationType.equals( "TimerTwo" ))
        {
            System.out.println("[LOG] | Timer for view started...");
            try
            {
                // wait 15 seconds
                Thread.sleep( 15000 );
                if(!viewReceivedState)
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

                if (viewReceivedState)
                {
                    highestPrior = ServerState.getInstance().getServerValue();
                    for (int serverid : view)
                    {
                        if (serverid > highestPrior){
                            highestPrior = serverid;
                        }
                        continue;
                    }
                    if (highestPrior == ServerState.getInstance().getServerValue())
                    {
                        Leader.getLeader().setLeaderID( ServerState.getInstance().getServerValue() );
                        leaderState = true;
                        System.out.println( "[LOG] | Server s" + Leader.getLeader().getLeaderID() + " is the leader! " );
                        Leader.getLeader().resetLeaderInfo(); // reset leader lists when newly elected

                        Runnable sender = new FastBullyAlgorithm( "Sender", "coordinator" );
                        new Thread( sender ).start();

                    }

                    else
                    {
                        Leader.getLeader().setLeaderID( highestPrior);
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
                    }
                }
            }
            catch( Exception e )
            {
                System.out.println( "[ERROR] | Exception in timer thread!" );
            }
        }

        else if(operationType.equals( "TimerTwoElect" ))
        {
            System.out.println("[LOG] | Timer for election started...");
            try
            {
                // wait 15 seconds
                Thread.sleep( 15000 );
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

                if( receivedState)
                {
                    highestPrior = ServerState.getInstance().getServerValue();
                    for (int serverid : answer)
                    {
                        if (serverid > highestPrior){
                            highestPrior = serverid;
                        }
                        continue;
                    }

                    for (int serverid : answer)
                    {
                        if (highestPrior == serverid){
                            answer.remove(highestPrior);
                            break;
                        }
                        continue;
                    }
                    System.out.println( "[LOG] | Sending nomination to " + highestPrior );
                    Runnable sender = new FastBullyAlgorithm( "Sender", "nomination" );
                    new Thread( sender ).start();
                }
            }
            catch( Exception e )
            {
                System.out.println( "[ERROR] | Exception in timer thread!" );
            }
        }

        else if (operationType.equals("TimerThree"))
        {
            System.out.println("[LOG] | Timer three started...");
            try {
                // wait 15 seconds
                Thread.sleep(15000);
                if (!leaderState )
                {
                    if (answer.isEmpty())
                    {
                        Runnable sender = new FastBullyAlgorithm("Sender","election");
                        new Thread(sender).start();
                    }
                    else{
                        highestPrior = ServerState.getInstance().getServerValue();
                        for (int serverid : answer)
                        {
                            if (serverid > highestPrior){
                                highestPrior = serverid;
                            }
                            continue;
                        }

                        for (int serverid : answer)
                        {
                            if (highestPrior == serverid){
                                answer.remove(highestPrior);
                                break;
                            }
                            continue;
                        }
                        Runnable sender = new FastBullyAlgorithm( "Sender", "nomination" );
                        new Thread( sender ).start();
                    }
                }
            }
            catch( Exception e )
            {
                System.out.println( "[ERROR] | Exception in timer thread!" );
            }
        }

        else if (operationType.equals("TimerFour"))
        {
            System.out.println("[LOG] | Timerfour started...");
            try {
                // wait 8 seconds
                Thread.sleep(8000);
                if (!nominationRecieved && !leaderState)
                {
                    System.out.println("[LOG] | Election procedure restarting");
                    Runnable sender = new FastBullyAlgorithm( "Sender", "election" );
                    new Thread( sender ).start();
                }
            }

            catch( Exception e )
            {
                System.out.println( "[ERROR] | Exception in timer thread!" );
            }
        }

        else if(operationType.equals( "Heartbeat" ))
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
                        MessagePassing.sender( MessageServer.heartbeat( String.valueOf( ServerState.getInstance().getServerValue()) ), receiver );
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

        else if(operationType.equals( "Sender" ))
        {
            if(requestType.equals( "iamup" ))
            {
                try
                {
                    sendIAmUpMessage();
                }
                catch( Exception e )
                {
                    System.out.println( "[ERROR] | Server has failed,  request cannot be processed" );
                }
            }
            if(requestType.equals( "view" ))
            {
                try
                {
                    sendViewMessage();
                }
                catch( Exception e )
                {
                    System.out.println( "[ERROR] | Server has failed, election request cannot be processed" );
                }
            }
            if(requestType.equals( "nomination" ))
            {
                try
                {
                    sendNominationMessage();
                }
                catch( Exception e )
                {
                    System.out.println( "[ERROR] | Server has failed, election request cannot be processed" );
                }
            }
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


    public static void sendCoordinatorMessage()
    {
        int numberOfRequestsNotSent = 0;
        for ( int key : ServerState.getInstance().getSetOfservers().keySet() ) {
            if ( key != ServerState.getInstance().getServerValue() ){
                Server receiver = ServerState.getInstance().getSetOfservers().get(key);

                try {
                    MessagePassing.sender(
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

    public static void sendOkMessage()
    {
        try {
            Server receiver = ServerState.getInstance().getSetOfservers().get(sourceID);
            MessagePassing.sender( MessageServer.sendOk( String.valueOf(ServerState.getInstance().getServerValue()) ), receiver );

            System.out.println( "[LOG] | Sent OK to s"+ receiver.getServerIdentity());
        }
        catch(Exception e) {
            System.out.println( "[ERROR] | Server s"+sourceID+" has failed. OK message cannot be sent");
        }
    }

    public static void sendNominationMessage()
    {
        System.out.println( "[LOG] | Sending Nomination Message!" );
        try {
            Server receiver = ServerState.getInstance().getSetOfservers().get(highestPrior);
            MessagePassing.sender( MessageServer.sendNomination( ServerState.getInstance().getServerValue()), receiver );

            System.out.println( "[LOG] | Sent OK to s"+ receiver.getServerIdentity());
            Runnable sender = new FastBullyAlgorithm("Sender","TimerThree");
            new Thread(sender).start();
        }
        catch(Exception e) {
            System.out.println( "[ERROR] | Server s"+sourceID+" has failed. OK message cannot be sent");
        }
    }

    public static void sendIAmUpMessage()
    {

        System.out.println( "[LOG] | I am up message " );

        int numberOfFailedRequests = 0;
        for ( int key : ServerState.getInstance().getSetOfservers().keySet() ) {

            if (key == ServerState.getInstance().getServerIdentity()) continue;
            Server receiver = ServerState.getInstance().getSetOfservers().get(key);
            try {
                MessagePassing.sender( MessageServer.iamup(ServerState.getInstance().getServerValue()),receiver);
                System.out.println( "[LOG] | I am up s" + receiver.getServerIdentity());
            }
            catch(Exception e){
                System.out.println( "[ERROR] | Server s"+receiver.getServerIdentity()+" has failed, cannot send i am up");
                numberOfFailedRequests++;
            }

        }

        Runnable timer = new FastBullyAlgorithm("TimerTwo");
        new Thread(timer).start();

    }

    public static void sendViewMessage()
    {
        System.out.println( "[LOG] | Sending View Message!" );
        try {
            Server receiver = ServerState.getInstance().getSetOfservers().get(sourceID);
            MessagePassing.sender( MessageServer.sendView( ServerState.getInstance().getServerValue()), receiver);

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
                    MessagePassing.sender( MessageServer.election( String.valueOf(ServerState.getInstance().getServerValue()) ), receiver);
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
                Runnable timer = new FastBullyAlgorithm("TimerTwoElect");
                new Thread(timer).start();
            }
        }
    }

    public static void receiveMessages(JSONObject j_object)
    {
        String option = j_object.get( "option" ).toString();
        Runnable sender , timer;
        switch( option ) {

            case "iamup" :
                sourceID = Integer.parseInt(j_object.get( "source" ).toString());
                System.out.println( "INFO : recovery from s" + sourceID );
                sender = new FastBullyAlgorithm( "Sender", "view" );
                new Thread( sender ).start();
                break;
            case "view" :
                sourceID = Integer.parseInt(j_object.get( "source" ).toString());
                System.out.println( "INFO : view from s" + sourceID );
                view.add(sourceID);
                viewReceivedState = true;
                break;

            case "nomination" :
                nominationRecieved = true;
                leaderState = true;
                sender = new FastBullyAlgorithm( "Sender", "coordinator" );
                new Thread( sender ).start();
                electionState = false;
                break;

            case "election":
                // {"option": "election", "source": 1}
                sourceID = Integer.parseInt(j_object.get( "source" ).toString());
                System.out.println( "INFO : Received election request from s" + sourceID );

                if( ServerState.getInstance().getServerValue() > sourceID ) {
                    sender = new FastBullyAlgorithm( "Sender", "ok" );
                    new Thread( sender ).start();
                }
                timer = new FastBullyAlgorithm( "TimerFour" );
                new Thread( timer ).start();
                break;
            case "ok": {
                // {"option": "ok", "sender": 1}
                receivedState = true;
                int senderID = Integer.parseInt(j_object.get( "sender" ).toString());
                answer.add(senderID);
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
        Runnable sender = new FastBullyAlgorithm("Sender","iamup");
        new Thread(sender).start();
    }

}
