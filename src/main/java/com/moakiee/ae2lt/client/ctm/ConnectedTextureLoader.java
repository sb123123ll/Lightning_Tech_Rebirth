package com.moakiee.ae2lt.client.ctm;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.geometry.IGeometryLoader;

public class ConnectedTextureLoader implements IGeometryLoader<ConnectedTextureGeometry> {
   public ConnectedTextureGeometry read(JsonObject json, JsonDeserializationContext context) {
      ResourceLocation connection = ResourceLocation.m_135820_(GsonHelper.m_13851_(json, "connection", "ae2lt:same_block"));
      RenderType renderType = parseRenderType(GsonHelper.m_13851_(json, "render_type", "minecraft:translucent"));
      boolean ambientOcclusion = GsonHelper.m_13855_(json, "ambientocclusion", true);
      boolean gui3d = GsonHelper.m_13855_(json, "gui3d", true);
      boolean usesBlockLight = GsonHelper.m_13855_(json, "uses_block_light", true);
      return new ConnectedTextureGeometry(connection, ChunkRenderTypeSet.of(new RenderType[]{renderType}), ambientOcclusion, gui3d, usesBlockLight);
   }

   private static RenderType parseRenderType(String name) {
      return switch (name) {
         case "solid", "minecraft:solid" -> RenderType.m_110451_();
         case "cutout", "minecraft:cutout" -> RenderType.m_110463_();
         case "cutout_mipped", "minecraft:cutout_mipped" -> RenderType.m_110457_();
         default -> RenderType.m_110466_();
      };
   }
}
