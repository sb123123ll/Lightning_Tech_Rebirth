package com.moakiee.ae2lt.network.hub;

import com.moakiee.ae2lt.client.ClientNetworkPacketHandlers;
import com.moakiee.ae2lt.item.railgun.RailgunExecutionMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record DeviceHubSyncPacket(
   int containerId,
   String deviceName,
   boolean hasCore,
   boolean powered,
   boolean terrainDestruction,
   boolean pvp,
   boolean soundEnabled,
   boolean chainDamage,
   RailgunExecutionMode executionMode,
   boolean chargedSplash,
   List<String> moduleNameKeys,
   List<Integer> moduleCounts,
   List<Boolean> moduleEnabled,
   int selectedModuleIndex,
   List<String> moduleConfigKeys,
   List<String> moduleConfigLabels,
   List<String> moduleConfigValues,
   List<Boolean> moduleConfigEditable
) {
   public static DeviceHubSyncPacket decode(FriendlyByteBuf buf) {
      int containerId = buf.m_130242_();
      String deviceName = buf.m_130136_(256);
      boolean hasCore = buf.readBoolean();
      boolean powered = buf.readBoolean();
      boolean terrainDestruction = buf.readBoolean();
      boolean pvp = buf.readBoolean();
      boolean soundEnabled = buf.readBoolean();
      boolean chainDamage = buf.readBoolean();
      RailgunExecutionMode executionMode = (RailgunExecutionMode)buf.m_130066_(RailgunExecutionMode.class);
      boolean chargedSplash = buf.readBoolean();
      int count = buf.m_130242_();
      List<String> nameKeys = new ArrayList<>(count);
      List<Integer> counts = new ArrayList<>(count);
      List<Boolean> enabled = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         nameKeys.add(buf.m_130136_(256));
         counts.add(buf.m_130242_());
         enabled.add(buf.readBoolean());
      }

      int selectedModuleIndex = buf.m_130242_();
      int configCount = buf.m_130242_();
      List<String> moduleConfigKeys = new ArrayList<>(configCount);
      List<String> moduleConfigLabels = new ArrayList<>(configCount);
      List<String> moduleConfigValues = new ArrayList<>(configCount);
      List<Boolean> moduleConfigEditable = new ArrayList<>(configCount);

      for (int i = 0; i < configCount; i++) {
         moduleConfigKeys.add(buf.m_130136_(128));
         moduleConfigLabels.add(buf.m_130136_(256));
         moduleConfigValues.add(buf.m_130136_(256));
         moduleConfigEditable.add(buf.readBoolean());
      }

      return new DeviceHubSyncPacket(
         containerId,
         deviceName,
         hasCore,
         powered,
         terrainDestruction,
         pvp,
         soundEnabled,
         chainDamage,
         executionMode,
         chargedSplash,
         nameKeys,
         counts,
         enabled,
         selectedModuleIndex,
         moduleConfigKeys,
         moduleConfigLabels,
         moduleConfigValues,
         moduleConfigEditable
      );
   }

   public void write(FriendlyByteBuf buf) {
      buf.m_130130_(this.containerId);
      buf.m_130072_(this.deviceName, 256);
      buf.writeBoolean(this.hasCore);
      buf.writeBoolean(this.powered);
      buf.writeBoolean(this.terrainDestruction);
      buf.writeBoolean(this.pvp);
      buf.writeBoolean(this.soundEnabled);
      buf.writeBoolean(this.chainDamage);
      buf.m_130068_(this.executionMode);
      buf.writeBoolean(this.chargedSplash);
      int count = Math.min(Math.min(this.moduleNameKeys.size(), this.moduleCounts.size()), this.moduleEnabled.size());
      buf.m_130130_(count);

      for (int i = 0; i < count; i++) {
         buf.m_130072_(this.moduleNameKeys.get(i), 256);
         buf.m_130130_(this.moduleCounts.get(i));
         buf.writeBoolean(this.moduleEnabled.get(i));
      }

      buf.m_130130_(this.selectedModuleIndex);
      int configCount = Math.min(
         Math.min(Math.min(this.moduleConfigKeys.size(), this.moduleConfigLabels.size()), this.moduleConfigValues.size()), this.moduleConfigEditable.size()
      );
      buf.m_130130_(configCount);

      for (int i = 0; i < configCount; i++) {
         buf.m_130072_(this.moduleConfigKeys.get(i), 128);
         buf.m_130072_(this.moduleConfigLabels.get(i), 256);
         buf.m_130072_(this.moduleConfigValues.get(i), 256);
         buf.writeBoolean(this.moduleConfigEditable.get(i));
      }
   }

   public static void handle(DeviceHubSyncPacket pkt, Supplier<Context> ctxSup) {
      Context ctx = ctxSup.get();
      ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNetworkPacketHandlers.handleDeviceHubSync(pkt)));
      ctx.setPacketHandled(true);
   }
}
