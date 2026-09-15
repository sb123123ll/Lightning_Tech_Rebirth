package com.moakiee.ae2lt.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class RecipeSerializationHelper {
   private RecipeSerializationHelper() {
   }

   public static ItemStack itemStackFromJson(JsonObject json) {
      Item item = itemFromId(resourceLocationFromJson(json, "id"));
      int count = GsonHelper.m_13824_(json, "count", 1);
      if (count <= 0) {
         throw new JsonSyntaxException("Item stack count must be positive");
      } else {
         return new ItemStack(item, count);
      }
   }

   public static ItemStack itemStackFromJson(JsonObject json, String key) {
      return itemStackFromJson(GsonHelper.m_13930_(json, key));
   }

   public static Block blockFromJson(JsonObject json, String key) {
      return blockFromId(ResourceLocation.m_135820_(GsonHelper.m_13906_(json, key)));
   }

   public static Block blockFromId(ResourceLocation id) {
      return (Block)BuiltInRegistries.f_256975_.m_6612_(id).orElseThrow(() -> new JsonSyntaxException("Unknown block id: " + id));
   }

   public static FluidStack fluidStackFromJson(JsonObject json) {
      ResourceLocation id = resourceLocationFromJson(json, "id");
      Fluid fluid = (Fluid)ForgeRegistries.FLUIDS.getValue(id);
      if (fluid == null) {
         throw new JsonSyntaxException("Unknown fluid id: " + id);
      } else {
         int amount = GsonHelper.m_13927_(json, "amount");
         if (amount <= 0) {
            throw new JsonSyntaxException("Fluid amount must be positive");
         } else {
            return new FluidStack(fluid, amount);
         }
      }
   }

   public static FluidStack optionalFluidStackFromJson(JsonObject json, String key) {
      return json.has(key) && !json.get(key).isJsonNull() ? fluidStackFromJson(GsonHelper.m_13930_(json, key)) : FluidStack.EMPTY;
   }

   public static ResourceLocation resourceLocationFromJson(JsonObject json, String key) {
      return ResourceLocation.m_135820_(GsonHelper.m_13906_(json, key));
   }

   public static ResourceLocation resourceLocationFromJson(JsonElement json) {
      return ResourceLocation.m_135820_(GsonHelper.m_13805_(json, "resource_location"));
   }

   public static <T extends StringRepresentable> T enumFromJson(JsonObject json, String key, T defaultValue, T[] values) {
      String serializedName = GsonHelper.m_13851_(json, key, defaultValue.m_7912_());

      for (T value : values) {
         if (value.m_7912_().equals(serializedName)) {
            return value;
         }
      }

      throw new JsonSyntaxException("Unknown " + key + " value: " + serializedName);
   }

   private static Item itemFromId(ResourceLocation id) {
      return (Item)BuiltInRegistries.f_257033_.m_6612_(id).orElseThrow(() -> new JsonSyntaxException("Unknown item id: " + id));
   }
}
