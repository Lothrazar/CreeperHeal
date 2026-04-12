package com.lothrazar.creeperheal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.lothrazar.creeperheal.handler.ExplosionEventHandler;
import com.lothrazar.creeperheal.handler.WorldEventHandler;
import com.lothrazar.creeperheal.handler.WorldTickEventHandler;
import com.lothrazar.creeperheal.worldhealer.WorldHealerSaveDataSupplier;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(ForgeCreeperHeal.MODID)
public class ForgeCreeperHeal {

  public static final String MODID = "creeperheal";
  public static final Logger LOGGER = LogManager.getLogger();
  private static WorldEventHandler WEV;

  public ForgeCreeperHeal(IEventBus modEventBus, ModContainer modContainer) {
    modContainer.registerConfig(ModConfig.Type.COMMON, ConfigRegistryCreeperheal.CONFIG);
    modEventBus.addListener(this::setup);
    ForgeCreeperHeal.WEV = new WorldEventHandler();
  }

  private void setup(final FMLCommonSetupEvent event) {
    new WorldTickEventHandler();
    new ExplosionEventHandler();
  }

  public static WorldHealerSaveDataSupplier getWorldHealer(ServerLevel level) {
    return WEV.getWorldHealers().get(level);
  }
}
