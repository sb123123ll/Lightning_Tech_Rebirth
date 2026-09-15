package com.moakiee.ae2lt.gametest;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.crafting.CraftingPlan;
import appeng.me.service.CraftingService;
import com.moakiee.ae2lt.crafting.runtime.ExecuteLoopPattern;
import com.moakiee.ae2lt.crafting.timewheel.Ae2LtTimeWheelCraftingCpuLogic;
import com.moakiee.ae2lt.crafting.timewheel.TimeWheelCraftingCPU;
import com.moakiee.ae2lt.crafting.timewheel.TimeWheelCraftingCpuHost;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopExpandedPatternDetails;
import com.moakiee.thunderbolt.api.crafting.batch.IBatchCraftingProvider;
import com.moakiee.thunderbolt.api.crafting.cpu.ExtendedCraftingCpuClusterHost;
import com.moakiee.thunderbolt.core.crafting.batch.SharedBatchInputPattern;
import com.moakiee.thunderbolt.core.crafting.loop.ClosedLoopBatchPatternDetails;
import com.moakiee.thunderbolt.core.crafting.loop.CraftingCpuRestrictedPattern;
import com.moakiee.thunderbolt.core.crafting.loop.PatternFiringExpander;
import com.moakiee.thunderbolt.core.crafting.loop.ReusableSeedPattern;
import com.moakiee.thunderbolt.core.crafting.pattern.ReusableStockSource;
import com.moakiee.thunderbolt.core.crafting.plan.LoopCraftingPlan;
import com.moakiee.thunderbolt.core.crafting.plan.LoopCraftingPlan.HostReusableSeedAllocation;
import com.moakiee.thunderbolt.core.crafting.support.CraftingPatternDelegates;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ae2lt")
@PrefixGameTestTemplate(false)
public final class ClosedLoopSharedSeedBatchGameTests {
   private static final String EMPTY_TEMPLATE = "bastion/mobs/empty";

   private ClosedLoopSharedSeedBatchGameTests() {
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty",
      m_177042_ = 30
   )
   public static void unevenSharedSeedBatchesReleaseNetOutputAndCloseJob(GameTestHelper helper) {
      runScenario(helper, new long[]{4L, 3L, 3L}, true);
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty",
      m_177042_ = 30
   )
   public static void singleCopyFallbackStillClosesLoopJob(GameTestHelper helper) {
      runScenario(helper, new long[]{1L, 1L, 1L}, false);
   }

   private static void runScenario(GameTestHelper helper, long[] dispatches, boolean assertIncrementalNetOutput) {
      ClosedLoopSharedSeedBatchGameTests.Fixture fixture = ClosedLoopSharedSeedBatchGameTests.Fixture.create(helper, dispatches);
      long cumulative = 0L;

      for (int i = 0; i < dispatches.length; i++) {
         long expectedDispatch = dispatches[i];
         cumulative += expectedDispatch;
         helper.m_177306_((long)(1 + i * 2), () -> fixture.dispatch(helper, expectedDispatch, cumulative, assertIncrementalNetOutput));
      }

      long expectedNetOutput = cumulative;
      helper.m_177306_(
         (long)(2 + dispatches.length * 2),
         () -> {
            try {
               helper.m_246336_(
                  fixture.provider.validationFailure == null,
                  fixture.provider.validationFailure == null ? "The provider received valid shared-batch inputs" : fixture.provider.validationFailure
               );
               helper.m_246336_(fixture.provider.totalAccepted == expectedNetOutput, "Every planned loop copy must reach the provider");
               helper.m_246336_(!fixture.cpu.getCraftingLogic().hasJob(), "The closed-loop job must leave no active job after all output returns");
               helper.m_246336_(fixture.cpu.getJobStatus() == null, "A completed closed-loop job must not retain a crafting status");
               helper.m_246336_(fixture.storage.amount(fixture.output) == expectedNetOutput, "The network must receive exactly one net output per loop copy");
               helper.m_246336_(fixture.storage.amount(fixture.ingredient) == 0L, "All ordinary ingredients must be consumed");
               helper.m_246336_(fixture.host.seedAmount() == 1L, "The single reusable seed must return to host storage");
            } finally {
               fixture.craftingService.removeNode(fixture.providerNode);
            }

            helper.m_177412_();
         }
      );
   }

