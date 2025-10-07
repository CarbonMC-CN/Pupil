package net.irisshaders.iris.gl.buffer;

import com.mojang.blaze3d.platform.GlStateManager;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.gl.IrisRenderSystem;
import org.lwjgl.opengl.GL43C;

public final class ShaderStorageBuffer {
	private final int index;
	private final ShaderStorageInfo info;
	private int id;

	public ShaderStorageBuffer(int index, ShaderStorageInfo info) {
		this.index = index;
		this.info = info;
		this.id = IrisRenderSystem.createBuffers();
		GLDebug.nameObject(GL43C.GL_BUFFER, id, "SSBO " + index);
	}

	public int getIndex() {
		return index;
	}

	public long getSize() {
		return info.size();
	}

	void destroy() {
		IrisRenderSystem.bindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, index, 0);
		IrisRenderSystem.deleteBuffers(id);
	}

	public void bind() {
		IrisRenderSystem.bindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, index, id);
	}

	public void resizeIfRelative(int width, int height) {
		if (!info.relative()) return;
		IrisRenderSystem.deleteBuffers(id);
		int newId = GlStateManager._glGenBuffers();
		GlStateManager._glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, newId);
		int w = (int) (width * info.scaleX());
		int h = (int) (height * info.scaleY());
		int size = w * h * (int) info.size();
		IrisRenderSystem.bufferStorage(GL43C.GL_SHADER_STORAGE_BUFFER, size, 0);
		IrisRenderSystem.clearBufferSubData(GL43C.GL_SHADER_STORAGE_BUFFER, GL43C.GL_R8, 0, size, GL43C.GL_RED, GL43C.GL_BYTE, new int[]{0});
		IrisRenderSystem.bindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, index, newId);
		id = newId;
	}

	public int getId() {
		return id;
	}
}