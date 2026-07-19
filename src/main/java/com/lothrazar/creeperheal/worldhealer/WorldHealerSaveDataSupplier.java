package com.lothrazar.creeperheal.worldhealer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.function.Supplier;
import com.lothrazar.creeperheal.ConfigRegistryCreeperheal;
import com.lothrazar.creeperheal.ForgeCreeperHeal;
import com.lothrazar.library.data.BlockStatePosWrapper;
import com.lothrazar.library.data.TickContainer;
import com.lothrazar.library.data.TickingHealList;
import com.lothrazar.library.util.LevelWorldUtil;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.event.level.ExplosionEvent;

public class WorldHealerSaveDataSupplier extends SavedData implements Supplier<Object> {

  static final String NBT_HEALTASK_LIST = "healtasklist";
  static final String NBT_BLOCKDATA_LIST = "blockdatalist";
  static final String NBT_TICKS = "ticks";

  static final Codec<WorldHealerSaveDataSupplier> CODEC = CompoundTag.CODEC.xmap(tag -> {
    WorldHealerSaveDataSupplier w = new WorldHealerSaveDataSupplier();
    w.deserializeNBT(tag);
    return w;
  }, w -> w.save(new CompoundTag()));
  static final SavedDataType<WorldHealerSaveDataSupplier> TYPE = new SavedDataType<>(
      Identifier.fromNamespaceAndPath(ForgeCreeperHeal.MODID, "worldhealer"), WorldHealerSaveDataSupplier::new, CODEC);

  private Level level;
  private TickingHealList healTask;

  public WorldHealerSaveDataSupplier() {
    healTask = new TickingHealList();
  }

  public void onTick() {
    Collection<BlockStatePosWrapper> blocksToHeal = healTask.tick();
    if (blocksToHeal != null) {
      for (BlockStatePosWrapper blockData : blocksToHeal) {
        heal(blockData);
      }
    }
  }

  public void onDetonate(ExplosionEvent.Detonate event) {
    Level world = event.getLevel();
    int maxTicksBeforeHeal = 0;
    //Process primary blocks
    for (BlockPos blockPosExplosion : event.getAffectedBlocks()) {
      BlockState blockStateExplosion = world.getBlockState(blockPosExplosion);
      if (!isValid(blockStateExplosion)) {
        continue;
      }
      if (!blockStateExplosion.isAir()) {
        int ticksBeforeHeal = ConfigRegistryCreeperheal.getMinimumTicksBeforeHeal() + world.getRandom().nextInt(ConfigRegistryCreeperheal.getRandomTickVar());
        if (ticksBeforeHeal > maxTicksBeforeHeal) {
          maxTicksBeforeHeal = ticksBeforeHeal;
        }
        onBlockHealed(blockPosExplosion, blockStateExplosion, ticksBeforeHeal);
      }
    }
    maxTicksBeforeHeal++;
    //Process secondary blocks. ex: Leaves must come AFTER dirt
    for (BlockPos blockPosExplosion : event.getAffectedBlocks()) {
      BlockState blockStateExplosion = world.getBlockState(blockPosExplosion);
      if (!isValid(blockStateExplosion)) {
        continue;
      }
      if (!blockStateExplosion.isAir()) {
        onBlockHealed(blockPosExplosion, blockStateExplosion, maxTicksBeforeHeal + world.getRandom().nextInt(ConfigRegistryCreeperheal.getRandomTickVar()));
      }
    }
  }

  private boolean isValid(BlockState state) {
    if (state.is(BlockTags.DOORS) || state.is(BlockTags.BEDS) || state.getBlock() instanceof TallFlowerBlock) {
      return false;
    }
    return true;
  }

  private void onBlockHealed(BlockPos blockPosExplosion, BlockState blockStateExplosion, int ticks) {
    healTask.add(ticks, new BlockStatePosWrapper(level, blockPosExplosion, blockStateExplosion));
    level.removeBlockEntity(blockPosExplosion);
    level.setBlock(blockPosExplosion, Blocks.AIR.defaultBlockState(), 7);
  }

