package dev.foxgirl.damagenumbers.client;

import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.NotNull;

public interface DamageNumbersHandler {
    @NotNull Screen createConfigScreen(@NotNull Screen parent);
}