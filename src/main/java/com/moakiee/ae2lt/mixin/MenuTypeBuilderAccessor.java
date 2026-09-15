package com.moakiee.ae2lt.mixin;

import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.locator.MenuLocator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({MenuTypeBuilder.class})
public interface MenuTypeBuilderAccessor<M extends AEBaseMenu, I> {
   @Accessor(
      value = "id",
      remap = false
   )
   void ae2lt$setId(ResourceLocation var1);

   @Accessor(
      value = "id",
      remap = false
   )
   ResourceLocation ae2lt$getId();

   @Accessor(
      value = "menuType",
      remap = false
   )
   void ae2lt$setMenuType(MenuType<M> var1);

   @Accessor(
      value = "menuType",
      remap = false
   )
   MenuType<M> ae2lt$getMenuType();

   @Invoker(
      value = "fromNetwork",
      remap = false
   )
   M ae2lt$invokeFromNetwork(int var1, Inventory var2, FriendlyByteBuf var3);

   @Invoker(
      value = "open",
      remap = false
   )
   boolean ae2lt$invokeOpen(Player var1, MenuLocator var2, boolean var3);
}
