package com.moakiee.ae2lt.device.network;

import com.moakiee.ae2lt.logic.railgun.RailgunBinding;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class RailgunNetworkBinding implements DeviceNetworkBinding {
   public static final RailgunNetworkBinding INSTANCE = new RailgunNetworkBinding();

   private RailgunNetworkBinding() {
   }

   @Override
   public GlobalPos getBoundPos(ItemStack stack) {
      return RailgunBinding.getBoundPos(stack);
   }

   @Override
   public void bind(ItemStack stack, GlobalPos pos) {
      RailgunBinding.bind(stack, pos);
   }

   @Override
   public void unbind(ItemStack stack) {
      RailgunBinding.unbind(stack);
   }

   @Override
   public BindingResolveResult resolve(ItemStack stack, ServerPlayer player) {
      RailgunBinding.Result result = RailgunBinding.resolve(stack, player);
      return result.success() ? BindingResolveResult.ok(result.grid(), result.ap()) : BindingResolveResult.fail(mapFailure(result.failure()));
   }

   private static BindingResolveResult.FailureReason mapFailure(RailgunBinding.FailReason failure) {
      return switch (failure) {
         case NOT_BOUND -> BindingResolveResult.FailureReason.NOT_BOUND;
         case DIM_NOT_LOADED -> BindingResolveResult.FailureReason.DIM_NOT_LOADED;
         case NO_AP -> BindingResolveResult.FailureReason.NO_AP;
         case INACTIVE_AP -> BindingResolveResult.FailureReason.INACTIVE_AP;
         case OUT_OF_RANGE -> BindingResolveResult.FailureReason.OUT_OF_RANGE;
         case WRONG_DIMENSION -> BindingResolveResult.FailureReason.WRONG_DIMENSION;
      };
   }
}
