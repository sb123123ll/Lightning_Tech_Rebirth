package com.moakiee.ae2lt.logic;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.patternprovider.PatternProviderReturnInventory;
import com.moakiee.ae2lt.blockentity.OverloadedPatternProviderBlockEntity;
import com.moakiee.ae2lt.logic.energy.PowerCostUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

final class OverloadedAutoReturnController {
   private static final int MAX_RETURN_POLLS_PER_TICK = 64;
   private final OverloadedAutoReturnController.Environment environment;
   private final ProviderReturnSweep sweep = new ProviderReturnSweep();
   private final MachineAdapter.OutputSink outputSink = new MachineAdapter.OutputSink() {
      @Override
      public long maxAccept(AEKey what, long available) {
         long affordable = PowerCostUtil.maxAffordable(OverloadedAutoReturnController.this.environment.gridNode().getGrid(), what, available);
         return affordable <= 0L ? 0L : OverloadedAutoReturnController.this.environment.returnInventory().insert(0, what, affordable, Actionable.SIMULATE);
      }

      @Override
      public long accept(AEKey what, long amount) {
         long inserted = OverloadedAutoReturnController.this.environment.returnInventory().insert(0, what, amount, Actionable.MODULATE);
         if (inserted > 0L) {
            PowerCostUtil.consume(OverloadedAutoReturnController.this.environment.gridNode().getGrid(), what, inserted);
         }

         return inserted;
      }

      @Override
      public void acceptOverflow(AEKey what, long amount) {
         OverloadedAutoReturnController.this.forceInsertToNetwork(what, amount);
      }
   };

   OverloadedAutoReturnController(OverloadedAutoReturnController.Environment environment) {
      this.environment = environment;
   }

   void tick() {
      OverloadedPatternProviderBlockEntity provider = this.environment.provider();
      if (provider.getReturnMode() == OverloadedPatternProviderBlockEntity.ReturnMode.AUTO && this.environment.gridNode().isActive()) {
         AllowedOutputFilter allowedOutputs = this.environment.outputFilter();
         if (!allowedOutputs.isEmpty()) {
            if (provider.m_58904_() instanceof ServerLevel level) {
               long var11 = level.m_46467_();
               List targets = this.currentTargets(level, var11);
               this.sweep.synchronize(targets, var11);

               for (int scans = 0; scans < 64; scans++) {
                  ProviderTarget target = this.sweep.pollDue(var11);
                  if (target == null) {
                     break;
                  }

                  ServerLevel targetLevel = this.resolveTargetLevel(level, target);
                  OutputReturnResult result;
                  if (targetLevel == null) {
                     result = OutputReturnResult.UNAVAILABLE;
                  } else {
                     if (!target.claimOutputReturnScan(var11)) {
                        this.sweep.recordDispatch(target, var11);
                        continue;
                     }

                     result = target.returnOutputs(targetLevel, allowedOutputs, this.environment.actionSource(), this.outputSink);
                  }

                  this.sweep.recordPeriodic(target, var11, result);
               }
            }
         }
      }
   }

   void beforeDispatch(ServerLevel targetLevel, ProviderTarget target) {
      if (this.environment.provider().getReturnMode() == OverloadedPatternProviderBlockEntity.ReturnMode.AUTO && this.environment.gridNode().isActive()) {
         AllowedOutputFilter allowedOutputs = this.environment.outputFilter();
         if (!allowedOutputs.isEmpty()) {
            long gameTick = targetLevel.m_46467_();
            if (target.claimOutputReturnScan(gameTick)) {
               target.returnOutputs(targetLevel, allowedOutputs, this.environment.actionSource(), this.outputSink);
            }

            this.sweep.recordDispatch(target, gameTick);
         }
      }
   }

   long nextPollTick(ServerLevel providerLevel) {
      long gameTick = providerLevel.m_46467_();
      this.sweep.synchronize(this.currentTargets(providerLevel, gameTick), gameTick);
      return this.sweep.nextDueTick();
   }

   void clear() {
      this.sweep.clear();
   }

   void clearSchedule() {
      this.sweep.clear();
   }

   private List<ProviderTarget> currentTargets(ServerLevel providerLevel, long gameTick) {
      if (this.environment.provider().getProviderMode() == OverloadedPatternProviderBlockEntity.ProviderMode.WIRELESS) {
         return new ArrayList<>(this.environment.validConnections(providerLevel, gameTick));
      } else {
         ArrayList<ProviderTarget> targets = new ArrayList<>();

         for (Direction direction : this.environment.normalTargetDirections()) {
            targets.add(this.environment.normalTarget(providerLevel, direction));
         }

         return targets;
      }
   }

   @Nullable
   private ServerLevel resolveTargetLevel(ServerLevel providerLevel, ProviderTarget target) {
      return target instanceof OverloadedPatternProviderBlockEntity.WirelessConnection connection
         ? this.environment.resolveTargetLevel(providerLevel, connection)
         : providerLevel;
   }

   private void forceInsertToNetwork(AEKey what, long amount) {
      IGrid grid = this.environment.gridNode().getGrid();
      long inserted = grid == null ? 0L : grid.getStorageService().getInventory().insert(what, amount, Actionable.MODULATE, this.environment.actionSource());
      if (inserted < amount) {
         LoggerFactory.getLogger("ae2lt").warn("Auto-return voided {} x{}: return inventory, machine and network all rejected it", what, amount - inserted);
      }

      if (inserted > 0L) {
         this.environment.onReturnedStack(new GenericStack(what, inserted));
      }
   }

   interface Environment {
      OverloadedPatternProviderBlockEntity provider();

      IManagedGridNode gridNode();

      IActionSource actionSource();

      AllowedOutputFilter outputFilter();

      PatternProviderReturnInventory returnInventory();

      List<OverloadedPatternProviderBlockEntity.WirelessConnection> validConnections(ServerLevel var1, long var2);

      @Nullable
      ServerLevel resolveTargetLevel(ServerLevel var1, OverloadedPatternProviderBlockEntity.WirelessConnection var2);

      ProviderTarget normalTarget(ServerLevel var1, Direction var2);

      List<Direction> normalTargetDirections();

      void onReturnedStack(GenericStack var1);
   }
}
