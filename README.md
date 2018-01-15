```
public class Service1 {
    ...

    @Methodx(args = {"id", "name", "@body"})
    public Result method1(Integer id, String name, User user) {
        ...
    }
}
```

```
> curl -d "{\"id\":1,\"name\":\"ivan\"}" localhost:8080/service1.method1?id=lorem&name=ipsum
{"status":200,"data": { <response> }}
```

```
import methodx.MethodxHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.Server;

public class Main {

    public static void main(String[] args) throws Exception {

        Service1 service1 = new Service1();

        Server server = new Server(8080);
        server.setHandler(new MethodxHandler.Builder()
                .addBean("service1", service1)
                .setObjectMapper(new ObjectMapper())
                .build());
        server.start();
        server.join();
    }
}
```