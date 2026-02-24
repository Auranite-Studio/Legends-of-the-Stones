package com.auranite.legendsofthestones;

import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 🔹 ОБРАБОТЧИК РЕГИСТРАЦИИ ЭЛЕМЕНТАЛЬНОГО ОРУЖИЯ 🔹
 *
 * Вызывается при инициализации мода для регистрации существующих предметов.
 */
@EventBusSubscriber
public class ElementalWeaponRegistrationHandler {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LegendsOfTheStones.LOGGER.info("⚔️ Registering elemental weapons...");

            registerFireWeapons();
            registerPhysicalWeapons();
            registerWindWeapons();
            registerWaterWeapons();
            registerEarthWeapons();
            registerIceWeapons();
            registerElectricWeapons();
            registerSourceWeapons();
            registerNaturalWeapons();
            registerQuantumWeapons();

            LegendsOfTheStones.LOGGER.info("✅ Elemental weapon registration complete! Total: {}",
                    ElementalWeaponRegistry.getRegisteredCount());
        });
    }

    private static void registerFireWeapons() {
        ElementalWeaponUtils.registerItem(Items.BLAZE_ROD, ElementType.FIRE, 10f);
        ElementalWeaponUtils.registerItem(Items.FLINT_AND_STEEL, ElementType.FIRE, 1f);
    }

    private static void registerPhysicalWeapons() {
        ElementalWeaponUtils.registerItem(Items.NETHERITE_SWORD, ElementType.PHYSICAL, 15f);
        ElementalWeaponUtils.registerItem(Items.DIAMOND_SWORD, ElementType.PHYSICAL, 10f);
        ElementalWeaponUtils.registerItem(Items.GOLDEN_SWORD, ElementType.PHYSICAL, 6f);
        ElementalWeaponUtils.registerItem(Items.IRON_SWORD, ElementType.PHYSICAL, 7f);
        ElementalWeaponUtils.registerItem(Items.STONE_SWORD, ElementType.PHYSICAL, 4f);
        ElementalWeaponUtils.registerItem(Items.WOODEN_SWORD, ElementType.PHYSICAL, 2f);

        ElementalWeaponUtils.registerItem(Items.NETHERITE_AXE, ElementType.PHYSICAL, 20f);
        ElementalWeaponUtils.registerItem(Items.DIAMOND_AXE, ElementType.PHYSICAL, 15f);
        ElementalWeaponUtils.registerItem(Items.GOLDEN_AXE, ElementType.PHYSICAL, 9f);
        ElementalWeaponUtils.registerItem(Items.IRON_AXE, ElementType.PHYSICAL, 10f);
        ElementalWeaponUtils.registerItem(Items.STONE_AXE, ElementType.PHYSICAL, 5f);
        ElementalWeaponUtils.registerItem(Items.WOODEN_AXE, ElementType.PHYSICAL, 3f);

        ElementalWeaponUtils.registerItem(Items.CROSSBOW, ElementType.PHYSICAL, 20f);
        ElementalWeaponUtils.registerItem(Items.TRIDENT, ElementType.PHYSICAL, 12f);
        ElementalWeaponUtils.registerItem(Items.MACE, ElementType.PHYSICAL, 50f);
        ElementalWeaponUtils.registerItem(Items.BOW, ElementType.PHYSICAL, 5f);
    }

    private static void registerWindWeapons() {
    }

    private static void registerWaterWeapons() {
    }

    private static void registerEarthWeapons() {
    }
    private static void registerIceWeapons() {
    }
    private static void registerElectricWeapons() {
    }
    private static void registerSourceWeapons() {
    }
    private static void registerNaturalWeapons() {
    }
    private static void registerQuantumWeapons() {
    }
}