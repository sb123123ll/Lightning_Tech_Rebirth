package com.moakiee.ae2lt.network.tianshu;

import appeng.api.stacks.AEKey;
import com.moakiee.ae2lt.client.ClientNetworkPacketHandlers;
import com.moakiee.ae2lt.logic.tianshu.maintenance.InventoryMaintenanceStatus;
import com.moakiee.ae2lt.logic.tianshu.maintenance.ReservedStockMatchMode;
import com.moakiee.ae2lt.logic.tianshu.terminal.MaintenanceEditorData;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record MaintenanceEditorSyncPacket(int containerId, int selectionRevision, MaintenanceEditorData data) {
   public MaintenanceEditorSyncPacket(int containerId, int selectionRevision, MaintenanceEditorData data) {
      TianshuPacketLimits.requireListSize("maintenance topology", data.topology().size());
      TianshuPacketLimits.requireListSize("maintenance variants", data.variants().size());
      this.containerId = containerId;
      this.selectionRevision = selectionRevision;
      this.data = data;
   }

   public void write(FriendlyByteBuf buf) {
      buf.m_130130_(this.containerId);
      buf.m_130130_(this.selectionRevision);
      AEKey.writeKey(buf, this.data.target());
      buf.writeBoolean(this.data.ruleId() != null);
      if (this.data.ruleId() != null) {
         buf.m_130077_(this.data.ruleId());
      }

      buf.m_130103_(this.data.lowerThreshold());
      buf.m_130103_(this.data.upperThreshold());
      buf.m_130103_(this.data.amountPerJob());
      buf.writeBoolean(this.data.enabled());
      buf.m_130068_(this.data.status());
      buf.m_130103_(this.data.currentStock());
      buf.writeBoolean(this.data.craftable());
      buf.writeBoolean(this.data.recoveryPage());
      buf.m_130130_(this.data.topology().size());

      for (MaintenanceEditorData.TopologyEntry entry : this.data.topology()) {
         AEKey.writeKey(buf, entry.key());
         buf.m_130130_(entry.depth());
         buf.writeBoolean(entry.craftable());
         buf.m_130103_(entry.storedAmount());
         buf.writeLong(entry.globalReserve());
         buf.m_130068_(entry.globalMode());
         buf.writeLong(entry.ruleReserve());
         buf.m_130068_(entry.ruleMode());
      }

      buf.m_130130_(this.data.variants().size());

      for (MaintenanceEditorData.VariantEntry entry : this.data.variants()) {
         AEKey.writeKey(buf, entry.key());
         buf.m_130103_(entry.storedAmount());
         buf.writeBoolean(entry.craftable());
      }
   }

   public static MaintenanceEditorSyncPacket decode(FriendlyByteBuf buf) {
      int container = buf.m_130242_();
      int selectionRevision = buf.m_130242_();
      AEKey target = AEKey.readKey(buf);
      UUID ruleId = buf.readBoolean() ? buf.m_130259_() : null;
      long lower = buf.m_130258_();
      long upper = buf.m_130258_();
      long batch = buf.m_130258_();
      boolean enabled = buf.readBoolean();
      InventoryMaintenanceStatus status = (InventoryMaintenanceStatus)buf.m_130066_(InventoryMaintenanceStatus.class);
      long currentStock = buf.m_130258_();
      boolean craftable = buf.readBoolean();
      boolean recoveryPage = buf.readBoolean();
      int topologySize = TianshuPacketLimits.requireDecodedListSize("maintenance topology", buf.m_130242_());
      ArrayList<MaintenanceEditorData.TopologyEntry> topology = new ArrayList<>(topologySize);

      for (int i = 0; i < topologySize; i++) {
         topology.add(
            new MaintenanceEditorData.TopologyEntry(
               AEKey.readKey(buf),
               buf.m_130242_(),
               buf.readBoolean(),
               buf.m_130258_(),
               buf.readLong(),
               (ReservedStockMatchMode)buf.m_130066_(ReservedStockMatchMode.class),
               buf.readLong(),
               (ReservedStockMatchMode)buf.m_130066_(ReservedStockMatchMode.class)
            )
         );
      }

      int variantSize = TianshuPacketLimits.requireDecodedListSize("maintenance variants", buf.m_130242_());
      ArrayList<MaintenanceEditorData.VariantEntry> variants = new ArrayList<>(variantSize);

      for (int i = 0; i < variantSize; i++) {
         variants.add(new MaintenanceEditorData.VariantEntry(AEKey.readKey(buf), buf.m_130258_(), buf.readBoolean()));
      }

      return new MaintenanceEditorSyncPacket(
         container,
         selectionRevision,
         new MaintenanceEditorData(target, ruleId, lower, upper, batch, enabled, status, currentStock, craftable, recoveryPage, topology, variants)
      );
   }

   public static void handle(MaintenanceEditorSyncPacket packet, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNetworkPacketHandlers.handleMaintenanceEditorSync(packet)));
      ctx.setPacketHandled(true);
   }
}
