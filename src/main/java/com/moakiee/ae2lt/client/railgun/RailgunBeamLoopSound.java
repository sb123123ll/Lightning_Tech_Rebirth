package com.moakiee.ae2lt.client.railgun;

import com.moakiee.ae2lt.item.railgun.ElectromagneticRailgunItem;
import com.moakiee.ae2lt.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public final class RailgunBeamLoopSound extends AbstractTickableSoundInstance {
   private static final int FADE_TICKS = 8;
   private static final float TARGET_VOLUME = 0.85F;
   private final LocalPlayer player;
   private boolean active = true;
   private int fadeIn = 0;
   private int fadeOut = -1;

   public RailgunBeamLoopSound(LocalPlayer player) {
      super((SoundEvent)ModSounds.RAILGUN_BEAM_LOOP.get(), SoundSource.PLAYERS, RandomSource.m_216327_());
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
         this.fadeOut = 8;
      }
   }

   public boolean m_7784_() {
      return true;
   }

   public void m_7788_() {
      Minecraft mc = Minecraft.m_91087_();
      if (!this.player.m_213877_() && this.player.m_6084_() && mc.f_91073_ != null) {
         ItemStack main = this.player.m_21205_();
         if (!(main.m_41720_() instanceof ElectromagneticRailgunItem)) {
            this.requestStop();
         }

         this.f_119575_ = this.player.m_20185_();
         this.f_119576_ = this.player.m_20186_() + (double)this.player.m_20192_() * 0.5;
         this.f_119577_ = this.player.m_20189_();
         if (this.active) {
            if (this.fadeIn < 8) {
               this.fadeIn++;
            }

            this.f_119573_ = 0.85F * ((float)this.fadeIn / 8.0F);
         } else if (this.fadeOut > 0) {
            this.fadeOut--;
            this.f_119573_ = 0.85F * ((float)this.fadeOut / 8.0F);
         } else {
            this.f_119573_ = 0.0F;
            this.m_119609_();
         }
      } else {
         this.m_119609_();
      }
   }
}
