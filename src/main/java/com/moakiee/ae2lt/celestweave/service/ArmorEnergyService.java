package com.moakiee.ae2lt.celestweave.service;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import com.moakiee.ae2lt.celestweave.ArmorEnergyBuffer;
import com.moakiee.ae2lt.celestweave.ArmorNetworkRechargePolicy;
import com.moakiee.ae2lt.celestweave.BaseCelestweaveArmorItem;
import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import com.moakiee.ae2lt.celestweave.PhaseFlightPlayerState;
import com.moakiee.ae2lt.celestweave.module.PhaseFlightSubmodule;
import com.moakiee.ae2lt.celestweave.phase.CelestweaveEquipmentAccess;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import com.moakiee.ae2lt.device.module.OverloadDeviceModuleItem;
import com.moakiee.ae2lt.device.network.ArmorNetworkBinding;
import com.moakiee.ae2lt.device.network.BindingResolveResult;
import com.moakiee.ae2lt.logic.energy.AppFluxBridge;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class ArmorEnergyService {
   private static final ConcurrentHashMap<UUID, Long> NEXT_NETWORK_RETRY_TICK = new ConcurrentHashMap<>();
   private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

   private ArmorEnergyService() {
   }

   public static long refillFromBoundNetworkIfLow(Player player, ItemStack armor, Provider registries) {
      if (player instanceof ServerPlayer serverPlayer) {
         long stored = ArmorEnergyBuffer.read(armor, registries);
         long capacity = ArmorEnergyBuffer.capacity(armor, registries);
         long request = ArmorNetworkRechargePolicy.passiveRechargeRequest(stored, capacity);
         return rechargeFromNetwork(serverPlayer, armor, request, false);
      } else {
         return 0L;
      }
   }

   public static boolean consumePassiveDrain(Player player, ItemStack armor, Provider registries) {
      return consumePassiveDrain(player, armor, CelestweaveArmorState.collectInstalledSubmoduleEntries(armor, registries), registries);
   }

   public static boolean consumePassiveDrain(
      Player player, ItemStack armor, List<CelestweaveArmorState.InstalledSubmodule> installedSubmodules, Provider registries
   ) {
      if (player instanceof ServerPlayer serverPlayer) {
         ArmorEnergyService.PassiveCost cost = computePassiveCost(serverPlayer, armor, installedSubmodules, registries);
         if (!ArmorLightningService.hasCost(serverPlayer, armor, cost.lightning())) {
            ArmorResourceFeedback.noLightning(serverPlayer, armor, cost.lightning());
            return false;
         } else {
            ArmorEnergyService.EnergyPayment payment = consumeBufferedCost(serverPlayer, armor, cost.fe());
            if (!payment.paid()) {
               ArmorResourceFeedback.noFe(serverPlayer);
               return false;
            } else if (ArmorLightningService.consume(serverPlayer, armor, cost.lightning())) {
               return true;
            } else {
               ArmorResourceFeedback.noLightning(serverPlayer, armor, cost.lightning());
               payment.refund();
               return false;
            }
         }
      } else {
         return true;
      }
   }

   public static boolean consumeActiveCost(Player player, ItemStack armor, long amount) {
      return consumeActiveCostPayment(player, armor, amount).paid();
   }

   public static ArmorEnergyService.EnergyPayment consumeActiveCostPayment(Player player, ItemStack armor, long amount) {
      return consumeCost(player, armor, amount, true, true);
   }

   private static ArmorEnergyService.EnergyPayment consumeBufferedCost(Player player, ItemStack armor, long amount) {
      return consumeCost(player, armor, amount, false, false);
   }

   private static ArmorEnergyService.EnergyPayment consumeCost(
      Player player, ItemStack armor, long amount, boolean activeRecharge, boolean includeBoundNetworks
   ) {
      if (amount <= 0L) {
         return ArmorEnergyService.EnergyPayment.paid(player, List.of(), List.of());
      } else if (player instanceof ServerPlayer serverPlayer) {
         List candidates = collectEnergyCandidates(serverPlayer, armor);
         if (activeRecharge) {
            rechargeCandidatesForCost(serverPlayer, candidates, amount);
         }

         return consumePlannedCost(serverPlayer, candidates, amount, includeBoundNetworks);
      } else {
         return ArmorEnergyService.EnergyPayment.unpaid(player);
      }
   }

   private static long rechargeFromNetwork(ServerPlayer player, ItemStack armor, long request, boolean ignoreCooldown) {
      if (request <= 0L) {
         return 0L;
      } else {
         UUID armorId = CelestweaveArmorState.ensureArmorId(armor);
         long now = player.m_9236_().m_46467_();
         if (!ignoreCooldown) {
            long nextRetry = NEXT_NETWORK_RETRY_TICK.getOrDefault(armorId, 0L);
            if (ArmorNetworkRechargePolicy.isCoolingDown(nextRetry, now)) {
               return 0L;
            }
         }

         long received = ArmorEnergyBuffer.refillFromNetwork(armor, player, request);
         long storedAfter = ArmorEnergyBuffer.read(armor, player.m_9236_().m_9598_());
         long capacity = ArmorEnergyBuffer.capacity(armor, player.m_9236_().m_9598_());
         if (storedAfter >= capacity) {
            NEXT_NETWORK_RETRY_TICK.remove(armorId);
         } else if (ArmorNetworkRechargePolicy.shouldThrottlePassiveRetry(storedAfter, capacity, received)) {
            NEXT_NETWORK_RETRY_TICK.put(armorId, ArmorNetworkRechargePolicy.nextRetryTick(now));
         } else {
            NEXT_NETWORK_RETRY_TICK.remove(armorId);
         }

         return received;
      }
   }

   public static void refundCost(ServerPlayer player, ItemStack armor, long amount) {
      if (amount > 0L) {
         ArmorEnergyBuffer.write(armor, player.m_9236_().m_9598_(), ArmorEnergyBuffer.read(armor, player.m_9236_().m_9598_()) + amount);
      }
   }

   private static void rechargeCandidatesForCost(ServerPlayer player, List<ItemStack> candidates, long amount) {
      long remaining = amount;

      for (ItemStack candidate : candidates) {
         if (remaining <= 0L) {
            return;
         }

         long stored = ArmorEnergyBuffer.read(candidate, player.m_9236_().m_9598_());
         long capacity = ArmorEnergyBuffer.capacity(candidate, player.m_9236_().m_9598_());
         long request = ArmorNetworkRechargePolicy.activeRechargeRequest(stored, capacity, remaining);
         rechargeFromNetwork(player, candidate, request, true);
         remaining -= Math.min(remaining, ArmorEnergyBuffer.read(candidate, player.m_9236_().m_9598_()));
      }
   }

   private static ArmorEnergyService.EnergyPayment consumePlannedCost(
      ServerPlayer player, List<ItemStack> candidates, long amount, boolean includeBoundNetworks
   ) {
      ArrayList<ArmorEnergySpendPlan.Source> sources = new ArrayList<>();

      for (int i = 0; i < candidates.size(); i++) {
         sources.add(new ArmorEnergySpendPlan.Source(i, ArmorEnergyBuffer.read(candidates.get(i), player.m_9236_().m_9598_())));
      }

      List<ArmorEnergyService.NetworkEnergySource> networkSources = includeBoundNetworks ? collectNetworkEnergySources(player, candidates, amount) : List.of();
      int networkSourceOffset = sources.size();

      for (int i = 0; i < networkSources.size(); i++) {
         sources.add(new ArmorEnergySpendPlan.Source(networkSourceOffset + i, networkSources.get(i).available()));
      }

      ArmorEnergySpendPlan plan = ArmorEnergySpendPlan.create(amount, sources);
      if (!plan.canPay()) {
         return ArmorEnergyService.EnergyPayment.unpaid(player);
      } else {
         ArrayList<ArmorEnergyService.EnergyDebit> debits = new ArrayList<>();
         ArrayList<ArmorEnergyService.NetworkEnergyDebit> networkDebits = new ArrayList<>();

         for (ArmorEnergySpendPlan.Debit debit : plan.debits()) {
            if (debit.sourceIndex() < networkSourceOffset) {
               ItemStack stack = candidates.get(debit.sourceIndex());
               long current = ArmorEnergyBuffer.read(stack, player.m_9236_().m_9598_());
               ArmorEnergyBuffer.write(stack, player.m_9236_().m_9598_(), current - debit.amount());
               debits.add(new ArmorEnergyService.EnergyDebit(stack, debit.amount()));
            } else {
               ArmorEnergyService.NetworkEnergySource source = networkSources.get(debit.sourceIndex() - networkSourceOffset);
               long extracted = source.storage().extract(AppFluxBridge.FE_KEY, debit.amount(), Actionable.MODULATE, source.actionSource());
               if (extracted > 0L) {
                  networkDebits.add(new ArmorEnergyService.NetworkEnergyDebit(source.storage(), source.actionSource(), extracted));
               }

               if (extracted < debit.amount()) {
                  refundNetworkDebits(networkDebits);
                  refundEnergyDebits(player, debits);
                  return ArmorEnergyService.EnergyPayment.unpaid(player);
               }
            }
         }

         return ArmorEnergyService.EnergyPayment.paid(player, debits, networkDebits);
      }
   }

   private static List<ArmorEnergyService.NetworkEnergySource> collectNetworkEnergySources(ServerPlayer player, List<ItemStack> candidates, long maximumRequest) {
      if (maximumRequest > 0L && AppFluxBridge.isAvailable() && AppFluxBridge.FE_KEY != null) {
         Set<IGrid> seenGrids = Collections.newSetFromMap(new IdentityHashMap<>());
         IActionSource actionSource = IActionSource.ofPlayer(player);
         ArrayList<ArmorEnergyService.NetworkEnergySource> sources = new ArrayList<>();

         for (ItemStack candidate : candidates) {
            BindingResolveResult bound = ArmorNetworkBinding.INSTANCE.resolve(candidate, player);
            IGrid grid = bound.success() ? bound.grid() : null;
            if (grid != null && seenGrids.add(grid)) {
               MEStorage storage = grid.getStorageService().getInventory();
               long available = storage.extract(AppFluxBridge.FE_KEY, maximumRequest, Actionable.SIMULATE, actionSource);
               if (available > 0L) {
                  sources.add(new ArmorEnergyService.NetworkEnergySource(storage, actionSource, available));
               }
            }
         }

         return List.copyOf(sources);
      } else {
         return List.of();
      }
   }

   private static void refundEnergyDebits(ServerPlayer player, List<ArmorEnergyService.EnergyDebit> debits) {
      for (int i = debits.size() - 1; i >= 0; i--) {
         ArmorEnergyService.EnergyDebit debit = debits.get(i);
         refundCost(player, debit.armor(), debit.amount());
      }
   }

   private static void refundNetworkDebits(List<ArmorEnergyService.NetworkEnergyDebit> debits) {
      if (AppFluxBridge.FE_KEY != null) {
         for (int i = debits.size() - 1; i >= 0; i--) {
            ArmorEnergyService.NetworkEnergyDebit debit = debits.get(i);
            debit.storage().insert(AppFluxBridge.FE_KEY, debit.amount(), Actionable.MODULATE, debit.actionSource());
         }
      }
   }

   public static long receiveExternalEnergy(ServerPlayer player, ItemStack preferredArmor, long amount) {
      if (player != null && amount > 0L) {
         long remaining = amount;
         long received = 0L;

         for (ItemStack candidate : collectEnergyCandidates(player, preferredArmor)) {
            long accepted = ArmorEnergyBuffer.receiveFe(candidate, player.m_9236_().m_9598_(), remaining, false);
            received += accepted;
            remaining -= accepted;
            if (remaining <= 0L) {
               break;
            }
         }

         return received;
      } else {
         return 0L;
      }
   }

   private static List<ItemStack> collectEnergyCandidates(ServerPlayer player, ItemStack preferredArmor) {
      ArrayList<ItemStack> candidates = new ArrayList<>();
      if (isEnergyCandidate(preferredArmor)) {
         candidates.add(preferredArmor);
      }

      for (EquipmentSlot slot : ARMOR_SLOTS) {
         ItemStack equipped = CelestweaveEquipmentAccess.findArmor(player, slot);
         if (isEnergyCandidate(equipped) && !containsSameArmor(candidates, equipped)) {
            candidates.add(equipped);
         }
      }

      return candidates;
   }

   private static boolean isEnergyCandidate(ItemStack armor) {
      return armor != null && !armor.m_41619_() && armor.m_41720_() instanceof BaseCelestweaveArmorItem;
   }

   private static boolean containsSameArmor(List<ItemStack> candidates, ItemStack armor) {
      UUID armorId = CelestweaveArmorState.getArmorId(armor);

      for (ItemStack candidate : candidates) {
         if (candidate == armor) {
            return true;
         }

         UUID candidateId = CelestweaveArmorState.getArmorId(candidate);
         if (candidateId != null && candidateId.equals(armorId)) {
            return true;
         }
      }

      return false;
   }

   private static ArmorEnergyService.PassiveCost computePassiveCost(
      ServerPlayer player, ItemStack armor, List<CelestweaveArmorState.InstalledSubmodule> installedSubmodules, Provider registries
   ) {
      if (!CelestweaveArmorState.hasCore(armor, registries)) {
         return new ArmorEnergyService.PassiveCost(0L, ArmorLightningService.LightningCost.NONE);
      } else {
         long drain = 0L;
         double multiplier = 1.0;
         ArmorLightningService.LightningCost lightning = ArmorLightningService.LightningCost.NONE;
         Set<ItemStack> chargedStacks = Collections.newSetFromMap(new IdentityHashMap<>());

         for (CelestweaveArmorState.InstalledSubmodule entry : installedSubmodules) {
            if (CelestweaveArmorState.isSubmoduleEnabled(armor, entry.submodule())) {
               ItemStack module = entry.stack();
               if (module.m_41720_() instanceof OverloadDeviceModuleItem provider && chargedStacks.add(module)) {
                  List<DeviceCapability> capabilities = provider.capabilities(module);
                  boolean movingFlight = hasFlightMode(capabilities) && isMovingInFlight(player);
                  boolean phaseTraversalActive = hasPhaseTraversal(capabilities) && PhaseFlightSubmodule.shouldUsePhaseTraversal(player, armor);
                  int count = Math.max(1, entry.count());
                  ArmorLightningService.LightningCost moduleLightning = ArmorModuleLightningPolicy.passiveCost(
                        capabilities,
                        movingFlight,
                        phaseTraversalActive,
                        AE2LTCommonConfig.overloadArmorPassiveHvPerTick(),
                        AE2LTCommonConfig.overloadArmorFlightHvPerTick(),
                        AE2LTCommonConfig.overloadArmorPhaseFlightHvPerTick()
                     )
                     .times((long)count);
                  lightning = lightning.plus(moduleLightning);
                  long moduleDrain = 0L;

                  for (DeviceCapability capability : capabilities) {
                     if (capability instanceof DeviceCapability.PassiveDrain) {
                        DeviceCapability.PassiveDrain passiveDrain = (DeviceCapability.PassiveDrain)capability;
                        long fePerTick = Math.max(0L, passiveDrain.fePerTick());
                        if (movingFlight) {
                           fePerTick = Math.max(fePerTick, 10000L);
                        }

                        moduleDrain += fePerTick;
                     } else {
                        if (capability instanceof DeviceCapability.PhaseTraversal) {
                           DeviceCapability.PhaseTraversal traversal = (DeviceCapability.PhaseTraversal)capability;
                           if (phaseTraversalActive) {
                              moduleDrain = Math.max(moduleDrain, traversal.activeFePerTick());
                              continue;
                           }
                        }

                        if (capability instanceof DeviceCapability.EnergyEfficiency efficiency) {
                           multiplier *= Math.max(0.0, efficiency.drainMul());
                        }
                     }
                  }

                  drain += moduleDrain * (long)count;
               }
            }
         }

         return new ArmorEnergyService.PassiveCost((long)Math.ceil((double)drain * multiplier), lightning);
      }
   }

   private static boolean hasFlightMode(List<DeviceCapability> capabilities) {
      for (DeviceCapability capability : capabilities) {
         if (capability instanceof DeviceCapability.FlightMode) {
            return true;
         }
      }

      return false;
   }

   private static boolean hasPhaseTraversal(List<DeviceCapability> capabilities) {
      for (DeviceCapability capability : capabilities) {
         if (capability instanceof DeviceCapability.PhaseTraversal) {
            return true;
         }
      }

      return false;
   }

   private static boolean isMovingInFlight(ServerPlayer player) {
      if (!player.m_150110_().f_35935_ && !PhaseFlightPlayerState.isFlying(player) && !player.m_21255_()) {
         return false;
      } else {
         Vec3 motion = player.m_20184_();
         return motion.m_82556_() > 1.0E-4;
      }
   }

   private static record EnergyDebit(ItemStack armor, long amount) {
   }

   public static final class EnergyPayment {
      private final Player player;
      private final boolean paid;
      private final List<ArmorEnergyService.EnergyDebit> debits;
      private final List<ArmorEnergyService.NetworkEnergyDebit> networkDebits;

      private EnergyPayment(Player player, boolean paid, List<ArmorEnergyService.EnergyDebit> debits, List<ArmorEnergyService.NetworkEnergyDebit> networkDebits) {
         this.player = player;
         this.paid = paid;
         this.debits = List.copyOf(debits);
         this.networkDebits = List.copyOf(networkDebits);
      }

      private static ArmorEnergyService.EnergyPayment paid(
         Player player, List<ArmorEnergyService.EnergyDebit> debits, List<ArmorEnergyService.NetworkEnergyDebit> networkDebits
      ) {
         return new ArmorEnergyService.EnergyPayment(player, true, debits, networkDebits);
      }

      private static ArmorEnergyService.EnergyPayment unpaid(Player player) {
         return new ArmorEnergyService.EnergyPayment(player, false, List.of(), List.of());
      }

      public boolean paid() {
         return this.paid;
      }

      public void refund() {
         if (this.player instanceof ServerPlayer serverPlayer) {
            ArmorEnergyService.refundNetworkDebits(this.networkDebits);
            ArmorEnergyService.refundEnergyDebits(serverPlayer, this.debits);
         }
      }
   }

   private static record NetworkEnergyDebit(MEStorage storage, IActionSource actionSource, long amount) {
   }

   private static record NetworkEnergySource(MEStorage storage, IActionSource actionSource, long available) {
   }

   private static record PassiveCost(long fe, ArmorLightningService.LightningCost lightning) {
   }
}
