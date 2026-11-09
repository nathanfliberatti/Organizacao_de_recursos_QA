package br.com.organizacao.recursos.reservas;

public record AppUser(String username, String passwordHash, String displayName, UserRole role, boolean active) {
}
