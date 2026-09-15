package com.moakiee.ae2lt.item.railgun;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum RailgunModuleType implements StringRepresentable {
   CORE("core"),
   COMPUTE("compute"),
   ACCELERATION("acceleration"),
   OVERLOAD_EXECUTION("overload_execution"),
   RANGE("range"),
   MULTIDIMENSIONAL_EXECUTION("multidimensional_execution");

   public static final Codec<RailgunModuleType> CODEC = StringRepresentable.m_216439_(RailgunModuleType::values);
   private final String name;

   private RailgunModuleType(String name) {
      this.name = name;
   }

   public String m_7912_() {
      return this.name;
   }
}
