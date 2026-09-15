package com.moakiee.ae2lt.client.ctm;

import java.util.function.Function;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

public class ConnectedTextureGeometry implements IUnbakedGeometry<ConnectedTextureGeometry> {
   private final ResourceLocation connectionId;
   private final ChunkRenderTypeSet renderTypes;
   private final boolean ambientOcclusion;
   private final boolean gui3d;
   private final boolean usesBlockLight;

   public ConnectedTextureGeometry(
      ResourceLocation connectionId, ChunkRenderTypeSet renderTypes, boolean ambientOcclusion, boolean gui3d, boolean usesBlockLight
   ) {
      this.connectionId = connectionId;
      this.renderTypes = renderTypes;
      this.ambientOcclusion = ambientOcclusion;
      this.gui3d = gui3d;
      this.usesBlockLight = usesBlockLight;
   }

   public BakedModel bake(
      IGeometryBakingContext context,
      ModelBaker baker,
      Function<Material, TextureAtlasSprite> spriteGetter,
      ModelState modelState,
      ItemOverrides overrides,
      ResourceLocation modelLocation
   ) {
      TextureAtlasSprite base = spriteGetter.apply(context.getMaterial("base"));
      TextureAtlasSprite ctm = spriteGetter.apply(context.getMaterial("ctm"));
      TextureAtlasSprite overlay = context.hasMaterial("overlay") ? spriteGetter.apply(context.getMaterial("overlay")) : null;
      ConnectionPredicate predicate = ConnectionPredicates.get(this.connectionId);
      return new ConnectedTextureBakedModel(base, ctm, overlay, predicate, this.renderTypes, this.ambientOcclusion, this.gui3d, this.usesBlockLight);
   }
}
