package xmethod;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.Request;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
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
        private final ObjectMapper mapper;

        private Item(Method method, Object object, ObjectMapper mapper) {
            this.method = method;
            this.object = object;
            this.argNames = method.getAnnotation(XMethod.class).args();
            this.mapper = mapper;
        }

        public Object invoke(Object[] args) throws InvocationTargetException, IllegalAccessException {
            return method.invoke(object, args);
        }

        public Object[] parseArguments(Request req) throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

            Class<?>[] argTypes = method.getParameterTypes();

            Map<String, String[]> parameterMap = null;

            Object[] args = new Object[argNames.length];
            for (int i = 0; i < argNames.length; i++) {
                String argName = argNames[i];
                switch (argName) {
                    case "@body":
                        args[i] = mapper.readValue(req.getReader(), method.getParameterTypes()[i]);
                        break;
                    default:
                        if (parameterMap == null) {
                            parameterMap = req.getParameterMap();
                        }
                        if (argTypes[i] == String.class) {
                            args[i] = parameterMap.get(argName)[0];
                        }
                        else {
                            Constructor<?> constructor = argTypes[i].getDeclaredConstructor(String.class);
                            args[i] = constructor.newInstance(parameterMap.get(argName)[0]);
                        }
                        break;
                }
            }
            return args;
        }

        public static Map<String, XMethod.Item> collect(Collection<?> source, ObjectMapper mapper) {
            Map<String, Item> map = new HashMap<>();
            for (Object bean : source) {
                for (Method method : bean.getClass().getDeclaredMethods()) {
                    if (null != method.getAnnotation(XMethod.class)) {
                        Item _new = new Item(method, bean, mapper);
                        String id = method.getDeclaringClass().getSimpleName() + "." + method.getName();
                        Item old = map.put(id, _new);
                        if (null != old) {
                            throw new RuntimeException(format("method duplicate: old=%s; new=%s", old, _new));
                        }
                    }
                }
            }
            return map;
        }
    }
}
