package com.moakiee.ae2lt.logic.railgun;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ae2lt")
@PrefixGameTestTemplate(false)
public final class OverloadExecutionGameTests {
   private OverloadExecutionGameTests() {
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty"
   )
   public static void canceledDeathCallbackFallsBackToNormalSettlement(GameTestHelper helper) {
      OverloadExecutionGameTests.CancelingZombie target = new OverloadExecutionGameTests.CancelingZombie(helper.m_177100_());
      OverloadExecutionService.completeNormalDeath(target, helper.m_177100_().m_269111_().m_287172_(), target.m_21233_());
      helper.m_246336_(target.f_20890_, "Fallback did not commit the LivingEntity death state");
      helper.m_246336_(target.m_20089_() == Pose.DYING, "Fallback did not enter the dying pose");
      helper.m_177412_();
   }

   private static final class CancelingZombie extends Zombie {
      private CancelingZombie(Level level) {
         super(level);
      }

      public void m_6667_(DamageSource source) {
      }

      public void m_6668_(DamageSource source) {
      }
   }
}
