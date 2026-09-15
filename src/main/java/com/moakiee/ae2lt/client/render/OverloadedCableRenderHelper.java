package com.moakiee.ae2lt.client.render;

import appeng.api.parts.IPartModel;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.client.render.cablebus.CableBusRenderState;
import appeng.client.render.cablebus.CubeBuilder;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public final class OverloadedCableRenderHelper {
   private static final String OVERLOAD_CABLE_LINE_TEXTURE_FOLDER = "part/cable/overload/";
   private static final String OVERLOAD_CABLE_CORE_TEXTURE_FOLDER = "part/cable/core/overload/";

   private OverloadedCableRenderHelper() {
   }

   public static void addCableQuads(CableBusRenderState renderState, List<BakedQuad> quadsOut) {
      AECableType cableType = renderState.getCableType();
      if (cableType != AECableType.NONE) {
         TextureAtlasSprite textureCore = getCoreTexture(renderState.getCableColor());
         TextureAtlasSprite textureLine = getLineTexture(renderState.getCableColor());
         EnumMap<Direction, AECableType> connectionTypes = renderState.getConnectionTypes();
         boolean noAttachments = !renderState.getAttachments().values().stream().anyMatch(IPartModel::requireCableConnection);
         if (noAttachments && isStraightLine(cableType, connectionTypes)) {
            addStraightDenseConnection((Direction)connectionTypes.keySet().iterator().next(), textureLine, quadsOut);
         } else {
            addDenseCore(textureCore, quadsOut);

            for (Entry<Direction, AECableType> connection : connectionTypes.entrySet()) {
               Direction facing = connection.getKey();
               AECableType connectionType = connection.getValue();
               boolean cableBusAdjacent = renderState.getCableBusAdjacent().contains(facing);
               if (connectionType != AECableType.GLASS && connectionType != AECableType.COVERED && connectionType != AECableType.SMART) {
                  addDenseConnection(facing, textureLine, quadsOut);
               } else {
                  addCoveredConnection(facing, connectionType, cableBusAdjacent, textureLine, quadsOut);
               }
            }
         }
      }
   }

   private static TextureAtlasSprite getCoreTexture(AEColor color) {
      Function<ResourceLocation, TextureAtlasSprite> atlas = Minecraft.m_91087_().m_91258_(TextureAtlas.f_118259_);
      return atlas.apply(new ResourceLocation("ae2lt", "part/cable/core/overload/" + color.name().toLowerCase(Locale.ROOT)));
   }

   private static TextureAtlasSprite getLineTexture(AEColor color) {
      Function<ResourceLocation, TextureAtlasSprite> atlas = Minecraft.m_91087_().m_91258_(TextureAtlas.f_118259_);
      return atlas.apply(new ResourceLocation("ae2lt", "part/cable/overload/" + color.name().toLowerCase(Locale.ROOT)));
   }

   private static boolean isStraightLine(AECableType cableType, EnumMap<Direction, AECableType> sides) {
      Iterator<Entry<Direction, AECableType>> it = sides.entrySet().iterator();
      if (!it.hasNext()) {
         return false;
      } else {
         Entry<Direction, AECableType> firstConnection = it.next();
         Direction firstSide = firstConnection.getKey();
         AECableType firstType = firstConnection.getValue();
         if (!it.hasNext()) {
            return false;
         } else if (firstSide.m_122424_() != it.next().getKey()) {
            return false;
         } else if (it.hasNext()) {
            return false;
         } else {
            AECableType secondType = sides.get(firstSide.m_122424_());
            return firstType == secondType && cableType == firstType;
         }
      }
   }

   private static void addDenseCore(TextureAtlasSprite texture, List<BakedQuad> quadsOut) {
      CubeBuilder cubeBuilder = new CubeBuilder(quadsOut);
      cubeBuilder.setTexture(texture);
      cubeBuilder.addCube(3.0F, 3.0F, 3.0F, 13.0F, 13.0F, 13.0F);
   }

   private static void addDenseConnection(Direction facing, TextureAtlasSprite texture, List<BakedQuad> quadsOut) {
      CubeBuilder cubeBuilder = new CubeBuilder(quadsOut);
      cubeBuilder.setDrawFaces(EnumSet.complementOf(EnumSet.of(facing)));
      cubeBuilder.setTexture(texture);
      addDenseCableSizedCube(facing, cubeBuilder);
   }

   private static void addCoveredConnection(
      Direction facing, AECableType connectionType, boolean cableBusAdjacent, TextureAtlasSprite texture, List<BakedQuad> quadsOut
   ) {
      CubeBuilder cubeBuilder = new CubeBuilder(quadsOut);
      cubeBuilder.setDrawFaces(EnumSet.complementOf(EnumSet.of(facing)));
      cubeBuilder.setTexture(texture);
      if (connectionType != AECableType.GLASS && !cableBusAdjacent) {
         addBigCoveredCableSizedCube(facing, cubeBuilder);
      }

      addCoveredCableSizedCube(facing, cubeBuilder);
   }

   private static void addStraightDenseConnection(Direction facing, TextureAtlasSprite texture, List<BakedQuad> quadsOut) {
      CubeBuilder cubeBuilder = new CubeBuilder(quadsOut);
      cubeBuilder.setTexture(texture);
      setStraightCableUVs(cubeBuilder, facing, 3.0F, 13.0F);
      addStraightDenseCableSizedCube(facing, cubeBuilder);
   }

   private static void setStraightCableUVs(CubeBuilder cubeBuilder, Direction facing, float x, float y) {
      switch (facing) {
         case DOWN:
         case UP:
            cubeBuilder.setCustomUv(Direction.NORTH, x, 0.0F, y, x);
            cubeBuilder.setCustomUv(Direction.EAST, x, 0.0F, y, x);
            cubeBuilder.setCustomUv(Direction.SOUTH, x, 0.0F, y, x);
            cubeBuilder.setCustomUv(Direction.WEST, x, 0.0F, y, x);
            break;
         case EAST:
         case WEST:
            cubeBuilder.setCustomUv(Direction.UP, 0.0F, x, x, y);
            cubeBuilder.setCustomUv(Direction.DOWN, 0.0F, x, x, y);
            cubeBuilder.setCustomUv(Direction.NORTH, 0.0F, x, x, y);
            cubeBuilder.setCustomUv(Direction.SOUTH, 0.0F, x, x, y);
            break;
         case NORTH:
         case SOUTH:
            cubeBuilder.setCustomUv(Direction.UP, x, 0.0F, y, x);
            cubeBuilder.setCustomUv(Direction.DOWN, x, 0.0F, y, x);
            cubeBuilder.setCustomUv(Direction.EAST, 0.0F, x, x, y);
            cubeBuilder.setCustomUv(Direction.WEST, 0.0F, x, x, y);
      }
   }

   private static void addStraightDenseCableSizedCube(Direction facing, CubeBuilder cubeBuilder) {
      switch (facing) {
         case DOWN:
         case UP:
            cubeBuilder.setUvRotation(Direction.EAST, 2);
            cubeBuilder.addCube(3.0F, -0.01F, 3.0F, 13.0F, 16.01F, 13.0F);
            cubeBuilder.setUvRotation(Direction.EAST, 0);
            break;
         case EAST:
         case WEST:
            cubeBuilder.setUvRotation(Direction.SOUTH, 2);
            cubeBuilder.setUvRotation(Direction.NORTH, 2);
            cubeBuilder.addCube(-0.01F, 3.0F, 3.0F, 16.01F, 13.0F, 13.0F);
            cubeBuilder.setUvRotation(Direction.SOUTH, 0);
            cubeBuilder.setUvRotation(Direction.NORTH, 0);
            break;
         case NORTH:
         case SOUTH:
            cubeBuilder.setUvRotation(Direction.EAST, 2);
            cubeBuilder.setUvRotation(Direction.WEST, 2);
            cubeBuilder.addCube(3.0F, 3.0F, -0.01F, 13.0F, 13.0F, 16.01F);
            cubeBuilder.setUvRotation(Direction.EAST, 0);
            cubeBuilder.setUvRotation(Direction.WEST, 0);
      }
   }

   private static void addDenseCableSizedCube(Direction facing, CubeBuilder cubeBuilder) {
      switch (facing) {
         case DOWN:
            cubeBuilder.addCube(4.0F, 0.0F, 4.0F, 12.0F, 5.0F, 12.0F);
            break;
         case UP:
            cubeBuilder.addCube(4.0F, 11.0F, 4.0F, 12.0F, 16.0F, 12.0F);
            break;
         case EAST:
            cubeBuilder.addCube(11.0F, 4.0F, 4.0F, 16.0F, 12.0F, 12.0F);
            break;
         case WEST:
            cubeBuilder.addCube(0.0F, 4.0F, 4.0F, 5.0F, 12.0F, 12.0F);
            break;
         case NORTH:
            cubeBuilder.addCube(4.0F, 4.0F, 0.0F, 12.0F, 12.0F, 5.0F);
            break;
         case SOUTH:
            cubeBuilder.addCube(4.0F, 4.0F, 11.0F, 12.0F, 12.0F, 16.0F);
      }
   }

   private static void addCoveredCableSizedCube(Direction facing, CubeBuilder cubeBuilder) {
      switch (facing) {
         case DOWN:
            cubeBuilder.addCube(6.0F, 0.0F, 6.0F, 10.0F, 5.0F, 10.0F);
            break;
         case UP:
            cubeBuilder.addCube(6.0F, 11.0F, 6.0F, 10.0F, 16.0F, 10.0F);
            break;
         case EAST:
            cubeBuilder.addCube(11.0F, 6.0F, 6.0F, 16.0F, 10.0F, 10.0F);
            break;
         case WEST:
            cubeBuilder.addCube(0.0F, 6.0F, 6.0F, 5.0F, 10.0F, 10.0F);
            break;
         case NORTH:
            cubeBuilder.addCube(6.0F, 6.0F, 0.0F, 10.0F, 10.0F, 5.0F);
            break;
         case SOUTH:
            cubeBuilder.addCube(6.0F, 6.0F, 11.0F, 10.0F, 10.0F, 16.0F);
      }
   }

   private static void addBigCoveredCableSizedCube(Direction facing, CubeBuilder cubeBuilder) {
      switch (facing) {
         case DOWN:
            cubeBuilder.addCube(5.0F, 0.0F, 5.0F, 11.0F, 4.0F, 11.0F);
            break;
         case UP:
            cubeBuilder.addCube(5.0F, 12.0F, 5.0F, 11.0F, 16.0F, 11.0F);
            break;
         case EAST:
            cubeBuilder.addCube(12.0F, 5.0F, 5.0F, 16.0F, 11.0F, 11.0F);
            break;
         case WEST:
            cubeBuilder.addCube(0.0F, 5.0F, 5.0F, 4.0F, 11.0F, 11.0F);
            break;
         case NORTH:
            cubeBuilder.addCube(5.0F, 5.0F, 0.0F, 11.0F, 11.0F, 4.0F);
            break;
         case SOUTH:
            cubeBuilder.addCube(5.0F, 5.0F, 12.0F, 11.0F, 11.0F, 16.0F);
      }
   }
}
