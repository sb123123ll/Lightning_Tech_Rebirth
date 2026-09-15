package com.moakiee.ae2lt.logic.tianshu.loop;

import com.moakiee.ae2lt.blockentity.TianshuSupercomputerPortBlockEntity;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuTerminalAction;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuTerminalCapabilities;

public final class ClosedLoopPatternUploadService {
   public static ClosedLoopPatternRepository.PutResult upload(TianshuSupercomputerPortBlockEntity target, ClosedLoopPatternPayload payload) {
      if (target != null && payload != null) {
         TianshuTerminalCapabilities capabilities = TianshuTerminalCapabilities.forTianshu(target.isFormed(), target.getFunctionProfile());
         if (!capabilities.allows(TianshuTerminalAction.UPLOAD_CLOSED_LOOP_PATTERN)) {
            return ClosedLoopPatternRepository.PutResult.UNAVAILABLE;
         } else if (target.m_58904_() != null && ClosedLoopPatternValidator.validate(payload, target.m_58904_()).valid()) {
            ClosedLoopPatternRepository repository = target.getClosedLoopPatternRepository();
            if (repository == null) {
               return ClosedLoopPatternRepository.PutResult.UNAVAILABLE;
            } else {
               ClosedLoopPatternRepository.PutResult result = repository.add(payload);
               if (result == ClosedLoopPatternRepository.PutResult.ADDED) {
                  target.closedLoopPatternsChanged();
               }

               return result;
            }
         } else {
            return ClosedLoopPatternRepository.PutResult.INVALID;
         }
      } else {
         return ClosedLoopPatternRepository.PutResult.INVALID;
      }
   }

   private ClosedLoopPatternUploadService() {
   }
}
