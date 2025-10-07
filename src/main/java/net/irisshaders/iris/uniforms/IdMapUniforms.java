package net.irisshaders.iris.uniforms;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import net.irisshaders.iris.api.v0.item.IrisItemLightProvider;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.shaderpack.IdMap;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import static net.irisshaders.iris.gl.uniform.UniformUpdateFrequency.PER_FRAME;

public final class IdMapUniforms {

	private IdMapUniforms() {
	}

	public static void addIdMapUniforms(FrameUpdateNotifier notifier, UniformHolder uniforms, IdMap idMap, boolean isOldHandLight) {
		HeldItemSupplier mainHandSupplier = new HeldItemSupplier(InteractionHand.MAIN_HAND, idMap.getItemIdMap(), isOldHandLight);
		HeldItemSupplier offHandSupplier = new HeldItemSupplier(InteractionHand.OFF_HAND, idMap.getItemIdMap(), false);
		notifier.addListener(mainHandSupplier::update);
		notifier.addListener(offHandSupplier::update);

		uniforms
				.uniform1i(UniformUpdateFrequency.PER_FRAME, "heldItemId", mainHandSupplier::getIntID)
				.uniform1i(UniformUpdateFrequency.PER_FRAME, "heldItemId2", offHandSupplier::getIntID)
				.uniform1i(PER_FRAME, "heldBlockLightValue", mainHandSupplier::getLightValue)
				.uniform1i(PER_FRAME, "heldBlockLightValue2", offHandSupplier::getLightValue)
				.uniform3f(PER_FRAME, "heldBlockLightColor", mainHandSupplier::getLightColor)
				.uniform3f(PER_FRAME, "heldBlockLightColor2", offHandSupplier::getLightColor);
	}

	private static final class HeldItemSupplier {
		private static final Vector3f DEFAULT_LIGHT_COLOR = IrisItemLightProvider.DEFAULT_LIGHT_COLOR;

		private final InteractionHand hand;
		private final Object2IntFunction<NamespacedId> itemIdMap;
		private final boolean applyOldHandLight;

		private int intID = -1;
		private int lightValue = 0;
		private final Vector3f lightColor = new Vector3f(DEFAULT_LIGHT_COLOR);

		HeldItemSupplier(InteractionHand hand, Object2IntFunction<NamespacedId> itemIdMap, boolean applyOldHandLight) {
			this.hand = hand;
			this.itemIdMap = itemIdMap;
			this.applyOldHandLight = applyOldHandLight && hand == InteractionHand.MAIN_HAND;
		}

		void update() {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player == null) {
				invalidate();
				return;
			}

			ItemStack stack = player.getItemInHand(hand);
			if (stack.isEmpty()) {
				invalidate();
				return;
			}

			Item item = stack.getItem();
			ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
			NamespacedId id = NamespacedId.of(key.getNamespace(), key.getPath()); // 使用缓存或不可变对象池
			intID = itemIdMap.applyAsInt(id);

			if (!(item instanceof IrisItemLightProvider provider)) {
				invalidate();
				return;
			}

			lightValue = provider.getLightEmission(player, stack);
			provider.getLightColor(player, stack, lightColor);

			if (applyOldHandLight) {
				applyOldHandLighting(player);
			}
		}

		private void applyOldHandLighting(LocalPlayer player) {
			ItemStack offStack = player.getItemInHand(InteractionHand.OFF_HAND);
			if (offStack.isEmpty() || !(offStack.getItem() instanceof IrisItemLightProvider offProvider)) {
				return;
			}

			int offLight = offProvider.getLightEmission(player, offStack);
			if (offLight > lightValue) {
				lightValue = offLight;
				offProvider.getLightColor(player, offStack, lightColor);
			}
		}

		private void invalidate() {
			intID = -1;
			lightValue = 0;
			lightColor.set(DEFAULT_LIGHT_COLOR);
		}

		int getIntID() {
			return intID;
		}

		int getLightValue() {
			return lightValue;
		}

		Vector3f getLightColor() {
			return lightColor;
		}
	}
}