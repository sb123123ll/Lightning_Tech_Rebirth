package com.moakiee.ae2lt.gametest;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.core.sync.BasePacketHandler.PacketTypes;
import appeng.core.sync.network.NetworkHandler;
import appeng.crafting.CraftingPlan;
import appeng.crafting.execution.ElapsedTimeTracker;
import appeng.menu.me.crafting.CraftingCPUMenu;
import appeng.menu.me.crafting.CraftingStatus;
import appeng.menu.me.crafting.CraftingStatusEntry;
import com.moakiee.ae2lt.crafting.timewheel.TimeWheelCraftingCPU;
import com.moakiee.ae2lt.crafting.timewheel.TimeWheelCraftingCpuHost;
import com.moakiee.ae2lt.crafting.timewheel.TimeWheelCraftingCpuPool;
import com.moakiee.ae2lt.crafting.timewheel.TimeWheelCraftingCpuPoolHost;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ae2lt")
@PrefixGameTestTemplate(false)
public final class TimeWheelCraftingStatusGameTests {
   private TimeWheelCraftingStatusGameTests() {
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty",
      m_177042_ = 20
   )
   public static void finishedVirtualCpuSendsEmptyFullStatus(GameTestHelper helper) {
      IGrid grid = emptyGrid();
      AEItemKey output = AEItemKey.of(Items.f_41905_);
      TimeWheelCraftingCPU cpu = submitEmittedJob(helper, grid, output, 1L, new KeyCounter());
      TimeWheelCraftingStatusGameTests.MenuHarness harness = openMenu(helper, "time-wheel-finish");

      try {
         selectCpu(harness.menu(), cpu);
         ElapsedTimeTracker runningTracker = cpu.getCraftingLogic().getElapsedTimeTracker();
         harness.connection().clear();
         harness.menu().m_38946_();
         CraftingStatus running = harness.connection().requireOnlyCraftingStatus(harness.menu().f_38840_);
         helper.m_246336_(running.isFullStatus(), "Selecting the virtual CPU must send an initial full status");
         helper.m_246336_(running.getEntries().size() == 1, "The running job must expose its emitted output in the status table");
         helper.m_246336_(
            running.getRemainingItemCount() == runningTracker.getRemainingItemCount(), "The status header must use the tracker's remaining item count"
         );
         helper.m_246336_(running.getStartItemCount() == runningTracker.getStartItemCount(), "The status header must use the tracker's initial item count");
         cpu.getCraftingLogic().insert(output, 1L, Actionable.MODULATE);
         helper.m_277053_(cpu.getCraftingLogic().hasJob(), "Returning the emitted final output must finish the virtual job");
         ElapsedTimeTracker finishedTracker = cpu.getCraftingLogic().getElapsedTimeTracker();
         harness.connection().clear();
         harness.menu().m_38946_();
         CraftingStatus finished = harness.connection().requireOnlyCraftingStatus(harness.menu().f_38840_);
         helper.m_246336_(finished.isFullStatus(), "The active-to-finished transition must replace the client status view");
         helper.m_246336_(finished.getEntries().isEmpty(), "The replacement status must not retain entries from the finished job");
         helper.m_246336_(
            finished.getRemainingItemCount() == finishedTracker.getRemainingItemCount(),
            "The terminal status must use the empty tracker's remaining item count"
         );
         helper.m_246336_(
            finished.getStartItemCount() == finishedTracker.getStartItemCount(), "The terminal status must use the empty tracker's initial item count"
         );
      } finally {
         harness.close();
      }

      helper.m_177412_();
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty",
      m_177042_ = 20
   )
   public static void runningJobUsesIncrementalStatusAndStableSerial(GameTestHelper helper) {
      IGrid grid = emptyGrid();
      AEItemKey output = AEItemKey.of(Items.f_41905_);
      TimeWheelCraftingCPU cpu = submitEmittedJob(helper, grid, output, 4L, new KeyCounter());
      TimeWheelCraftingStatusGameTests.MenuHarness harness = openMenu(helper, "time-wheel-incremental");

      try {
         selectCpu(harness.menu(), cpu);
         harness.connection().clear();
         harness.menu().m_38946_();
         CraftingStatus initial = harness.connection().requireOnlyCraftingStatus(harness.menu().f_38840_);
         helper.m_246336_(initial.isFullStatus() && initial.getEntries().size() == 1, "Selecting the running CPU must establish one full baseline entry");
         CraftingStatusEntry initialEntry = (CraftingStatusEntry)initial.getEntries().get(0);
         helper.m_246336_(output.equals(initialEntry.getWhat()), "The full baseline must identify the emitted output");
         helper.m_246336_(initialEntry.getActiveAmount() == 4L, "The full baseline must expose all four waiting outputs");
         cpu.getCraftingLogic().insert(output, 1L, Actionable.MODULATE);
         helper.m_246336_(cpu.getCraftingLogic().hasJob(), "Returning one of four outputs must leave the job active");
         ElapsedTimeTracker tracker = cpu.getCraftingLogic().getElapsedTimeTracker();
         harness.connection().clear();
         harness.menu().m_38946_();
         CraftingStatus update = harness.connection().requireOnlyCraftingStatus(harness.menu().f_38840_);
         helper.m_277053_(update.isFullStatus(), "An in-job item change must remain an incremental status update");
         helper.m_246336_(update.getEntries().size() == 1, "The incremental update must contain only the changed output");
         CraftingStatusEntry updatedEntry = (CraftingStatusEntry)update.getEntries().get(0);
         helper.m_246336_(updatedEntry.getSerial() == initialEntry.getSerial(), "The incremental update must retain the baseline serial");
         helper.m_246336_(updatedEntry.getWhat() == null, "A known incremental serial must omit the repeated AE key payload");
         helper.m_246336_(updatedEntry.getActiveAmount() == 3L, "The incremental update must report the reduced waiting amount");
         helper.m_246336_(
            update.getRemainingItemCount() == tracker.getRemainingItemCount(), "The incremental header must use the live tracker's remaining count"
         );
         helper.m_246336_(update.getStartItemCount() == tracker.getStartItemCount(), "The incremental header must use the live tracker's initial count");
      } finally {
         harness.close();
         cpu.cancelJob();
      }

      helper.m_177412_();
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty",
      m_177042_ = 20
   )
   public static void cancelledVirtualCpuSendsOneEmptyFullStatus(GameTestHelper helper) {
      IGrid grid = emptyGrid();
      AEItemKey output = AEItemKey.of(Items.f_41905_);
      TimeWheelCraftingCPU cpu = submitEmittedJob(helper, grid, output, 1L, new KeyCounter());
      TimeWheelCraftingStatusGameTests.MenuHarness harness = openMenu(helper, "time-wheel-cancel");

      try {
         selectCpu(harness.menu(), cpu);
         harness.connection().clear();
         harness.menu().m_38946_();
         harness.connection().requireOnlyCraftingStatus(harness.menu().f_38840_);
         cpu.cancelJob();
         helper.m_277053_(cpu.getCraftingLogic().hasJob(), "Canceling an ordinary virtual job must close its lifecycle immediately");
         harness.connection().clear();
         harness.menu().m_38946_();
         CraftingStatus cancelled = harness.connection().requireOnlyCraftingStatus(harness.menu().f_38840_);
         helper.m_246336_(cancelled.isFullStatus(), "Cancellation must replace the client's incremental status view");
         helper.m_246336_(cancelled.getEntries().isEmpty(), "An empty canceled CPU must not retain entries from its old job");
         harness.connection().clear();
         harness.menu().m_38946_();
         harness.connection().requireNoCraftingStatus(harness.menu().f_38840_);
      } finally {
         harness.close();
      }

      helper.m_177412_();
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty",
      m_177042_ = 20
   )
   public static void removedPoolCpuStillSendsTerminalFullStatus(GameTestHelper helper) {
      IGrid grid = emptyGrid();
      AEItemKey output = AEItemKey.of(Items.f_41905_);
      TimeWheelCraftingStatusGameTests.TestPoolHost host = new TimeWheelCraftingStatusGameTests.TestPoolHost(helper, grid);
      TimeWheelCraftingCpuPool pool = host.getTimeWheelCraftingCpuPool();
      CraftingPlan plan = syntheticPlan(output, 1L, new KeyCounter());
      helper.m_246336_(pool.submitJob(grid, plan, IActionSource.empty(), null).successful(), "The pool must accept the synthetic emitted-item job");
      helper.m_246336_(pool.getActiveCpus().size() == 1, "Submitting the job must publish one virtual CPU");
      TimeWheelCraftingCPU selectedCpu = pool.getActiveCpus().get(0);
      TimeWheelCraftingStatusGameTests.MenuHarness harness = openMenu(helper, "time-wheel-pool-removal");

      try {
         selectCpu(harness.menu(), selectedCpu);
         harness.connection().clear();
         harness.menu().m_38946_();
         harness.connection().requireOnlyCraftingStatus(harness.menu().f_38840_);
         pool.cancelAll();
         helper.m_246336_(pool.getActiveCpus().isEmpty(), "The pool must remove a canceled virtual CPU with no retained state");
         harness.connection().clear();
         harness.menu().m_38946_();
         CraftingStatus terminal = harness.connection().requireOnlyCraftingStatus(harness.menu().f_38840_);
         helper.m_246336_(terminal.isFullStatus(), "A menu retaining the removed CPU object must receive a terminal full snapshot");
         helper.m_246336_(terminal.getEntries().isEmpty(), "The removed empty CPU must replace the old client entries with an empty view");
      } finally {
         harness.close();
         pool.cancelAll();
      }

      helper.m_177412_();
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty",
      m_177042_ = 20
   )
   public static void terminalFullStatusRetainsUnstoredCpuInventory(GameTestHelper helper) {
      AEItemKey ingredient = AEItemKey.of(Items.f_42329_);
      AEItemKey output = AEItemKey.of(Items.f_41905_);
      TimeWheelCraftingStatusGameTests.RejectingStorage storage = new TimeWheelCraftingStatusGameTests.RejectingStorage(counterOf(ingredient, 1L));
      IGrid grid = gridWithStorage(storage);
      TimeWheelCraftingCPU cpu = submitEmittedJob(helper, grid, output, 1L, counterOf(ingredient, 1L));
      TimeWheelCraftingStatusGameTests.MenuHarness harness = openMenu(helper, "time-wheel-retained");

      try {
         helper.m_246336_(cpu.getCraftingLogic().getStored(ingredient) == 1L, "The job must initially hold its extracted ingredient");
         selectCpu(harness.menu(), cpu);
         harness.connection().clear();
         harness.menu().m_38946_();
         harness.connection().requireOnlyCraftingStatus(harness.menu().f_38840_);
         cpu.getCraftingLogic().insert(output, 1L, Actionable.MODULATE);
         helper.m_277053_(cpu.getCraftingLogic().hasJob(), "Returning the final output must finish the synthetic job");
         helper.m_246336_(cpu.getCraftingLogic().getStored(ingredient) == 1L, "Rejected network insertion must leave the ingredient in CPU inventory");
         harness.connection().clear();
         harness.menu().m_38946_();
         CraftingStatus terminal = harness.connection().requireOnlyCraftingStatus(harness.menu().f_38840_);
         helper.m_246336_(terminal.isFullStatus(), "The job boundary must still send a replacement snapshot with retained inventory");
         helper.m_246336_(terminal.getEntries().size() == 1, "The replacement snapshot must contain exactly the retained ingredient");
         CraftingStatusEntry retained = (CraftingStatusEntry)terminal.getEntries().get(0);
         helper.m_246336_(ingredient.equals(retained.getWhat()), "The replacement snapshot must identify the retained ingredient");
         helper.m_246336_(retained.getStoredAmount() == 1L, "The replacement snapshot must preserve the retained stored amount");
         helper.m_246336_(
            retained.getActiveAmount() == 0L && retained.getPendingAmount() == 0L,
            "Retained inventory must not inherit active or pending counts from the old job"
         );
      } finally {
         harness.close();
      }

      helper.m_177412_();
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty",
      m_177042_ = 20
   )
   public static void switchingVirtualCpuReplacesStatusAndDetachesOldListener(GameTestHelper helper) {
      IGrid grid = emptyGrid();
      AEItemKey firstOutput = AEItemKey.of(Items.f_41905_);
      AEItemKey secondOutput = AEItemKey.of(Items.f_42329_);
      TimeWheelCraftingCPU firstCpu = submitEmittedJob(helper, grid, firstOutput, 1L, new KeyCounter());
      TimeWheelCraftingCPU secondCpu = submitEmittedJob(helper, grid, secondOutput, 1L, new KeyCounter());
      TimeWheelCraftingStatusGameTests.MenuHarness harness = openMenu(helper, "time-wheel-switch");

      try {
         selectCpu(harness.menu(), firstCpu);
         harness.connection().clear();
         harness.menu().m_38946_();
         harness.connection().requireOnlyCraftingStatus(harness.menu().f_38840_);
         harness.connection().clear();
         selectCpu(harness.menu(), secondCpu);
         harness.menu().m_38946_();
         CraftingStatus switched = harness.connection().requireOnlyCraftingStatus(harness.menu().f_38840_);
         helper.m_246336_(switched.isFullStatus() && switched.getEntries().size() == 1, "Switching virtual CPUs must establish a new full baseline");
         helper.m_246336_(
            secondOutput.equals(((CraftingStatusEntry)switched.getEntries().get(0)).getWhat()),
            "The new baseline must contain only the newly selected CPU's output"
         );
         harness.connection().clear();
         firstCpu.cancelJob();
         harness.menu().m_38946_();
         harness.connection().requireNoCraftingStatus(harness.menu().f_38840_);
      } finally {
         harness.close();
         firstCpu.cancelJob();
         secondCpu.cancelJob();
      }

      helper.m_177412_();
   }