   private static LoopCraftingPlan loopPlan(ClosedLoopSharedSeedBatchGameTests.TestLoopMacro macro, AEKey output, AEKey ingredient, long copies) {
      KeyCounter usedItems = counterOf(ingredient, copies);
      CraftingPlan delegate = new CraftingPlan(
         new GenericStack(output, copies), 1L, false, false, usedItems, new KeyCounter(), new KeyCounter(), Map.of(macro, copies)
      );
      ReusableStockSource source = macro.reusableStockSource();
      HostReusableSeedAllocation allocation = new HostReusableSeedAllocation(
         source.storageScope(), source.poolScope(), source.routingScope(), output, output, 1L, macro.reusableSeedGroupId(), true, false
      );
      return new LoopCraftingPlan(delegate, List.of(macro), Map.of(output, 1L), Map.of(output, 1L), List.of(allocation));
   }

   private static IStorageService storageService(ClosedLoopSharedSeedBatchGameTests.TestNetworkStorage storage) {
      return (IStorageService)Proxy.newProxyInstance(IStorageService.class.getClassLoader(), new Class[]{IStorageService.class}, (proxy, method, args) -> {
         if (method.getName().equals("getInventory")) {
            return storage;
         } else if (method.getName().equals("getCachedInventory")) {
            KeyCounter contents = new KeyCounter();
            storage.getAvailableStacks(contents);
            return contents;
         } else {
            return defaultValue(method.getReturnType());
         }
      });
   }

   private static IEnergyService energyService() {
      return (IEnergyService)Proxy.newProxyInstance(IEnergyService.class.getClassLoader(), new Class[]{IEnergyService.class}, (proxy, method, args) -> {
         if (method.getName().equals("extractAEPower")) {
            return args[0];
         } else {
            return method.getName().equals("isNetworkPowered") ? true : defaultValue(method.getReturnType());
         }
      });
   }

   private static IGrid grid(IStorageService storageService, IEnergyService energyService, AtomicReference<CraftingService> craftingService) {
      return (IGrid)Proxy.newProxyInstance(IGrid.class.getClassLoader(), new Class[]{IGrid.class}, (proxy, method, args) -> {
         if (method.getName().equals("getStorageService")) {
            return storageService;
         } else if (method.getName().equals("getEnergyService")) {
            return energyService;
         } else if (method.getName().equals("getCraftingService")) {
            return craftingService.get();
         } else {
            if (method.getName().equals("getService") && args != null && args.length == 1) {
               if (args[0] == IStorageService.class) {
                  return storageService;
               }

               if (args[0] == IEnergyService.class) {
                  return energyService;
               }

               if (args[0] == ICraftingService.class) {
                  return craftingService.get();
               }
            }

            return defaultValue(method.getReturnType());
         }
      });
   }

   private static IGridNode providerNode(GameTestHelper helper, IGrid grid, ICraftingProvider provider) {
      return (IGridNode)Proxy.newProxyInstance(
         IGridNode.class.getClassLoader(),
         new Class[]{IGridNode.class},
         (proxy, method, args) -> {
            if (method.getName().equals("getService") && args != null && args.length == 1 && args[0] == ICraftingProvider.class) {
               return provider;
            } else if (method.getName().equals("getGrid")) {
               return grid;
            } else if (method.getName().equals("getLevel")) {
               return helper.m_177100_();
            } else if (method.getName().equals("getOwner")) {
               return provider;
            } else {
               return !method.getName().equals("isActive")
                     && !method.getName().equals("isOnline")
                     && !method.getName().equals("isPowered")
                     && !method.getName().equals("hasGridBooted")
                     && !method.getName().equals("meetsChannelRequirements")
                  ? defaultValue(method.getReturnType())
                  : true;
            }
         }
      );
   }

   private static KeyCounter counterOf(AEKey key, long amount) {
      KeyCounter result = new KeyCounter();
      result.add(key, amount);
      return result;
   }

