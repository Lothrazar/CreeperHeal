package com.lothrazar.creeperheal.handler;

import com.lothrazar.creeperheal.ForgeCreeperHeal;
import com.lothrazar.creeperheal.worldhealer.WorldHealerSaveDataSupplier;
import com.lothrazar.library.events.EventFlib;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public class WorldTickEventHandler extends EventFlib {

  @SubscribeEvent
  public void onWorldTick(LevelTickEvent.Post event) {
    if (!event.getLevel().isClientSide()) {
      WorldHealerSaveDataSupplier worldHealer = ForgeCreeperHeal.getWorldHealer((ServerLevel) event.getLevel());
      if (worldHealer != null) {
        worldHealer.onTick();
      }
    }
  }
}
