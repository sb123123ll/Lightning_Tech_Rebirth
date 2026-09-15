package com.moakiee.ae2lt.logic;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageHelper;
import appeng.me.helpers.MachineSource;
import appeng.parts.automation.AnnihilationPlanePart;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.registry.ModItems;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.BlockHitResult;

public final class FloatingMatterCapture {
   private FloatingMatterCapture() {
   }

   public static boolean tryCapture(ShulkerBullet bullet, BlockHitResult hit) {
      if (AE2LTCommonConfig.shulkerBulletCollectionEnabled() && bullet.m_6084_() && !bullet.m_9236_().m_5776_()) {
         if (bullet.m_9236_().m_7702_(hit.m_82425_()) instanceof IPartHost host) {
            if (host.getPart(hit.m_82434_()) instanceof AnnihilationPlanePart plane && plane.getMainNode().isActive() && hasSilkTouch(plane)) {
               IGrid grid = plane.getMainNode().getGrid();
               if (grid == null) {
                  return false;
               }

               IActionSource source = new MachineSource(plane);
               long inserted = StorageHelper.poweredInsert(
                  grid.getEnergyService(),
                  grid.getStorageService().getInventory(),
                  AEItemKey.of((ItemLike)ModItems.FLOATING_MATTER.get()),
                  1L,
                  source,
                  Actionable.MODULATE
               );
               if (inserted != 1L) {
                  return false;
               }

               bullet.m_146870_();
               return true;
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean hasSilkTouch(AnnihilationPlanePart plane) {
      for (Enchantment enchantment : plane.getEnchantments().keySet()) {
         if (enchantment == Enchantments.f_44985_) {
            return true;
         }
      }

      return false;
   }
}
