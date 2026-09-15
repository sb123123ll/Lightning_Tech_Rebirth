package com.moakiee.ae2lt.logic.tianshu.loop;

public record ClosedLoopValidationResult(ClosedLoopValidationResult.Status status, ClosedLoopAnalysis analysis) {
   public boolean valid() {
      return this.status == ClosedLoopValidationResult.Status.VALID;
   }

   public static enum Status {
      VALID,
      MEMBER_UNDECODABLE,
      MEMBER_IS_CLOSED_LOOP,
      MEMBER_WITHOUT_INPUT_SEED,
      STRUCTURE_CHECK_FAILED,
      DECLARATION_MISMATCH,
      NO_VALID_NET_OUTPUT;
   }
}
