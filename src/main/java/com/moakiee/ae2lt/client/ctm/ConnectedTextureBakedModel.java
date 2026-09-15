package com.moakiee.ae2lt.client.ctm;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

public class ConnectedTextureBakedModel implements IDynamicBakedModel {
   public static final ModelProperty<CtmConnectionState> CONNECTION = new ModelProperty();
   private static final Direction[] DIRECTIONS = Direction.values();
   private static final float OVERLAY_OFFSET = 9.765625E-4F;
   private final TextureAtlasSprite baseSprite;
   private final TextureAtlasSprite ctmSprite;
   @Nullable
   private final TextureAtlasSprite overlaySprite;
   private final ConnectionPredicate predicate;
   private final ChunkRenderTypeSet renderTypes;
   private final boolean ambientOcclusion;
   private final boolean gui3d;
   private final boolean usesBlockLight;

   public ConnectedTextureBakedModel(
      TextureAtlasSprite baseSprite,
      TextureAtlasSprite ctmSprite,
      @Nullable TextureAtlasSprite overlaySprite,
      ConnectionPredicate predicate,
      ChunkRenderTypeSet renderTypes,
      boolean ambientOcclusion,
      boolean gui3d,
      boolean usesBlockLight
   ) {
      this.baseSprite = baseSprite;
      this.ctmSprite = ctmSprite;
      this.overlaySprite = overlaySprite;
      this.predicate = predicate;
      this.renderTypes = renderTypes;
      this.ambientOcclusion = ambientOcclusion;
      this.gui3d = gui3d;
      this.usesBlockLight = usesBlockLight;
   }

   public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
      if (!this.predicate.isActive(level, pos, state)) {
         return modelData;
      } else {
         boolean[] culled = new boolean[DIRECTIONS.length];
         int[] edges = new int[DIRECTIONS.length];
         int[] corners = new int[DIRECTIONS.length];

         for (Direction face : DIRECTIONS) {
            int idx = face.m_122411_();
            culled[idx] = this.predicate.connects(level, pos, state, face);
            int mask = 0;

            for (int edge = 0; edge < 4; edge++) {
               if (this.predicate.connects(level, pos, state, CtmFaceGeometry.neighborDir(face, edge))) {
                  mask |= 1 << edge;
               }
            }

            edges[idx] = mask;
            int cornerMask = 0;

            for (CtmTileSelector.Quadrant quadrant : CtmTileSelector.Quadrant.values()) {
               if (this.predicate.connects(level, pos, state, CtmFaceGeometry.cornerPos(pos, face, quadrant))) {
                  cornerMask |= 1 << quadrant.ordinal();
               }
            }

            corners[idx] = cornerMask;
         }

         return modelData.derive().with(CONNECTION, new CtmConnectionState(culled, edges, corners)).build();
      }
   }

   public List<BakedQuad> getQuads(
      @Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType
   ) {
      if (side == null) {
         return List.of();
      } else {
         CtmConnectionState conn = (CtmConnectionState)extraData.get(CONNECTION);
         if (conn == null) {
            return this.overlaySprite == null
               ? List.of(CtmFaceGeometry.fullFace(side, this.baseSprite))
               : List.of(CtmFaceGeometry.fullFace(side, this.baseSprite), CtmFaceGeometry.fullFace(side, this.overlaySprite, 9.765625E-4F));
         } else if (conn.culled(side)) {
            return List.of();
         } else {
            int edges = conn.edges(side);
            int corners = conn.corners(side);
            List<BakedQuad> quads = new ArrayList<>(4);

            for (int sq = 0; sq < 2; sq++) {
               for (int tq = 0; tq < 2; tq++) {
                  CtmTileSelector.Tile tile = CtmTileSelector.select(CtmTileSelector.quadrant(sq, tq), edges, corners);
                  TextureAtlasSprite sprite = tile.source() == CtmTileSelector.Source.BASE ? this.baseSprite : this.ctmSprite;
                  quads.add(CtmFaceGeometry.quadrant(side, sq, tq, tile, sprite));
               }
            }

            if (this.overlaySprite != null) {
               quads.add(CtmFaceGeometry.fullFace(side, this.overlaySprite, 9.765625E-4F));
            }

            return quads;
         }
      }
   }

   public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
      return this.renderTypes;
   }

   public boolean m_7541_() {
      return this.ambientOcclusion;
   }

   public boolean m_7539_() {
      return this.gui3d;
   }

   public boolean m_7547_() {
      return this.usesBlockLight;
   }

   public boolean m_7521_() {
      return false;
   }

   public TextureAtlasSprite m_6160_() {
      return this.baseSprite;
   }

   public ItemOverrides m_7343_() {
      return ItemOverrides.f_111734_;
   }
}
