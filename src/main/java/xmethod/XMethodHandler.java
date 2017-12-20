package xmethod;

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
import java.util.ArrayList;
import java.util.Map;

public class XMethodHandler extends AbstractHandler {

    private static final Logger log = LoggerFactory.getLogger(XMethodHandler.class);

    private final Map<String, XMethod.Item> methods;

    private final ObjectMapper omap;

    private XMethodHandler(Map<String, XMethod.Item> methods, ObjectMapper omap) {
        this.methods = methods;
        this.omap = omap;
    }

    @Override
    public void handle(String path, Request req, HttpServletRequest httpReq, HttpServletResponse res) throws IOException {

        path = path.substring(1, path.length());

        Message body = new Message();

        XMethod.Item method = methods.get(path);
        if (method == null) {
            body.error = new RuntimeException("method not found: " + path);
            body.status = 404;
        } else {
            try {
                body.data = method.invoke(req);
                body.status = 200;
            } catch (Exception e) {
                if (e instanceof InvocationTargetException) {
                    e = (Exception) e.getCause();
                }
                body.error = e;
                body.status = 500;
                log.error("failed to process [{}] method", path, e);
            }
        }
        res.setContentType("application/json;charset=utf-8");
        res.setStatus(body.status);
        res.getWriter().print(omap.writeValueAsString(body));
        req.setHandled(true);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class Message {
        public int status;
        public Object data, error;
    }

    public static class Builder {

        private ArrayList<Object> beans = new ArrayList<>();

        private ObjectMapper omap = null;

        public Builder addBean(Object obj) {
            beans.add(obj);
            return this;
        }

        public Builder setObjectMapper(ObjectMapper omap) {
            this.omap = omap;
            return this;
        }

        public XMethodHandler build() {
            Map<String, XMethod.Item> methods = XMethod.Item.collect(beans, omap);
            return new XMethodHandler(methods, omap);
        }
    }
}