   private static Object defaultValue(Class<?> type) {
      if (type == void.class) {
         return null;
      } else if (type == boolean.class) {
         return false;
      } else if (type == byte.class) {
         return (byte)0;
      } else if (type == short.class) {
         return (short)0;
      } else if (type == int.class) {
         return 0;
      } else if (type == long.class) {
         return 0L;
      } else if (type == float.class) {
         return 0.0F;
      } else if (type == double.class) {
         return 0.0;
      } else if (type == char.class) {
         return '\u0000';
      } else if (type == Optional.class) {
         return Optional.empty();
      } else if (Set.class.isAssignableFrom(type)) {
         return Set.of();
      } else if (List.class.isAssignableFrom(type)) {
         return List.of();
      } else if (Map.class.isAssignableFrom(type)) {
         return Map.of();
      } else if (Collection.class.isAssignableFrom(type)) {
         return List.of();
      } else {
         return Iterable.class.isAssignableFrom(type) ? List.of() : null;
      }
   }

   private static record ExactInput(AEKey key) implements IInput {
      public GenericStack[] getPossibleInputs() {
         return new GenericStack[]{new GenericStack(this.key, 1L)};
      }

      public long getMultiplier() {
         return 1L;
      }

      public boolean isValid(AEKey input, Level level) {
         return this.key.equals(input);
      }

      public AEKey getRemainingKey(AEKey template) {
         return null;
      }
   }

