package com.moakiee.ae2lt.logic;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderTarget;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public interface MachineAdapter {
   boolean supports(ServerLevel var1, BlockPos var2);

   boolean canAccept(ServerLevel var1, BlockPos var2, Direction var3, IPatternDetails var4);

   default boolean canAccept(ServerLevel level, BlockPos pos, Direction face, IPatternDetails pattern, @Nullable PatternProviderTarget cachedTarget) {
      return this.canAccept(level, pos, face, pattern);
   }

   default boolean supportsBatch(ServerLevel level, BlockPos pos, Direction face, IPatternDetails pattern) {
      return true;
   }

   PushResult pushCopies(
      ServerLevel var1,
      BlockPos var2,
      Direction var3,
      IPatternDetails var4,
      KeyCounter[] var5,
      int var6,
      boolean var7,
      Set<AEKey> var8,
      IActionSource var9,
      @Nullable PatternProviderTarget var10
   );

   default PushResult pushCopies(
      ServerLevel level,
      BlockPos pos,
      Direction face,
      IPatternDetails pattern,
      KeyCounter[] inputs,
      int maxCopies,
      PatternInputAcceptance inputAcceptance,
      boolean blocking,
      Set<AEKey> patternInputs,
      IActionSource source,
      @Nullable PatternProviderTarget cachedTarget
   ) {
      return this.pushCopies(level, pos, face, pattern, inputs, maxCopies, blocking, patternInputs, source, cachedTarget);
   }

   default boolean flushOverflow(
      ServerLevel level, BlockPos pos, Direction face, List<GenericStack> overflow, IActionSource source, @Nullable PatternProviderTarget cachedTarget
   ) {
      if (!level.m_46749_(pos)) {
         return false;
      } else {
         PatternProviderTarget target = cachedTarget;
         if (cachedTarget == null) {
            BlockEntity be = level.m_7702_(pos);
            target = PatternProviderTarget.get(level, pos, be, face, source);
         }

         if (target == null) {
            return false;
         } else {
            ListIterator<GenericStack> it = overflow.listIterator();

            while (it.hasNext()) {
               GenericStack stack = it.next();
               long inserted = target.insert(stack.what(), stack.amount(), Actionable.MODULATE);
               if (inserted >= stack.amount()) {
                  it.remove();
               } else if (inserted > 0L) {
                  it.set(new GenericStack(stack.what(), stack.amount() - inserted));
               }
            }

            return overflow.isEmpty();
         }
      }
   }

   default OutputReturnResult extractOutputs(
      ServerLevel level, BlockPos pos, Direction face, AllowedOutputFilter allowedOutputs, IActionSource source, MachineAdapter.OutputSink sink
   ) {
      return OutputReturnResult.UNAVAILABLE;
   }

   public interface OutputSink {
      long maxAccept(AEKey var1, long var2);

      long accept(AEKey var1, long var2);

      void acceptOverflow(AEKey var1, long var2);
   }
}
