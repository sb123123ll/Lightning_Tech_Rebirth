package com.moakiee.ae2lt.network.tianshu;

import appeng.api.stacks.AEKey;
import com.moakiee.ae2lt.logic.tianshu.maintenance.ReservedStockMatchMode;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record SaveMaintenanceRulePacket(
   int containerId,
   int selectionRevision,
   AEKey target,
   UUID expectedRuleId,
   boolean delete,
   long lower,
   long upper,
   long amountPerJob,
   boolean enabled,
   List<SaveMaintenanceRulePacket.ReserveEdit> reserves
) {
   public SaveMaintenanceRulePacket(
      int containerId,
      int selectionRevision,
      AEKey target,
      UUID expectedRuleId,
      boolean delete,
      long lower,
      long upper,
      long amountPerJob,
      boolean enabled,
      List<SaveMaintenanceRulePacket.ReserveEdit> reserves
   ) {
      reserves = List.copyOf(reserves);
      TianshuPacketLimits.requireListSize("maintenance reserve edits", reserves.size());
      this.containerId = containerId;
      this.selectionRevision = selectionRevision;
      this.target = target;
      this.expectedRuleId = expectedRuleId;
      this.delete = delete;
      this.lower = lower;
      this.upper = upper;
      this.amountPerJob = amountPerJob;
      this.enabled = enabled;
      this.reserves = reserves;
   }

   public void write(FriendlyByteBuf buf) {
      buf.m_130130_(this.containerId);
      buf.m_130130_(this.selectionRevision);
      AEKey.writeKey(buf, this.target);
      buf.writeBoolean(this.expectedRuleId != null);
      if (this.expectedRuleId != null) {
         buf.m_130077_(this.expectedRuleId);
      }

      buf.writeBoolean(this.delete);
      buf.m_130103_(this.lower);
      buf.m_130103_(this.upper);
      buf.m_130103_(this.amountPerJob);
      buf.writeBoolean(this.enabled);
      buf.m_130130_(this.reserves.size());

      for (SaveMaintenanceRulePacket.ReserveEdit edit : this.reserves) {
         AEKey.writeKey(buf, edit.key());
         buf.writeLong(edit.globalAmount());
         buf.m_130068_(edit.globalMode());
         buf.writeLong(edit.ruleAmount());
         buf.m_130068_(edit.ruleMode());
      }
   }

   public static SaveMaintenanceRulePacket decode(FriendlyByteBuf buf) {
      int container = buf.m_130242_();
      int selectionRevision = buf.m_130242_();
      AEKey target = AEKey.readKey(buf);
      UUID id = buf.readBoolean() ? buf.m_130259_() : null;
      boolean delete = buf.readBoolean();
      long lower = buf.m_130258_();
      long upper = buf.m_130258_();
      long batch = buf.m_130258_();
      boolean enabled = buf.readBoolean();
      int size = TianshuPacketLimits.requireDecodedListSize("maintenance reserve edits", buf.m_130242_());
      ArrayList<SaveMaintenanceRulePacket.ReserveEdit> edits = new ArrayList<>(size);

      for (int i = 0; i < size; i++) {
         edits.add(
            new SaveMaintenanceRulePacket.ReserveEdit(
               AEKey.readKey(buf),
               buf.readLong(),
               (ReservedStockMatchMode)buf.m_130066_(ReservedStockMatchMode.class),
               buf.readLong(),
               (ReservedStockMatchMode)buf.m_130066_(ReservedStockMatchMode.class)
            )
         );
      }

      return new SaveMaintenanceRulePacket(container, selectionRevision, target, id, delete, lower, upper, batch, enabled, edits);
   }

   public static void handle(SaveMaintenanceRulePacket packet, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null && player.f_36096_ instanceof TianshuPatternEncodingTermMenu menu && menu.f_38840_ == packet.containerId()) {
            menu.saveMaintenanceRule(packet);
         }
      });
      ctx.setPacketHandled(true);
   }

   public static record ReserveEdit(AEKey key, long globalAmount, ReservedStockMatchMode globalMode, long ruleAmount, ReservedStockMatchMode ruleMode) {
   }
}
