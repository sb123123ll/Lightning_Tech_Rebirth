package com.moakiee.ae2lt.registry;

import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypes {
   public static final ResourceKey<DamageType> ELECTROMAGNETIC = ResourceKey.m_135785_(Registries.f_268580_, new ResourceLocation("ae2lt", "electromagnetic"));
   private static final ConcurrentHashMap<ServerLevel, Holder<DamageType>> ELECTROMAGNETIC_CACHE = new ConcurrentHashMap<>();

   public static Holder<DamageType> electromagneticHolder(ServerLevel level) {
      return ELECTROMAGNETIC_CACHE.computeIfAbsent(level, l -> l.m_9598_().m_175515_(Registries.f_268580_).m_246971_(ELECTROMAGNETIC));
   }

   public static void clearCache() {
      ELECTROMAGNETIC_CACHE.clear();
   }

   private ModDamageTypes() {
   }
}