  private void heal(BlockStatePosWrapper blockData) {
    boolean isAir = this.level.isEmptyBlock(blockData.getBlockPos());
    if (ConfigRegistryCreeperheal.isOverride() || isAir) {
      if (ConfigRegistryCreeperheal.isDropIfAlreadyBlock() && !isAir) {
        if (level.getBlockState(blockData.getBlockPos()).getBlock() != null) {
          dropBlockData(blockData);
        }
      }
      level.setBlock(blockData.getBlockPos(), blockData.getBlockState(), 7);
      if (blockData.getTileEntityTag() != null) {
        BlockEntity te = level.getBlockEntity(blockData.getBlockPos());
        if (te != null) {
          te.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), blockData.getTileEntityTag()));
          level.setBlockEntity(te);
        }
      }
    }
    else if (ConfigRegistryCreeperheal.isDropIfAlreadyBlock() && blockData.getBlockState().getBlock() != null) {
      dropBlockData(blockData);
    }
  }

  private void dropBlockData(BlockStatePosWrapper blockData) {
    Block block = blockData.getBlockState().getBlock();
    LevelWorldUtil.dropItemStackRandomMotion(level, blockData.getBlockPos(), new ItemStack(block), 0.05F);
    if (blockData.getTileEntityTag() != null && block instanceof EntityBlock) {
      BlockEntity te = ((EntityBlock) block).newBlockEntity(blockData.getBlockPos(), blockData.getBlockState());
      if (te instanceof Container ct) {
        te.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), blockData.getTileEntityTag()));
        Containers.dropContents(level, blockData.getBlockPos(), ct);
      }
    }
  }

  public CompoundTag save(CompoundTag tag) {
    ListTag tagList = new ListTag();
    for (TickContainer<Collection<BlockStatePosWrapper>> tc : this.healTask.getLinkedList()) {
      CompoundTag tickContainerTag = new CompoundTag();
      tickContainerTag.putInt(NBT_TICKS, tc.getTick());
      ListTag blockDataListTag = new ListTag();
      for (BlockStatePosWrapper blockData : tc.getData()) {
        CompoundTag blockDataTag = new CompoundTag();
        blockData.writeToNBT(blockDataTag);
        blockDataListTag.add(blockDataTag);
      }
      tickContainerTag.put(NBT_BLOCKDATA_LIST, blockDataListTag);
      tagList.add(tickContainerTag);
    }
    tag.put(NBT_HEALTASK_LIST, tagList);
    return tag;
  }

  public void deserializeNBT(CompoundTag tag) {
    ListTag tagList = tag.getListOrEmpty(NBT_HEALTASK_LIST);
    for (ListIterator<Tag> iter = tagList.listIterator(); iter.hasNext();) {
      CompoundTag tickContainerTag = (CompoundTag) iter.next();
      int ticksLeft = tickContainerTag.getIntOr(NBT_TICKS, 0);
      LinkedList<BlockStatePosWrapper> blockDataList = new LinkedList<BlockStatePosWrapper>();
      ListTag blockDataListTag = tickContainerTag.getListOrEmpty(NBT_BLOCKDATA_LIST);
      for (ListIterator<Tag> iter0 = blockDataListTag.listIterator(); iter0.hasNext();) {
        CompoundTag blockDataTag = (CompoundTag) iter0.next();
        BlockStatePosWrapper blockData = new BlockStatePosWrapper();
        blockData.readFromNBT(blockDataTag, this.level);
        blockDataList.add(blockData);
      }
      healTask.getLinkedList().addLast(new TickContainer<Collection<BlockStatePosWrapper>>(ticksLeft, blockDataList));
    }
  }

  public static WorldHealerSaveDataSupplier loadWorldHealer(ServerLevel serverLevelIn) {
    SavedDataStorage storage = serverLevelIn.getDataStorage();
    WorldHealerSaveDataSupplier result = storage.computeIfAbsent(TYPE);
    result.level = serverLevelIn;
    return result;
  }

  @Override
  public boolean isDirty() {
    return true;
  }

  @Override
  public Object get() {
    return this;
  }
}
