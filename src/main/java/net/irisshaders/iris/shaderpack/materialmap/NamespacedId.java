package net.irisshaders.iris.shaderpack.materialmap;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class NamespacedId {
	private static final ConcurrentHashMap<NamespacedId, NamespacedId> INTERN_POOL = new ConcurrentHashMap<>();

	private final String namespace;
	private final String name;
	private final int hash;

	public NamespacedId(String combined) {
		int colon = combined.indexOf(':');
		this.namespace = colon == -1 ? "minecraft" : combined.substring(0, colon);
		this.name     = colon == -1 ? combined      : combined.substring(colon + 1);
		this.hash     = 31 * namespace.hashCode() + name.hashCode();
	}

	public NamespacedId(String namespace, String name) {
		this.namespace = Objects.requireNonNull(namespace);
		this.name     = Objects.requireNonNull(name);
		this.hash     = 31 * namespace.hashCode() + name.hashCode();
	}

	public static NamespacedId of(String namespace, String name) {
		NamespacedId id = new NamespacedId(namespace, name);
		return INTERN_POOL.putIfAbsent(id, id);
	}

	public static NamespacedId fromCombined(String combined) {
		NamespacedId id = new NamespacedId(combined);
		return INTERN_POOL.putIfAbsent(id, id);
	}

	public String getNamespace() { return namespace; }
	public String getName()     { return name; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof NamespacedId that)) return false;
		return hash == that.hash && namespace.equals(that.namespace) && name.equals(that.name);
	}

	@Override
	public int hashCode() { return hash; }

	@Override
	public String toString() { return namespace + ':' + name; }
}