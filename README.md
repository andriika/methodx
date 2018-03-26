# Methodx

RPC over HTTP

## About

**Methodx** is an experimental approach and framework for building easy-to-use, developer-friendly web-services.

> REST is so boring and complex, all these GET, POST, PATCH, path/to/container/resource/id... Why bother with all of this mess if, most of the time, we just need RPC over HTTP?

## Quick Start
This guide walks you through the process of creating a "hello $name" web service with MethodX.

### Dependencies

Add **Methodx** jar to your classpath using following maven dependency:
```
<dependency>
    <groupId>net.mainclass</groupId>
    <artifactId>methodx</artifactId>
    <version>1.0</version>
</dependency>
```
Or visit [maven central](https://mvnrepository.com/artifact/net.mainclass/methodx)
 for details on the latest version and other available build tools declaration (e.g. Gradle, SBT, Ivy).

Methodx uses slf4j for logging, make sure you have any slf4j implementation in the classpath, for example, it could be logback:
```
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.2.3</version>
</dependency>
```

### Code

Now, let's create some simple service class and annotate its public method with `@Methodx` annotation:
```
import methodx.Methodx;
import java.util.concurrent.atomic.AtomicLong;

public class GreetingService {

    private final AtomicLong count = new AtomicLong();

    @Methodx(args = {"name"})
    public Greeting sayHello(String name) {
        Greeting res = new Greeting();
        res.id = count.incrementAndGet();
        res.message = "hello " + name;
        return res;
    }

    public static class Greeting {
        public Long id;
        public String message;
    }
}
```
Lastly, create class with main method as below. Following class configures and runs our web-service on top of embedded jetty server and right from the main method:
```
import com.fasterxml.jackson.databind.ObjectMapper;
import methodx.MethodxHandler;
import org.eclipse.jetty.server.Server;

public class Main {

    public static void main(String[] args) throws Exception {

        // create greeting service
        GreetingService greetingService = new GreetingService();
        // create methodx request handler with greeting service
        MethodxHandler handler = new MethodxHandler.Builder()
                .addBean("greetingService", greetingService)
                .setObjectMapper(new ObjectMapper())
                .build();

        // create and start jetty server with methodx request handler
        Server server = new Server(8080);
        server.setHandler(handler);
        server.start();
        server.join();
    }
}
```
## Run and Test

Run java main method class from above. Now that the server is up, visit http://localhost:8080/, where you see list of exposed methods with the single item of `greetingService.sayHello` - only method we annotated with the `@Methodx`
```
{
    "status":200,
    "data":[
        "greetingService.sayHello [name]"
    ]
}
```
Let's call `greetingService.sayHello` and pass "world" as argument "name" value: http://localhost:8080/greetingService.sayHello?name=world, where you see:
```
{
    "status":200,
    "data": {
        "id":1,
        "message":"hello world"
    }
}
```
Provide a different `name` query parameter, like http://localhost:8080/greetingService.sayHello?name=sun. Notice how the value of the message attribute changes from "hello world" to "hello sun":
```
{
    "status":200,
    "data": {
        "id":2,
        "message":"hello sun"
    }
}
```
Notice also how the id attribute has changed from 1 to 2. This proves that you are working against the same GreetingService instance across multiple requests.

### Next steps

When you’re ready to go further, please check out the advanced tutorial for building web-service with MethodX and Spring Framework.

TODO: advanced tutorial
