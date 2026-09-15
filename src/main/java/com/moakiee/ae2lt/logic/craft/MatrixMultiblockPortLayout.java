package com.moakiee.ae2lt.logic.craft;

import java.util.Objects;
import net.minecraft.core.Direction;

public record MatrixMultiblockPortLayout(Direction controllerFace, Direction interfaceFace) {
   public MatrixMultiblockPortLayout(Direction controllerFace, Direction interfaceFace) {
      Objects.requireNonNull(controllerFace);
      Objects.requireNonNull(interfaceFace);
      this.controllerFace = controllerFace;
      this.interfaceFace = interfaceFace;
   }

   public boolean usesPreferredSameFacePlacement() {
      return this.controllerFace == this.interfaceFace;
   }
}
