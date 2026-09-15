package com.moakiee.ae2lt.logic.craft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

public final class MatrixMultiblockTemplate {
   public static final int SIZE_X = 7;
   public static final int SIZE_Y = 11;
   public static final int SIZE_Z = 7;
   public static final BlockPos CONTROLLER_LOCAL = new BlockPos(0, 5, 3);
   public static final BlockPos CRAFTING_CENTER_LOCAL = new BlockPos(3, 5, 3);
   public static final int CRAFTING_SLOT_COUNT = 81;
   public static final int PATTERN_BAY_SLOT_COUNT = 50;
   public static final int PORT_CANDIDATE_COUNT = 3;
   private static final String[][] LAYERS = new String[][]{
      {"SSSSSSS", "SSSSSSS", "SSSSSSS", "SSSSSSS", "SSSSSSS", "SSSSSSS", "SSSSSSS"},
      {"SPPPPPS", "PTTTTTP", "PTTTTTP", "PTTTTTP", "PTTTTTP", "PTTTTTP", "SPPPPPS"},
      {"SSSSSSS", "SPPPPPS", "SPPPPPS", "SPPPPPS", "SPPPPPS", "SPPPPPS", "SSSSSSS"},
      {"S.....S", ".GGGGG.", ".GHHHG.", ".GHHHG.", ".GHHHG.", ".GGGGG.", "S.....S"},
      {"S.PPP.S", ".GHHHG.", "PHHHHHP", "PHHHHHP", "PHHHHHP", ".GHHHG.", "S.PPP.S"},
      {"SPPIPPS", "PGHHHGP", "PHHHHHP", "CHHHHHI", "PHHHHHP", "PGHHHGP", "SPPIPPS"},
      {"S.PPP.S", ".GHHHG.", "PHHHHHP", "PHHHHHP", "PHHHHHP", ".GHHHG.", "S.PPP.S"},
      {"S.....S", ".GGGGG.", ".GHHHG.", ".GHHHG.", ".GHHHG.", ".GGGGG.", "S.....S"},
      {"SSSSSSS", "SPPPPPS", "SPPPPPS", "SPPPPPS", "SPPPPPS", "SPPPPPS", "SSSSSSS"},
      {"SPPPPPS", "PTTTTTP", "PTTTTTP", "PTTTTTP", "PTTTTTP", "PTTTTTP", "SPPPPPS"},
      {"SSSSSSS", "SSSSSSS", "SSSSSSS", "SSSSSSS", "SSSSSSS", "SSSSSSS", "SSSSSSS"}
   };
   private static final List<MatrixMultiblockTemplate.Entry> ENTRIES = createEntries();

   private MatrixMultiblockTemplate() {
   }

   public static MatrixMultiblockRole roleAt(BlockPos localPos) {
      return roleAt(localPos.m_123341_(), localPos.m_123342_(), localPos.m_123343_());
   }

   public static MatrixMultiblockRole roleAt(int x, int y, int z) {
      return x >= 0 && x < 7 && y >= 0 && y < 11 && z >= 0 && z < 7 ? MatrixMultiblockRole.fromTemplateKey(LAYERS[y][z].charAt(x)) : MatrixMultiblockRole.EMPTY;
   }

   public static List<MatrixMultiblockTemplate.Entry> entries() {
      return ENTRIES;
   }

   private static List<MatrixMultiblockTemplate.Entry> createEntries() {
      ArrayList<MatrixMultiblockTemplate.Entry> entries = new ArrayList<>(539);

      for (int y = 0; y < 11; y++) {
         for (int z = 0; z < 7; z++) {
            String row = LAYERS[y][z];
            if (row.length() != 7) {
               throw new IllegalStateException("Invalid matrix template row at y=" + y + ", z=" + z);
            }

            for (int x = 0; x < 7; x++) {
               entries.add(new MatrixMultiblockTemplate.Entry(new BlockPos(x, y, z), MatrixMultiblockRole.fromTemplateKey(row.charAt(x))));
            }
         }
      }

      return List.copyOf(entries);
   }

   public static record Entry(BlockPos localPos, MatrixMultiblockRole role) {
   }
}
