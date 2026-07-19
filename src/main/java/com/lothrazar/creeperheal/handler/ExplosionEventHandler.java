package com.lothrazar.creeperheal.handler;

import com.lothrazar.creeperheal.ConfigRegistryCreeperheal;
import com.lothrazar.creeperheal.ForgeCreeperHeal;
import com.lothrazar.creeperheal.worldhealer.WorldHealerSaveDataSupplier;
import com.lothrazar.library.events.EventFlib;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

public class ExplosionEventHandler extends EventFlib {

  @SubscribeEvent
  public void onDetonate(ExplosionEvent.Detonate event) {
    if (event.getLevel().isClientSide()) {
      return;
    }
    Entity exploder = event.getExplosion().getDirectSourceEntity();
    boolean isCreeper = exploder instanceof Creeper;
    if (ConfigRegistryCreeperheal.isOnlyCreepers() == false ||
        (ConfigRegistryCreeperheal.isOnlyCreepers() && isCreeper)) {
      //if only creeper is false, dont need to check
      //only creepers allowed in, so it better be one
      WorldHealerSaveDataSupplier worldHealer = ForgeCreeperHeal.getWorldHealer((ServerLevel) event.getLevel());
      if (worldHealer != null) {
        worldHealer.onDetonate(event);
      }
    }
  }
}
