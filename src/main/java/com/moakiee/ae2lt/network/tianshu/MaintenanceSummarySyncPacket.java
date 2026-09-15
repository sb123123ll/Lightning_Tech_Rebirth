package com.moakiee.ae2lt.network.tianshu;

import appeng.api.stacks.AEKey;
import com.moakiee.ae2lt.client.ClientNetworkPacketHandlers;
import com.moakiee.ae2lt.logic.tianshu.maintenance.InventoryMaintenanceStatus;
import com.moakiee.ae2lt.logic.tianshu.maintenance.ReservedStockMatchMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record MaintenanceSummarySyncPacket(
   int containerId, int selectionRevision, long revision, boolean overflow, List<MaintenanceSummarySyncPacket.Entry> entries
) {
   public MaintenanceSummarySyncPacket(
      int containerId, int selectionRevision, long revision, boolean overflow, List<MaintenanceSummarySyncPacket.Entry> entries
   ) {
      entries = List.copyOf(entries);
      TianshuPacketLimits.requireListSize("maintenance summary", entries.size());
      this.containerId = containerId;
      this.selectionRevision = selectionRevision;
      this.revision = revision;
      this.overflow = overflow;
      this.entries = entries;
   }

   public void write(FriendlyByteBuf buf) {
      buf.m_130130_(this.containerId);
      buf.m_130130_(this.selectionRevision);
      buf.m_130103_(this.revision);
      buf.writeBoolean(this.overflow);
      buf.m_130130_(this.entries.size());

      for (MaintenanceSummarySyncPacket.Entry entry : this.entries) {
         AEKey.writeKey(buf, entry.key());
         buf.writeBoolean(entry.ruleConfigured());
         buf.m_130068_(entry.status());
         buf.m_130103_(entry.storedAmount());
         buf.m_130103_(entry.lowerThreshold());
         buf.m_130103_(entry.upperThreshold());
         buf.m_130103_(entry.amountPerJob());
         buf.writeLong(entry.globalReserve());
         buf.m_130068_(entry.globalMode());
         buf.writeBoolean(entry.globalReserveConfigured());
         buf.writeBoolean(entry.craftable());
         buf.writeBoolean(entry.ruleReserveOverflow());
      }
   }

   public static MaintenanceSummarySyncPacket decode(FriendlyByteBuf buf) {
      int container = buf.m_130242_();
      int selectionRevision = buf.m_130242_();
      long revision = buf.m_130258_();
      boolean overflow = buf.readBoolean();
      int size = TianshuPacketLimits.requireDecodedListSize("maintenance summary", buf.m_130242_());
      ArrayList<MaintenanceSummarySyncPacket.Entry> entries = new ArrayList<>(size);

      for (int i = 0; i < size; i++) {
         entries.add(
            new MaintenanceSummarySyncPacket.Entry(
               AEKey.readKey(buf),
               buf.readBoolean(),
               (InventoryMaintenanceStatus)buf.m_130066_(InventoryMaintenanceStatus.class),
               buf.m_130258_(),
               buf.m_130258_(),
               buf.m_130258_(),
               buf.m_130258_(),
               buf.readLong(),
               (ReservedStockMatchMode)buf.m_130066_(ReservedStockMatchMode.class),
               buf.readBoolean(),
               buf.readBoolean(),
               buf.readBoolean()
            )
         );
      }

      return new MaintenanceSummarySyncPacket(container, selectionRevision, revision, overflow, entries);
   }

   public static void handle(MaintenanceSummarySyncPacket packet, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNetworkPacketHandlers.handleMaintenanceSummarySync(packet)));
      ctx.setPacketHandled(true);
   }

   public static record Entry(
      AEKey key,
      boolean ruleConfigured,
      InventoryMaintenanceStatus status,
      long storedAmount,
      long lowerThreshold,
      long upperThreshold,
      long amountPerJob,
      long globalReserve,
      ReservedStockMatchMode globalMode,
      boolean globalReserveConfigured,
      boolean craftable,
      boolean ruleReserveOverflow
   ) {
   }
}
