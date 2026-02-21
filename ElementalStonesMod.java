package com.example.elementalstones;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("elementalstones")
public class ElementalStonesMod {
    public static final String MOD_ID = "elementalstones";
    public static final Logger LOGGER = LogManager.getLogger();

    public ElementalStonesMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        // Регистрация элементов
        ElementalRegistry.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM PREINITIALIZATION");
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        // Клиентские настройки
    }
}