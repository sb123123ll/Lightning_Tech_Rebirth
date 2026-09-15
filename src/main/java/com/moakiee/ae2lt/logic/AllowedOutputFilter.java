package com.moakiee.ae2lt.logic;

import appeng.api.stacks.AEKey;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class AllowedOutputFilter {
   private final Set<AEKey> strictOutputs = new LinkedHashSet<>();
   private final Set<AEKey> idOnlyKeys = new LinkedHashSet<>();

   public void allowStrict(AEKey key) {
      Objects.requireNonNull(key, "key");
      this.strictOutputs.add(key);
   }

   public void allowIdOnly(AEKey key) {
      Objects.requireNonNull(key, "key");
      this.idOnlyKeys.add(key.dropSecondary());
   }

   public boolean isEmpty() {
      return this.strictOutputs.isEmpty() && this.idOnlyKeys.isEmpty();
   }

   public boolean matches(AEKey key) {
      Objects.requireNonNull(key, "key");
      return this.strictOutputs.contains(key) ? true : this.idOnlyKeys.contains(key.dropSecondary());
   }

   @Override
   public String toString() {
      return "AllowedOutputFilter[strict=" + this.strictOutputs + ", idOnly=" + this.idOnlyKeys + "]";
   }
}
