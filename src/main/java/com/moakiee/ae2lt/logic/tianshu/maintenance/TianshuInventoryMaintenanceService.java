package com.moakiee.ae2lt.logic.tianshu.maintenance;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.crafting.CraftingLink;
import appeng.me.service.CraftingService;
import com.google.common.collect.ImmutableSet;
import com.moakiee.ae2lt.crafting.timewheel.TimeWheelCraftingCPU;
import com.moakiee.ae2lt.me.GridNodeAccess;
import com.moakiee.thunderbolt.core.crafting.pattern.CraftingStockPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;

public final class TianshuInventoryMaintenanceService implements ICraftingRequester, ICraftingSimulationRequester {
   private static final String TAG_REPOSITORY = "Repository";
   private static final String TAG_LINKS = "Links";
   private static final String TAG_RESERVED_STOCK = "ReservedStock";
   private static final String TAG_RULE_RESERVED_STOCK = "RuleReservedStock";
   private static final int BACKGROUND_SCAN_PERIOD = 64;
   private static final int MAX_URGENT_CHECKS_PER_TICK = 32;
   private static final long RETRY_INTERVAL = 100L;
   private final TianshuInventoryMaintenanceHost host;
   private final InventoryMaintenanceRepository repository;
   private final ReservedStockRepository reservedStock;
   private final Map<UUID, ReservedStockRepository> ruleReservedStock = new HashMap<>();
   private final Map<UUID, TianshuInventoryMaintenanceService.PendingCalculation> calculations = new HashMap<>();
   private final Map<UUID, InventoryMaintenanceStatus> statuses = new HashMap<>();
   private final Map<UUID, Long> retryAfter = new HashMap<>();
   private final LinkedHashSet<CraftingLink> links = new LinkedHashSet<>();
   private final LinkedHashSet<UUID> urgentChecks = new LinkedHashSet<>();
   private final FixedPeriodPollingSchedule backgroundSchedule = new FixedPeriodPollingSchedule(64);
   private List<UUID> backgroundRuleIds = List.of();
   private Set<UUID> activeRuleIds = Set.of();
   private boolean ruleScheduleDirty = true;
   private long lastServiceTick = Long.MIN_VALUE;
   private boolean linksRestored;
   private IGrid restoredGrid;

   public TianshuInventoryMaintenanceService(TianshuInventoryMaintenanceHost host) {
      this.host = host;
      this.repository = new InventoryMaintenanceRepository(() -> host.getFunctionProfile().maintenanceRuleCapacity());
      this.reservedStock = new ReservedStockRepository(() -> host.getFunctionProfile().maintenanceRuleCapacity());
   }

   public InventoryMaintenanceRepository repository() {
      return this.repository;
   }

   public ReservedStockRepository reservedStock() {
      return this.reservedStock;
   }

   public ReservedStockRepository reservedStock(UUID ruleId) {
      return ruleId == null
         ? this.reservedStock
         : this.ruleReservedStock
            .computeIfAbsent(ruleId, ignored -> new ReservedStockRepository(() -> this.host.getFunctionProfile().maintenanceRuleCapacity()));
   }

   public ReservedStockRepository.PutResult setMaintenanceWideReservedStock(AEKey key, ReservedStockMatchMode mode, long amount) {
      if (!this.canConfigure()) {
         return ReservedStockRepository.PutResult.UNAVAILABLE;
      } else {
         ReservedStockRepository.PutResult result = this.reservedStock.set(key, mode, amount);
         if (result != ReservedStockRepository.PutResult.INVALID
            && result != ReservedStockRepository.PutResult.FULL
            && result != ReservedStockRepository.PutResult.UNAVAILABLE) {
            this.host.maintenanceStateChanged();
         }

         return result;
      }
   }

   public InventoryMaintenanceStatus status(UUID ruleId) {
      return this.statuses.getOrDefault(ruleId, InventoryMaintenanceStatus.IDLE);
   }

