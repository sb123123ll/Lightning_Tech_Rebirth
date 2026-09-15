package com.moakiee.ae2lt.client.ctm;

import java.util.EnumMap;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.joml.Vector3f;

public final class CtmFaceGeometry {
   public static final int UP = 0;
   public static final int RIGHT = 1;
   public static final int DOWN = 2;
   public static final int LEFT = 3;
   private static final EnumMap<Direction, Vector3f[]> CORNERS = buildCorners();
   private static final EnumMap<Direction, Direction[]> NEIGHBORS = buildNeighbors();

   private CtmFaceGeometry() {
   }

   public static Direction neighborDir(Direction face, int edge) {
      return NEIGHBORS.get(face)[edge];
   }

   static BlockPos cornerPos(BlockPos pos, Direction face, CtmTileSelector.Quadrant quadrant) {
      return switch (quadrant) {
         case TOP_LEFT -> pos.m_121945_(neighborDir(face, 0)).m_121945_(neighborDir(face, 3));
         case TOP_RIGHT -> pos.m_121945_(neighborDir(face, 0)).m_121945_(neighborDir(face, 1));
         case BOTTOM_RIGHT -> pos.m_121945_(neighborDir(face, 2)).m_121945_(neighborDir(face, 1));
         case BOTTOM_LEFT -> pos.m_121945_(neighborDir(face, 2)).m_121945_(neighborDir(face, 3));
      };
   }

   public static BakedQuad fullFace(Direction side, TextureAtlasSprite sprite) {
      return fullFace(side, sprite, 0.0F);
   }

   public static BakedQuad fullFace(Direction side, TextureAtlasSprite sprite, float offset) {
      Vector3f[] c = CORNERS.get(side);
      Vec3i n = side.m_122436_();
      return quad(side, offset(c[0], n, offset), offset(c[1], n, offset), offset(c[2], n, offset), offset(c[3], n, offset), sprite, 0.0F, 0.0F, 1.0F, 1.0F);
   }

   static BakedQuad quadrant(Direction side, int sq, int tq, CtmTileSelector.Tile tile, TextureAtlasSprite sprite) {
      Vector3f[] c = CORNERS.get(side);
      float s0 = (float)sq * 0.5F;
      float s1 = s0 + 0.5F;
      float t0 = (float)tq * 0.5F;
      float t1 = t0 + 0.5F;
      Vector3f tl = lerp(c, s0, t0);
      Vector3f bl = lerp(c, s0, t1);
      Vector3f br = lerp(c, s1, t1);
      Vector3f tr = lerp(c, s1, t0);
      float step = 1.0F / (float)tile.source().gridSize();
      float u0 = (float)tile.x() * step;
      float u1 = u0 + step;
      float v0 = (float)tile.y() * step;
      float v1 = v0 + step;
      return quad(side, tl, bl, br, tr, sprite, u0, v0, u1, v1);
   }

   private static Vector3f lerp(Vector3f[] c, float s, float t) {
      Vector3f tl = c[0];
      Vector3f bl = c[1];
      Vector3f tr = c[3];
      return new Vector3f(
         tl.x + s * (tr.x - tl.x) + t * (bl.x - tl.x), tl.y + s * (tr.y - tl.y) + t * (bl.y - tl.y), tl.z + s * (tr.z - tl.z) + t * (bl.z - tl.z)
      );
   }

   private static Vector3f offset(Vector3f point, Vec3i normal, float amount) {
      return new Vector3f(
         point.x + (float)normal.m_123341_() * amount, point.y + (float)normal.m_123342_() * amount, point.z + (float)normal.m_123343_() * amount
      );
   }

   private static BakedQuad quad(
      Direction side, Vector3f tl, Vector3f bl, Vector3f br, Vector3f tr, TextureAtlasSprite sprite, float u0, float v0, float u1, float v1
   ) {
      BakedQuad[] holder = new BakedQuad[1];
      QuadBakingVertexConsumer builder = new QuadBakingVertexConsumer(quad -> holder[0] = quad);
      builder.setSprite(sprite);
      builder.setDirection(side);
      builder.setShade(true);
      Vec3i n = side.m_122436_();
      putVertex(builder, n, tl, sprite, u0, v0);
      putVertex(builder, n, bl, sprite, u0, v1);
      putVertex(builder, n, br, sprite, u1, v1);
      putVertex(builder, n, tr, sprite, u1, v0);
      return holder[0];
   }

   private static void putVertex(QuadBakingVertexConsumer builder, Vec3i normal, Vector3f pos, TextureAtlasSprite sprite, float u, float v) {
      builder.m_5483_((double)pos.x, (double)pos.y, (double)pos.z);
      builder.m_6122_(255, 255, 255, 255);
      builder.m_5601_((float)normal.m_123341_(), (float)normal.m_123342_(), (float)normal.m_123343_());
      builder.m_7421_(sprite.m_118367_((double)(u * 16.0F)), sprite.m_118393_((double)(v * 16.0F)));
      builder.m_5752_();
   }

   private static EnumMap<Direction, Vector3f[]> buildCorners() {
      EnumMap<Direction, Vector3f[]> map = new EnumMap<>(Direction.class);

      for (Direction facing : Direction.values()) {
         float o = facing.m_122421_() == AxisDirection.NEGATIVE ? 0.0F : 1.0F;

         Vector3f[] corners = switch (facing.m_122434_()) {
            case X -> new Vector3f[]{new Vector3f(o, 1.0F, 1.0F), new Vector3f(o, 0.0F, 1.0F), new Vector3f(o, 0.0F, 0.0F), new Vector3f(o, 1.0F, 0.0F)};
            case Y -> new Vector3f[]{new Vector3f(1.0F, o, 1.0F), new Vector3f(1.0F, o, 0.0F), new Vector3f(0.0F, o, 0.0F), new Vector3f(0.0F, o, 1.0F)};
            case Z -> new Vector3f[]{new Vector3f(0.0F, 1.0F, o), new Vector3f(0.0F, 0.0F, o), new Vector3f(1.0F, 0.0F, o), new Vector3f(1.0F, 1.0F, o)};
            default -> throw new IncompatibleClassChangeError();
         };
         if (facing.m_122421_() == AxisDirection.NEGATIVE) {
            corners = new Vector3f[]{corners[3], corners[2], corners[1], corners[0]};
         }

         map.put((Enum)facing, corners);
      }

      return map;
   }

   private static EnumMap<Direction, Direction[]> buildNeighbors() {
      EnumMap<Direction, Direction[]> map = new EnumMap<>(Direction.class);
      map.put(Direction.DOWN, new Direction[]{Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.WEST});
      map.put(Direction.UP, new Direction[]{Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST});
      map.put(Direction.NORTH, new Direction[]{Direction.UP, Direction.WEST, Direction.DOWN, Direction.EAST});
      map.put(Direction.SOUTH, new Direction[]{Direction.UP, Direction.EAST, Direction.DOWN, Direction.WEST});
      map.put(Direction.WEST, new Direction[]{Direction.UP, Direction.SOUTH, Direction.DOWN, Direction.NORTH});
      map.put(Direction.EAST, new Direction[]{Direction.UP, Direction.NORTH, Direction.DOWN, Direction.SOUTH});
      return map;
   }
}
