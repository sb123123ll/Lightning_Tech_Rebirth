package com.moakiee.ae2lt.menu;

import appeng.menu.AEBaseMenu;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.MenuTypeBuilder;
import com.moakiee.ae2lt.mixin.MenuTypeBuilderAccessor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;

public final class Ae2ltMenuBuilder {
   private Ae2ltMenuBuilder() {
   }

   public static <M extends AEBaseMenu, I> MenuType<M> buildUnregistered(MenuTypeBuilder<M, I> builder, ResourceLocation id) {
      MenuTypeBuilderAccessor<M, I> accessor = (MenuTypeBuilderAccessor<M, I>)builder;
      if (accessor.ae2lt$getMenuType() != null) {
         throw new IllegalStateException("buildUnregistered already called for " + id);
      } else {
         accessor.ae2lt$setId(id);
         MenuType<M> menuType = IForgeMenuType.create((containerId, inv, buf) -> fromNetwork(accessor, containerId, inv, buf));
         accessor.ae2lt$setMenuType(menuType);
         MenuOpener.addOpener(menuType, accessor::ae2lt$invokeOpen);
         return menuType;
      }
   }

   private static <M extends AEBaseMenu, I> M fromNetwork(MenuTypeBuilderAccessor<M, I> accessor, int containerId, Inventory inv, FriendlyByteBuf buf) {
      return accessor.ae2lt$invokeFromNetwork(containerId, inv, buf);
   }
}
