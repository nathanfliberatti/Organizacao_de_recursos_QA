package br.com.organizacao.recursos.reservas;

import java.util.List;

public record ProductUser(String login, String passwordHash, boolean active, List<ProductUserRole> roles) {
}