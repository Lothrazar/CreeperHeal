package com.lothrazar.creeperheal.handler;

import java.util.HashMap;
import java.util.Map;
import com.lothrazar.creeperheal.worldhealer.WorldHealerSaveDataSupplier;
import com.lothrazar.library.events.EventFlib;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

public class WorldEventHandler extends EventFlib {

  private Map<ServerLevel, WorldHealerSaveDataSupplier> worldHealers = new HashMap<ServerLevel, WorldHealerSaveDataSupplier>();

  public Map<ServerLevel, WorldHealerSaveDataSupplier> getWorldHealers() {
    return worldHealers;
  }

  @SubscribeEvent
  public void onLoad(LevelEvent.Load event) {
    if (!event.getLevel().isClientSide()
        && event.getLevel() instanceof ServerLevel) {
      worldHealers.put((ServerLevel) event.getLevel(), WorldHealerSaveDataSupplier.loadWorldHealer((ServerLevel) event.getLevel()));
    }
  }

  @SubscribeEvent
  public void onUnload(LevelEvent.Unload event) {
    if (!event.getLevel().isClientSide()) {
      worldHealers.remove(event.getLevel());
    }
  }
}
