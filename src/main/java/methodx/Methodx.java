package methodx;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.Request;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Methodx {

    String[] args();

    class Item {

        private final Method method;
        private final Object object;
        private final String[] argNames;
        private final ObjectMapper mapper;

        private Item(Method method, Object object, ObjectMapper mapper) {
            this.method = method;
            this.object = object;
            this.argNames = method.getAnnotation(Methodx.class).args();
            this.mapper = mapper;
        }

        Object invoke(Object[] args) throws InvocationTargetException, IllegalAccessException {
            return method.invoke(object, args);
        }

        Object[] parseArguments(Request req) throws Exceptions.ParseArgumentException {

            Class<?>[] argTypes = method.getParameterTypes();

            Map<String, String[]> parameterMap = null;

            Object[] args = new Object[argNames.length];
            for (int i = 0; i < argNames.length; i++) {
                String argName = argNames[i];
                switch (argName) {
                    case "@body": {
                        Class<?> argType = null;
                        try {
                            argType = method.getParameterTypes()[i];
                            args[i] = mapper.readValue(req.getReader(), method.getParameterTypes()[i]);
                        }
                        catch (Exception e) {
                            throw new Exceptions.ParseArgumentException(argName, argType, "[see request body]", e);
                        }
                        break;
                    }
                    default: {
                        if (parameterMap == null) {
                            parameterMap = req.getParameterMap();
                        }
                        Class<?> argType = argTypes[i];
                        String value = parameterMap.get(argName)[0];
                        if (argTypes[i] == String.class) {
                            args[i] = value;
                        }
                        else {
                            try {
                                Constructor<?> constructor = argTypes[i].getDeclaredConstructor(String.class);
                                args[i] = constructor.newInstance(value);
                            }
                            catch (Exception e) {
                                throw new Exceptions.ParseArgumentException(argName, argType, value, e);
                            }
                        }
                    }
                }
            }
            return args;
        }

        static Map<String, Methodx.Item> collect(Map<String, ?> source, ObjectMapper mapper) {
            Map<String, Item> map = new HashMap<>();
            for (Map.Entry<String, ?> entry : source.entrySet()) {
                Object bean = entry.getValue();
                for (Method method : bean.getClass().getDeclaredMethods()) {
                    if (null != method.getAnnotation(Methodx.class)) {
                        Item _new = new Item(method, bean, mapper);
                        String id = entry.getKey() + "." + method.getName();
                        Item old = map.put(id, _new);
                        if (null != old) {
                            throw new RuntimeException(format("method duplicate: old=%s; new=%s", old, _new));
                        }
                    }
                }
            }
            return map;
        }

        String[] getArgNames() {
            return argNames;
        }
    }
}
