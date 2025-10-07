package net.irisshaders.iris.gl.buffer;

import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.sampler.SamplerLimits;
import org.lwjgl.opengl.GL43C;

public final class ShaderStorageBufferHolder {
	private int cachedW, cachedH;
	private ShaderStorageBuffer[] buffers;
	private boolean destroyed;

	public ShaderStorageBufferHolder(Int2ObjectArrayMap<ShaderStorageInfo> overrides, int width, int height) {
		cachedW = width;
		cachedH = height;
		int max = overrides.int2ObjectEntrySet().stream().mapToInt(e -> e.getIntKey()).max().orElse(-1);
		buffers = new ShaderStorageBuffer[max + 1];
		overrides.forEach((i, info) -> {
			long sz = info.size();
			if (sz > IrisRenderSystem.getVRAM())
				throw new OutOfVideoMemoryError("We only have " + (IrisRenderSystem.getVRAM() >> 20) + "MiB of VRAM, but pack requests " + sz + "!");
			if (i > SamplerLimits.get().getMaxShaderStorageUnits())
				throw new IllegalStateException("We don't have enough SSBO units??? (index: " + i + ", max: " + SamplerLimits.get().getMaxShaderStorageUnits());
			buffers[i] = new ShaderStorageBuffer(i, info);
			int id = buffers[i].getId();
			if (info.relative()) buffers[i].resizeIfRelative(width, height);
			else {
				GlStateManager._glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, id);
				IrisRenderSystem.bufferStorage(GL43C.GL_SHADER_STORAGE_BUFFER, sz, 0);
				IrisRenderSystem.clearBufferSubData(GL43C.GL_SHADER_STORAGE_BUFFER, GL43C.GL_R8, 0, sz, GL43C.GL_RED, GL43C.GL_BYTE, new int[]{0});
				IrisRenderSystem.bindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, i, id);
			}
		});
		GlStateManager._glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);
	}

	public void hasResizedScreen(int width, int height) {
		if (width != cachedW || height != cachedH) {
			cachedW = width;
			cachedH = height;
			for (ShaderStorageBuffer b : buffers) if (b != null) b.resizeIfRelative(width, height);
		}
	}

	public void setupBuffers() {
		if (destroyed) throw new IllegalStateException("Tried to use destroyed buffer objects");
		for (ShaderStorageBuffer b : buffers) if (b != null) b.bind();
	}

	public int getBufferIndex(int index) {
		if (index >= buffers.length || buffers[index] == null)
			throw new RuntimeException("Tried to query a buffer for indirect dispatch that doesn't exist!");
		return buffers[index].getId();
	}

	public void destroyBuffers() {
		for (ShaderStorageBuffer b : buffers) if (b != null) b.destroy();
		buffers = null;
		destroyed = true;
	}

	private static final class OutOfVideoMemoryError extends RuntimeException {
		OutOfVideoMemoryError(String s) { super(s); }
	}
}