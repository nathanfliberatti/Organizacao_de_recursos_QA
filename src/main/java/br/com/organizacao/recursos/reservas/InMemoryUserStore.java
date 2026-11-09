package br.com.organizacao.recursos.reservas;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
@Profile("!postgres & !perf")
public class InMemoryUserStore implements UserStore {
    private final ConcurrentMap<String, AppUser> users = new ConcurrentHashMap<>();

    @Override
    public Optional<AppUser> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    @Override
    public List<AppUser> findAll() {
        return users.values().stream().sorted(Comparator.comparing(AppUser::username)).toList();
    }

    @Override
    public AppUser create(AppUser user) {
        if (users.putIfAbsent(user.username(), user) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }
        return user;
    }

    @Override
    public long count() {
        return users.size();
    }
}