   public List<MaintenanceVariantService.Variant> variants(AEKey selected) {
      IGrid grid = this.host.getGrid();
      return grid != null && selected != null
         ? MaintenanceVariantService.list(grid.getStorageService().getInventory(), grid.getCraftingService(), selected)
         : List.of();
   }

   public InventoryMaintenanceRepository.PutResult putRule(InventoryMaintenanceRule rule) {
      if (this.canConfigure() && rule != null) {
         InventoryMaintenanceRule previous = this.repository.get(rule.key());
         if (previous != null && !previous.id().equals(rule.id())) {
            return InventoryMaintenanceRepository.PutResult.INVALID;
         } else {
            InventoryMaintenanceRepository.PutResult result = this.repository.put(rule);
            if (result == InventoryMaintenanceRepository.PutResult.ADDED || result == InventoryMaintenanceRepository.PutResult.UPDATED) {
               this.ruleScheduleDirty = true;
               this.urgentChecks.add(rule.id());
               this.host.maintenanceStateChanged();
            }

            return result;
         }
      } else {
         return InventoryMaintenanceRepository.PutResult.UNAVAILABLE;
      }
   }

   public boolean removeRule(UUID ruleId) {
      InventoryMaintenanceRule rule = this.repository.getById(ruleId);
      if (this.canConfigure() && rule != null) {
         this.cancelRuleTask(ruleId);
         this.cancelCalculation(ruleId);
         boolean removed = this.repository.remove(rule.key());
         this.ruleReservedStock.remove(ruleId);
         this.retryAfter.remove(ruleId);
         this.statuses.remove(ruleId);
         this.urgentChecks.remove(ruleId);
         if (removed) {
            this.ruleScheduleDirty = true;
            this.host.maintenanceStateChanged();
         }

         return removed;
      } else {
         return false;
      }
   }

   public ReservedStockRepository.PutResult setReservedStock(UUID ruleId, AEKey key, long amount) {
      return this.setReservedStock(ruleId, key, ReservedStockMatchMode.EXACT, amount);
   }

   public ReservedStockRepository.PutResult setReservedStock(UUID ruleId, AEKey key, ReservedStockMatchMode mode, long amount) {
      if (this.canConfigure() && this.repository.getById(ruleId) != null) {
         ReservedStockRepository.PutResult result = this.reservedStock(ruleId).set(key, mode, amount);
         if (result != ReservedStockRepository.PutResult.INVALID
            && result != ReservedStockRepository.PutResult.FULL
            && result != ReservedStockRepository.PutResult.UNAVAILABLE) {
            this.host.maintenanceStateChanged();
         }

         return result;
      } else {
         return ReservedStockRepository.PutResult.UNAVAILABLE;
      }
   }

