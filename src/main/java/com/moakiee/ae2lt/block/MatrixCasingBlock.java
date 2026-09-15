package com.moakiee.ae2lt.block;

import com.moakiee.ae2lt.logic.craft.MatrixMultiblockComponent;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class MatrixCasingBlock extends MatrixFormedBlock {
   public static final BooleanProperty FORMED = MatrixFormedBlock.FORMED;

   public MatrixCasingBlock(Properties properties, MatrixMultiblockComponent component) {
      super(properties, component);
   }
}
