package com.example.elementalstones;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

public class ElementalStoneItem extends Item {
    private final String element;

    public ElementalStoneItem(String element) {
        super(new Item.Properties());
        this.element = element;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            // Логика активации камня
            activateElementalStone(player, element);
        }
        return InteractionResultHolder.success(stack);
    }

    private void activateElementalStone(Player player, String element) {
        // Здесь будет логика активации эффектов стихии
        ElementalStonesMod.LOGGER.info("{} activated by {}", element, player.getName().getString());
    }
}