   public boolean cancelRuleTask(UUID ruleId) {
      InventoryMaintenanceRule rule = this.repository.getById(ruleId);
      if (rule != null && rule.activeCraftingId() != null) {
         UUID craftingId = rule.activeCraftingId();

         for (TimeWheelCraftingCPU cpu : this.host.getTimeWheelCraftingCpuPool().getActiveCpus()) {
            ICraftingLink link = cpu.getCraftingLogic().getLastLink();
            if (link != null && craftingId.equals(link.getCraftingID())) {
               cpu.cancelJob();
               this.statuses.put(ruleId, InventoryMaintenanceStatus.CANCELLING);
               return true;
            }
         }

         CraftingLink link = this.findLink(craftingId);
         if (link != null) {
            link.cancel();
            this.statuses.put(ruleId, InventoryMaintenanceStatus.CANCELLING);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean retryNow(UUID ruleId) {
      if (this.repository.getById(ruleId) == null) {
         return false;
      } else {
         this.retryAfter.remove(ruleId);
         this.urgentChecks.add(ruleId);
         this.statuses.put(ruleId, InventoryMaintenanceStatus.IDLE);
         return true;
      }
   }

   private boolean canConfigure() {
      return this.host.isFormed() && this.host.getFunctionProfile().supportsInventoryMaintenance();
   }

   public void tick() {
      Level level = this.host.m_58904_();
      IGrid grid = this.host.getGrid();
      if (level != null && !level.f_46443_ && grid != null && this.host.isCpuActive() && this.host.getFunctionProfile().supportsInventoryMaintenance()) {
         this.restoreLinks((CraftingService)grid.getCraftingService());
         long now = level.m_46467_();
         if (now != this.lastServiceTick) {
            this.lastServiceTick = now;
            CraftingService crafting = (CraftingService)grid.getCraftingService();
            this.rebuildRuleScheduleIfNeeded();
            this.pollCalculations(crafting, this.activeRuleIds);
            HashSet<UUID> checkedThisTick = new HashSet<>();
            int urgentBudget = 32;
            Iterator<UUID> urgentIterator = this.urgentChecks.iterator();

            while (urgentBudget-- > 0 && urgentIterator.hasNext()) {
               UUID ruleId = urgentIterator.next();
               urgentIterator.remove();
               if (checkedThisTick.add(ruleId)) {
                  this.checkRule(crafting, ruleId, now);
               }
            }

            int backgroundChecks = this.backgroundSchedule.checksThisTick(this.backgroundRuleIds.size());

            for (int i = 0; i < backgroundChecks; i++) {
               UUID ruleId = this.backgroundRuleIds.get(this.backgroundSchedule.nextIndex(this.backgroundRuleIds.size()));
               if (checkedThisTick.add(ruleId)) {
                  this.checkRule(crafting, ruleId, now);
               }
            }
         }
      }
   }

   private void rebuildRuleScheduleIfNeeded() {
      if (this.ruleScheduleDirty) {
         this.ruleScheduleDirty = false;
         HashSet<UUID> activeIds = new HashSet<>();
         ArrayList<UUID> enabledIds = new ArrayList<>();

         for (InventoryMaintenanceRule rule : this.repository.activeRules()) {
            activeIds.add(rule.id());
            if (rule.enabled()) {
               enabledIds.add(rule.id());
            } else {
               this.statuses.put(rule.id(), InventoryMaintenanceStatus.DISABLED);
            }
         }

         for (InventoryMaintenanceRule rulex : this.repository.rules()) {
            if (!activeIds.contains(rulex.id())) {
               this.statuses.put(rulex.id(), InventoryMaintenanceStatus.DISABLED);
            }
         }

         this.activeRuleIds = Set.copyOf(activeIds);
         this.backgroundRuleIds = List.copyOf(enabledIds);
         this.urgentChecks.removeIf(ruleId -> !this.activeRuleIds.contains(ruleId));
         this.backgroundSchedule.reset();
      }
   }

   private void checkRule(CraftingService crafting, UUID ruleId, long now) {
      IGrid grid = this.host.getGrid();
      if (grid != null) {
         InventoryMaintenanceRule rule = this.repository.getById(ruleId);
         if (rule != null) {
            CraftingLink link = this.findLink(rule.activeCraftingId());
            boolean activeLink = link != null && !link.isDone() && !link.isCanceled();
            if (link != null && !activeLink) {
               this.links.remove(link);
               rule = rule.withRuntime(rule.replenishing(), null);
               this.repository.put(rule);
               this.host.maintenanceStateChanged();
            }

            if (!this.activeRuleIds.contains(rule.id())) {
               this.cancelCalculation(rule.id());
               this.statuses.put(rule.id(), activeLink ? InventoryMaintenanceStatus.CRAFTING : InventoryMaintenanceStatus.DISABLED);
            } else if (!rule.enabled()) {
               this.statuses.put(rule.id(), InventoryMaintenanceStatus.DISABLED);
            } else {
               long stock = grid.getStorageService().getInventory().extract(rule.key(), Long.MAX_VALUE, Actionable.SIMULATE, this.host.getActionSource());
               boolean calculationActive = this.calculations.containsKey(rule.id());
               boolean otherCalculationActive = InventoryMaintenanceCalculationClaims.claimedByOther(grid, rule.key(), rule.id());
               boolean networkTaskActive = activeLink || calculationActive || crafting.isRequesting(rule.key()) || otherCalculationActive;
               InventoryMaintenanceDecision decision = InventoryMaintenanceDecision.evaluate(rule, stock, networkTaskActive);
               if (decision.replenishing() != rule.replenishing()) {
                  rule = rule.withRuntime(decision.replenishing(), rule.activeCraftingId());
                  this.repository.put(rule);
                  this.host.maintenanceStateChanged();
               }

               if (activeLink) {
                  this.statuses.put(rule.id(), InventoryMaintenanceStatus.CRAFTING);
               } else if (calculationActive) {
                  this.statuses.put(rule.id(), InventoryMaintenanceStatus.CALCULATING);
               } else if (!decision.replenishing()) {
                  this.retryAfter.remove(rule.id());
                  this.statuses.put(rule.id(), stock >= rule.upperThreshold() ? InventoryMaintenanceStatus.SATISFIED : InventoryMaintenanceStatus.IDLE);
               } else if (now < this.retryAfter.getOrDefault(rule.id(), 0L)) {
                  this.statuses.putIfAbsent(rule.id(), InventoryMaintenanceStatus.WAITING_RETRY);
               } else if (otherCalculationActive) {
                  this.statuses.put(rule.id(), InventoryMaintenanceStatus.CALCULATING);
               } else if (networkTaskActive) {
                  this.statuses.put(rule.id(), InventoryMaintenanceStatus.CRAFTING);
               } else if (decision.requestAmount() > 0L) {
                  this.beginCalculation(crafting, rule, decision.requestAmount());
               }
            }
         }
      }
   }

   private void beginCalculation(CraftingService crafting, InventoryMaintenanceRule rule, long amount) {
      IGrid grid = this.host.getGrid();
      Level level = this.host.m_58904_();
      if (!InventoryMaintenanceCalculationClaims.tryClaim(grid, rule.key(), rule.id())) {
         this.statuses.put(rule.id(), InventoryMaintenanceStatus.CALCULATING);
      } else if (!MaintenanceRequestability.isRequestable(crafting, rule.key())) {
         this.statuses.put(rule.id(), InventoryMaintenanceStatus.MISSING_PATTERN);
         this.scheduleRetry(rule.id());
         InventoryMaintenanceCalculationClaims.release(grid, rule.key(), rule.id());
      } else {
         IGridNode calculationNode = this.host.getActionableNode();
         if (GridNodeAccess.getActiveGrid(calculationNode) != grid) {
            this.statuses.put(rule.id(), InventoryMaintenanceStatus.WAITING_RETRY);
            this.scheduleRetry(rule.id());
            InventoryMaintenanceCalculationClaims.release(grid, rule.key(), rule.id());
         } else {
            IActionSource calculationSource = this.host.getActionSource();

            Future<ICraftingPlan> future;
            try {
               future = crafting.beginCraftingCalculation(
                  level,
                  new TianshuInventoryMaintenanceService.RuleCalculationRequester(rule.id(), calculationNode, calculationSource),
                  rule.key(),
                  amount,
                  CalculationStrategy.REPORT_MISSING_ITEMS
               );
            } catch (RuntimeException var11) {
               InventoryMaintenanceCalculationClaims.release(grid, rule.key(), rule.id());
               this.statuses.put(rule.id(), InventoryMaintenanceStatus.WAITING_RETRY);
               this.scheduleRetry(rule.id());
               return;
            }

            this.calculations.put(rule.id(), new TianshuInventoryMaintenanceService.PendingCalculation(grid, rule.key(), amount, future));
            this.statuses.put(rule.id(), InventoryMaintenanceStatus.CALCULATING);
            this.host.maintenanceStateChanged();
         }
      }
   }

   private void pollCalculations(CraftingService crafting, Set<UUID> activeRuleIds) {
      Iterator<Entry<UUID, TianshuInventoryMaintenanceService.PendingCalculation>> iterator = this.calculations.entrySet().iterator();

      while (iterator.hasNext()) {
         Entry<UUID, TianshuInventoryMaintenanceService.PendingCalculation> entry = iterator.next();
         TianshuInventoryMaintenanceService.PendingCalculation pending = entry.getValue();
         if (pending.grid() != this.host.getGrid()) {
            iterator.remove();
            pending.future().cancel(false);
            InventoryMaintenanceCalculationClaims.release(pending.grid(), pending.key(), entry.getKey());
            this.statuses.put(entry.getKey(), InventoryMaintenanceStatus.IDLE);
         } else if (pending.future().isDone()) {
            iterator.remove();
            InventoryMaintenanceCalculationClaims.release(pending.grid(), pending.key(), entry.getKey());
            InventoryMaintenanceRule rule = this.repository.get(pending.key());
            if (rule != null && rule.id().equals(entry.getKey()) && rule.enabled() && activeRuleIds.contains(rule.id())) {
               try {
                  ICraftingPlan plan = pending.future().get();
                  if (plan == null || plan.simulation() || !plan.missingItems().isEmpty()) {
                     this.statuses.put(rule.id(), InventoryMaintenanceStatus.MISSING_INGREDIENTS);
                     this.scheduleRetry(rule.id());
                  } else if (crafting.isRequesting(rule.key())) {
                     this.statuses.put(rule.id(), InventoryMaintenanceStatus.IDLE);
                  } else if (!this.respectsCurrentReservedStock(rule.id(), plan)) {
                     this.statuses.put(rule.id(), InventoryMaintenanceStatus.WAITING_RETRY);
                     this.scheduleRetry(rule.id());
                  } else {
                     ICraftingSubmitResult submitted = crafting.submitJob(plan, this, null, false, this.host.getActionSource());
                     if (submitted.successful() && submitted.link() != null) {
                        CraftingLink link = (CraftingLink)submitted.link();
                        this.links.add(link);
                        this.repository.put(rule.withRuntime(true, link.getCraftingID()));
                        this.statuses.put(rule.id(), InventoryMaintenanceStatus.CRAFTING);
                        this.retryAfter.remove(rule.id());
                        this.host.maintenanceStateChanged();
                     } else {
                        this.statuses.put(rule.id(), InventoryMaintenanceStatus.WAITING_CPU);
                        this.scheduleRetry(rule.id());
                     }
                  }
               } catch (Exception var10) {
                  this.statuses.put(rule.id(), InventoryMaintenanceStatus.MISSING_INGREDIENTS);
                  this.scheduleRetry(rule.id());
               }
            }
         }
      }
   }

   private void scheduleRetry(UUID ruleId) {
      Level level = this.host.m_58904_();
      this.retryAfter.put(ruleId, (level != null ? level.m_46467_() : 0L) + 100L);
   }

   private boolean respectsCurrentReservedStock(UUID ruleId, ICraftingPlan plan) {
      IGrid grid = this.host.getGrid();
      if (grid != null && plan != null) {
         LayeredReservedStockPolicy policy = this.reservedStockPolicy(ruleId);
         if (policy.isEmpty()) {
            return true;
         } else {
            MEStorage inventory = grid.getStorageService().getInventory();
            KeyCounter available = inventory.getAvailableStacks();

            for (it.unimi.dsi.fastutil.objects.Object2LongMap.Entry<AEKey> used : plan.usedItems()) {
               if (used.getLongValue() > 0L) {
                  AEKey key = (AEKey)used.getKey();
                  long current = Math.max(0L, available.get(key));
                  long usable;
                  if (!policy.groupsSecondaryVariants(key)) {
                     usable = policy.usablePreexistingStock(key, current);
                  } else {
                     HashMap<AEKey, Long> group = new HashMap<>();

                     for (it.unimi.dsi.fastutil.objects.Object2LongMap.Entry<AEKey> entry : available) {
                        if (((AEKey)entry.getKey()).dropSecondary().equals(key.dropSecondary())) {
                           group.put((AEKey)entry.getKey(), Math.max(0L, entry.getLongValue()));
                        }
                     }

                     usable = policy.usablePreexistingStock(key, current, group);
                  }

                  if (used.getLongValue() > usable) {
                     return false;
                  }
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private LayeredReservedStockPolicy reservedStockPolicy(UUID ruleId) {
      return new LayeredReservedStockPolicy(this.reservedStock, this.ruleReservedStock.get(ruleId));
   }

   private void cancelCalculation(UUID ruleId) {
      TianshuInventoryMaintenanceService.PendingCalculation pending = this.calculations.remove(ruleId);
      if (pending != null) {
         pending.future().cancel(false);
         InventoryMaintenanceCalculationClaims.release(pending.grid(), pending.key(), ruleId);
      }
   }

   public void shutdownCalculations() {
      for (Entry<UUID, TianshuInventoryMaintenanceService.PendingCalculation> entry : this.calculations.entrySet()) {
         entry.getValue().future().cancel(false);
         InventoryMaintenanceCalculationClaims.release(entry.getValue().grid(), entry.getValue().key(), entry.getKey());
      }

      this.calculations.clear();
   }

   public void functionCapacityChanged() {
      HashSet<UUID> activeRuleIds = new HashSet<>();

      for (InventoryMaintenanceRule rule : this.repository.activeRules()) {
         activeRuleIds.add(rule.id());
      }

      for (UUID ruleId : List.copyOf(this.calculations.keySet())) {
         if (!activeRuleIds.contains(ruleId)) {
            this.cancelCalculation(ruleId);
         }
      }

      this.ruleScheduleDirty = true;
      this.urgentChecks.addAll(activeRuleIds);
   }

   private CraftingLink findLink(UUID id) {
      if (id == null) {
         return null;
      } else {
         for (CraftingLink link : this.links) {
            if (id.equals(link.getCraftingID())) {
               return link;
            }
         }

         return null;
      }
   }

   private void restoreLinks(CraftingService crafting) {
      IGrid grid = this.host.getGrid();
      if (!this.linksRestored || this.restoredGrid != grid) {
         this.linksRestored = true;
         this.restoredGrid = grid;

         for (CraftingLink link : this.links) {
            crafting.addLink(link);
         }
      }
   }

   public ImmutableSet<ICraftingLink> getRequestedJobs() {
      return ImmutableSet.copyOf(this.links);
   }

   public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
      IGrid grid = this.host.getGrid();
      return grid != null && what != null && amount > 0L ? grid.getStorageService().getInventory().insert(what, amount, mode, this.host.getActionSource()) : 0L;
   }

   public void jobStateChange(ICraftingLink changed) {
      if (changed != null && (changed.isDone() || changed.isCanceled())) {
         this.links.removeIf(link -> link.getCraftingID().equals(changed.getCraftingID()));

         for (InventoryMaintenanceRule rule : this.repository.rules()) {
            if (changed.getCraftingID().equals(rule.activeCraftingId())) {
               this.repository.put(rule.withRuntime(rule.replenishing(), null));
               this.statuses.put(rule.id(), InventoryMaintenanceStatus.IDLE);
               if (changed.isCanceled()) {
                  this.scheduleRetry(rule.id());
               } else {
                  this.urgentChecks.add(rule.id());
               }
            }
         }

         this.host.maintenanceStateChanged();
      }
   }

   public IGridNode getActionableNode() {
      return this.host.getActionableNode();
   }

   public IActionSource getActionSource() {
      return this.host.getActionSource();
   }

   public void writeTo(CompoundTag parent, RegistryAccess registries) {
      CompoundTag repoTag = new CompoundTag();
      this.repository.writeTo(repoTag, registries);
      parent.m_128365_("Repository", repoTag);
      CompoundTag reservedTag = new CompoundTag();
      this.reservedStock.writeTo(reservedTag, registries);
      parent.m_128365_("ReservedStock", reservedTag);
      ListTag ruleReserves = new ListTag();

      for (Entry<UUID, ReservedStockRepository> entry : this.ruleReservedStock.entrySet()) {
         if (entry.getValue().size() > 0) {
            CompoundTag tag = new CompoundTag();
            tag.m_128362_("RuleId", entry.getKey());
            entry.getValue().writeTo(tag, registries);
            ruleReserves.add(tag);
         }
      }

      parent.m_128365_("RuleReservedStock", ruleReserves);
      ListTag linkTags = new ListTag();

      for (CraftingLink link : this.links) {
         CompoundTag tag = new CompoundTag();
         link.writeToNBT(tag);
         linkTags.add(tag);
      }

      parent.m_128365_("Links", linkTags);
   }

   public void readFrom(CompoundTag parent, RegistryAccess registries) {
      this.shutdownCalculations();
      this.statuses.clear();
      this.retryAfter.clear();
      this.urgentChecks.clear();
      this.ruleReservedStock.clear();
      this.links.clear();
      this.activeRuleIds = Set.of();
      this.backgroundRuleIds = List.of();
      this.backgroundSchedule.reset();
      this.ruleScheduleDirty = true;
      this.lastServiceTick = Long.MIN_VALUE;
      this.linksRestored = false;
      this.restoredGrid = null;
      this.repository.readFrom(parent.m_128469_("Repository"), registries);
      this.reservedStock.readFrom(parent.m_128469_("ReservedStock"), registries);
      ListTag ruleReserves = parent.m_128437_("RuleReservedStock", 10);

      for (int i = 0; i < ruleReserves.size(); i++) {
         CompoundTag tag = ruleReserves.m_128728_(i);
         if (tag.m_128403_("RuleId")) {
            ReservedStockRepository profile = new ReservedStockRepository(() -> this.host.getFunctionProfile().maintenanceRuleCapacity());
            profile.readFrom(tag, registries);
            this.ruleReservedStock.put(tag.m_128342_("RuleId"), profile);
         }
      }

      ListTag linkTags = parent.m_128437_("Links", 10);

      for (int ix = 0; ix < linkTags.size(); ix++) {
         try {
            this.links.add(new CraftingLink(linkTags.m_128728_(ix), this));
         } catch (RuntimeException var7) {
         }
      }
   }

   private static record PendingCalculation(IGrid grid, AEKey key, long amount, Future<ICraftingPlan> future) {
   }

   private final class RuleCalculationRequester implements ICraftingSimulationRequester, CraftingStockPolicy {
      private final UUID ruleId;
      private final IGridNode gridNode;
      private final IActionSource actionSource;

      private RuleCalculationRequester(UUID ruleId, IGridNode gridNode, IActionSource actionSource) {
         this.ruleId = ruleId;
         this.gridNode = gridNode;
         this.actionSource = actionSource;
      }

      public IActionSource getActionSource() {
         return this.actionSource;
      }

      public IGridNode getGridNode() {
         return this.gridNode;
      }

      public long usablePreexistingStock(AEKey key, long snapshotAmount) {
         return TianshuInventoryMaintenanceService.this.reservedStockPolicy(this.ruleId).usablePreexistingStock(key, snapshotAmount);
      }

      public boolean groupsSecondaryVariants(AEKey key) {
         return TianshuInventoryMaintenanceService.this.reservedStockPolicy(this.ruleId).groupsSecondaryVariants(key);
      }

      public long usablePreexistingStock(AEKey key, long snapshotAmount, Map<AEKey, Long> groupSnapshot) {
         return TianshuInventoryMaintenanceService.this.reservedStockPolicy(this.ruleId).usablePreexistingStock(key, snapshotAmount, groupSnapshot);
      }
   }
}