   private static TimeWheelCraftingCPU submitEmittedJob(GameTestHelper helper, IGrid grid, AEKey output, long amount, KeyCounter usedItems) {
      TimeWheelCraftingCPU cpu = new TimeWheelCraftingCPU(new TimeWheelCraftingStatusGameTests.TestCpuHost(helper, grid), 1L, 0, 1L, false);
      CraftingPlan plan = syntheticPlan(output, amount, usedItems);
      helper.m_246336_(
         cpu.submitJob(grid, plan, IActionSource.empty(), null).successful(), "The synthetic emitted-item job must be accepted by the virtual CPU"
      );
      return cpu;
   }

   private static CraftingPlan syntheticPlan(AEKey output, long amount, KeyCounter usedItems) {
      return new CraftingPlan(new GenericStack(output, amount), 1L, false, false, usedItems, counterOf(output, amount), new KeyCounter(), Map.of());
   }

   private static TimeWheelCraftingStatusGameTests.MenuHarness openMenu(GameTestHelper helper, String playerName) {
      ServerPlayer player = new ServerPlayer(helper.m_177100_().m_7654_(), helper.m_177100_(), new GameProfile(UUID.randomUUID(), playerName));
      TimeWheelCraftingStatusGameTests.RecordingConnection connection = new TimeWheelCraftingStatusGameTests.RecordingConnection();
      new ServerGamePacketListenerImpl(helper.m_177100_().m_7654_(), connection, player);
      CraftingCPUMenu menu = new CraftingCPUMenu(CraftingCPUMenu.TYPE, 1, player.m_150109_(), null);
      return new TimeWheelCraftingStatusGameTests.MenuHarness(player, connection, menu);
   }

