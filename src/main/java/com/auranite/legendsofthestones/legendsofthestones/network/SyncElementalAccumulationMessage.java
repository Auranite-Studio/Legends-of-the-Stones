package com.auranite.legendsofthestones.legendsofthestones.network;

import com.auranite.legendsofthestones.legendsofthestones.ElementType;
import com.auranite.legendsofthestones.legendsofthestones.LegendsOfTheStones;
import com.auranite.legendsofthestones.legendsofthestones.LegendsOfTheStonesAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.NetworkDirection;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record SyncElementalAccumulationMessage(int entityId, Map<ElementType, Integer> accumulationData) {

    public static final int ID = LegendsOfTheStones.NETWORK_ID_BASE + 2; // Use next available ID
    
    public static SyncElementalAccumulationMessage decode(FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        int size = buf.readVarInt();
        Map<ElementType, Integer> accumulationData = new HashMap<>();
        
        for (int i = 0; i < size; i++) {
            ElementType type = ElementType.valueOf(buf.readUtf());
            int points = buf.readInt();
            accumulationData.put(type, points);
        }
        
        return new SyncElementalAccumulationMessage(entityId, accumulationData);
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeVarInt(accumulationData.size());
        
        for (Map.Entry<ElementType, Integer> entry : accumulationData.entrySet()) {
            buf.writeUtf(entry.getKey().name());
            buf.writeInt(entry.getValue());
        }
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void handle(SyncElementalAccumulationMessage message, PlayPayloadContext ctx) {
        ctx.workHandler().execute(() -> {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                Entity entity = player.level().getEntity(message.entityId());
                if (entity != null && entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
                    // Update the client-side accumulation data for the entity
                    // We need to update the attachment data on the client side
                    for (Map.Entry<ElementType, Integer> entry : message.accumulationData().entrySet()) {
                        LegendsOfTheStonesAttachments.setPoints(livingEntity, entry.getKey(), entry.getValue());
                    }
                    
                    System.out.println("Received elemental accumulation sync for entity " + message.entityId() + 
                                     " with data: " + message.accumulationData());
                }
            }
        });
    }
    
    // Method to send the sync message to a specific player
    public static void sendToPlayer(Player player, int entityId, Map<ElementType, Integer> accumulationData) {
        PacketDistributor.sendToPlayer(player, new SyncElementalAccumulationMessage(entityId, accumulationData));
    }
    
    // Method to broadcast the sync message to all players tracking an entity
    public static void sendToTracking(Entity entity, Map<ElementType, Integer> accumulationData) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, 
            new SyncElementalAccumulationMessage(entity.getId(), accumulationData));
    }
}