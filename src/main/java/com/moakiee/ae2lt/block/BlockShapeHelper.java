package com.moakiee.ae2lt.block;

import java.util.EnumMap;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

final class BlockShapeHelper {
   private BlockShapeHelper() {
   }

   static VoxelShape or(VoxelShape... shapes) {
      VoxelShape result = Shapes.m_83040_();

      for (VoxelShape shape : shapes) {
         result = Shapes.m_83110_(result, shape);
      }

      return result;
   }

   static EnumMap<Direction, VoxelShape> createAllFacingShapes(VoxelShape upShape) {
      EnumMap<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

      for (Direction direction : Direction.values()) {
         shapes.put(direction, rotateFromUp(upShape, direction));
      }

      return shapes;
   }

   static EnumMap<Direction, VoxelShape> createHorizontalFacingShapes(VoxelShape northShape) {
      EnumMap<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

      for (Direction direction : Plane.HORIZONTAL) {
         shapes.put(direction, rotateFromNorth(northShape, direction));
      }

      return shapes;
   }

   private static VoxelShape rotateFromUp(VoxelShape shape, Direction direction) {
      if (direction == Direction.UP) {
         return shape;
      } else {
         VoxelShape[] rotated = new VoxelShape[]{Shapes.m_83040_()};
         shape.m_83286_((minX, minY, minZ, maxX, maxY, maxZ) -> {
            rotated[0] = Shapes.m_83110_(rotated[0], switch (direction) {
               case EAST -> Shapes.m_83048_(minY, minZ, minX, maxY, maxZ, maxX);
               case SOUTH -> Shapes.m_83048_(1.0 - maxX, minZ, minY, 1.0 - minX, maxZ, maxY);
               case WEST -> Shapes.m_83048_(1.0 - maxY, minZ, 1.0 - maxX, 1.0 - minY, maxZ, 1.0 - minX);
               case NORTH -> Shapes.m_83048_(minX, minZ, 1.0 - maxY, maxX, maxZ, 1.0 - minY);
               case DOWN -> Shapes.m_83048_(minX, 1.0 - maxY, 1.0 - maxZ, maxX, 1.0 - minY, 1.0 - minZ);
               case UP -> Shapes.m_83048_(minX, minY, minZ, maxX, maxY, maxZ);
               default -> throw new IncompatibleClassChangeError();
            });
         });
         return rotated[0];
      }
   }

   private static VoxelShape rotateFromNorth(VoxelShape shape, Direction direction) {
      if (direction == Direction.NORTH) {
         return shape;
      } else {
         VoxelShape[] rotated = new VoxelShape[]{Shapes.m_83040_()};
         shape.m_83286_((minX, minY, minZ, maxX, maxY, maxZ) -> {
            rotated[0] = Shapes.m_83110_(rotated[0], switch (direction) {
               case EAST -> Shapes.m_83048_(1.0 - maxZ, minY, minX, 1.0 - minZ, maxY, maxX);
               case SOUTH -> Shapes.m_83048_(1.0 - maxX, minY, 1.0 - maxZ, 1.0 - minX, maxY, 1.0 - minZ);
               case WEST -> Shapes.m_83048_(minZ, minY, 1.0 - maxX, maxZ, maxY, 1.0 - minX);
               case NORTH -> Shapes.m_83048_(minX, minY, minZ, maxX, maxY, maxZ);
               default -> throw new IllegalArgumentException("Direction must be horizontal: " + direction);
            });
         });
         return rotated[0];
      }
   }
}
