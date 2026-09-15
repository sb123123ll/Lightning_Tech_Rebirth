package com.moakiee.ae2lt.registry;

import com.moakiee.ae2lt.entity.FloatingMatterEntity;
import com.moakiee.ae2lt.entity.OverloadTntEntity;
import com.moakiee.ae2lt.entity.RitualHyperdimensionalPigmeeEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
   public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.f_256939_, "ae2lt");
   public static final RegistryObject<EntityType<OverloadTntEntity>> OVERLOAD_TNT = ENTITY_TYPES.register(
      "overload_tnt",
      () -> Builder.m_20704_(OverloadTntEntity::new, MobCategory.MISC).m_20699_(0.98F, 0.98F).m_20719_().m_20702_(10).m_20717_(10).m_20712_("overload_tnt")
   );
   public static final RegistryObject<EntityType<FloatingMatterEntity>> FLOATING_MATTER = ENTITY_TYPES.register(
      "floating_matter",
      () -> Builder.m_20704_(FloatingMatterEntity::new, MobCategory.MISC).m_20699_(0.25F, 0.25F).m_20702_(6).m_20717_(20).m_20712_("floating_matter")
   );
   public static final RegistryObject<EntityType<RitualHyperdimensionalPigmeeEntity>> RITUAL_HYPERDIMENSIONAL_PIGMEE = ENTITY_TYPES.register(
      "ritual_hyperdimensional_pigmee",
      () -> Builder.m_20704_(RitualHyperdimensionalPigmeeEntity::new, MobCategory.MISC)
            .m_20699_(0.25F, 0.25F)
            .m_20702_(10)
            .m_20717_(10)
            .m_20712_("ritual_hyperdimensional_pigmee")
   );

   private ModEntities() {
   }
}
