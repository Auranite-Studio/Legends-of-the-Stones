package com.example.elementalstones;

import net.minecraft.world.entity.player.Player;

public class ElementalPowerManager {
    private int energy = 100; // Максимальная энергия
    private int ultimateEnergy = 100; // Энергия ульты
    private final int ENERGY_PER_SKILL = 50; // Стоимость навыка
    private final int ENERGY_PER_SECOND = 2; // Восстановление энергии в секунду
    private final int ULTIMATE_PER_HIT = 5; // Восстановление ульты за удар
    private boolean[] activeStones = new boolean[3]; // Активные камни (до 3)
    private String[] activeElements = new String[3]; // Активные элементы

    public void activateStone(Player player, String element) {
        for (int i = 0; i < 3; i++) {
            if (!activeStones[i]) {
                activeStones[i] = true;
                activeElements[i] = element;
                applyElementalEffects(player, element);
                ElementalStonesMod.LOGGER.info("Activated {} stone for {}", element, player.getName().getString());
                return;
            }
        }
        ElementalStonesMod.LOGGER.warn("Player {} tried to activate more than 3 stones", player.getName().getString());
    }

    public void deactivateStone(Player player, int slot) {
        if (slot >= 0 && slot < 3 && activeStones[slot]) {
            removeElementalEffects(player, activeElements[slot]);
            activeStones[slot] = false;
            activeElements[slot] = null;
            ElementalStonesMod.LOGGER.info("Deactivated stone in slot {} for {}", slot, player.getName().getString());
        }
    }

    private void applyElementalEffects(Player player, String element) {
        // Применить броню и оружие соответствующей стихии
        // Выдать навык и ульту
    }

    private void removeElementalEffects(Player player, String element) {
        // Снять броню и оружие
        // Удалить навык и ульту
    }

    public void useSkill() {
        if (energy >= ENERGY_PER_SKILL) {
            energy -= ENERGY_PER_SKILL;
            // Активация навыка
        }
    }

    public void useUltimate() {
        if (ultimateEnergy >= 100) {
            ultimateEnergy = 0;
            // Активация ульты
        }
    }

    public void restoreEnergy() {
        if (energy < 100) {
            energy = Math.min(100, energy + ENERGY_PER_SECOND);
        }
        if (ultimateEnergy < 100) {
            ultimateEnergy = Math.min(100, ultimateEnergy + ULTIMATE_PER_HIT); // Увеличивается при атаках
        }
    }

    public int getEnergy() { return energy; }
    public int getUltimateEnergy() { return ultimateEnergy; }
    public boolean[] getActiveStones() { return activeStones; }
}