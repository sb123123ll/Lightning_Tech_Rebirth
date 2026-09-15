package com.moakiee.ae2lt.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

public final class MachineAdapterRegistry {
   private static final List<MachineAdapter> ADAPTERS = new ArrayList<>();

   private MachineAdapterRegistry() {
   }

   public static void register(MachineAdapter adapter) {
      ADAPTERS.add(0, adapter);
   }

   @Nullable
   public static MachineAdapter find(ServerLevel level, BlockPos pos) {
      for (MachineAdapter adapter : ADAPTERS) {
         if (adapter.supports(level, pos)) {
            return adapter;
         }
      }

      return null;
   }

   public static List<MachineAdapter> getAll() {
      return Collections.unmodifiableList(ADAPTERS);
   }

   public static void init() {
      register(AE2NativeMachineAdapter.INSTANCE);
   }
}
