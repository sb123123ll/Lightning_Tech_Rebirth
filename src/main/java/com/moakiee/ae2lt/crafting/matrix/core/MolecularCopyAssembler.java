package com.moakiee.ae2lt.crafting.matrix.core;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.menu.AutoCraftingMenu;
import com.moakiee.thunderbolt.core.crafting.batch.SharedBatchInputs;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class MolecularCopyAssembler implements CopyAssembler {
   private final Supplier<Level> levelSupplier;

   public MolecularCopyAssembler(Level level) {
      this((Supplier<Level>)(() -> level));
   }

   public MolecularCopyAssembler(Supplier<Level> levelSupplier) {
      this.levelSupplier = Objects.requireNonNull(levelSupplier);
   }

   @Override
   public CopyAssembler.AssembledCopy assembleOneCopy(IPatternDetails details, KeyCounter[] oneCopyInputs) {
      if (!(details instanceof IMolecularAssemblerSupportedPattern pattern)) {
         return null;
      } else {
         Level level = this.levelSupplier.get();
         if (level == null) {
            return null;
         } else {
            HashMap<AEKey, Long> sharedRemaindersLeft = sharedRemainderQuotas(details, oneCopyInputs);
            CraftingContainer input = buildCraftingInput(pattern, oneCopyInputs);
            ItemStack output = pattern.assemble(input, level);
            if (output.m_41619_()) {
               return null;
            } else {
               ArrayList<CopyAssembler.Stack> remainders = new ArrayList<>();
               ArrayList<CopyAssembler.Stack> sharedRemainders = new ArrayList<>();
               NonNullList<ItemStack> remainingItems = pattern.getRemainingItems(input);
               if (remainingItems != null) {
                  for (ItemStack remainder : remainingItems) {
                     if (!remainder.m_41619_()) {
                        AEItemKey key = AEItemKey.of(remainder);
                        long count = (long)remainder.m_41613_();
                        long sharedCount = Math.min(count, Math.max(0L, sharedRemaindersLeft.getOrDefault(key, 0L)));
                        if (sharedCount > 0L) {
                           sharedRemainders.add(new CopyAssembler.Stack(key, sharedCount));
                           sharedRemaindersLeft.put(key, sharedRemaindersLeft.getOrDefault(key, 0L) - sharedCount);
                        }

                        if (count > sharedCount) {
                           remainders.add(new CopyAssembler.Stack(key, count - sharedCount));
                        }
                     }
                  }
               }

               return new CopyAssembler.AssembledCopy(AEItemKey.of(output), (long)output.m_41613_(), List.copyOf(remainders), List.copyOf(sharedRemainders));
            }
         }
      }
   }

   private static HashMap<AEKey, Long> sharedRemainderQuotas(IPatternDetails details, KeyCounter[] oneCopyInputs) {
      HashMap<AEKey, Long> result = new HashMap<>();
      IInput[] inputs = details.getInputs();

      for (int slot = 0; slot < inputs.length && slot < oneCopyInputs.length; slot++) {
         AEKey concreteKey = selectedKey(oneCopyInputs[slot]);
         if (SharedBatchInputs.isSharedInput(details, slot, concreteKey)) {
            AEKey remaining = inputs[slot].getRemainingKey(concreteKey);
            if (remaining != null) {
               result.merge(remaining, inputs[slot].getMultiplier(), MolecularCopyAssembler::saturatingAdd);
            }
         }
      }

      return result;
   }

   private static AEKey selectedKey(KeyCounter input) {
      if (input == null) {
         return null;
      } else {
         for (Entry<AEKey> entry : input) {
            if (entry.getKey() != null && entry.getLongValue() > 0L) {
               return (AEKey)entry.getKey();
            }
         }

         return null;
      }
   }

   private static long saturatingAdd(long left, long right) {
      if (left <= 0L) {
         return Math.max(0L, right);
      } else if (right <= 0L) {
         return left;
      } else {
         return left >= Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
      }
   }

   private static CraftingContainer buildCraftingInput(IMolecularAssemblerSupportedPattern pattern, KeyCounter[] oneCopyInputs) {
      ItemStack[] grid = new ItemStack[9];

      for (int i = 0; i < grid.length; i++) {
         grid[i] = ItemStack.f_41583_;
      }

      pattern.fillCraftingGrid(oneCopyInputs, (slot, stack) -> {
         if (slot >= 0 && slot < grid.length && stack != null) {
            grid[slot] = stack;
         }
      });
      TransientCraftingContainer container = new TransientCraftingContainer(new AutoCraftingMenu(), 3, 3);

      for (int i = 0; i < grid.length; i++) {
         container.m_6836_(i, grid[i]);
      }

      return container;
   }
}
