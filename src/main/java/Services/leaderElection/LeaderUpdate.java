package services.leaderElection;

import chatServer.Server;
import data.ServerState;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import services.message.MessagePassing;
import services.message.MessageServer;

import java.util.List;

public class LeaderUpdate extends Thread {
    int numOfServersWithLowerIds = ServerState.getInstance().getServerIdentity() - 1;
    int numOfReceivedUpdates = 0;
    volatile boolean leaderUpdateInProgress = true;

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        long end = start + 5000;

        try {
            while ( leaderUpdateInProgress ) {
                if( System.currentTimeMillis() > end || numOfReceivedUpdates == numOfServersWithLowerIds ) {
                    leaderUpdateInProgress = false;
                    FastBullyAlgorithm.leaderUpdateComplete = true;
                    System.out.println("[LOG] | Leader updated successfully!");

                    List<String> clientIdentityList = ServerState.getInstance().getClientIdList();
                    List<List<String>> chatRoomList = ServerState.getInstance().getChatRoomList();

                    for( String clientId : clientIdentityList ) {
                        Leader.getLeader().addClientUpdate( clientId );
                    }

                    for( List<String> chatRoom : chatRoomList ) {
                        Leader.getLeader().addApprovedRoom( chatRoom.get( 0 ), chatRoom.get( 1 ), Integer.parseInt(chatRoom.get( 2 )) );
                    }
                    System.out.println("[LOG] | Finalized clients: " + Leader.getLeader().getClientIdList() + " & rooms: " + Leader.getLeader().getRoomIdList());

                    for ( int key : ServerState.getInstance().getSetOfservers().keySet() ) {
                        if ( key != ServerState.getInstance().getServerIdentity() ){
                            Server endpointServer = ServerState.getInstance().getSetOfservers().get(key);

                            try {
                                MessagePassing.sender(MessageServer.getLeaderStateUpdateComplete( String.valueOf(ServerState.getInstance().getServerIdentity()) ), endpointServer);
                                System.out.println("[LOG] | Sent leader updated message to s" + endpointServer.getServerIdentity());
                            }
                            catch(Exception e) {
                                System.out.println("[WARN] | Server s" + endpointServer.getServerIdentity() + " will not receive the leader updated message, as it has failed");
                            }
                        }
                    }
                }
                Thread.sleep(10);
            }

        } catch( Exception e ) {
            System.out.println( "[WARN] | Exception in leader update thread : "+ e );
        }

    }

    public void receiveUpdate( JSONObject jsonObject ) {
        numOfReceivedUpdates += 1;
        JSONArray clientIdList = ( JSONArray ) jsonObject.get( "clients" );
        JSONArray chatRoomsList = ( JSONArray ) jsonObject.get( "chatrooms" );

        for( Object clientID : clientIdList ) {
            Leader.getLeader().addClientUpdate( clientID.toString() );
        }

        for( Object chatRoom : chatRoomsList ) {
            JSONObject jsonObject_room = (JSONObject)chatRoom;
            Leader.getLeader().addApprovedRoom( jsonObject_room.get("clientid").toString(), jsonObject_room.get("roomid").toString(), Integer.parseInt(jsonObject_room.get("serverid").toString()) );
        }
    }
}
