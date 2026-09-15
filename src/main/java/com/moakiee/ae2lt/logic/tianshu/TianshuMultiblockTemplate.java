package com.moakiee.ae2lt.logic.tianshu;

import net.minecraft.core.BlockPos;

public final class TianshuMultiblockTemplate {
   public static final int SIZE = 7;
   public static final BlockPos CONTROLLER = new BlockPos(6, 0, 3);
   public static final BlockPos LOWER_PORT = new BlockPos(3, 0, 3);
   public static final BlockPos UPPER_PORT = new BlockPos(3, 6, 3);

   public static TianshuMultiblockRole roleAt(BlockPos pos) {
      int x = pos.m_123341_();
      int y = pos.m_123342_();
      int z = pos.m_123343_();
      if (!inside(x) || !inside(y) || !inside(z)) {
         return TianshuMultiblockRole.IGNORED;
      } else if (pos.equals(CONTROLLER)) {
         return TianshuMultiblockRole.CONTROLLER;
      } else if (pos.equals(LOWER_PORT) || pos.equals(UPPER_PORT)) {
         return TianshuMultiblockRole.PORT_CANDIDATE;
      } else if (betweenTwoAndFour(x) && betweenTwoAndFour(y) && betweenTwoAndFour(z)) {
         return TianshuMultiblockRole.CORE_RESERVED;
      } else if (!betweenOneAndFive(x) || !betweenOneAndFive(y) || !betweenOneAndFive(z) || x != 1 && x != 5 && y != 1 && y != 5 && z != 1 && z != 5) {
         if ((y == 0 || y == 6) && betweenTwoAndFour(x) && betweenTwoAndFour(z)) {
            return TianshuMultiblockRole.COOLING;
         } else {
            int boundaryCount = boundary(x) + boundary(y) + boundary(z);
            if (boundaryCount >= 2) {
               return TianshuMultiblockRole.CASING;
            } else {
               return y != 0 && y != 6 ? TianshuMultiblockRole.IGNORED : TianshuMultiblockRole.CASING;
            }
         }
      } else {
         return TianshuMultiblockRole.GLASS;
      }
   }

   private static int boundary(int value) {
      return value != 0 && value != 6 ? 0 : 1;
   }

   private static boolean inside(int value) {
      return value >= 0 && value < 7;
   }

   private static boolean betweenOneAndFive(int value) {
      return value >= 1 && value <= 5;
   }

   private static boolean betweenTwoAndFour(int value) {
      return value >= 2 && value <= 4;
   }

   private TianshuMultiblockTemplate() {
   }
}
