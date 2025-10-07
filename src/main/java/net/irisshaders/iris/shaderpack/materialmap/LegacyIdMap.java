package net.irisshaders.iris.shaderpack.materialmap;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Provides legacy block ID mappings for compatibility with older shader packs
 * that use fixed numerical IDs instead of modern namespaced identifiers.
 */
public final class LegacyIdMap {
	private static final ImmutableList<String> COLORS = ImmutableList.of(
			"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
			"light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
	);

	private static final ImmutableList<String> WOOD_TYPES = ImmutableList.of(
			"oak", "birch", "jungle", "spruce", "acacia", "dark_oak"
	);

	// Common block groups for better organization
	private static final List<BlockEntry> STONE_BLOCKS = List.of(
			block("stone"), block("granite"), block("diorite"), block("andesite")
	);

	private static final List<BlockEntry> SMALL_FLOWERS = List.of(
			block("dandelion"), block("poppy"), block("blue_orchid"), block("allium"),
			block("azure_bluet"), block("red_tulip"), block("pink_tulip"), block("white_tulip"),
			block("orange_tulip"), block("oxeye_daisy"), block("cornflower"),
			block("lily_of_the_valley"), block("wither_rose")
	);

	private static final List<BlockEntry> TALL_PLANTS = List.of(
			block("sunflower"), block("lilac"), block("tall_grass"), block("large_fern"),
			block("rose_bush"), block("peony"), block("tall_seagrass")
	);

	private static final List<BlockEntry> CROPS = List.of(
			block("wheat"), block("carrots"), block("potatoes"), block("beetroots")
	);

	// Estimated number of entries for initial capacity
	private static final int EXPECTED_ENTRIES = 40;

	/**
	 * Populates the provided map with legacy block ID mappings for shader pack compatibility.
	 *
	 * @param blockIdMap the map to populate with legacy ID mappings
	 */
	public static void addLegacyValues(Int2ObjectMap<List<BlockEntry>> blockIdMap) {
		// Create a new properly sized map if possible, otherwise use the provided one
		if (blockIdMap instanceof Int2ObjectOpenHashMap) {
			// For OpenHashMap, we can't access ensureCapacity, but we can create a properly sized map
			// and copy the existing contents if any
			if (blockIdMap.isEmpty()) {
				initializeMap((Int2ObjectOpenHashMap<List<BlockEntry>>) blockIdMap);
			} else {
				// Map already has content, just add our values
				addAllMappings(blockIdMap);
			}
		} else {
			// For other map implementations, just add the mappings
			addAllMappings(blockIdMap);
		}
	}

	/**
	 * Initializes an empty OpenHashMap with proper capacity.
	 */
	private static void initializeMap(Int2ObjectOpenHashMap<List<BlockEntry>> map) {
		// FastUtil's OpenHashMap doesn't expose ensureCapacity, but we can rely on
		// its automatic resizing. Just add all mappings efficiently.
		addAllMappings(map);
	}

	/**
	 * Adds all legacy mappings to the target map.
	 */
	private static void addAllMappings(Int2ObjectMap<List<BlockEntry>> map) {
		// Basic building blocks
		put(map, 1,  STONE_BLOCKS);
		put(map, 2,  block("grass_block"));
		put(map, 4,  block("cobblestone"));
		put(map, 12, block("sand"));
		put(map, 24, block("sandstone"));

		// Ores and valuable blocks
		put(map, 41, block("gold_block"));
		put(map, 42, block("iron_block"));
		put(map, 57, block("diamond_block"));
		put(map, -123, block("emerald_block"));

		// Light sources
		put(map, 50, block("torch"));
		put(map, 76, block("redstone_torch"));
		put(map, 89, block("glowstone"));
		put(map, 124, block("redstone_lamp"));

		// Colored blocks
		putMany(map, 35, COLORS, color -> block(color + "_wool"));
		putMany(map, 95, COLORS, color -> block(color + "_stained_glass"));
		putMany(map, 160, COLORS, color -> block(color + "_stained_glass_pane"));

		// Liquids and transparent blocks
		put(map, 9,  block("water"));
		put(map, 11, block("lava"));
		put(map, 79, block("ice"));
		put(map, 111, block("lily_pad"));

		// Natural blocks
		putMany(map, 18, WOOD_TYPES, wood -> block(wood + "_leaves"));
		put(map, 31, block("grass"), block("fern"), block("seagrass"), block("sweet_berry_bush"));

		// Plants and flowers
		put(map, 37, SMALL_FLOWERS);
		put(map, 175, TALL_PLANTS);
		put(map, 59, CROPS);

		// Special blocks
		put(map, 51, block("fire"));
	}

	/**
	 * Creates a BlockEntry for a Minecraft block with the given name.
	 */
	private static BlockEntry block(String name) {
		return new BlockEntry(NamespacedId.fromCombined("minecraft:" + name), Collections.emptyMap());
	}

	/**
	 * Adds multiple block entries for a single legacy ID.
	 */
	private static void put(Int2ObjectMap<List<BlockEntry>> map, int id, BlockEntry... entries) {
		map.put(id, List.of(entries));
	}

	/**
	 * Adds a list of block entries for a single legacy ID.
	 */
	private static void put(Int2ObjectMap<List<BlockEntry>> map, int id, List<BlockEntry> entries) {
		map.put(id, entries);
	}

	/**
	 * Adds multiple block entries generated from a list of prefixes.
	 */
	private static void putMany(Int2ObjectMap<List<BlockEntry>> map, int id,
								List<String> prefixes, Function<String, BlockEntry> generator) {
		List<BlockEntry> entries = prefixes.stream()
				.map(generator)
				.collect(ImmutableList.toImmutableList());
		map.put(id, entries);
	}

	/**
	 * Creates a new Int2ObjectOpenHashMap with legacy values pre-populated.
	 * This is the recommended way to get a properly configured map.
	 */
	public static Int2ObjectOpenHashMap<List<BlockEntry>> createLegacyMap() {
		Int2ObjectOpenHashMap<List<BlockEntry>> map = new Int2ObjectOpenHashMap<>(EXPECTED_ENTRIES);
		addAllMappings(map);
		return map;
	}

	private LegacyIdMap() {
		throw new AssertionError("Cannot instantiate LegacyIdMap");
	}
}