package com.moakiee.ae2lt.celestweave.phase;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

public record PhaseLockProjectionLink(UUID armorId, long update) {
   public static final Codec<PhaseLockProjectionLink> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               UUIDUtil.f_235867_.fieldOf("armor_id").forGetter(PhaseLockProjectionLink::armorId),
               Codec.LONG.fieldOf("update").forGetter(PhaseLockProjectionLink::update)
            )
            .apply(instance, PhaseLockProjectionLink::new)
   );
}
