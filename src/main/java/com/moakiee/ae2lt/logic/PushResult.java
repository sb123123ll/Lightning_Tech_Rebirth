package com.moakiee.ae2lt.logic;

import appeng.api.stacks.GenericStack;
import java.util.List;

public record PushResult(int acceptedCopies, List<GenericStack> overflow) {
   public static final PushResult REJECTED = new PushResult(0, List.of());
}
