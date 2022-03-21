package ChatServer;

public class Server {
    private int serverid;
    private String server_address;
    private int clients_port;
    private int coordination_port;

    public Server (int serverIdentity, String server_address, int clients_port, int coordination_port){
        this.serverid = serverIdentity;
        this.server_address = server_address;
        this.clients_port = clients_port;
        this.coordination_port = coordination_port;

    }

    public int getServerIdentity(){
        return serverid;
    }

    public void setServerIdentity (int serverid){
        this.serverid = serverid;
    }

}
