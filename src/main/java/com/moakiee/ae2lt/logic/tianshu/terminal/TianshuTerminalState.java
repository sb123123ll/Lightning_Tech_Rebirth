package com.moakiee.ae2lt.logic.tianshu.terminal;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public final class TianshuTerminalState {
   private TianshuEncodingMode encodingMode = TianshuEncodingMode.CRAFTING;
   private boolean maintainableView;
   @Nullable
   private ClosedLoopTerminalDraft closedLoopDraft;
   @Nullable
   private ProcessingPatternTerminalDraft processingDraft;

   public TianshuEncodingMode getEncodingMode() {
      return this.encodingMode;
   }

   public boolean setEncodingMode(@Nullable TianshuEncodingMode mode) {
      if (mode != null && mode != this.encodingMode) {
         this.encodingMode = mode;
         return true;
      } else {
         return false;
      }
   }

   public boolean isMaintainableView() {
      return this.maintainableView;
   }

   public boolean setMaintainableView(boolean enabled) {
      if (this.maintainableView == enabled) {
         return false;
      } else {
         this.maintainableView = enabled;
         return true;
      }
   }

   @Nullable
   public ClosedLoopTerminalDraft getClosedLoopDraft() {
      return this.closedLoopDraft;
   }

   public boolean setClosedLoopDraft(@Nullable ClosedLoopTerminalDraft draft) {
      if (ClosedLoopTerminalDraft.sameState(this.closedLoopDraft, draft)) {
         return false;
      } else {
         this.closedLoopDraft = draft;
         return true;
      }
   }

   @Nullable
   public ProcessingPatternTerminalDraft getProcessingDraft() {
      return this.processingDraft;
   }

   public boolean setProcessingDraft(@Nullable ProcessingPatternTerminalDraft draft) {
      if (ProcessingPatternTerminalDraft.sameState(this.processingDraft, draft)) {
         return false;
      } else {
         this.processingDraft = draft;
         return true;
      }
   }

   public void read(CompoundTag data, TianshuTerminalState.NbtFormat format) {
      try {
         this.encodingMode = TianshuEncodingMode.valueOf(data.m_128461_(format.mode));
      } catch (IllegalArgumentException var4) {
         this.encodingMode = TianshuEncodingMode.CRAFTING;
      }

      this.maintainableView = data.m_128471_(format.view);
      this.closedLoopDraft = data.m_128425_(format.closedLoop, 10) ? ClosedLoopTerminalDraft.read(data.m_128469_(format.closedLoop)) : null;
      this.processingDraft = data.m_128425_(format.processing, 10) ? ProcessingPatternTerminalDraft.read(data.m_128469_(format.processing)) : null;
   }

   public void write(CompoundTag data, TianshuTerminalState.NbtFormat format) {
      data.m_128359_(format.mode, this.encodingMode.name());
      data.m_128379_(format.view, this.maintainableView);
      if (this.closedLoopDraft != null) {
         data.m_128365_(format.closedLoop, this.closedLoopDraft.write());
      } else {
         data.m_128473_(format.closedLoop);
      }

      if (this.processingDraft != null) {
         data.m_128365_(format.processing, this.processingDraft.write());
      } else {
         data.m_128473_(format.processing);
      }
   }

   public static enum NbtFormat {
      PART("TianshuEncodingMode", "MaintainableView", "ClosedLoopDraft", "ProcessingDraft"),
      WIRELESS("tianshuMode", "tianshuMaintainableView", "tianshuClosedLoopDraft", "tianshuProcessingDraft");

      private final String mode;
      private final String view;
      private final String closedLoop;
      private final String processing;

      private NbtFormat(String mode, String view, String closedLoop, String processing) {
         this.mode = mode;
         this.view = view;
         this.closedLoop = closedLoop;
         this.processing = processing;
      }
   }
}
