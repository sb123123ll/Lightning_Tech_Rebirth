package com.moakiee.ae2lt.device.network;

import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ArmorNetworkBinding implements DeviceNetworkBinding {
   public static final ArmorNetworkBinding INSTANCE = new ArmorNetworkBinding();
   private static final String TAG_ACCESS_POINT_POS = "accessPoint";

   private ArmorNetworkBinding() {
   }

   @Nullable
   @Override
   public GlobalPos getBoundPos(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      return tag != null && tag.m_128425_("accessPoint", 10)
         ? (GlobalPos)GlobalPos.f_122633_.parse(NbtOps.f_128958_, tag.m_128423_("accessPoint")).resultOrPartial(error -> {
         }).orElse(null)
         : null;
   }

   @Override
   public void bind(ItemStack stack, GlobalPos pos) {
      GlobalPos.f_122633_.encodeStart(NbtOps.f_128958_, pos).result().ifPresent(tag -> stack.m_41784_().m_128365_("accessPoint", tag));
   }

   @Override
   public void unbind(ItemStack stack) {
      stack.m_41749_("accessPoint");
   }

   @Override
   public BindingResolveResult resolve(ItemStack stack, ServerPlayer player) {
      GlobalPos pos = this.getBoundPos(stack);
      if (pos == null) {
         return BindingResolveResult.fail(BindingResolveResult.FailureReason.NOT_BOUND);
      } else {
         MinecraftServer server = player.m_20194_();
         if (server == null) {
            return BindingResolveResult.fail(BindingResolveResult.FailureReason.DIM_NOT_LOADED);
         } else {
            ServerLevel target = server.m_129880_(pos.m_122640_());
            if (target == null) {
               return BindingResolveResult.fail(BindingResolveResult.FailureReason.DIM_NOT_LOADED);
            } else {
               BlockPos blockPos = pos.m_122646_();
               if (!target.m_46749_(blockPos)) {
                  return BindingResolveResult.fail(BindingResolveResult.FailureReason.DIM_NOT_LOADED);
               } else if (target.m_7702_(blockPos) instanceof IWirelessAccessPoint accessPoint) {
                  if (!accessPoint.isActive()) {
                     return BindingResolveResult.fail(BindingResolveResult.FailureReason.INACTIVE_AP);
                  } else {
                     IGrid grid = accessPoint.getGrid();
                     return grid == null ? BindingResolveResult.fail(BindingResolveResult.FailureReason.NO_AP) : BindingResolveResult.ok(grid, accessPoint);
                  }
               } else {
                  return BindingResolveResult.fail(BindingResolveResult.FailureReason.NO_AP);
               }
            }
         }
      }
   }
}
