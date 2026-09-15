package com.moakiee.ae2lt.celestweave;

import java.util.Map;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.crafting.Ingredient;

public final class CelestweaveArmorMaterials {
   private static final int ENCHANTMENT_VALUE = 32;
   private static final Map<Type, Integer> DEFENSE = Map.of(Type.HELMET, 6, Type.CHESTPLATE, 12, Type.LEGGINGS, 8, Type.BOOTS, 5);
   public static final ArmorMaterial CELESTWEAVE = new ArmorMaterial() {
      public int m_266425_(Type type) {
         return 0;
      }

      public int m_7366_(Type type) {
         return CelestweaveArmorMaterials.DEFENSE.getOrDefault(type, 0);
      }

      public int m_6646_() {
         return 32;
      }

      public SoundEvent m_7344_() {
         return SoundEvents.f_11675_;
      }

      public Ingredient m_6230_() {
         return Ingredient.f_43901_;
      }

      public String m_6082_() {
         return "celestweave";
      }

      public float m_6651_() {
         return 5.0F;
      }

      public float m_6649_() {
         return 0.2F;
      }
   };

   private CelestweaveArmorMaterials() {
   }
}
