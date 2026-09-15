package com.moakiee.ae2lt.logic;

import appeng.api.stacks.AEKey;

@FunctionalInterface
interface ReturnSlotFilter {
   boolean isAllowed(int var1, AEKey var2);
}
