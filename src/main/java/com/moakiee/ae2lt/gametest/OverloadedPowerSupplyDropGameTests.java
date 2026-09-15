package com.moakiee.ae2lt.gametest;

import com.moakiee.ae2lt.block.OverloadedPowerSupplyBlock;
import com.moakiee.ae2lt.blockentity.OverloadedPowerSupplyBlockEntity;
import com.moakiee.ae2lt.registry.ModBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("ae2lt")
@PrefixGameTestTemplate(false)
public final class OverloadedPowerSupplyDropGameTests {
   private OverloadedPowerSupplyDropGameTests() {
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty"
   )
   public static void dropsBlockAndInstalledCell(GameTestHelper helper) {
      if (!ModBlocks.hasOverloadedPowerSupply()) {
         ResourceLocation blockId = new ResourceLocation("ae2lt", "overloaded_power_supply");
         helper.m_277053_(ForgeRegistries.BLOCKS.containsKey(blockId), "The AppFlux-only power supply must not be registered without AppFlux");
         helper.m_177412_();
      } else {
         BlockPos relativePos = BlockPos.f_121853_;
         OverloadedPowerSupplyBlock block = (OverloadedPowerSupplyBlock)ModBlocks.OVERLOADED_POWER_SUPPLY.get();
         helper.m_177245_(relativePos, block);
         BlockEntity blockEntity = helper.m_177347_(relativePos);
         helper.m_246336_(blockEntity instanceof OverloadedPowerSupplyBlockEntity, "The placed power supply must create its block entity");
         Item fluxCell = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("appflux", "fe_1k_cell"));
         helper.m_246336_(fluxCell != null && fluxCell != Items.f_41852_, "The AppFlux test profile must provide the 1k FE cell");
         ItemStack cellStack = new ItemStack(fluxCell);
         OverloadedPowerSupplyBlockEntity powerSupply = (OverloadedPowerSupplyBlockEntity)blockEntity;
         helper.m_246336_(powerSupply.getCellInventory().isItemValid(0, cellStack), "The real AppFlux FE cell must be accepted by the power supply");
         powerSupply.getCellInventory().setItemDirect(0, cellStack);
         BlockPos absolutePos = helper.m_177449_(relativePos);
         helper.m_246336_(helper.m_177100_().m_46961_(absolutePos, true), "The placed power supply must be destroyable");
         List<ItemEntity> itemEntities = helper.m_177100_().m_45976_(ItemEntity.class, new AABB(absolutePos).m_82400_(2.0));
         int blockItemCount = itemEntities.stream()
            .<ItemStack>map(ItemEntity::m_32055_)
            .filter(stack -> stack.m_150930_(block.m_5456_()))
            .mapToInt(ItemStack::m_41613_)
            .sum();
         int cellItemCount = itemEntities.stream()
            .<ItemStack>map(ItemEntity::m_32055_)
            .filter(stack -> stack.m_150930_(fluxCell))
            .mapToInt(ItemStack::m_41613_)
            .sum();
         helper.m_246336_(blockItemCount == 1, "Expected exactly one overloaded power supply from its actual loot table, got " + itemEntities);
         helper.m_246336_(cellItemCount == 1, "Expected exactly one installed FE cell from AE2's additional-drop hook, got " + itemEntities);
         helper.m_177412_();
      }
   }
}
