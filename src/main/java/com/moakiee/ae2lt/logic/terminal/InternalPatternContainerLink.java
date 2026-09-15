package com.moakiee.ae2lt.logic.terminal;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class InternalPatternContainerLink {
   private static final IGridNodeListener<BlockEntity> NODE_LISTENER = (owner, node) -> {
   };
   private final BlockEntity owner;
   private final ItemLike visualRepresentation;
   private IManagedGridNode managedNode;
   private IGridNode linkedPortNode;
   private IGridConnection connection;

   public InternalPatternContainerLink(BlockEntity owner, ItemLike visualRepresentation) {
      this.owner = owner;
      this.visualRepresentation = visualRepresentation;
   }

   public void bind(IManagedGridNode portNode) {
      Level level = this.owner.m_58904_();
      IGridNode targetNode = portNode != null && portNode.isReady() ? portNode.getNode() : null;
      if (level != null && !level.f_46443_ && targetNode != null) {
         if (this.connection == null || this.linkedPortNode != targetNode || this.managedNode == null || !this.managedNode.isReady()) {
            this.disconnect();
            this.managedNode = GridHelper.createManagedNode(this.owner, NODE_LISTENER)
               .setInWorldNode(false)
               .setVisualRepresentation(this.visualRepresentation)
               .setIdlePowerUsage(0.0);
            this.managedNode.create(level, this.owner.m_58899_());
            this.connection = GridHelper.createConnection(this.managedNode.getNode(), targetNode);
            this.linkedPortNode = targetNode;
         }
      }
   }

   public void disconnect() {
      if (this.connection != null) {
         this.connection.destroy();
         this.connection = null;
      }

      if (this.managedNode != null) {
         this.managedNode.destroy();
         this.managedNode = null;
      }

      this.linkedPortNode = null;
   }

   public IGrid getGrid() {
      return this.managedNode != null ? this.managedNode.getGrid() : null;
   }

   public boolean isActive() {
      return this.connection != null && this.managedNode != null && this.managedNode.isActive();
   }
}