   private static KeyCounter counterOf(AEKey key, long amount) {
      KeyCounter result = new KeyCounter();
      result.add(key, amount);
      return result;
   }

   private static void selectCpu(CraftingCPUMenu menu, ICraftingCPU cpu) {
      try {
         Method method = CraftingCPUMenu.class.getDeclaredMethod("setCPU", ICraftingCPU.class);
         method.setAccessible(true);
         method.invoke(menu, cpu);
      } catch (IllegalAccessException | NoSuchMethodException var5) {
         throw new AssertionError("Could not invoke CraftingCPUMenu#setCPU", var5);
      } catch (InvocationTargetException var6) {
         Throwable cause = var6.getCause();
         if (cause instanceof RuntimeException runtimeException) {
            throw runtimeException;
         } else if (cause instanceof Error error) {
            throw error;
         } else {
            throw new AssertionError("CraftingCPUMenu#setCPU failed", cause);
         }
      }
   }

   private static IGrid emptyGrid() {
      return gridWithStorage(new TimeWheelCraftingStatusGameTests.RejectingStorage(new KeyCounter()));
   }

   private static IGrid gridWithStorage(MEStorage inventory) {
      IStorageService storageService = (IStorageService)Proxy.newProxyInstance(
         IStorageService.class.getClassLoader(), new Class[]{IStorageService.class}, (proxy, method, args) -> {
            if (method.getName().equals("getInventory")) {
               return inventory;
            } else {
               return method.getName().equals("getCachedInventory") ? inventory.getAvailableStacks() : defaultValue(method.getReturnType());
            }
         }
      );
      return (IGrid)Proxy.newProxyInstance(
         IGrid.class.getClassLoader(),
         new Class[]{IGrid.class},
         (proxy, method, args) -> {
            if (method.getName().equals("getStorageService")) {
               return storageService;
            } else {
               return method.getName().equals("getService") && args != null && args.length == 1 && args[0] == IStorageService.class
                  ? storageService
                  : defaultValue(method.getReturnType());
            }
         }
      );
   }

