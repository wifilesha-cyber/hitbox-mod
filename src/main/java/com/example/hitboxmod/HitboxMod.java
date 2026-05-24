package com.example.hitboxmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;

@Mod("hitboxmod")
public class HitboxMod {

    public static float hitboxScale = 0.0F;

    private static KeyBinding keyIncrease;
    private static KeyBinding keyDecrease;

    public HitboxMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        keyIncrease = new KeyBinding("key.hitbox.increase", GLFW.GLFW_KEY_INSERT, "category.hitbox");
        keyDecrease = new KeyBinding("key.hitbox.decrease", GLFW.GLFW_KEY_DELETE, "category.hitbox");
        
        ClientRegistry.registerKeyBinding(keyIncrease);
        ClientRegistry.registerKeyBinding(keyDecrease);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (Minecraft.getInstance().player == null) return;

        if (event.getAction() == GLFW.GLFW_PRESS) {
            if (event.getKey() == GLFW.GLFW_KEY_INSERT) {
                hitboxScale += 0.2F;
                if (hitboxScale > 3.0F) hitboxScale = 3.0F;
                printMessage("§a[Hitbox] Размер увеличен: +" + hitboxScale);
            }
            
            if (event.getKey() == GLFW.GLFW_KEY_DELETE) {
                hitboxScale -= 0.2F;
                if (hitboxScale < 0.0F) hitboxScale = 0.0F;
                printMessage("§c[Hitbox] Размер уменьшен: +" + hitboxScale);
            }
        }
    }

    @SubscribeEvent
    public void onClickInput(InputEvent.ClickInputEvent event) {
        if (event.isAttack() && hitboxScale > 0.0F) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null && mc.gameMode != null) {
                
                double maxReach = 3.0D + hitboxScale; 
                Entity target = getExtendedHitResult(mc.player, maxReach);
                
                if (target != null) {
                    event.setCanceled(true); 
                    
                    mc.gameMode.attack(mc.player, target);
                    mc.player.swing(Hand.MAIN_HAND);
                }
            }
        }
    }

    private Entity getExtendedHitResult(PlayerEntity player, double distance) {
        Vector3d eyePosition = player.getEyePosition(1.0F);
        Vector3d lookVector = player.getViewVector(1.0F);
        
        Vector3d reachVector = eyePosition.add(lookVector.x * distance, lookVector.y * distance, lookVector.z * distance);
        
        AxisAlignedBB searchArea = player.getBoundingBox()
                .expandTowards(lookVector.scale(distance))
                .inflate(1.0D, 1.0D, 1.0D); 
        
        if (player.level == null) return null;
        
        List<Entity> entities = player.level.getEntities(
            player, 
            searchArea, 
            entity -> entity != player && entity.isAlive() && entity.isPickable()
        );
        
        Entity closestEntity = null;
        double closestDistance = distance;

        for (Entity entity : entities) {
            AxisAlignedBB virtualBox = entity.getBoundingBox().inflate(hitboxScale, 0.0D, hitboxScale);
            Optional<Vector3d> rayTraceResult = virtualBox.clip(eyePosition, reachVector);
            
            if (virtualBox.contains(eyePosition)) {
                if (closestDistance >= 0.0D) {
                    closestEntity = entity;
                    closestDistance = 0.0D;
                }
            } else if (rayTraceResult.isPresent()) {
                double distanceToHit = eyePosition.distanceTo(rayTraceResult.get());
                if (distanceToHit < closestDistance || closestDistance == 0.0D) {
                    closestEntity = entity;
                    closestDistance = distanceToHit;
                }
            }
        }
        return closestEntity;
    }

    private void printMessage(String text) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                ITextComponent.nullToEmpty(text), true
            );
        }
    }
}