   private static record Fixture(
      AEItemKey output,
      AEItemKey ingredient,
      ClosedLoopSharedSeedBatchGameTests.TestNetworkStorage storage,
      ClosedLoopSharedSeedBatchGameTests.TestHost host,
      ClosedLoopSharedSeedBatchGameTests.ScriptedBatchProvider provider,
      IGridNode providerNode,
      CraftingService craftingService,
      IEnergyService energyService,
      TimeWheelCraftingCPU cpu
   ) {
      private static ClosedLoopSharedSeedBatchGameTests.Fixture create(GameTestHelper helper, long[] dispatches) {
         AEItemKey output = AEItemKey.of(Items.f_42587_);
         AEItemKey ingredient = AEItemKey.of(Items.f_42749_);
         long totalCopies = 0L;

         for (long dispatch : dispatches) {
            totalCopies += dispatch;
         }

         ClosedLoopSharedSeedBatchGameTests.TestNetworkStorage storage = new ClosedLoopSharedSeedBatchGameTests.TestNetworkStorage();
         storage.put(ingredient, totalCopies);
         IStorageService storageService = ClosedLoopSharedSeedBatchGameTests.storageService(storage);
         IEnergyService energyService = ClosedLoopSharedSeedBatchGameTests.energyService();
         AtomicReference<CraftingService> craftingServiceRef = new AtomicReference<>();
         IGrid grid = ClosedLoopSharedSeedBatchGameTests.grid(storageService, energyService, craftingServiceRef);
         CraftingService craftingService = new CraftingService(grid, storageService, energyService);
         craftingServiceRef.set(craftingService);
         UUID groupId = UUID.randomUUID();
         ClosedLoopSharedSeedBatchGameTests.PhysicalLoopPattern physicalPattern = new ClosedLoopSharedSeedBatchGameTests.PhysicalLoopPattern(output, ingredient);
         ClosedLoopSharedSeedBatchGameTests.SharedLoopMember member = new ClosedLoopSharedSeedBatchGameTests.SharedLoopMember(physicalPattern, output, groupId);
         ClosedLoopSharedSeedBatchGameTests.TestLoopMacro macro = new ClosedLoopSharedSeedBatchGameTests.TestLoopMacro(member, output, groupId, new Object());
         ClosedLoopSharedSeedBatchGameTests.ScriptedBatchProvider provider = new ClosedLoopSharedSeedBatchGameTests.ScriptedBatchProvider(
            physicalPattern, output, ingredient, dispatches
         );
         IGridNode providerNode = ClosedLoopSharedSeedBatchGameTests.providerNode(helper, grid, provider);
         craftingService.addNode(providerNode, null);
         ClosedLoopSharedSeedBatchGameTests.TestHost host = new ClosedLoopSharedSeedBatchGameTests.TestHost(helper, grid, output);
         TimeWheelCraftingCPU cpu = new TimeWheelCraftingCPU(host, 1L, 0, totalCopies, false);
         LoopCraftingPlan plan = ClosedLoopSharedSeedBatchGameTests.loopPlan(macro, output, ingredient, totalCopies);
         helper.m_246336_(cpu.submitJob(grid, plan, IActionSource.empty(), null).successful(), "The closed-loop plan must be accepted by the time-wheel CPU");
         helper.m_246336_(host.seedAmount() == 0L, "Submitting the job must borrow its reusable seed from the host");
         helper.m_246336_(storage.amount(ingredient) == 0L, "Submitting the job must move all ordinary ingredients into the CPU");
         return new ClosedLoopSharedSeedBatchGameTests.Fixture(output, ingredient, storage, host, provider, providerNode, craftingService, energyService, cpu);
      }

      private void dispatch(GameTestHelper helper, long expectedDispatch, long expectedCumulative, boolean assertIncrementalNetOutput) {
         Ae2LtTimeWheelCraftingCpuLogic.TickUsage usage = this.cpu
            .getCraftingLogic()
            .tickCraftingLogic(this.energyService, this.craftingService, 1, expectedDispatch);
         helper.m_246336_(usage.dispatchedCopies() == expectedDispatch, "The scheduler must dispatch the requested batch split");
         helper.m_246336_(this.provider.lastAccepted == expectedDispatch, "The provider must accept the complete scheduled slice");
         long produced = this.provider.takeProduced();
         helper.m_246336_(produced == expectedDispatch + 1L, "A shared batch must return one seed plus one net output per copy");
         long acceptedByCpu = this.cpu.getCraftingLogic().insert(this.output, produced, Actionable.MODULATE);
         long networkRemainder = produced - acceptedByCpu;
         long inserted = this.storage.insert(this.output, networkRemainder, Actionable.MODULATE, IActionSource.empty());
         helper.m_246336_(inserted == networkRemainder, "The test network must accept every public final output");
         if (assertIncrementalNetOutput) {
            helper.m_246336_(this.storage.amount(this.output) == expectedCumulative, "A successful shared batch must not retain outputs for future copies");
            long remaining = this.provider.totalPlanned - expectedCumulative;
            if (remaining > 0L) {
               GenericStack displayed = this.cpu.getDisplayedOutput();
               helper.m_246336_(displayed != null && displayed.amount() == remaining, "Job progress must decrease by the net output of each shared batch");
            }
         } else if (expectedCumulative < this.provider.totalPlanned) {
            long expectedPublicOutput = Math.max(0L, expectedCumulative - 1L);
            helper.m_246336_(this.storage.amount(this.output) == expectedPublicOutput, "Ordinary dispatch must retain one public output for the next copy");
            GenericStack displayed = this.cpu.getDisplayedOutput();
            helper.m_246336_(
               displayed != null && displayed.amount() == this.provider.totalPlanned - expectedPublicOutput,
               "Ordinary fallback must preserve per-copy seed demand until the next push"
            );
         }
      }
   }

   private static final class PhysicalLoopPattern implements IPatternDetails {
      private final AEItemKey definition = AEItemKey.of(Items.f_42516_);
      private final IInput[] inputs;
      private final GenericStack[] outputs;

      private PhysicalLoopPattern(AEKey seed, AEKey ingredient) {
         this.inputs = new IInput[]{new ClosedLoopSharedSeedBatchGameTests.ExactInput(seed), new ClosedLoopSharedSeedBatchGameTests.ExactInput(ingredient)};
         this.outputs = new GenericStack[]{new GenericStack(seed, 2L)};
      }

      public AEItemKey getDefinition() {
         return this.definition;
      }

      public IInput[] getInputs() {
         return (IInput[])this.inputs.clone();
      }

      public GenericStack[] getOutputs() {
         return (GenericStack[])this.outputs.clone();
      }

      public boolean supportsPushInputsToExternalInventory() {
         return true;
      }
   }

