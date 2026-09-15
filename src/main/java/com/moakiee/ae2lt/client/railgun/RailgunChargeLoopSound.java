package com.moakiee.ae2lt.client.railgun;

import com.moakiee.ae2lt.item.railgun.ElectromagneticRailgunItem;
import com.moakiee.ae2lt.registry.ModDataComponents;
import com.moakiee.ae2lt.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public final class RailgunChargeLoopSound extends AbstractTickableSoundInstance {
   private static final int FADE_TICKS = 4;
   private static final float TARGET_VOLUME = 0.7F;
   private final LocalPlayer player;
   private boolean active = true;
   private int fadeIn = 0;
   private int fadeOut = -1;

   public RailgunChargeLoopSound(LocalPlayer player) {
      super((SoundEvent)ModSounds.RAILGUN_CHARGE_SUSTAIN.get(), SoundSource.PLAYERS, RandomSource.m_216327_());
      this.player = player;
      this.f_119578_ = true;
      this.f_119579_ = 0;
      this.f_119573_ = 0.0F;
      this.f_119574_ = 1.0F;
      this.f_119575_ = player.m_20185_();
      this.f_119576_ = player.m_20186_();
      this.f_119577_ = player.m_20189_();
      this.f_119582_ = false;
      this.f_119580_ = Attenuation.LINEAR;
   }

   public void requestStop() {
      this.active = false;
      if (this.fadeOut < 0) {
         this.fadeOut = 4;
      }
   }

   public boolean m_7784_() {
      return true;
   }

   public void m_7788_() {
      Minecraft mc = Minecraft.m_91087_();
      if (!this.player.m_213877_() && this.player.m_6084_() && mc.f_91073_ != null) {
         ItemStack using = this.player.m_21211_();
         if (!this.player.m_6117_() || !(using.m_41720_() instanceof ElectromagneticRailgunItem)) {
            this.requestStop();
         }

         long chargeTicks = ModDataComponents.RAILGUN_CHARGE_TICKS.getOrDefault(using, 0L);
         if (RailgunChargeSoundPhase.fromChargeTicks(chargeTicks, 40L) == RailgunChargeSoundPhase.RAMP) {
            this.requestStop();
         }

         this.f_119575_ = this.player.m_20185_();
         this.f_119576_ = this.player.m_20186_() + (double)this.player.m_20192_() * 0.5;
         this.f_119577_ = this.player.m_20189_();
         if (this.active) {
            if (this.fadeIn < 4) {
               this.fadeIn++;
            }

            this.f_119573_ = 0.7F * ((float)this.fadeIn / 4.0F);
         } else if (this.fadeOut > 0) {
            this.fadeOut--;
            this.f_119573_ = 0.7F * ((float)this.fadeOut / 4.0F);
         } else {
            this.f_119573_ = 0.0F;
            this.m_119609_();
         }
      } else {
         this.m_119609_();
      }
   }
}
