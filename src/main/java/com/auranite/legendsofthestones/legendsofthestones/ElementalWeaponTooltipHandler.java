package com.auranite.legendsofthestones.legendsofthestones;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = LegendsOfTheStones.MODID)
public class ElementalWeaponTooltipHandler {

	// === ПЕРЕВОДНЫЕ КЛЮЧИ ===
	private static final String KEY_ELEMENT_FIRE = "elemental.tooltip.fire";
	private static final String KEY_ELEMENT_PHYSICAL = "elemental.tooltip.physical";
	private static final String KEY_ELEMENT_WIND = "elemental.tooltip.wind";
	private static final String KEY_ELEMENT_WATER = "elemental.tooltip.water";
	private static final String KEY_ELEMENT_EARTH = "elemental.tooltip.earth";
	private static final String KEY_ELEMENT_ICE = "elemental.tooltip.ice";
	private static final String KEY_ELEMENT_ELECTRIC = "elemental.tooltip.electric";
	private static final String KEY_ELEMENT_SOURCE = "elemental.tooltip.source";
	private static final String KEY_ELEMENT_NATURAL = "elemental.tooltip.natural";
	private static final String KEY_ELEMENT_QUANTUM = "elemental.tooltip.quantum";
	private static final String KEY_ELEMENT_DEFAULT = "elemental.tooltip.element";

	private static final String KEY_ACCUM_MULTIPLIER = "elemental.tooltip.accum_multiplier";

	@SubscribeEvent
	public static void onItemTooltip(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();

		ElementType type = ElementalWeaponUtils.getElementType(stack);
		float accumMultiplier = ElementalWeaponUtils.getAccumulationMultiplier(stack);

		if (type != null && accumMultiplier != 0.0f && accumMultiplier != 1.0f) {
			// Название элемента с переводом и цветом
			MutableComponent elementText = getElementText(type);
			event.getToolTip().add(1, elementText);

			// Множитель резонанса
			MutableComponent accumText = Component.translatable(
					KEY_ACCUM_MULTIPLIER,
					String.format("%.1f", accumMultiplier)
			);
			accumText.setStyle(accumText.getStyle().withColor(0x00AA00));
			event.getToolTip().add(Component.literal(" ").append(accumText));
		}
	}

	/**
	 * Возвращает переводимый текст элемента с цветом.
	 */
	private static MutableComponent getElementText(ElementType type) {
		MutableComponent text = switch (type) {
			case FIRE -> Component.translatable(KEY_ELEMENT_FIRE);
			case PHYSICAL -> Component.translatable(KEY_ELEMENT_PHYSICAL);
			case WIND -> Component.translatable(KEY_ELEMENT_WIND);
			case WATER -> Component.translatable(KEY_ELEMENT_WATER);
			case EARTH -> Component.translatable(KEY_ELEMENT_EARTH);
			case ICE -> Component.translatable(KEY_ELEMENT_ICE);
			case ELECTRIC -> Component.translatable(KEY_ELEMENT_ELECTRIC);
			case SOURCE -> Component.translatable(KEY_ELEMENT_SOURCE);
			case NATURAL -> Component.translatable(KEY_ELEMENT_NATURAL);
			case QUANTUM -> Component.translatable(KEY_ELEMENT_QUANTUM);
			default -> Component.translatable(KEY_ELEMENT_DEFAULT, type.name());
		};

		text.setStyle(text.getStyle().withColor(getElementColor(type)));
		return text;
	}

	/**
	 * Получает цвет для элемента.
	 */
	private static int getElementColor(ElementType type) {
		return switch (type) {
			case FIRE -> 0xFF5500;
			case PHYSICAL -> 0xFFAA00;
			case WIND -> 0x00FFFF;
			case WATER -> 0x0080FF;
			case EARTH -> 0x8B4513;
			case ICE -> 0x00BFFF;
			case ELECTRIC -> 0x9932CC;
			case SOURCE -> 0xFF5C77;
			case NATURAL -> 0x32CD32;
			case QUANTUM -> 0x9400D3;
			default -> 0xFFFFFF;
		};
	}

	/**
	 * Вспомогательный метод для получения сущности, на которую смотрит игрок.
	 * (На данный момент не используется в этом классе, но может пригодиться в будущем)
	 */
	private static Entity getLookedEntity(net.minecraft.world.entity.player.Player player) {
		if (player == null) return null;

		HitResult hitResult = player.pick(5.0, 0.0f, false);

		if (hitResult instanceof EntityHitResult entityHitResult) {
			return entityHitResult.getEntity();
		}

		return null;
	}
}