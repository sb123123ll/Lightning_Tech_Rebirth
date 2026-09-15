package com.moakiee.ae2lt.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ae2lt")
@PrefixGameTestTemplate(false)
public final class LightningCompatibilityGameTests {
   private static final String NATURAL_TRANSFORM_CHECKED_TAG = "ae2lt.natural_transform_checked";
   private static final String ITEM_TRANSFORM_CHECKED_TAG = "ae2lt.lightning_item_transform_checked";

   private LightningCompatibilityGameTests() {
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty",
      m_177042_ = 20
   )
   public static void processesLightningSubclassThatDoesNotCallSuperTick(GameTestHelper helper) {
      LightningCompatibilityGameTests.NoSuperTickLightningBolt lightning = new LightningCompatibilityGameTests.NoSuperTickLightningBolt(helper.m_177100_());
      BlockPos spawnPos = helper.m_177449_(new BlockPos(0, 2, 0));
      lightning.m_146884_(Vec3.m_82539_(spawnPos));
      helper.m_177100_().m_7967_(lightning);
      helper.m_177306_(2L, () -> {
         CompoundTag data = lightning.getPersistentData();
         helper.m_246336_(data.m_128471_("ae2lt.natural_transform_checked"), "The server pre-tick dispatcher must run structure lightning processing");
         helper.m_246336_(data.m_128471_("ae2lt.lightning_item_transform_checked"), "The server pre-tick dispatcher must run item lightning processing");
         helper.m_177412_();
      });
   }

   private static final class NoSuperTickLightningBolt extends LightningBolt {
      private NoSuperTickLightningBolt(Level level) {
         super(EntityType.f_20465_, level);
      }

      public void m_8119_() {
         this.m_6075_();
         this.m_146870_();
      }
   }
}
