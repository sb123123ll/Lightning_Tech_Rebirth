package com.moakiee.ae2lt.part;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.items.parts.ColoredPartItem;
import appeng.parts.networking.CoveredDenseCablePart;
import appeng.parts.networking.IUsedChannelProvider;
import com.moakiee.ae2lt.registry.ModItems;
import com.moakiee.thunderbolt.api.channel.HighCapacityChannelOwner;
import net.minecraft.world.entity.player.Player;

public class OverloadedCablePart extends CoveredDenseCablePart implements HighCapacityChannelOwner, IUsedChannelProvider {
   public OverloadedCablePart(ColoredPartItem<?> partItem) {
      super(partItem);
   }

   protected IManagedGridNode createMainNode() {
      return super.createMainNode().setTagName("overloaded_cable");
   }

   public AECableType getCableConnectionType() {
      return AECableType.DENSE_COVERED;
   }

   public int getUsedChannelsInfo() {
      int used = 0;
      IGridNode node = this.getGridNode();
      if (node != null && node.isActive()) {
         for (IGridConnection connection : node.getConnections()) {
            used = Math.max(used, connection.getUsedChannels());
         }
      }

      return used;
   }

   public int getMaxChannelsInfo() {
      return this.getGridNode() == null ? 0 : -1;
   }

   public boolean changeColor(AEColor newColor, Player who) {
      if (this.getCableColor() == newColor) {
         return false;
      } else {
         ColoredPartItem<OverloadedCablePart> newPart = ModItems.getOverloadedCable(newColor);
         if (this.isClientSide()) {
            return true;
         } else {
            this.setPartItem(newPart);
            this.getMainNode().setGridColor(this.getCableColor());
            this.getHost().partChanged();
            this.getHost().markForUpdate();
            this.getHost().markForSave();
            return true;
         }
      }
   }
}
