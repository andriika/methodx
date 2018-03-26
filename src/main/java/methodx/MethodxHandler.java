package methodx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MethodxHandler extends AbstractHandler {

    private static final Logger log = LoggerFactory.getLogger(MethodxHandler.class);

    private final Map<String, Methodx.Item> methods;

    private final ObjectMapper mapper;

    private MethodxHandler(Map<String, Methodx.Item> methods, ObjectMapper mapper) {
        this.methods = methods;
        this.mapper = mapper;
    }

    @Override
    public void handle(String path, Request req, HttpServletRequest httpReq, HttpServletResponse res) throws IOException {

        String sign = getMethodSignature(path);

        Message body = new Message();
        Methodx.Item method = methods.get(sign);

        if (sign.equals("")) {
            body.status = 200;
            body.data = methods.entrySet().stream()
                    .map(e -> e.getKey() + " " + Arrays.toString(e.getValue().getArgNames()))
                    .collect(Collectors.toList());
        }
        else if (method == null) {
            body.error = "method not found: " + sign;
            body.status = 404;
        }
        else {
            try {
                Object[] args = method.parseArguments(req);
                try {
                    body.data = method.invoke(args);
                    body.status = 200;
                }
                catch (Exception e) {
                    if (e instanceof InvocationTargetException) {
                        e = (Exception) e.getCause();
                    }
                    body.error = e;
                    body.status = 500;
                    log.error("failed to process [{}] method", sign, e);
                }
            }
            catch (Exception e) {
                if (e instanceof Exceptions.ParseArgumentException) {
                    body.error = e.getMessage();
                }
                else {
                    body.error = e;
                }
                body.status = 500;
                log.error("failed to parse [{}] method arguments", sign, e);
            }
        }
        res.setContentType("application/json;charset=utf-8");
        res.setStatus(body.status);
        res.getWriter().print(mapper.writeValueAsString(body));
        req.setHandled(true);
    }

    private String getMethodSignature(String path) {
        return path.substring(1, path.length());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class Message {
        public int status;
        public Object data, error;
    }

    public static class Builder {

        private Map<String, Object> beans = new HashMap<>();

        private ObjectMapper mapper = null;

        public Builder addBean(String id, Object obj) {
            beans.put(id, obj);
            return this;
        }

        public Builder setObjectMapper(ObjectMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        public MethodxHandler build() {
            Map<String, Methodx.Item> methods = Methodx.Item.collect(beans, mapper);
            return new MethodxHandler(methods, mapper);
        }
    }
}
