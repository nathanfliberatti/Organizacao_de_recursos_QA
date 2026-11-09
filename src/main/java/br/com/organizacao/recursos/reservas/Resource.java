package br.com.organizacao.recursos.reservas;

public record Resource(
        String id,
        String name,
        ResourceType type,
        int capacity,
        String location,
        String competency,
        boolean restricted) {
    public Resource {
        if (name == null || name.isBlank()) {
            name = id;
        }
        if (type == null) {
            type = ResourceType.SALA;
        }
    }

    public Resource(String id, int capacity) {
        this(id, id, ResourceType.SALA, capacity, null, null, false);
    }
}
