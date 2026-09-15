package com.moakiee.ae2lt.block;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.storage.loot.LootParams.Builder;

public final class SiliconBlock extends Block {
   public SiliconBlock(Properties properties) {
      super(properties);
   }

   public List<ItemStack> m_49635_(BlockState state, Builder builder) {
      return List.of(new ItemStack(this));
   }
}