   private static Object defaultValue(Class<?> type) {
      if (!type.isPrimitive()) {
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
      } else {
         throw new AssertionError("Unsupported primitive type: " + type);
      }
   }

   private static record MenuHarness(ServerPlayer player, TimeWheelCraftingStatusGameTests.RecordingConnection connection, CraftingCPUMenu menu) {
      private void close() {
         this.menu.m_6877_(this.player);
      }
   }

   private static final class RecordingConnection extends Connection {
      private final List<Packet<?>> packets = new ArrayList<>();

      private RecordingConnection() {
         super(PacketFlow.SERVERBOUND);
      }

      public void m_243124_(Packet<?> packet, PacketSendListener listener) {
         this.packets.add(packet);
      }

      private void clear() {
         this.packets.clear();
      }

      private CraftingStatus requireOnlyCraftingStatus(int expectedContainerId) {
         List<CraftingStatus> statuses = this.readCraftingStatuses(expectedContainerId);
         if (statuses.size() != 1) {
            throw new AssertionError("Expected exactly one crafting-status packet, got " + statuses.size());
         } else {
            return statuses.get(0);
         }
      }

      private void requireNoCraftingStatus(int expectedContainerId) {
         List<CraftingStatus> statuses = this.readCraftingStatuses(expectedContainerId);
         if (!statuses.isEmpty()) {
            throw new AssertionError("Expected no crafting-status packet, got " + statuses.size());
         }
      }

