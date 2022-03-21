import data.ServerState;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class ServerMain {

    private final Logger LOGGER = LoggerFactory.getLogger(ServerMain.class);

    public static void main(String[] args){
        System.out.println("LOG  | ARG[0] : " + args[0] + " ARG[1] : " + args[1]  );
        log.info("args [0] : ", args[0]);
        log.info("args [1] : ", args[1]);

        ServerState.getInstance().initializeServer(args[0], args[1]);
    }

}
