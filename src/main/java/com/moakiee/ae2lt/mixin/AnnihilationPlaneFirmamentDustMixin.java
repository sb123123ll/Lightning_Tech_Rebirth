package com.moakiee.ae2lt.mixin;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageHelper;
import appeng.me.helpers.MachineSource;
import appeng.parts.automation.AnnihilationPlanePart;
import com.moakiee.ae2lt.logic.FirmamentDustGenerationRules;
import com.moakiee.ae2lt.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {AnnihilationPlanePart.class},
   remap = false
)
public abstract class AnnihilationPlaneFirmamentDustMixin {
   @Unique
   private int ae2lt$firmamentGenerationTicks;

   @Inject(
      method = {"tickingRequest"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ae2lt$generateFirmamentDustInTheEnd(IGridNode node, int ticksSinceLastCall, CallbackInfoReturnable<TickRateModulation> cir) {
      AnnihilationPlanePart self = (AnnihilationPlanePart)this;
      BlockEntity host = self.getBlockEntity();
      if (host != null && host.m_58904_() instanceof ServerLevel level) {
         BlockPos var12 = host.m_58899_();
         boolean shouldGenerate = FirmamentDustGenerationRules.shouldGenerate(
            level.m_46472_().m_135782_().toString(), self.getSide().m_7912_(), var12.m_123342_(), level.m_151558_()
         );
         if (!shouldGenerate) {
            this.ae2lt$firmamentGenerationTicks = 0;
         } else if (!self.isActive()) {
            this.ae2lt$firmamentGenerationTicks = 0;
            cir.setReturnValue(TickRateModulation.SLEEP);
         } else {
            this.ae2lt$firmamentGenerationTicks += ticksSinceLastCall;
            if (this.ae2lt$firmamentGenerationTicks >= 200) {
               long amount = (long)(this.ae2lt$firmamentGenerationTicks / 200);
               AEItemKey key = AEItemKey.of((ItemLike)ModItems.FIRMAMENT_DUST.get());
               if (key != null) {
                  this.ae2lt$insertIntoGrid(self, key, amount);
               }

               this.ae2lt$firmamentGenerationTicks = (int)((long)this.ae2lt$firmamentGenerationTicks - amount * 200L);
            }

            cir.setReturnValue(TickRateModulation.IDLE);
         }
      } else {
         this.ae2lt$firmamentGenerationTicks = 0;
      }
   }

   @Unique
   private long ae2lt$insertIntoGrid(AnnihilationPlanePart self, AEKey what, long amount) {
      IGrid grid = self.getMainNode().getGrid();
      return grid == null
         ? 0L
         : StorageHelper.poweredInsert(grid.getEnergyService(), grid.getStorageService().getInventory(), what, amount, new MachineSource(self));
   }
}
