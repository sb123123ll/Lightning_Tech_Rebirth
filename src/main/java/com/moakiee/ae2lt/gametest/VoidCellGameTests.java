package com.moakiee.ae2lt.gametest;

import appeng.api.config.Actionable;
import appeng.api.config.CondenserOutput;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.core.definitions.AEItems;
import com.moakiee.ae2lt.item.VoidStorageCellItem;
import com.moakiee.ae2lt.me.cell.VoidCellData;
import com.moakiee.ae2lt.me.cell.VoidCellInventory;
import com.moakiee.ae2lt.me.cell.VoidCellMode;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ae2lt")
@PrefixGameTestTemplate(false)
public final class VoidCellGameTests {
   private VoidCellGameTests() {
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty"
   )
   public static void acceptsEveryConfiguredAeKeyType(GameTestHelper helper) {
      assertAccepted(helper, AEItemKey.of(Items.f_41905_), 1L);
      assertAccepted(helper, AEFluidKey.of(Fluids.f_76193_), 1000L);
      assertAccepted(helper, LightningKey.HIGH_VOLTAGE, 1L);
      ItemStack unpartitionedStack = new ItemStack((ItemLike)ModItems.VOID_CELL.get());
      VoidCellInventory unpartitioned = new VoidCellInventory(unpartitionedStack, null);
      helper.m_246336_(
         unpartitioned.insert(AEItemKey.of(Items.f_41905_), 1L, Actionable.SIMULATE, IActionSource.empty()) == 0L,
         "An unpartitioned void cell must reject input like the 1.21 implementation"
      );
      helper.m_177412_();
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty"
   )
   public static void condensesInputIntoExtractableMatterBalls(GameTestHelper helper) {
      int requiredPower = CondenserOutput.MATTER_BALLS.requiredPower;
      helper.m_246336_(requiredPower > 0, "AE2 Matter Ball condenser power must be configured");
      ItemStack stack = configuredCell(AEItemKey.of(Items.f_41905_));
      VoidCellData.writeMode(stack, VoidCellMode.MATTER_BALLS);
      VoidCellInventory cell = new VoidCellInventory(stack, null);
      long inserted = cell.insert(AEItemKey.of(Items.f_41905_), (long)requiredPower, Actionable.MODULATE, IActionSource.empty());
      AEItemKey matterBall = AEItemKey.of(AEItems.MATTER_BALL);
      helper.m_246336_(inserted == (long)requiredPower, "The complete input amount must be accepted");
      helper.m_246336_(cell.getAvailableStacks().get(matterBall) == 1L, "One condenser threshold of input must produce one Matter Ball");
      helper.m_246336_(
         cell.extract(matterBall, 1L, Actionable.SIMULATE, IActionSource.empty()) == 1L, "Produced Matter Balls must remain extractable from the cell"
      );
      helper.m_177412_();
   }

   private static void assertAccepted(GameTestHelper helper, AEKey key, long amount) {
      VoidCellInventory cell = new VoidCellInventory(configuredCell(key), null);
      long accepted = cell.insert(key, amount, Actionable.MODULATE, IActionSource.empty());
      helper.m_246336_(accepted == amount, "Configured AE key type was rejected: " + key.getType().getId());
   }

   private static ItemStack configuredCell(AEKey key) {
      ItemStack stack = new ItemStack((ItemLike)ModItems.VOID_CELL.get());
      VoidStorageCellItem item = (VoidStorageCellItem)stack.m_41720_();
      item.getConfigInventory(stack).addFilter(key);
      return stack;
   }
}
