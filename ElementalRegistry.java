package com.example.elementalstones;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ElementalRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ElementalStonesMod.MOD_ID);

    // Регистрация всех 51 камня стихий
    public static final RegistryObject<Item> FIRE_STONE = ITEMS.register("fire_stone", () -> new ElementalStoneItem("Fire"));
    public static final RegistryObject<Item> AIR_STONE = ITEMS.register("air_stone", () -> new ElementalStoneItem("Air"));
    public static final RegistryObject<Item> WATER_STONE = ITEMS.register("water_stone", () -> new ElementalStoneItem("Water"));
    public static final RegistryObject<Item> EARTH_STONE = ITEMS.register("earth_stone", () -> new ElementalStoneItem("Earth"));
    public static final RegistryObject<Item> ENERGY_STONE = ITEMS.register("energy_stone", () -> new ElementalStoneItem("Energy"));
    public static final RegistryObject<Item> ICE_STONE = ITEMS.register("ice_stone", () -> new ElementalStoneItem("Ice"));
    public static final RegistryObject<Item> LIGHTNING_STONE = ITEMS.register("lightning_stone", () -> new ElementalStoneItem("Lightning"));
    public static final RegistryObject<Item> SOUND_STONE = ITEMS.register("sound_stone", () -> new ElementalStoneItem("Sound"));
    public static final RegistryObject<Item> CRYSTAL_STONE = ITEMS.register("crystal_stone", () -> new ElementalStoneItem("Crystal"));
    public static final RegistryObject<Item> LAVA_STONE = ITEMS.register("lava_stone", () -> new ElementalStoneItem("Lava"));
    public static final RegistryObject<Item> RAIN_STONE = ITEMS.register("rain_stone", () -> new ElementalStoneItem("Rain"));
    public static final RegistryObject<Item> TORNADO_STONE = ITEMS.register("tornado_stone", () -> new ElementalStoneItem("Tornado"));
    public static final RegistryObject<Item> OCEAN_STONE = ITEMS.register("ocean_stone", () -> new ElementalStoneItem("Ocean"));
    public static final RegistryObject<Item> GREEN_STONE = ITEMS.register("green_stone", () -> new ElementalStoneItem("Green"));
    public static final RegistryObject<Item> ANIMALS_STONE = ITEMS.register("animals_stone", () -> new ElementalStoneItem("Animals"));
    public static final RegistryObject<Item> METAL_STONE = ITEMS.register("metal_stone", () -> new ElementalStoneItem("Metal"));
    public static final RegistryObject<Item> LIGHT_STONE = ITEMS.register("light_stone", () -> new ElementalStoneItem("Light"));
    public static final RegistryObject<Item> SHADOW_STONE = ITEMS.register("shadow_stone", () -> new ElementalStoneItem("Shadow"));
    public static final RegistryObject<Item> VACUUM_STONE = ITEMS.register("vacuum_stone", () -> new ElementalStoneItem("Vacuum"));
    public static final RegistryObject<Item> SUN_STONE = ITEMS.register("sun_stone", () -> new ElementalStoneItem("Sun"));
    public static final RegistryObject<Item> MOON_STONE = ITEMS.register("moon_stone", () -> new ElementalStoneItem("Moon"));
    public static final RegistryObject<Item> COSMOS_STONE = ITEMS.register("cosmos_stone", () -> new ElementalStoneItem("Cosmos"));
    public static final RegistryObject<Item> BLOOD_STONE = ITEMS.register("blood_stone", () -> new ElementalStoneItem("Blood"));
    public static final RegistryObject<Item> TIME_STONE = ITEMS.register("time_stone", () -> new ElementalStoneItem("Time"));
    public static final RegistryObject<Item> TECHNOLOGY_STONE = ITEMS.register("technology_stone", () -> new ElementalStoneItem("Technology"));
    public static final RegistryObject<Item> TELEPORTATION_STONE = ITEMS.register("teleportation_stone", () -> new ElementalStoneItem("Teleportation"));
    public static final RegistryObject<Item> EXPLOSION_STONE = ITEMS.register("explosion_stone", () -> new ElementalStoneItem("Explosion"));
    public static final RegistryObject<Item> AMBER_STONE = ITEMS.register("amber_stone", () -> new ElementalStoneItem("Amber"));
    public static final RegistryObject<Item> CREATION_STONE = ITEMS.register("creation_stone", () -> new ElementalStoneItem("Creation"));
    public static final RegistryObject<Item> DESTRUCTION_STONE = ITEMS.register("destruction_stone", () -> new ElementalStoneItem("Destruction"));
    public static final RegistryObject<Item> FOG_STONE = ITEMS.register("fog_stone", () -> new ElementalStoneItem("Fog"));
    public static final RegistryObject<Item> SAND_STONE = ITEMS.register("sand_stone", () -> new ElementalStoneItem("Sand"));
    public static final RegistryObject<Item> SPEED_STONE = ITEMS.register("speed_stone", () -> new ElementalStoneItem("Speed"));
    public static final RegistryObject<Item> POISON_STONE = ITEMS.register("poison_stone", () -> new ElementalStoneItem("Poison"));
    public static final RegistryObject<Item> MAGNET_STONE = ITEMS.register("magnet_stone", () -> new ElementalStoneItem("Magnet"));
    public static final RegistryObject<Item> FUNGUS_STONE = ITEMS.register("fungus_stone", () -> new ElementalStoneItem("Fungus"));
    public static final RegistryObject<Item> MERCURY_STONE = ITEMS.register("mercury_stone", () -> new ElementalStoneItem("Mercury"));
    public static final RegistryObject<Item> MUSIC_STONE = ITEMS.register("music_stone", () -> new ElementalStoneItem("Music"));
    public static final RegistryObject<Item> PLAGUE_STONE = ITEMS.register("plague_stone", () -> new ElementalStoneItem("Plague"));
    public static final RegistryObject<Item> BLUE_FLAME_STONE = ITEMS.register("blue_flame_stone", () -> new ElementalStoneItem("BlueFlame"));
    public static final RegistryObject<Item> GRAVITY_STONE = ITEMS.register("gravity_stone", () -> new ElementalStoneItem("Gravity"));
    public static final RegistryObject<Item> SMOKE_STONE = ITEMS.register("smoke_stone", () -> new ElementalStoneItem("Smoke"));
    public static final RegistryObject<Item> SPIRIT_STONE = ITEMS.register("spirit_stone", () -> new ElementalStoneItem("Spirit"));
    public static final RegistryObject<Item> ETHER_STONE = ITEMS.register("ether_stone", () -> new ElementalStoneItem("Ether"));
    public static final RegistryObject<Item> FORM_STONE = ITEMS.register("form_stone", () -> new ElementalStoneItem("Form"));
    public static final RegistryObject<Item> MIND_STONE = ITEMS.register("mind_stone", () -> new ElementalStoneItem("Mind"));
    public static final RegistryObject<Item> GOLD_DUST_STONE = ITEMS.register("gold_dust_stone", () -> new ElementalStoneItem("GoldDust"));
    public static final RegistryObject<Item> DARKNESS_STONE = ITEMS.register("darkness_stone", () -> new ElementalStoneItem("Darkness"));
    public static final RegistryObject<Item> HEAT_STONE = ITEMS.register("heat_stone", () -> new ElementalStoneItem("Heat"));
    public static final RegistryObject<Item> IMPULSE_STONE = ITEMS.register("impulse_stone", () -> new ElementalStoneItem("Impulse"));
    public static final RegistryObject<Item> PAINT_STONE = ITEMS.register("paint_stone", () -> new ElementalStoneItem("Paint"));
}