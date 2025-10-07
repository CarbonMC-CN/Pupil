package net.irisshaders.iris.api.v0.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

public interface IrisItemLightProvider {

	Vector3f DEFAULT_LIGHT_COLOR = new Vector3f(1.0f, 1.0f, 1.0f);

	default int getLightEmission(Player player, ItemStack stack) {
		if (stack.getItem() instanceof BlockItem item) {
			return item.getBlock().defaultBlockState().getLightEmission();
		}
		return 0;
	}

	default void getLightColor(Player player, ItemStack stack, Vector3f dest) {
		dest.set(DEFAULT_LIGHT_COLOR);
	}

	default Vector3f getLightColor(Player player, ItemStack stack) {
		Vector3f color = new Vector3f();
		getLightColor(player, stack, color);
		return color;
	}
}