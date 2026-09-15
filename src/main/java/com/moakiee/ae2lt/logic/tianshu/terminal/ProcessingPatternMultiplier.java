package com.moakiee.ae2lt.logic.tianshu.terminal;

import appeng.api.stacks.GenericStack;
import appeng.util.ConfigInventory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ProcessingPatternMultiplier {
   public static boolean apply(ConfigInventory inputs, ConfigInventory outputs, int factor) {
      if (inputs != null && outputs != null && factor != 0 && factor != 1 && factor != -1) {
         List<GenericStack> inputResult = scaled(snapshot(inputs), factor);
         List<GenericStack> outputResult = scaled(snapshot(outputs), factor);
         if (inputResult != null && outputResult != null) {
            write(inputs, inputResult);
            write(outputs, outputResult);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static List<GenericStack> scaled(List<GenericStack> stacks, int factor) {
      if (stacks != null && factor != 0 && factor != 1 && factor != -1) {
         boolean divide = factor < 0;
         long amountFactor = Math.abs((long)factor);
         ArrayList<GenericStack> result = new ArrayList<>(stacks.size());

         for (GenericStack stack : stacks) {
            if (stack != null) {
               long amount = stack.amount();
               long scaled;
               if (divide) {
                  if (amount <= 0L || amount % amountFactor != 0L) {
                     return null;
                  }

                  scaled = amount / amountFactor;
               } else {
                  if (amount <= 0L || amount > Long.MAX_VALUE / amountFactor) {
                     return null;
                  }

                  scaled = amount * amountFactor;
               }

               if (scaled <= 0L) {
                  return null;
               }

               result.add(new GenericStack(stack.what(), scaled));
            } else {
               result.add(null);
            }
         }

         return Collections.unmodifiableList(result);
      } else {
         return null;
      }
   }

   private static List<GenericStack> snapshot(ConfigInventory inventory) {
      ArrayList<GenericStack> result = new ArrayList<>(inventory.size());

      for (int i = 0; i < inventory.size(); i++) {
         result.add(inventory.getStack(i));
      }

      return result;
   }

   private static void write(ConfigInventory inventory, List<GenericStack> values) {
      inventory.beginBatch();

      try {
         for (int i = 0; i < values.size(); i++) {
            inventory.setStack(i, values.get(i));
         }
      } finally {
         inventory.endBatch();
      }
   }

   private ProcessingPatternMultiplier() {
   }
}
