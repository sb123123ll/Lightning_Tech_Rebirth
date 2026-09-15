package com.moakiee.ae2lt.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moakiee.ae2lt.celestweave.PhaseFlightMovementGuard;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({Commands.class})
public abstract class CommandsPhaseTeleportMixin {
   @WrapMethod(
      method = {"performPrefixedCommand(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)I"}
   )
   private int ae2lt$bindTeleportCommandSource(CommandSourceStack source, String command, Operation<Integer> original) {
      return PhaseFlightMovementGuard.runAsCommandExecution(source, () -> (Integer)original.call(new Object[]{source, command}));
   }
}
