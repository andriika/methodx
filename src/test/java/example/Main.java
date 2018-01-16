package example;

import methodx.MethodxHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.Server;

public class Main {

    public static void main(String[] args) throws Exception {

        UserService userService = new UserService();

        Server server = new Server(8080);
        server.setHandler(new MethodxHandler.Builder()
                .addBean("userService", userService)
                .setObjectMapper(new ObjectMapper())
                .build());
        server.start();
        server.join();
    }
}