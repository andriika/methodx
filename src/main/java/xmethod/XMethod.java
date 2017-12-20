package xmethod;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.Request;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface XMethod {

    String[] args();

    class Item {

        private final Method method;
        private final Object object;
        private final String[] argNames;
        private final ObjectMapper omap;

        private Item(Method method, Object object, ObjectMapper omap) {
            this.method = method;
            this.object = object;
            this.argNames = method.getAnnotation(XMethod.class).args();
            this.omap = omap;
        }

        public Object invoke(Request req) throws InvocationTargetException, IllegalAccessException, IOException {
            return method.invoke(object, toArgs(req));
        }

        private Object[] toArgs(Request req) throws IOException {

            Map<String, String[]> parameterMap = null;

            Object[] args = new Object[argNames.length];
            for (int i = 0; i < argNames.length; i++) {
                String argName = argNames[i];
                switch (argName) {
                    case "@body":
                        args[i] = omap.readValue(req.getReader(), method.getParameterTypes()[i]);
                        break;
                    default:
                        if (parameterMap == null) {
                            parameterMap = req.getParameterMap();
                        }
                        args[i] = parameterMap.get(argName)[0];
                        break;
                }
            }
            return args;
        }

        public static Map<String, XMethod.Item> collect(Collection<?> source, ObjectMapper omap) {
            Map<String, Item> map = new HashMap<>();
            for (Object bean : source) {
                for (Method method : bean.getClass().getDeclaredMethods()) {
                    if (null != method.getAnnotation(XMethod.class)) {
                        Item _new = new Item(method, bean, omap);
                        String id = method.getDeclaringClass().getSimpleName() + "." + method.getName();
                        Item old = map.put(id, _new);
                        if (null != old) {
                            throw new RuntimeException(format(
                                    "exposed method duplicate: old=%s; new=%s", old, _new));
                        }
                    }
                }
            }
            return map;
        }
    }
}
