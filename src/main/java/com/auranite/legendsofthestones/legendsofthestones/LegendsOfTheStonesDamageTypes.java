package com.auranite.legendsofthestones.legendsofthestones;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class LegendsOfTheStonesDamageTypes {
	public static final DeferredRegister<DamageType> DAMAGE_TYPES =
			DeferredRegister.create(Registries.DAMAGE_TYPE, LegendsOfTheStones.MODID);

	public static final Holder<DamageType> FIRE_ELEMENT = DAMAGE_TYPES.register("fire_element",
			() -> new DamageType("fire_element", 0.1f));

	public static final Holder<DamageType> PHYSICAL_ELEMENT = DAMAGE_TYPES.register("physical_element",
			() -> new DamageType("physical_element", 0.1f));

	public static void register(IEventBus modEventBus) {
		DAMAGE_TYPES.register(modEventBus);
	}
}