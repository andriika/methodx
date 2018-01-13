package example;

import xmethod.XMethod;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {

    private final ConcurrentHashMap<Integer, User> users = new ConcurrentHashMap<>();

    @XMethod(args = {"id"})
    public User getById(Integer id) {
        return users.get(id);
    }

    @XMethod(args = {})
    public Collection<User> getAll() {
        return users.values();
    }

    @XMethod(args = {"@body"})
    public void add(User user) {
        User current = users.putIfAbsent(user.id, user);
        if (current != null) {
            throw new RuntimeException("user already exists; id=" + user.id);
        }
    }

    @XMethod(args = {})
    public void deleteAll() {
        users.clear();
    }

    public static class User {
        public Integer id;
        public String name;
    }
}
