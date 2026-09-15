package com.moakiee.ae2lt.gametest;

import com.moakiee.ae2lt.celestweave.ArmorEnergyBuffer;
import com.moakiee.ae2lt.celestweave.CelestweaveArmorDamageHandler;
import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import com.moakiee.ae2lt.celestweave.PhaseFlightMovementGuard;
import com.moakiee.ae2lt.celestweave.service.ArmorCapabilityCollector;
import com.moakiee.ae2lt.celestweave.state.ArmorRuntimeRegistry;
import com.moakiee.ae2lt.registry.ModItems;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.PlayLevelSoundEvent.AtPosition;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ae2lt")
@PrefixGameTestTemplate(false)
public final class CelestweavePhaseSemanticsGameTests {
   private static final String EMPTY_TEMPLATE = "bastion/mobs/empty";
   private static final float EPSILON = 0.001F;

   private CelestweavePhaseSemanticsGameTests() {
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty",
      m_177042_ = 20
   )
   public static void repeatedPhaseLockProjectionEchoesRemainSilent(GameTestHelper helper) {
      Zombie wearer = (Zombie)helper.m_177176_(EntityType.f_20501_, new BlockPos(1, 2, 1));
      List<SoundEvent> projectionSounds = new ArrayList<>();
      Consumer<AtPosition> soundListener = event -> {
         if (event.getLevel() == helper.m_177100_()
            && event.getSound() != null
            && event.getSource() == wearer.m_5720_()
            && !(event.getPosition().m_82557_(wearer.m_20182_()) > 1.0E-6)) {
            SoundEvent sound = (SoundEvent)event.getSound().m_203334_();
            if (sound == SoundEvents.f_271165_ || sound == SoundEvents.f_11675_) {
               projectionSounds.add(sound);
            }
         }
      };
      MinecraftForge.EVENT_BUS.addListener(soundListener);
      helper.m_177306_(2L, () -> equipProjectionEcho(wearer, 1));
      helper.m_177306_(4L, () -> equipProjectionEcho(wearer, 2));
      helper.m_177306_(6L, () -> equipProjectionEcho(wearer, 3));
      helper.m_177306_(8L, () -> {
         MinecraftForge.EVENT_BUS.unregister(soundListener);
         long genericEquipSounds = projectionSounds.stream().filter(sound -> sound == SoundEvents.f_11675_).count();
         long emptySounds = projectionSounds.stream().filter(sound -> sound == SoundEvents.f_271165_).count();
         helper.m_246336_(genericEquipSounds == 0L, "Repeated projection echoes emitted " + genericEquipSounds + " audible equip sounds");
         helper.m_246336_(emptySounds == 3L, "Expected three silent projection equip transitions, observed " + emptySounds);
         helper.m_177412_();
      });
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty"
   )
   public static void completeShieldCancelsBeforeVanillaDamageProcessing(GameTestHelper helper) {
      ServerPlayer player = newTestPlayer(helper, "shield-target");
      ItemStack chest = equipChest(helper, player, (Item)ModItems.CELESTWEAVE_SUBMODULE_MULTIDIMENSIONAL_PROTECTION.get());

      try {
         player.m_21153_(player.m_21233_());
         player.m_7911_(4.0F);
         float healthBefore = player.m_21223_();
         float absorptionBefore = player.m_6103_();
         boolean accepted = player.m_6469_(helper.m_177100_().m_269111_().m_287172_(), 12.0F);
         helper.m_277053_(accepted, "A completely shielded hit must return false from LivingEntity.hurt");
         assertClose(helper, healthBefore, player.m_21223_(), "A completely shielded hit changed player health");
         assertClose(helper, absorptionBefore, player.m_6103_(), "A completely shielded hit consumed absorption");
         helper.m_246336_(player.f_19802_ == 0, "A completely shielded hit reached vanilla invulnerability processing");
         helper.m_177412_();
      } finally {
         cleanupArmorState(player, chest);
      }
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty"
   )
   public static void reflectUsesOriginalHurtAmountAcrossInvulnerabilityFrames(GameTestHelper helper) {
      CelestweavePhaseSemanticsGameTests.FinalDamageDispatchServerPlayer player = new CelestweavePhaseSemanticsGameTests.FinalDamageDispatchServerPlayer(
         helper.m_177100_()
      );
      ItemStack chest = equipChest(helper, player, (Item)ModItems.CELESTWEAVE_SUBMODULE_REFLECT.get());
      ArmorEnergyBuffer.write(chest, helper.m_177100_().m_9598_(), 100000L);

      try {
         CelestweavePhaseSemanticsGameTests.RecordingServerPlayer attacker = new CelestweavePhaseSemanticsGameTests.RecordingServerPlayer(helper.m_177100_());
         DamageSource source = new DamageSource(helper.m_177100_().m_9598_().m_175515_(Registries.f_268580_).m_246971_(DamageTypes.f_286979_), attacker);
         hurtUnconnectedPlayer(player, source, 4.0F);
         hurtUnconnectedPlayer(player, source, 10.0F);
         assertClose(helper, 4.2F, attacker.reflectedDamage, "The second reflection used the cooldown delta instead of the original 10 damage");
         Pig fallbackAttacker = (Pig)helper.m_177176_(EntityType.f_20510_, new BlockPos(2, 2, 1));
         fallbackAttacker.m_21153_(fallbackAttacker.m_21233_());
         CelestweaveArmorDamageHandler.onPre(new LivingDamageEvent(player, helper.m_177100_().m_269111_().m_269333_(fallbackAttacker), 2.0F));
         assertClose(helper, fallbackAttacker.m_21233_() - 0.6F, fallbackAttacker.m_21223_(), "The completed hurt call left stale original-damage scope behind");
         helper.m_177412_();
      } finally {
         cleanupArmorState(player, chest);
      }
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty"
   )
   public static void originalDamageScopeIsClearedWhenActuallyHurtThrows(GameTestHelper helper) {
      CelestweavePhaseSemanticsGameTests.ThrowingServerPlayer player = new CelestweavePhaseSemanticsGameTests.ThrowingServerPlayer(helper.m_177100_());
      ItemStack chest = equipChest(helper, player, (Item)ModItems.CELESTWEAVE_SUBMODULE_REFLECT.get());
      ArmorEnergyBuffer.write(chest, helper.m_177100_().m_9598_(), 100000L);

      try {
         boolean threw = false;

         try {
            player.m_6469_(helper.m_177100_().m_269111_().m_287172_(), 11.0F);
         } catch (CelestweavePhaseSemanticsGameTests.ExpectedDamageException var8) {
            threw = true;
         }

         helper.m_246336_(threw, "The throwing player did not exercise the actuallyHurt wrapper");
         Pig attacker = (Pig)helper.m_177176_(EntityType.f_20510_, new BlockPos(1, 2, 1));
         attacker.m_21153_(attacker.m_21233_());
         CelestweaveArmorDamageHandler.onPre(new LivingDamageEvent(player, helper.m_177100_().m_269111_().m_269333_(attacker), 2.0F));
         assertClose(helper, attacker.m_21233_() - 0.6F, attacker.m_21223_(), "Exceptional damage processing leaked the original 11 damage into the next event");
         helper.m_177412_();
      } finally {
         cleanupArmorState(player, chest);
      }
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty"
   )
   public static void payloadHandlerAuthorizationIsSenderBoundAndFinallySafe(GameTestHelper helper) {
      ServerPlayer sender = new ServerPlayer(helper.m_177100_().m_7654_(), helper.m_177100_(), new GameProfile(UUID.randomUUID(), "payload-sender"));
      ServerPlayer other = new ServerPlayer(helper.m_177100_().m_7654_(), helper.m_177100_(), new GameProfile(UUID.randomUUID(), "payload-other"));
      helper.m_277053_(PhaseFlightMovementGuard.isSelfTeleportAuthorized(sender), "The sender started with stale payload authorization");
      PhaseFlightMovementGuard.runAsPlayerPayloadHandler(
         sender,
         () -> {
            helper.m_246336_(PhaseFlightMovementGuard.isSelfTeleportAuthorized(sender), "The sending player was not authorized inside its payload handler");
            helper.m_277053_(PhaseFlightMovementGuard.isSelfTeleportAuthorized(other), "One player's payload authorized a different player");
            PhaseFlightMovementGuard.runAsPlayerPayloadHandler(
               sender, () -> helper.m_246336_(PhaseFlightMovementGuard.isSelfTeleportAuthorized(sender), "Nested payload authorization lost the outer sender")
            );
            helper.m_246336_(PhaseFlightMovementGuard.isSelfTeleportAuthorized(sender), "Closing a nested payload scope cleared the outer scope");
         }
      );
      helper.m_277053_(PhaseFlightMovementGuard.isSelfTeleportAuthorized(sender), "A completed payload handler leaked teleport authorization");
      boolean threw = false;

      try {
         PhaseFlightMovementGuard.runAsPlayerPayloadHandler(sender, () -> {
            throw new CelestweavePhaseSemanticsGameTests.ExpectedPayloadException();
         });
      } catch (CelestweavePhaseSemanticsGameTests.ExpectedPayloadException var8) {
         threw = true;
      } finally {
         PhaseFlightMovementGuard.clear(sender);
         PhaseFlightMovementGuard.clear(other);
      }

      helper.m_246336_(threw, "The exceptional payload path was not exercised");
      helper.m_277053_(PhaseFlightMovementGuard.isSelfTeleportAuthorized(sender), "An exceptional payload handler leaked teleport authorization");
      helper.m_177412_();
   }

