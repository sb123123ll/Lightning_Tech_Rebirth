package com.moakiee.ae2lt.logic.tianshu.loop;

import appeng.api.stacks.AEItemKey;
import com.moakiee.ae2lt.overload.runtime.pattern.SourcePatternSnapshot;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;

public final class ClosedLoopPatternIdentity {
   private static final String DOMAIN = "ae2lt:closed-loop:";

   public static UUID runtimeGroupId(AEItemKey definition, RegistryAccess registries) {
      if (definition != null && registries != null) {
         String fingerprint = SourcePatternSnapshot.fromItemStack(definition.toStack()).fingerprint();
         return UUID.nameUUIDFromBytes(("ae2lt:closed-loop:" + fingerprint).getBytes(StandardCharsets.UTF_8));
      } else {
         throw new IllegalArgumentException("closed-loop definition and registries are required");
      }
   }

   private ClosedLoopPatternIdentity() {
   }
}
