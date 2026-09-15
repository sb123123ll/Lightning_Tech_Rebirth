package com.moakiee.ae2lt.client;

import appeng.client.render.crafting.AssemblerAnimationStatus;
import com.moakiee.ae2lt.blockentity.PigmeeMolecularAssemblerBlockEntity;
import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import com.moakiee.ae2lt.celestweave.PhaseFlightPlayerState;
import com.moakiee.ae2lt.client.gui.FrequencyScreen;
import com.moakiee.ae2lt.entity.RitualHyperdimensionalPigmeeEntity;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import com.moakiee.ae2lt.menu.hub.DeviceHubMenu;
import com.moakiee.ae2lt.network.CelestweaveSubmoduleActivePacket;
import com.moakiee.ae2lt.network.FlightInertiaSyncPacket;
import com.moakiee.ae2lt.network.PhaseLockProtectionSyncPacket;
import com.moakiee.ae2lt.network.PigmeeAssemblerAnimationPacket;
import com.moakiee.ae2lt.network.RitualItemBurstPacket;
import com.moakiee.ae2lt.network.SyncFrequencyListPacket;
import com.moakiee.ae2lt.network.UpdateFrequencyBasicPacket;
import com.moakiee.ae2lt.network.hub.DeviceHubSyncPacket;
import com.moakiee.ae2lt.network.tianshu.ClosedLoopResultPagePacket;
import com.moakiee.ae2lt.network.tianshu.MaintenanceEditorSyncPacket;
import com.moakiee.ae2lt.network.tianshu.MaintenanceSummarySyncPacket;
import com.moakiee.ae2lt.network.tianshu.UploadTargetsSyncPacket;
import com.moakiee.ae2lt.registry.ModItems;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class ClientNetworkPacketHandlers {
   private ClientNetworkPacketHandlers() {
   }

   public static void handleEasterEgg() {
      EasterEggOverlay.trigger();
   }

   public static void handleFrequencyResponse(Component message) {
      Minecraft minecraft = Minecraft.m_91087_();
      LocalPlayer player = minecraft.f_91074_;
      if (player != null) {
         if (minecraft.f_91080_ instanceof FrequencyScreen fs) {
            fs.showInlineError(message);
         } else {
            player.m_5661_(message, true);
         }
      }
   }

   public static void handleFrequencyList(List<SyncFrequencyListPacket.FrequencyEntry> entries) {
      ClientFrequencyCache.updateFromSync(entries);
   }

   public static void handleFrequencyDetail(int frequencyId, byte syncType, CompoundTag data) {
      if (syncType == 2) {
         ClientFrequencyCache.updateMembers(frequencyId, data);
      } else if (syncType == 10) {
         ClientFrequencyCache.updateConnections(frequencyId, data);
      }
   }

   public static void handleFrequencyBasicUpdate(UpdateFrequencyBasicPacket packet) {
      if (packet.deleted()) {
         ClientFrequencyCache.removeFrequency(packet.frequencyId());
      } else {
         ClientFrequencyCache.upsertFrequency(packet.frequencyId(), packet.name(), packet.color(), packet.ownerUUID(), packet.security());
      }
   }

   public static void handlePigmeeAssemblerAnimation(PigmeeAssemblerAnimationPacket packet) {
      ClientLevel level = Minecraft.m_91087_().f_91073_;
      if (level != null) {
         if (level.m_7702_(packet.pos()) instanceof PigmeeMolecularAssemblerBlockEntity assembler) {
            assembler.setAnimationStatus(new AssemblerAnimationStatus(packet.speed(), packet.output()));
         }
      }
   }

   public static void handleRitualItemBurst(RitualItemBurstPacket packet) {
      Minecraft minecraft = Minecraft.m_91087_();
      if (minecraft.f_91073_ != null) {
         Entity entity = minecraft.f_91073_.m_6815_(packet.entityId());
         if (entity instanceof RitualHyperdimensionalPigmeeEntity) {
            ItemStack activationItem = switch (packet.stage()) {
               case 0 -> new ItemStack((ItemLike)ModItems.PIGMEE_CORE.get());
               case 1 -> new ItemStack((ItemLike)ModItems.CELESTWEAVE_SUBMODULE_UNDYING.get());
               case 2 -> new ItemStack((ItemLike)ModItems.CELESTWEAVE_SUBMODULE_PHASE_LOCK.get());
               default -> ItemStack.f_41583_;
            };
            if (!activationItem.m_41619_()) {
               minecraft.f_91061_.m_107332_(entity, ParticleTypes.f_123767_, 30);
               minecraft.f_91073_.m_7785_(entity.m_20185_(), entity.m_20186_(), entity.m_20189_(), SoundEvents.f_12513_, entity.m_5720_(), 1.0F, 1.0F, false);
               minecraft.f_91063_.m_109113_(activationItem);
            }
         }
      }
   }

   public static void handleCelestweaveSubmoduleActive(CelestweaveSubmoduleActivePacket packet) {
      CelestweaveArmorState.markClientActive(packet.armorId(), packet.submoduleId(), packet.active());
   }

   public static void handleFlightInertia(FlightInertiaSyncPacket packet) {
      CelestweaveArmorState.setClientFlightSettings(packet.armorId(), packet.inertiaEnabled(), packet.phaseMode());
      LocalPlayer player = Minecraft.m_91087_().f_91074_;
      if (player != null) {
         if (!packet.flightControlActive() && !packet.flightLockEnabled()) {
            PhaseFlightPlayerState.endControl(player, packet.flying());
         } else {
            PhaseFlightPlayerState.activate(player);
            PhaseFlightPlayerState.setFlightLocked(player, packet.flightLockEnabled());
            PhaseFlightPlayerState.synchronizeFlying(player, packet.flying());
         }
      }
   }

   public static void handlePhaseLockProtection(PhaseLockProtectionSyncPacket packet) {
      CelestweaveArmorState.setClientPhaseLockProtection(packet.armorId(), packet.blockExternalForces());
   }

   public static void handleDeviceHubSync(DeviceHubSyncPacket packet) {
      LocalPlayer player = Minecraft.m_91087_().f_91074_;
      if (player != null && player.f_36096_ instanceof DeviceHubMenu menu && menu.f_38840_ == packet.containerId()) {
         menu.receiveSync(
            packet.deviceName(),
            packet.hasCore(),
            packet.powered(),
            packet.terrainDestruction(),
            packet.pvp(),
            packet.soundEnabled(),
            packet.chainDamage(),
            packet.executionMode(),
            packet.chargedSplash(),
            packet.moduleNameKeys(),
            packet.moduleCounts(),
            packet.moduleEnabled(),
            packet.selectedModuleIndex(),
            packet.moduleConfigKeys(),
            packet.moduleConfigLabels(),
            packet.moduleConfigValues(),
            packet.moduleConfigEditable()
         );
      }
   }

   public static void handleMaintenanceEditorSync(MaintenanceEditorSyncPacket packet) {
      TianshuPatternEncodingTermMenu menu = getTianshuMenu(packet.containerId());
      if (menu != null) {
         menu.receiveMaintenanceEditorData(packet.selectionRevision(), packet.data());
      }
   }

   public static void handleMaintenanceSummarySync(MaintenanceSummarySyncPacket packet) {
      TianshuPatternEncodingTermMenu menu = getTianshuMenu(packet.containerId());
      if (menu != null) {
         menu.receiveMaintenanceSummary(packet.selectionRevision(), packet.revision(), packet.overflow(), packet.entries());
      }
   }

   public static void handleUploadTargetsSync(UploadTargetsSyncPacket packet) {
      TianshuPatternEncodingTermMenu menu = getTianshuMenu(packet.containerId());
      if (menu != null) {
         menu.receiveUploadTargets(packet.targets());
      }
   }

   public static void handleClosedLoopResultPage(ClosedLoopResultPagePacket packet) {
      TianshuPatternEncodingTermMenu menu = getTianshuMenu(packet.containerId());
      if (menu != null) {
         menu.receiveClosedLoopResultPage(packet.page());
      }
   }

   private static TianshuPatternEncodingTermMenu getTianshuMenu(int containerId) {
      LocalPlayer player = Minecraft.m_91087_().f_91074_;
      return player != null && player.f_36096_ instanceof TianshuPatternEncodingTermMenu menu && menu.f_38840_ == containerId ? menu : null;
   }
}
