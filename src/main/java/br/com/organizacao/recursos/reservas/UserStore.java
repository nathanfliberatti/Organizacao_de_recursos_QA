package br.com.organizacao.recursos.reservas;

import java.util.List;
import java.util.Optional;

public interface UserStore {
    Optional<AppUser> findByUsername(String username);

    List<AppUser> findAll();

    AppUser create(AppUser user);

    long count();
}