   private static ItemStack equipChest(GameTestHelper helper, ServerPlayer player, Item... modules) {
      RegistryAccess registries = helper.m_177100_().m_9598_();
      ItemStack chest = new ItemStack((ItemLike)ModItems.CELESTWEAVE_CORE.get());
      CelestweaveArmorState.setSlot(chest, registries, 0, new ItemStack((ItemLike)ModItems.ULTIMATE_OVERLOAD_CORE.get()));
      UUID armorId = CelestweaveArmorState.ensureArmorId(chest);

      for (Item module : modules) {
         ItemStack moduleStack = new ItemStack(module);
         helper.m_246336_(CelestweaveArmorState.installOneModule(chest, registries, moduleStack), "Failed to install test armor module: " + module);
         String moduleId = CelestweaveArmorState.moduleTypeId(moduleStack);
         ArmorRuntimeRegistry.setSubmoduleRuntimeActive(armorId, moduleId, true);
      }

      player.m_8061_(EquipmentSlot.CHEST, chest);
      ArmorCapabilityCollector.clearCache(player);
      return chest;
   }

   private static void equipProjectionEcho(LivingEntity wearer, int revision) {
      ItemStack projection = new ItemStack((ItemLike)ModItems.PHASE_LOCK_PROJECTION.get());
      projection.m_41784_().m_128405_("ae2lt_gametest_echo_revision", revision);
      wearer.m_8061_(EquipmentSlot.CHEST, projection);
   }

