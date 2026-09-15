package com.moakiee.ae2lt.logic.railgun;

import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.util.DimensionalBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class RailgunBinding {
   private static final String TAG_ACCESS_POINT_POS = "accessPoint";

   private RailgunBinding() {
   }

   @Nullable
   public static GlobalPos getBoundPos(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      return tag != null && tag.m_128425_("accessPoint", 10)
         ? (GlobalPos)GlobalPos.f_122633_.parse(NbtOps.f_128958_, tag.m_128423_("accessPoint")).resultOrPartial(error -> {
         }).orElse(null)
         : null;
   }

   public static void bind(ItemStack stack, GlobalPos pos) {
      GlobalPos.f_122633_.encodeStart(NbtOps.f_128958_, pos).result().ifPresent(tag -> stack.m_41784_().m_128365_("accessPoint", tag));
   }

   public static void unbind(ItemStack stack) {
      stack.m_41749_("accessPoint");
   }

   public static RailgunBinding.Result resolve(ItemStack stack, ServerPlayer player) {
      GlobalPos pos = getBoundPos(stack);
      if (pos == null) {
         return RailgunBinding.Result.fail(RailgunBinding.FailReason.NOT_BOUND);
      } else {
         MinecraftServer server = player.m_20194_();
         if (server == null) {
            return RailgunBinding.Result.fail(RailgunBinding.FailReason.DIM_NOT_LOADED);
         } else {
            ServerLevel target = server.m_129880_(pos.m_122640_());
            if (target == null) {
               return RailgunBinding.Result.fail(RailgunBinding.FailReason.DIM_NOT_LOADED);
            } else {
               BlockPos bp = pos.m_122646_();
               if (!target.m_46749_(bp)) {
                  return RailgunBinding.Result.fail(RailgunBinding.FailReason.DIM_NOT_LOADED);
               } else if (target.m_7702_(bp) instanceof IWirelessAccessPoint ap) {
                  if (!ap.isActive()) {
                     return RailgunBinding.Result.fail(RailgunBinding.FailReason.INACTIVE_AP);
                  } else {
                     IGrid grid = ap.getGrid();
                     return grid == null ? RailgunBinding.Result.fail(RailgunBinding.FailReason.NO_AP) : RailgunBinding.Result.ok(grid, ap);
                  }
               } else {
                  return RailgunBinding.Result.fail(RailgunBinding.FailReason.NO_AP);
               }
            }
         }
      }
   }

   private static double distance(Entity player, IWirelessAccessPoint ap) {
      DimensionalBlockPos loc = ap.getLocation();
      BlockPos bp = loc.getPos();
      double dx = (double)bp.m_123341_() + 0.5 - player.m_20185_();
      double dy = (double)bp.m_123342_() + 0.5 - player.m_20186_();
      double dz = (double)bp.m_123343_() + 0.5 - player.m_20189_();
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   public static String failKey(RailgunBinding.FailReason r) {
      return switch (r) {
         case NOT_BOUND -> "ae2lt.railgun.fail.not_bound";
         case DIM_NOT_LOADED -> "ae2lt.railgun.fail.dim_not_loaded";
         case NO_AP -> "ae2lt.railgun.fail.no_ap";
         case INACTIVE_AP -> "ae2lt.railgun.fail.inactive_ap";
         case OUT_OF_RANGE -> "ae2lt.railgun.fail.out_of_range";
         case WRONG_DIMENSION -> "ae2lt.railgun.fail.wrong_dimension";
      };
   }

   public static enum FailReason {
      NOT_BOUND,
      DIM_NOT_LOADED,
      NO_AP,
      INACTIVE_AP,
      OUT_OF_RANGE,
      WRONG_DIMENSION;
   }

   public static record Result(@Nullable IGrid grid, @Nullable IWirelessAccessPoint ap, @Nullable RailgunBinding.FailReason failure) {
      public static RailgunBinding.Result ok(IGrid grid, IWirelessAccessPoint ap) {
         return new RailgunBinding.Result(grid, ap, null);
      }

      public static RailgunBinding.Result fail(RailgunBinding.FailReason r) {
         return new RailgunBinding.Result(null, null, r);
      }

      public boolean success() {
         return this.failure == null && this.grid != null;
      }
   }
}
