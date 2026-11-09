package br.com.organizacao.recursos.reservas;

public record ResourceFilter(
        ResourceType type, Integer minCapacity, String location, String competency, String text) {
    public static ResourceFilter none() {
        return new ResourceFilter(null, null, null, null, null);
    }

    public boolean matches(Resource resource) {
        return (type == null || resource.type() == type)
                && (minCapacity == null || resource.capacity() >= minCapacity)
                && containsIgnoreCase(resource.location(), location)
                && containsIgnoreCase(resource.competency(), competency)
                && (text == null || text.isBlank()
                        || containsIgnoreCase(resource.id(), text) || containsIgnoreCase(resource.name(), text));
    }

    private static boolean containsIgnoreCase(String value, String part) {
        if (part == null || part.isBlank()) {
            return true;
        }
        return value != null && value.toLowerCase().contains(part.trim().toLowerCase());
    }
}
