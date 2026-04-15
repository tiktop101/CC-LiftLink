package com.tom.createelevatorcc;

import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CreateElevatorCCMod.MOD_ID)
public class CreateElevatorCCMod {
    public static final String MOD_ID = "createelevatorcc";

    public CreateElevatorCCMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> ComputerCraftAPI.registerGenericSource(new ElevatorMethods()));
    }
}
