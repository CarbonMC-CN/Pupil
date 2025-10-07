package net.irisshaders.iris.shaderpack.materialmap;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class WorldRenderingSettings {
	public static final WorldRenderingSettings INSTANCE = new WorldRenderingSettings();

	private static final NamespacedId MINECRAFT_VILLAGER = NamespacedId.fromCombined("minecraft:villager");

	private boolean reloadRequired;
	private Object2IntMap<BlockState> blockStateIds;
	private Map<Holder.Reference<Block>, ChunkRenderTypeSet> blockTypeIds;
	private Object2IntFunction<NamespacedId> entityIds;
	private Object2IntFunction<NamespacedId> itemIds;

	private float ambientOcclusionLevel = 1.0F;

	private static final int FLAG_DISABLE_DIR_SHADE = 1 << 0;
	private static final int FLAG_USE_SEP_AO        = 1 << 1;
	private static final int FLAG_EXT_VERTEX_FMT    = 1 << 2;
	private static final int FLAG_SEP_ENTITY_DRAW   = 1 << 3;
	private static final int FLAG_VOXELIZE_LIGHT    = 1 << 4;
	private static final int FLAG_HAS_VILLAGER_ID   = 1 << 5;

	private int flags;


	public WorldRenderingSettings() {}

	private void markDirty() {
		reloadRequired = true;
	}

	private boolean flag(int mask) {
		return (flags & mask) != 0;
	}

	private void setFlag(int mask, boolean value) {
		int old = flags;
		flags = value ? (old | mask) : (old & ~mask);
		if (old != flags) markDirty();
	}

	public boolean isReloadRequired()               { return reloadRequired; }
	public void    clearReloadRequired()            { reloadRequired = false; }

	@Nullable
	public Object2IntMap<BlockState> getBlockStateIds() { return blockStateIds; }
	public void setBlockStateIds(Object2IntMap<BlockState> v) {
		if (java.util.Objects.equals(blockStateIds, v)) return;
		blockStateIds = v;
		markDirty();
	}

	@Nullable
	public Map<Holder.Reference<Block>, ChunkRenderTypeSet> getBlockTypeIds() { return blockTypeIds; }
	public void setBlockTypeIds(Map<Holder.Reference<Block>, ChunkRenderTypeSet> v) {
		if (java.util.Objects.equals(blockTypeIds, v)) return;
		blockTypeIds = v;
		markDirty();
	}

	@Nullable
	public Object2IntFunction<NamespacedId> getEntityIds() { return entityIds; }
	public void setEntityIds(Object2IntFunction<NamespacedId> v) {
		entityIds = v;
		setFlag(FLAG_HAS_VILLAGER_ID, v != null && v.containsKey(MINECRAFT_VILLAGER));
	}

	@Nullable
	public Object2IntFunction<NamespacedId> getItemIds() { return itemIds; }
	public void setItemIds(Object2IntFunction<NamespacedId> v) {
		itemIds = v;
	}

	public float getAmbientOcclusionLevel() { return ambientOcclusionLevel; }
	public void setAmbientOcclusionLevel(float v) {
		if (v == ambientOcclusionLevel) return;
		ambientOcclusionLevel = v;
		markDirty();
	}

	public boolean shouldDisableDirectionalShading() { return flag(FLAG_DISABLE_DIR_SHADE); }
	public void setDisableDirectionalShading(boolean v) { setFlag(FLAG_DISABLE_DIR_SHADE, v); }

	public boolean shouldUseSeparateAo()        { return flag(FLAG_USE_SEP_AO); }
	public void setUseSeparateAo(boolean v)     { setFlag(FLAG_USE_SEP_AO, v); }

	public boolean shouldUseExtendedVertexFormat() { return flag(FLAG_EXT_VERTEX_FMT); }
	public void setUseExtendedVertexFormat(boolean v) { setFlag(FLAG_EXT_VERTEX_FMT, v); }

	public boolean shouldVoxelizeLightBlocks()  { return flag(FLAG_VOXELIZE_LIGHT); }
	public void setVoxelizeLightBlocks(boolean v) { setFlag(FLAG_VOXELIZE_LIGHT, v); }

	public boolean shouldSeparateEntityDraws()  { return flag(FLAG_SEP_ENTITY_DRAW); }
	public void setSeparateEntityDraws(boolean v) { setFlag(FLAG_SEP_ENTITY_DRAW, v); }

	public boolean hasVillagerConversionId()    { return flag(FLAG_HAS_VILLAGER_ID); }
}