   private static final class ScriptedBatchProvider implements IBatchCraftingProvider {
      private final IPatternDetails pattern;
      private final AEKey seed;
      private final AEKey ingredient;
      private final long[] dispatches;
      private final long totalPlanned;
      private int index;
      private long produced;
      private long lastAccepted;
      private long totalAccepted;
      private String validationFailure;

      private ScriptedBatchProvider(IPatternDetails pattern, AEKey seed, AEKey ingredient, long[] dispatches) {
         this.pattern = pattern;
         this.seed = seed;
         this.ingredient = ingredient;
         this.dispatches = (long[])dispatches.clone();
         long total = 0L;

         for (long dispatch : dispatches) {
            total += dispatch;
         }

         this.totalPlanned = total;
      }

      public List<IPatternDetails> getAvailablePatterns() {
         return List.of(this.pattern);
      }

      public boolean isBusy() {
         return false;
      }

      public long getBatchCapacity(IPatternDetails details) {
         return this.index < this.dispatches.length ? this.dispatches[this.index] : 0L;
      }

      public boolean supportsSharedBatchInputs() {
         return true;
      }

      public long pushBatch(IPatternDetails details, KeyCounter[] oneCopyTemplate, long maxCraft) {
         this.lastAccepted = maxCraft;
         if (CraftingPatternDelegates.forProviderLookup(details) != this.pattern) {
            this.validationFailure = "The execution pattern must resolve to the registered provider pattern";
         } else if (maxCraft <= 1L
            || details instanceof SharedBatchInputPattern shared
               && shared.isSharedBatchInput(0, this.seed)
               && !shared.isSharedBatchInput(1, this.ingredient)
               && shared.sharedBatchOutputAmount(this.seed) == 1L) {
            if (maxCraft == 1L && details != this.pattern) {
               this.validationFailure = "Ordinary fallback must still receive the registered physical pattern";
            } else if (this.index >= this.dispatches.length || maxCraft != this.dispatches[this.index]) {
               this.validationFailure = "The provider received an unexpected scripted batch size";
            } else if (oneCopyTemplate.length != 2 || oneCopyTemplate[0].get(this.seed) != 1L || oneCopyTemplate[1].get(this.ingredient) != 1L) {
               this.validationFailure = "A batch template must contain one shared seed and one ingredient";
            }
         } else {
            this.validationFailure = "Shared batch execution must preserve one reusable seed and ordinary ingredients";
         }

         this.produced += maxCraft + 1L;
         this.totalAccepted += maxCraft;
         this.index++;
         return 0L;
      }

      private long takeProduced() {
         long result = this.produced;
         this.produced = 0L;
         return result;
      }
   }

   private static final class SharedLoopMember extends ClosedLoopExpandedPatternDetails implements SharedBatchInputPattern, ClosedLoopBatchPatternDetails {
      private final AEKey seed;

      private SharedLoopMember(IPatternDetails delegate, AEKey seed, UUID groupId) {
         super(delegate, Map.of(seed, 1L), Set.of(seed), groupId, true, Map.of(0, seed), delegate.getDefinition(), 0);
         this.seed = seed;
      }

      public boolean isSharedBatchInput(int slot, AEKey concreteKey) {
         return slot == 0 && this.seed.equals(concreteKey);
      }

      @Override
      public long sharedBatchOutputAmount(AEKey outputKey) {
         return super.sharedBatchOutputAmount(outputKey);
      }
   }

   private static final class TestHost implements TimeWheelCraftingCpuHost {
      private final GameTestHelper helper;
      private final IGrid grid;
      private final AEKey seed;
      private long seedAmount = 1L;

      private TestHost(GameTestHelper helper, IGrid grid, AEKey seed) {
         this.helper = helper;
         this.grid = grid;
         this.seed = seed;
      }

      private long seedAmount() {
         return this.seedAmount;
      }

      @Override
      public boolean isCpuActive() {
         return true;
      }

      @Override
      public IGrid getGrid() {
         return this.grid;
      }

      @Override
      public IActionSource getActionSource() {
         return IActionSource.empty();
      }

