package com.moakiee.ae2lt.menu;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;

public final class Ae2ltSlotBackgrounds {
   public static final ResourceLocation ELECTRO_CHIME_CRYSTAL = sprite("electro_chime_crystal");
   public static final ResourceLocation FILTER_COMPONENT = sprite("filter_component");
   public static final ResourceLocation LIGHTNING_COLLAPSE_MATRIX = sprite("lightning_collapse_matrix");

   private static ResourceLocation sprite(String name) {
      return new ResourceLocation("ae2lt", "block/slot/" + name);
   }

   public static <T extends Slot> T withBackground(T slot, ResourceLocation sprite) {
      slot.setBackground(InventoryMenu.f_39692_, sprite);
      return slot;
   }

   private Ae2ltSlotBackgrounds() {
   }
}
