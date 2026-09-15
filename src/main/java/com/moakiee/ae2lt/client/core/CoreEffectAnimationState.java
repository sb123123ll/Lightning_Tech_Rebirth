package com.moakiee.ae2lt.client.core;

final class CoreEffectAnimationState {
   private static final double RESPONSE_PER_SECOND = 6.0;
   private static final double AMBIENT_PERIOD_SECONDS = Math.PI * 4;
   private double lastRenderTick = Double.NaN;
   private double activity;
   private double primaryPhase;
   private double secondaryPhase;
   private double glowPhase;
   private double ambientTime;

   CoreEffectAnimationState.Sample sample(double renderTick, boolean working, CoreEffectAnimationState.MotionProfile profile) {
      double targetActivity = working ? 1.0 : 0.0;
      if (Double.isNaN(this.lastRenderTick)) {
         double worldSeconds = renderTick / 20.0;
         this.activity = targetActivity;
         this.primaryPhase = wrap(worldSeconds * rate(profile.primaryIdleRate(), profile.primaryWorkingRate(), this.activity), profile.primaryPeriod());
         this.secondaryPhase = wrap(worldSeconds * rate(profile.secondaryIdleRate(), profile.secondaryWorkingRate(), this.activity), profile.secondaryPeriod());
         this.glowPhase = wrap(worldSeconds * rate(profile.glowIdleRate(), profile.glowWorkingRate(), this.activity), Math.PI * 2);
         this.ambientTime = wrap(worldSeconds, Math.PI * 4);
         this.lastRenderTick = renderTick;
         return this.snapshot();
      } else {
         double elapsedSeconds = (renderTick - this.lastRenderTick) / 20.0;
         this.lastRenderTick = renderTick;
         if (elapsedSeconds <= 0.0) {
            return this.snapshot();
         } else {
            double previousActivity = this.activity;
            double decay = Math.exp(-6.0 * elapsedSeconds);
            this.activity = targetActivity + (previousActivity - targetActivity) * decay;
            double averageActivity = targetActivity + (previousActivity - targetActivity) * (1.0 - decay) / (6.0 * elapsedSeconds);
            this.primaryPhase = advance(
               this.primaryPhase, elapsedSeconds * rate(profile.primaryIdleRate(), profile.primaryWorkingRate(), averageActivity), profile.primaryPeriod()
            );
            this.secondaryPhase = advance(
               this.secondaryPhase,
               elapsedSeconds * rate(profile.secondaryIdleRate(), profile.secondaryWorkingRate(), averageActivity),
               profile.secondaryPeriod()
            );
            this.glowPhase = advance(this.glowPhase, elapsedSeconds * rate(profile.glowIdleRate(), profile.glowWorkingRate(), averageActivity), Math.PI * 2);
            this.ambientTime = advance(this.ambientTime, elapsedSeconds, Math.PI * 4);
            return this.snapshot();
         }
      }
   }

   private CoreEffectAnimationState.Sample snapshot() {
      return new CoreEffectAnimationState.Sample(this.activity, this.primaryPhase, this.secondaryPhase, this.glowPhase, this.ambientTime);
   }

   private static double rate(double idleRate, double workingRate, double activity) {
      return idleRate + (workingRate - idleRate) * activity;
   }

   private static double advance(double phase, double amount, double period) {
      return wrap(phase + amount, period);
   }

   private static double wrap(double value, double period) {
      double wrapped = value % period;
      return wrapped < 0.0 ? wrapped + period : wrapped;
   }

   static record MotionProfile(
      double primaryIdleRate,
      double primaryWorkingRate,
      double primaryPeriod,
      double secondaryIdleRate,
      double secondaryWorkingRate,
      double secondaryPeriod,
      double glowIdleRate,
      double glowWorkingRate
   ) {
   }

   static record Sample(double activity, double primaryPhase, double secondaryPhase, double glowPhase, double ambientTime) {
   }
}
