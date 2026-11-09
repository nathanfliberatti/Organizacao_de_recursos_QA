package br.com.organizacao.recursos.reservas;

public record ResourceResponse(
        String id,
        String name,
        ResourceType type,
        int capacity,
        String location,
        String competency,
        boolean restricted) {
    public static ResourceResponse from(Resource resource) {
        return new ResourceResponse(resource.id(), resource.name(), resource.type(), resource.capacity(),
                resource.location(), resource.competency(), resource.restricted());
    }
}