      private List<CraftingStatus> readCraftingStatuses(int expectedContainerId) {
         ArrayList<CraftingStatus> statuses = new ArrayList<>();

         for (Packet<?> packet : this.packets) {
            if (packet instanceof ClientboundCustomPayloadPacket payload && payload.m_132042_().equals(NetworkHandler.instance().getChannel())) {
               FriendlyByteBuf data = payload.m_132045_();

               try {
                  if (data.readInt() == PacketTypes.CRAFTING_STATUS.ordinal()) {
                     int containerId = data.readInt();
                     if (containerId != expectedContainerId) {
                        throw new AssertionError("Crafting status targeted container " + containerId + " instead of " + expectedContainerId);
                     }

                     statuses.add(CraftingStatus.read(data));
                  }
               } finally {
                  data.release();
               }
            }
         }

         return statuses;
      }
   }

   private static final class RejectingStorage implements MEStorage {
      private final KeyCounter contents = new KeyCounter();

      private RejectingStorage(KeyCounter initialContents) {
         this.contents.addAll(initialContents);
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
         return Component.m_237113_("GameTest rejecting storage");
      }
   }

   private static record TestCpuHost(GameTestHelper helper, IGrid grid) implements TimeWheelCraftingCpuHost {
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
      public Component getCpuDisplayName() {
         return Component.m_237113_("GameTest time-wheel CPU");
      }
   }

   private static final class TestPoolHost implements TimeWheelCraftingCpuPoolHost {
      private final GameTestHelper helper;
      private final IGrid grid;
      private final TimeWheelCraftingCpuPool pool;

      private TestPoolHost(GameTestHelper helper, IGrid grid) {
         this.helper = helper;
         this.grid = grid;
         this.pool = new TimeWheelCraftingCpuPool(this, 8L, 0, 1L, false);
      }

      @Override
      public TimeWheelCraftingCpuPool getTimeWheelCraftingCpuPool() {
         return this.pool;
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
      public Component getCpuDisplayName() {
         return Component.m_237113_("GameTest time-wheel CPU pool");
      }
   }
}