   private static void cleanupArmorState(ServerPlayer player, ItemStack chest) {
      ArmorCapabilityCollector.clearCache(player);
      CelestweaveArmorState.clearTransientRuntimeAndCaches(chest);
      PhaseFlightMovementGuard.clear(player);
   }

   private static void assertClose(GameTestHelper helper, float expected, float actual, String message) {
      helper.m_246336_(Math.abs(expected - actual) <= 0.001F, message + ": expected " + expected + ", got " + actual);
   }

   private static void hurtUnconnectedPlayer(CelestweavePhaseSemanticsGameTests.FinalDamageDispatchServerPlayer player, DamageSource source, float amount) {
      int dispatchesBefore = player.finalDamageDispatches;

      try {
         player.m_6469_(source, amount);
      } catch (NullPointerException var5) {
         if (player.f_8906_ != null || player.finalDamageDispatches != dispatchesBefore + 1) {
            throw var5;
         }
      }

      if (player.finalDamageDispatches != dispatchesBefore + 1) {
         throw new AssertionError("hurt did not reach the final-damage dispatch");
      }
   }

   private static ServerPlayer newTestPlayer(GameTestHelper helper, String name) {
      ServerPlayer player = new ServerPlayer(helper.m_177100_().m_7654_(), helper.m_177100_(), new GameProfile(UUID.randomUUID(), name));
      player.m_143403_(GameType.SURVIVAL);
      player.m_150110_().f_35934_ = false;
      return player;
   }

   private static final class ExpectedDamageException extends RuntimeException {
   }

   private static final class ExpectedPayloadException extends RuntimeException {
   }

   private static final class FinalDamageDispatchServerPlayer extends ServerPlayer {
      private int finalDamageDispatches;

      private FinalDamageDispatchServerPlayer(ServerLevel level) {
         super(level.m_7654_(), level, new GameProfile(UUID.randomUUID(), "final-damage-dispatcher"));
      }

      protected void m_6475_(DamageSource source, float amount) {
         CelestweaveArmorDamageHandler.onPre(new LivingDamageEvent(this, source, amount));
         this.finalDamageDispatches++;
      }

      public boolean m_7099_(Player other) {
         return true;
      }
   }

   private static final class RecordingServerPlayer extends ServerPlayer {
      private float reflectedDamage;

      private RecordingServerPlayer(ServerLevel level) {
         super(level.m_7654_(), level, new GameProfile(UUID.randomUUID(), "reflection-recorder"));
      }

      public boolean m_6469_(DamageSource source, float amount) {
         this.reflectedDamage += amount;
         return true;
      }
   }

   private static final class ThrowingServerPlayer extends ServerPlayer {
      private ThrowingServerPlayer(ServerLevel level) {
         super(level.m_7654_(), level, new GameProfile(UUID.randomUUID(), "damage-thrower"));
      }

      protected void m_6475_(DamageSource source, float amount) {
         throw new CelestweavePhaseSemanticsGameTests.ExpectedDamageException();
      }
   }
}