      @Override
      public Level getCpuLevel() {
         return this.helper.m_177100_();
      }

      @Override
      public void markCpuDirty() {
      }

      @Override
      public long extractReusableSeed(AEKey key, long amount, Actionable mode) {
         if (!this.seed.equals(key)) {
            return 0L;
         } else {
            long extracted = Math.min(Math.max(0L, amount), this.seedAmount);
            if (mode == Actionable.MODULATE) {
               this.seedAmount -= extracted;
            }

            return extracted;
         }
      }

      @Override
      public long insertReusableSeed(AEKey key, long amount, Actionable mode) {
         if (!this.seed.equals(key)) {
            return 0L;
         } else {
            long inserted = Math.max(0L, amount);
            if (mode == Actionable.MODULATE) {
               this.seedAmount += inserted;
            }

            return inserted;
         }
      }

      @Override
      public Component getCpuDisplayName() {
         return Component.m_237113_("Closed-loop shared-seed GameTest CPU");
      }
   }

   private static final class TestLoopMacro implements IPatternDetails, PatternFiringExpander, ReusableSeedPattern, CraftingCpuRestrictedPattern {
      private final ClosedLoopSharedSeedBatchGameTests.SharedLoopMember member;
      private final AEKey seed;
      private final UUID groupId;
      private final Object storageScope;

      private TestLoopMacro(ClosedLoopSharedSeedBatchGameTests.SharedLoopMember member, AEKey seed, UUID groupId, Object storageScope) {
         this.member = member;
         this.seed = seed;
         this.groupId = groupId;
         this.storageScope = storageScope;
      }

      public Map<IPatternDetails, Long> expandPatternFirings(long macroFirings) {
         if (macroFirings <= 0L) {
            return Map.of();
         } else {
            KeyCounter initialSeed = ClosedLoopSharedSeedBatchGameTests.counterOf(this.seed, 1L);
            KeyCounter inputSeed = ClosedLoopSharedSeedBatchGameTests.counterOf(this.seed, 1L);
            KeyCounter outputCredit = ClosedLoopSharedSeedBatchGameTests.counterOf(this.seed, 1L);
            ExecuteLoopPattern concrete = new ExecuteLoopPattern(this.member, this.groupId, initialSeed, inputSeed, Map.of(this.groupId, outputCredit));
            return Map.of(concrete, macroFirings);
         }
      }

      public Object reusableSeedStorageScope() {
         return this.storageScope;
      }

      public UUID reusableSeedGroupId() {
         return this.groupId;
      }

      public Set<AEKey> reusableSeedCycleKeys() {
         return Set.of(this.seed);
      }

      public boolean hasSingleSeedInputPerMember() {
         return true;
      }

      public Map<AEKey, Long> totalReusableSeedRequirements() {
         return Map.of(this.seed, 1L);
      }

      public boolean acceptsCraftingCpu(ExtendedCraftingCpuClusterHost host) {
         return true;
      }

      public AEItemKey getDefinition() {
         return this.member.getDefinition();
      }

      public IInput[] getInputs() {
         return this.member.getInputs();
      }

      public GenericStack[] getOutputs() {
         return this.member.getOutputs();
      }
   }

   private static final class TestNetworkStorage implements MEStorage {
      private final KeyCounter contents = new KeyCounter();

      private void put(AEKey key, long amount) {
         this.contents.add(key, amount);
      }

      private long amount(AEKey key) {
         return this.contents.get(key);
      }

      public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
         long inserted = Math.max(0L, amount);
         if (mode == Actionable.MODULATE && inserted > 0L) {
            this.contents.add(what, inserted);
         }

         return inserted;
      }

      public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
         long extracted = Math.min(Math.max(0L, amount), this.contents.get(what));
         if (mode == Actionable.MODULATE && extracted > 0L) {
            this.contents.remove(what, extracted);
            this.contents.removeZeros();
         }

         return extracted;
      }

      public void getAvailableStacks(KeyCounter out) {
         out.addAll(this.contents);
      }

      public Component getDescription() {
         return Component.m_237113_("Closed-loop GameTest storage");
      }
   }
}
