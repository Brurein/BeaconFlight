package com.extracraftx.minecraft.beaconflight.mixin;

import com.extracraftx.minecraft.beaconflight.BeaconFlight;
import com.extracraftx.minecraft.beaconflight.config.Config;
import com.extracraftx.minecraft.beaconflight.events.EventHandler;
import com.extracraftx.minecraft.beaconflight.interfaces.FlyEffectable;
import com.mojang.authlib.GameProfile;

import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements FlyEffectable{
    

    public ServerPlayerEntityMixin(World world_1, BlockPos blockPos_1, float float_1, GameProfile gameProfile_1) {
		super(world_1, blockPos_1, float_1, gameProfile_1);
		// TODO Auto-generated constructor stub
	}

	private int flyTicksLeft = 0;
    private float xpCounter;

    @Override
    public void allowFlight(int ticks, boolean setFlying) {
    	//BeaconFlight.log(Level.DEBUG, "Enabling flight");
        flyTicksLeft = Math.max(flyTicksLeft, ticks);
        if(Config.INSTANCE.xpDrainRate == 0 || totalExperience > 0){
            abilities.allowFlying = true;
            if(setFlying)
                abilities.flying = true;
            sendAbilitiesUpdate();
        }
       
    }

    @Override
    public void disallowFlight() {
    	//BeaconFlight.log(Level.INFO, "Disabling flight");
        abilities.allowFlying = false;
        abilities.flying = false;
        sendAbilitiesUpdate();
        //BeaconFlight.log(Level.DEBUG, "No longer flight active SLOW FAALLLLLLLLL");
        addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, Config.INSTANCE.slowFallingTime*20));
    }

    @Override
    public void tickFlight() {
        if(flyTicksLeft > 0){
            if(Config.INSTANCE.xpDrainRate != 0){
                if(abilities.flying){
                    xpCounter += Config.INSTANCE.xpDrainRate;
                    addExperience(-(int)Math.floor(xpCounter));
                    xpCounter %= 1;
                    if(Config.INSTANCE.xpDrainRate > 0 && totalExperience == 0)
                        disallowFlight();
                }
                if(totalExperience > 0)
                    allowFlight(flyTicksLeft, false);
            }
            flyTicksLeft --;
            if(flyTicksLeft == 0)
                disallowFlight();
        }
    }

    @Override
    public void setFlightTicks(int ticks) {
        flyTicksLeft = ticks;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo info){
        EventHandler.onPlayerTick(this);
    }

    @Inject(method = "setGameMode", at = @At("RETURN"))
    private void onSetGameMode(GameMode gameMode, CallbackInfo info){
    	//BeaconFlight.log(Level.DEBUG, "Gamemode: "+gameMode.getName() + " Enabled!");
        EventHandler.onSetGameMode(gameMode, this);
    }

    @Inject(method = "writeCustomDataToTag", at = @At("RETURN"))
    private void onWriteCustomDataToTag(CompoundTag tag, CallbackInfo info){
        tag.putInt("flyTicksLeft", flyTicksLeft);
    }

    @Inject(method = "readCustomDataFromTag", at = @At("RETURN"))
    private void onReadCustomDataFromTag(CompoundTag tag, CallbackInfo info){
        allowFlight(tag.getInt("flyTicksLeft"), false);
    }

    @Override
	@Shadow
    public void sendAbilitiesUpdate(){}

}