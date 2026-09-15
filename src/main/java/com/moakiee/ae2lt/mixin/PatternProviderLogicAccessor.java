package com.moakiee.ae2lt.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderReturnInventory;
import appeng.util.inv.AppEngInternalInventory;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(
   value = {PatternProviderLogic.class},
   remap = false
)
public interface PatternProviderLogicAccessor {
   @Invoker("onPushPatternSuccess")
   void invokeOnPushPatternSuccess(IPatternDetails var1);

   @Invoker("doWork")
   boolean invokeDoWork();

   @Invoker("hasWorkToDo")
   boolean invokeHasWorkToDo();

   @Invoker("getActiveSides")
   Set<Direction> invokeGetActiveSides();

   @Accessor("mainNode")
   IManagedGridNode getMainNode();

   @Accessor("patternInventory")
   AppEngInternalInventory getPatternInventory();

   @Mutable
   @Accessor("patternInventory")
   void setPatternInventory(AppEngInternalInventory var1);

   @Mutable
   @Accessor("returnInv")
   void setReturnInv(PatternProviderReturnInventory var1);

   @Accessor("patterns")
   List<IPatternDetails> getPatterns();

   @Accessor("patternInputs")
   Set<AEKey> getPatternInputs();

   @Accessor("sendList")
   List<GenericStack> getSendList();

   @Accessor("sendDirection")
   Direction getSendDirection();

   @Accessor("sendDirection")
   void setSendDirection(Direction var1);

   @Accessor("unlockStack")
   void setUnlockStack(GenericStack var1);

   @Invoker("addToSendList")
   void invokeAddToSendList(AEKey var1, long var2);

   @Invoker("sendStacksOut")
   boolean invokeSendStacksOut();
}
