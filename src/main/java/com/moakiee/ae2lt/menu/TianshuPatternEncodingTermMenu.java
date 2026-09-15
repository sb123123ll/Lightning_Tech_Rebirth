package com.moakiee.ae2lt.menu;

import appeng.api.config.Actionable;
import appeng.api.config.Settings;
import appeng.api.config.ViewItems;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageHelper;
import appeng.client.gui.Icon;
import appeng.core.definitions.AEItems;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.MenuTypeBuilder.MenuFactory;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.FakeSlot;
import appeng.parts.encoding.EncodingMode;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.moakiee.ae2lt.blockentity.TianshuSupercomputerPortBlockEntity;
import com.moakiee.ae2lt.client.TianshuRecipeTransferContext;
import com.moakiee.ae2lt.client.TianshuUploadTriggerClient;
import com.moakiee.ae2lt.config.AE2LTClientConfig;
import com.moakiee.ae2lt.item.ClosedLoopPatternItem;
import com.moakiee.ae2lt.item.OverloadPatternItem;
import com.moakiee.ae2lt.logic.AdvancedAECompat;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopDiscoveryCandidate;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopDiscoveryService;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopMemberPattern;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopPatternAnalyzer;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopPatternAuthoringService;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopPatternPayload;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopPatternRepository;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopPatternUploadService;
import com.moakiee.ae2lt.logic.tianshu.loop.TianshuSeedRefillService;
import com.moakiee.ae2lt.logic.tianshu.maintenance.InventoryMaintenanceRepository;
import com.moakiee.ae2lt.logic.tianshu.maintenance.InventoryMaintenanceRule;
import com.moakiee.ae2lt.logic.tianshu.maintenance.InventoryMaintenanceStatus;
import com.moakiee.ae2lt.logic.tianshu.maintenance.MaintenanceRequestability;
import com.moakiee.ae2lt.logic.tianshu.maintenance.MaintenanceTopologyService;
import com.moakiee.ae2lt.logic.tianshu.maintenance.MaintenanceVariantService;
import com.moakiee.ae2lt.logic.tianshu.maintenance.ReservedStockMatchMode;
import com.moakiee.ae2lt.logic.tianshu.maintenance.ReservedStockRepository;
import com.moakiee.ae2lt.logic.tianshu.maintenance.TianshuInventoryMaintenanceService;
import com.moakiee.ae2lt.logic.tianshu.terminal.ClosedLoopDraftStatus;
import com.moakiee.ae2lt.logic.tianshu.terminal.ClosedLoopDraftSync;
import com.moakiee.ae2lt.logic.tianshu.terminal.ClosedLoopResultPage;
import com.moakiee.ae2lt.logic.tianshu.terminal.ClosedLoopTerminalDraft;
import com.moakiee.ae2lt.logic.tianshu.terminal.ExtendedAEPlusEncodingCompat;
import com.moakiee.ae2lt.logic.tianshu.terminal.MaintenanceEditorData;
import com.moakiee.ae2lt.logic.tianshu.terminal.PatternEncodingDuplicateFilter;
import com.moakiee.ae2lt.logic.tianshu.terminal.ProcessingPatternEncodingType;
import com.moakiee.ae2lt.logic.tianshu.terminal.ProcessingPatternMultiplier;
import com.moakiee.ae2lt.logic.tianshu.terminal.ProcessingPatternTerminalDraft;
import com.moakiee.ae2lt.logic.tianshu.terminal.SeedRefillSync;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuEncodingMode;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuPatternTerminalHost;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuPatternUploadRouting;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuTerminalTarget;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuUploadTargetData;
import com.moakiee.ae2lt.me.GridNodeAccess;
import com.moakiee.ae2lt.network.PacketSender;
import com.moakiee.ae2lt.network.tianshu.ClosedLoopResultPagePacket;
import com.moakiee.ae2lt.network.tianshu.MaintenanceEditorSyncPacket;
import com.moakiee.ae2lt.network.tianshu.MaintenanceSummarySyncPacket;
import com.moakiee.ae2lt.network.tianshu.OpenMaintenanceEditorPacket;
import com.moakiee.ae2lt.network.tianshu.RequestClosedLoopResultPagePacket;
import com.moakiee.ae2lt.network.tianshu.RequestUploadTargetsPacket;
import com.moakiee.ae2lt.network.tianshu.SaveGlobalReservePacket;
import com.moakiee.ae2lt.network.tianshu.SaveMaintenanceRulePacket;
import com.moakiee.ae2lt.network.tianshu.UploadPatternToTargetPacket;
import com.moakiee.ae2lt.network.tianshu.UploadTargetsSyncPacket;
import com.moakiee.ae2lt.overload.pattern.PatternConversionService;
import com.moakiee.ae2lt.overload.runtime.model.EncodedOverloadPattern;
import com.moakiee.ae2lt.overload.runtime.model.MatchMode;
import com.moakiee.ae2lt.overload.runtime.model.OverloadPatternSlot;
import com.moakiee.ae2lt.overload.runtime.pattern.Ae2PlainPatternResolver;
import com.moakiee.ae2lt.overload.runtime.pattern.EditableOverloadPatternState;
import com.moakiee.ae2lt.overload.runtime.pattern.ParsedPatternDefinition;
import com.moakiee.ae2lt.overload.runtime.pattern.ParsedPatternInput;
import com.moakiee.ae2lt.overload.runtime.pattern.ParsedPatternOutput;
import com.moakiee.ae2lt.overload.runtime.pattern.SourcePatternSnapshot;
import com.moakiee.ae2lt.registry.ModItems;
import com.moakiee.ae2lt.util.SlotPositionAccess;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.anti_ad.mc.ipn.api.IPNIgnore;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@IPNIgnore
public class TianshuPatternEncodingTermMenu extends PatternEncodingTermMenu {
   private static final Logger DUPLICATE_LOG = LoggerFactory.getLogger("ae2lt/TianshuDuplicate");
   public static final int CLOSED_LOOP_MEMBER_SLOTS = 27;
   public static final int CLOSED_LOOP_OUTPUT_SLOTS = 9;
   public static final int CLOSED_LOOP_RESULT_SLOTS = 243;
   private static final int CLOSED_LOOP_OFFSCREEN = -10000;
   private static final MenuFactory<TianshuPatternEncodingTermMenu, TianshuPatternTerminalHost> FACTORY = TianshuPatternEncodingTermMenu::new;
   public static final MenuType<TianshuPatternEncodingTermMenu> TYPE = Ae2ltMenuBuilder.buildUnregistered(
      MenuTypeBuilder.create(FACTORY, TianshuPatternTerminalHost.class), new ResourceLocation("ae2lt", "tianshu_pattern_encoding_terminal")
   );
   @GuiSync(110)
   public TianshuEncodingMode tianshuMode = TianshuEncodingMode.CRAFTING;
   @GuiSync(111)
   public ProcessingPatternEncodingType processingEncodingType = ProcessingPatternEncodingType.NORMAL;
   @GuiSync(113)
   public int closedLoopCandidateCount;
   @GuiSync(114)
   public int closedLoopCandidateIndex;
   @GuiSync(115)
   public int closedLoopExecutionSeedMultiplier = 1;
   @GuiSync(116)
   public int uploadState;
   @GuiSync(117)
   public boolean maintenanceAvailable;
   @GuiSync(122)
   public boolean maintainableView;
   @GuiSync(123)
   public boolean encodedClosedLoop;
   @GuiSync(124)
   public int triggeredUploadAck;
   @GuiSync(125)
   public int closedLoopEncodeState;
   @GuiSync(130)
   public int tianshuSelectionRevision;
   @GuiSync(131)
   public int closedLoopStoredTaskMultiplier = 1;
   @Deprecated
   @GuiSync(132)
   public int closedLoopSeedMultiplier = 1;
   @GuiSync(133)
   public ClosedLoopDraftSync closedLoopDraftSync = ClosedLoopDraftSync.empty();
   @GuiSync(134)
   public ClosedLoopDraftStatus closedLoopDraftStatus = ClosedLoopDraftStatus.EMPTY;
   @GuiSync(135)
   public int closedLoopExternalInputCount;
   @GuiSync(136)
   public int closedLoopSeedInputCount;
   @GuiSync(137)
   public boolean seedRefillAvailable;
   @GuiSync(138)
   public SeedRefillSync seedRefillSync = SeedRefillSync.none();
   @GuiSync(139)
   public ProcessingPatternTerminalDraft processingDraftSync = ProcessingPatternTerminalDraft.empty();
   @GuiSync(140)
   public int closedLoopResultRevision;
   protected final TianshuPatternTerminalHost tianshuHost;
   @Nullable
   private TianshuTerminalTarget boundTianshuTarget;
   private final PatternConversionService conversionService = new PatternConversionService();
   private ItemStack configuredSource = ItemStack.f_41583_;
   private List<ClosedLoopDiscoveryCandidate> closedLoopCandidates = List.of();
   private List<ClosedLoopMemberPattern> closedLoopDraftMembers = List.of();
   @Nullable
   private AEKey closedLoopMainOutput;
   private final AppEngInternalInventory closedLoopMemberInventory;
   private final AppEngInternalInventory closedLoopOutputInventory;
   private final AppEngInternalInventory globalReserveMarkInventory;
   private final FakeSlot globalReserveMarkSlot;
   private final List<AppEngSlot> closedLoopMemberSlots = new ArrayList<>();
   private final List<AppEngSlot> closedLoopOutputSlots = new ArrayList<>();
   private List<GenericStack> closedLoopExternalInputs = List.of();
   private List<GenericStack> closedLoopSeeds = List.of();
   private final Map<ClosedLoopResultPage.Kind, ClosedLoopResultPage> closedLoopResultPages = new EnumMap<>(ClosedLoopResultPage.Kind.class);
   private final long[] closedLoopMemberCopies = new long[27];
   private final int[] closedLoopOutputRoles = new int[9];
   private boolean closedLoopBulkUpdating;
   private boolean closedLoopDraftDirty;
   private boolean closedLoopDraftRepresentsEncoded;
   @Nullable
   private ClosedLoopPatternPayload closedLoopPreparedPayload;
   @Nullable
   private MaintenanceEditorData maintenanceEditorData;
   private int maintenanceEditorRevision;
   private int maintenanceEditorSelectionRevision = Integer.MIN_VALUE;
   private List<PatternContainer> uploadTargets = List.of();
   private Map<PatternContainerGroup, List<TianshuPatternEncodingTermMenu.BoundUploadSlot>> boundUploadTargets = Map.of();
   private List<TianshuUploadTargetData> uploadTargetGroups = List.of();
   private int uploadTargetsRevision;
   private int lastMaintenanceSummaryTick = Integer.MIN_VALUE;
   @Nullable
   private List<MaintenanceSummarySyncPacket.Entry> lastSentMaintenanceSummary;
   private boolean lastSentMaintenanceSummaryOverflow;
   private long maintenanceSummaryRevision;
   private long receivedMaintenanceSummaryRevision = Long.MIN_VALUE;
   private int maintenanceSummarySelectionRevision = Integer.MIN_VALUE;
   private boolean maintenanceSummaryOverflow;
   private List<MaintenanceSummarySyncPacket.Entry> maintenanceSummary = List.of();
   private boolean pendingTriggeredUpload;
   private boolean pendingDirectUpload;
   private int pendingTriggeredUploadUntil;
   private int expectedTriggeredUploadAck;
   private boolean directUploadTargetsRequested;
   private int expectedDirectUploadTargetRevision;
   private boolean ae2EncodingInProgress;
   private ItemStack refundableEncodedPattern = ItemStack.f_41583_;

   public TianshuPatternEncodingTermMenu(int id, Inventory inventory, TianshuPatternTerminalHost host) {
      this(TYPE, id, inventory, host);
   }

   protected TianshuPatternEncodingTermMenu(MenuType<?> type, int id, Inventory inventory, TianshuPatternTerminalHost host) {
      super(type, id, inventory, host, true);
      this.tianshuHost = host;
      AppEngSlot inheritedBlankPatternSlot = (AppEngSlot)this.getSlots(SlotSemantics.BLANK_PATTERN).get(0);
      inheritedBlankPatternSlot.setSlotEnabled(false);
      this.closedLoopMemberInventory = new AppEngInternalInventory(new InternalInventoryHost() {
         public void onChangeInventory(InternalInventory inv, int slot) {
            if (!TianshuPatternEncodingTermMenu.this.closedLoopBulkUpdating) {
               TianshuPatternEncodingTermMenu.this.closedLoopDraftRepresentsEncoded = false;
               TianshuPatternEncodingTermMenu.this.closedLoopDraftDirty = true;
            }
         }

         public boolean isClientSide() {
            return TianshuPatternEncodingTermMenu.this.isClientSide();
         }

         public void saveChanges() {
         }
      }, 27, 1);
      this.closedLoopOutputInventory = new AppEngInternalInventory(null, 9, 1);
      this.globalReserveMarkInventory = new AppEngInternalInventory(null, 1, 1);
      this.globalReserveMarkSlot = new FakeSlot(this.globalReserveMarkInventory, 0);
      SlotPositionAccess.set(this.globalReserveMarkSlot, -10000, -10000);
      this.globalReserveMarkSlot.setIcon(Icon.BACKGROUND_PRIMARY_OUTPUT);
      this.addSlot(this.globalReserveMarkSlot, Ae2ltSlotSemantics.TIANSHU_GLOBAL_RESERVE_MARK);

      for (int i = 0; i < 27; i++) {
         TianshuPatternEncodingTermMenu.ClosedLoopMemberSlot slot = new TianshuPatternEncodingTermMenu.ClosedLoopMemberSlot(this.closedLoopMemberInventory, i);
         SlotPositionAccess.set(slot, -10000, -10000);
         this.addSlot(slot, Ae2ltSlotSemantics.TIANSHU_CLOSED_LOOP_MEMBER);
         this.closedLoopMemberSlots.add(slot);
      }

      for (int i = 0; i < 9; i++) {
         AppEngSlot slot;
         if (i == 0) {
            slot = new TianshuPatternEncodingTermMenu.ClosedLoopOutputSlot(this.closedLoopOutputInventory);
            slot.setIcon(Icon.BACKGROUND_PRIMARY_OUTPUT);
            slot.setEmptyTooltip(() -> List.of(Component.m_237115_("ae2lt.tianshu.closed_loop.primary_output_mark.tooltip")));
         } else {
            slot = new TianshuPatternEncodingTermMenu.ClosedLoopReadonlySlot(this.closedLoopOutputInventory, i);
            slot.setEmptyTooltip(() -> List.of(Component.m_237115_("ae2lt.tianshu.closed_loop.byproduct_output.tooltip")));
         }

         SlotPositionAccess.set(slot, -10000, -10000);
         this.addSlot(slot, Ae2ltSlotSemantics.TIANSHU_CLOSED_LOOP_OUTPUT_MARK);
         this.closedLoopOutputSlots.add(slot);
      }

      this.boundTianshuTarget = inventory.f_35978_.m_9236_().f_46443_ ? null : host.selectTianshuTarget();
      if (this.boundTianshuTarget != null) {
         this.tianshuSelectionRevision = 1;
      }

      this.tianshuMode = host.getTianshuEncodingMode();
      this.maintainableView = host.isMaintainableView();
      if (this.maintainableView && !inventory.f_35978_.m_9236_().f_46443_) {
         this.getConfigManager().putSetting(Settings.VIEW_MODE, ViewItems.ALL);
      }

      if (!inventory.f_35978_.m_9236_().f_46443_) {
         this.restoreProcessingDraft(host.getProcessingPatternTerminalDraft());
         this.restoreClosedLoopDraft(host.getClosedLoopTerminalDraft());
      }

      this.registerClientAction("setTianshuMode", TianshuEncodingMode.class, this::setTianshuModeServer);
      this.registerClientAction("multiplyProcessing", Integer.class, this::multiplyProcessingServer);
      this.registerClientAction("armAdvancedEncoding", ProcessingPatternEncodingType.AdvancedConfig.class, this::armAdvancedEncodingServer);
      this.registerClientAction("armOverloadEncoding", ProcessingPatternEncodingType.OverloadConfig.class, this::armOverloadEncodingServer);
      this.registerClientAction("resetProcessingEncoding", this::resetProcessingEncodingServer);
      this.registerClientAction("selectClosedLoopCandidate", Integer.class, this::selectClosedLoopCandidateServer);
      this.registerClientAction("changeClosedLoopExecutionSeedMultiplier", Integer.class, this::changeClosedLoopExecutionSeedMultiplierServer);
      this.registerClientAction("changeClosedLoopSeedMultiplier", Integer.class, this::changeClosedLoopSeedMultiplierServer);
      this.registerClientAction("changeClosedLoopStoredTaskMultiplier", Integer.class, this::changeClosedLoopStoredTaskMultiplierServer);
      this.registerClientAction("setClosedLoopMemberCopies", TianshuPatternEncodingTermMenu.ClosedLoopMemberEdit.class, this::setClosedLoopMemberCopiesServer);
      this.registerClientAction("moveClosedLoopMember", TianshuPatternEncodingTermMenu.ClosedLoopMemberMove.class, this::moveClosedLoopMemberServer);
      this.registerClientAction("setClosedLoopMultipliers", TianshuPatternEncodingTermMenu.ClosedLoopMultiplierEdit.class, this::setClosedLoopMultipliersServer);
      this.registerClientAction("autoFillClosedLoop", this::autoFillClosedLoopServer);
      this.registerClientAction("cycleClosedLoopOutput", this::cycleClosedLoopOutputServer);
      this.registerClientAction("refillClosedLoopSeeds", this::refillClosedLoopSeedsServer);
      this.registerClientAction("clearClosedLoopDraft", this::clearClosedLoopDraftServer);
      this.registerClientAction("encodeTianshu", Boolean.class, this::encodeServerWithOptions);
      this.registerClientAction("uploadEncodedPattern", Integer.class, this::uploadEncodedPatternServer);
      this.registerClientAction("setMaintainableView", Boolean.class, this::setMaintainableViewServer);
      this.registerClientAction("setMaintainableViewTemporarily", Boolean.class, this::setMaintainableViewTemporarilyServer);
      this.registerClientAction("maintenanceAction", TianshuPatternEncodingTermMenu.MaintenanceAction.class, this::maintenanceActionServer);
   }

   public void m_38946_() {
      if (this.isServerSide() && GridNodeAccess.getGridIfPresent(this.getNetworkNode()) == null) {
         this.setValidMenu(false);
      } else {
         if (this.isServerSide()) {
            this.returnLegacyBlankPatternsToNetwork();
            this.tianshuMode = this.tianshuHost.getTianshuEncodingMode();
            TianshuSupercomputerPortBlockEntity selected = this.resolveOrBindTianshu();
            this.maintenanceAvailable = selected != null && selected.getFunctionProfile().supportsInventoryMaintenance();
            this.seedRefillAvailable = selected != null && selected.isFormed() && selected.getFunctionProfile().supportsClosedLoopSeeds();
            if (!this.ae2EncodingInProgress) {
               this.refreshDerivedConfiguration();
            }

            this.refreshProcessingDraftBinding();
            if (this.closedLoopDraftDirty) {
               this.rebuildClosedLoopDraft();
            }

            this.closedLoopSeedMultiplier = this.closedLoopExecutionSeedMultiplier;
            this.refreshClosedLoopDraftSync();
            this.persistClosedLoopDraft();
            if (this.tianshuMode == TianshuEncodingMode.CLOSED_LOOP && this.getMode() != this.tianshuHost.getLogic().getMode()) {
               super.setMode(this.tianshuHost.getLogic().getMode());
            }
         }

         this.broadcastParentChanges();
         if (this.isServerSide()) {
            this.sendMaintenanceSummaryIfNeeded();
         }
      }
   }

   private void broadcastParentChanges() {
      super.m_38946_();
   }

   public void m_6877_(Player player) {
      if (this.isServerSide()) {
         if (this.closedLoopDraftDirty && this.tianshuMode == TianshuEncodingMode.CLOSED_LOOP) {
            this.rebuildClosedLoopDraft();
         }

         this.persistClosedLoopDraft();
      }

      super.m_6877_(player);
   }

   @Nullable
   private TianshuSupercomputerPortBlockEntity resolveBoundTianshu() {
      return this.tianshuHost.resolveTianshuTarget(this.boundTianshuTarget);
   }

   public void resetClientTianshuScopedState() {
      if (this.isClientSide()) {
         if (this.maintenanceSummarySelectionRevision != this.tianshuSelectionRevision) {
            this.maintenanceSummary = List.of();
            this.maintenanceSummaryOverflow = false;
         }

         if (this.maintenanceEditorSelectionRevision != this.tianshuSelectionRevision) {
            this.maintenanceEditorData = null;
            this.maintenanceEditorRevision++;
         }
      }
   }

   public void setMode(EncodingMode mode) {
      if (mode == null) {
         super.setMode(null);
      } else {
         TianshuEncodingMode extended = TianshuEncodingMode.fromAe2(mode);
         if (this.isClientSide()) {
            super.setMode(mode);
            this.sendClientAction("setTianshuMode", extended);
         } else {
            this.alignNativeModeServer(extended, mode);
         }
      }
   }

   public boolean consumeTriggeredUpload() {
      if (this.isClientSide() && this.pendingTriggeredUpload) {
         this.expirePendingTriggeredUpload();
         if (this.triggeredUploadAck == this.expectedTriggeredUploadAck) {
            return false;
         } else {
            this.pendingTriggeredUpload = false;
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean hasPendingDirectUpload() {
      if (!this.isClientSide()) {
         return false;
      } else {
         this.expirePendingTriggeredUpload();
         return this.pendingTriggeredUpload && this.pendingDirectUpload;
      }
   }

   public boolean hasTriggeredUploadAck() {
      if (this.isClientSide() && this.pendingTriggeredUpload) {
         this.expirePendingTriggeredUpload();
         return this.pendingTriggeredUpload && this.triggeredUploadAck != this.expectedTriggeredUploadAck;
      } else {
         return false;
      }
   }

   public boolean hasFreshDirectUploadTargets() {
      return this.isClientSide() && this.directUploadTargetsRequested && this.uploadTargetsRevision != this.expectedDirectUploadTargetRevision;
   }

   public boolean requestDirectUploadTargetsAfterEncoding() {
      if (this.isClientSide() && this.hasPendingDirectUpload() && this.hasTriggeredUploadAck()) {
         if (!this.directUploadTargetsRequested) {
            this.expectedDirectUploadTargetRevision = this.uploadTargetsRevision;
            this.directUploadTargetsRequested = true;
            this.requestUploadTargets();
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean consumeDirectUploadRequest() {
      boolean direct = this.pendingDirectUpload;
      this.pendingDirectUpload = false;
      this.directUploadTargetsRequested = false;
      return direct;
   }

   public void clearClientUploadSelectionState() {
      if (this.isClientSide()) {
         this.pendingTriggeredUpload = false;
         this.pendingDirectUpload = false;
         this.directUploadTargetsRequested = false;
         this.uploadTargetGroups = List.of();
         this.uploadTargetsRevision++;
      }
   }

   private void expirePendingTriggeredUpload() {
      if (this.pendingTriggeredUpload && this.getPlayer().f_19797_ > this.pendingTriggeredUploadUntil) {
         this.pendingTriggeredUpload = false;
         this.pendingDirectUpload = false;
         this.directUploadTargetsRequested = false;
      }
   }

   public void setTianshuMode(TianshuEncodingMode mode) {
      if (mode != null) {
         if (this.isClientSide()) {
            this.tianshuMode = mode;
            this.sendClientAction("setTianshuMode", mode);
         } else {
            this.setTianshuModeServer(mode);
         }
      }
   }

   private void setTianshuModeServer(TianshuEncodingMode mode) {
      if (this.isServerSide() && mode != null) {
         if (mode.ae2Mode() != null) {
            this.alignNativeModeServer(mode, mode.ae2Mode());
         } else {
            this.applyTianshuModeState(mode);
         }

         this.m_38946_();
      }
   }

   private void alignNativeModeServer(TianshuEncodingMode mode, EncodingMode nativeMode) {
      this.applyTianshuModeState(mode);
      PatternEncodingLogic logic = this.tianshuHost.getLogic();
      if (logic.getMode() != nativeMode) {
         logic.setMode(nativeMode);
      }

      if (this.getMode() != nativeMode) {
         super.setMode(nativeMode);
      }
   }

   private void applyTianshuModeState(TianshuEncodingMode mode) {
      if (this.tianshuMode != mode) {
         this.resetProcessingEncodingType();
      }

      this.tianshuMode = mode;
      this.tianshuHost.setTianshuEncodingMode(mode);
   }

   public void multiplyProcessing(int factor) {
      if (this.isClientSide()) {
         this.sendClientAction("multiplyProcessing", factor);
      } else {
         this.multiplyProcessingServer(factor);
      }
   }

   private void multiplyProcessingServer(int factor) {
      if (this.isServerSide() && this.tianshuMode == TianshuEncodingMode.PROCESSING && validFactor(factor)) {
         PatternEncodingLogic logic = this.tianshuHost.getLogic();
         if (ProcessingPatternMultiplier.apply(logic.getEncodedInputInv(), logic.getEncodedOutputInv(), factor)) {
            this.m_38946_();
         }
      }
   }

   private static boolean validFactor(int factor) {
      return factor == 2 || factor == 4 || factor == 5 || factor == 10 || factor == -2 || factor == -4 || factor == -5 || factor == -10;
   }

   public void armAdvancedEncoding(ProcessingPatternEncodingType.AdvancedConfig config) {
      if (config != null) {
         if (this.isClientSide()) {
            this.updateAdvancedEncodingConfig(config);
            this.sendClientAction("armAdvancedEncoding", config);
         } else {
            this.armAdvancedEncodingServer(config);
         }
      }
   }

   private void armAdvancedEncodingServer(ProcessingPatternEncodingType.AdvancedConfig config) {
      if (this.isServerSide()
         && config != null
         && config.directions() != null
         && config.directions().length <= this.getProcessingInputSlots().length
         && validDirections(config.directions())
         && AdvancedAECompat.canEncode()
         && this.tianshuMode == TianshuEncodingMode.PROCESSING) {
         this.updateAdvancedEncodingConfig(config);
         this.persistProcessingDraft();
         this.m_38946_();
      }
   }

   public void armOverloadEncoding(ProcessingPatternEncodingType.OverloadConfig config) {
      if (config != null) {
         if (this.isClientSide()) {
            this.updateOverloadEncodingConfig(config);
            this.sendClientAction("armOverloadEncoding", config);
         } else {
            this.armOverloadEncodingServer(config);
         }
      }
   }

   private void armOverloadEncodingServer(ProcessingPatternEncodingType.OverloadConfig config) {
      if (this.isServerSide()
         && config != null
         && config.inputIdOnly() != null
         && config.outputIdOnly() != null
         && config.inputIdOnly().length <= this.getProcessingInputSlots().length
         && config.outputIdOnly().length <= this.getProcessingOutputSlots().length
         && validSlots(config.inputIdOnly(), this.getProcessingInputSlots().length)
         && validSlots(config.outputIdOnly(), this.getProcessingOutputSlots().length)
         && this.tianshuMode == TianshuEncodingMode.PROCESSING) {
         this.updateOverloadEncodingConfig(config);
         this.persistProcessingDraft();
         this.m_38946_();
      }
   }

   private void updateAdvancedEncodingConfig(ProcessingPatternEncodingType.AdvancedConfig config) {
      List<GenericStack> inputs = this.snapshotProcessingInputs();
      List<GenericStack> outputs = this.snapshotProcessingOutputs();
      ProcessingPatternEncodingType.OverloadConfig overload = this.processingDraftSync.matches(inputs, outputs)
         ? this.processingDraftSync.overloadConfig()
         : null;
      this.setProcessingDraft(inputs, outputs, config, overload);
   }

   private void updateOverloadEncodingConfig(ProcessingPatternEncodingType.OverloadConfig config) {
      List<GenericStack> inputs = this.snapshotProcessingInputs();
      List<GenericStack> outputs = this.snapshotProcessingOutputs();
      ProcessingPatternEncodingType.AdvancedConfig advanced = this.processingDraftSync.matches(inputs, outputs)
         ? this.processingDraftSync.advancedConfig()
         : null;
      this.setProcessingDraft(inputs, outputs, advanced, config);
   }

   private void setProcessingDraft(
      List<GenericStack> inputs,
      List<GenericStack> outputs,
      @Nullable ProcessingPatternEncodingType.AdvancedConfig advanced,
      @Nullable ProcessingPatternEncodingType.OverloadConfig overload
   ) {
      this.processingDraftSync = ProcessingPatternTerminalDraft.configured(inputs, outputs, advanced, overload);
      this.processingEncodingType = this.processingDraftSync.type();
   }

   @Nullable
   public ProcessingPatternEncodingType.AdvancedConfig getAdvancedEncodingConfig() {
      return this.processingDraftSync.advancedConfig();
   }

   @Nullable
   public ProcessingPatternEncodingType.OverloadConfig getOverloadEncodingConfig() {
      return this.processingDraftSync.overloadConfig();
   }

   private void resetProcessingEncodingType() {
      this.processingEncodingType = ProcessingPatternEncodingType.NORMAL;
      this.processingDraftSync = ProcessingPatternTerminalDraft.empty();
      if (this.isServerSide()) {
         this.tianshuHost.setProcessingPatternTerminalDraft(null);
      }
   }

   public void resetProcessingEncoding() {
      if (this.isClientSide()) {
         this.resetProcessingEncodingType();
         this.sendClientAction("resetProcessingEncoding");
      } else {
         this.resetProcessingEncodingServer();
      }
   }

   private void resetProcessingEncodingServer() {
      if (this.isServerSide()) {
         this.resetProcessingEncodingType();
         this.m_38946_();
      }
   }

   private void refreshProcessingDraftBinding() {
      if (this.processingEncodingType != ProcessingPatternEncodingType.NORMAL) {
         if (this.tianshuMode != TianshuEncodingMode.PROCESSING
            || this.processingDraftSync.type() != this.processingEncodingType
            || !this.processingDraftSync.matches(this.snapshotProcessingInputs(), this.snapshotProcessingOutputs())) {
            this.resetProcessingEncodingType();
         }
      }
   }

   private void restoreProcessingDraft(@Nullable ProcessingPatternTerminalDraft draft) {
      if (draft != null) {
         boolean supported = draft.type() != ProcessingPatternEncodingType.NORMAL && (!draft.type().hasAdvanced() || AdvancedAECompat.canEncode());
         if (this.tianshuMode == TianshuEncodingMode.PROCESSING
            && supported
            && draft.matches(this.snapshotProcessingInputs(), this.snapshotProcessingOutputs())) {
            this.processingDraftSync = draft;
            this.processingEncodingType = draft.type();
         } else {
            this.tianshuHost.setProcessingPatternTerminalDraft(null);
         }
      }
   }

   private void persistProcessingDraft() {
      this.tianshuHost.setProcessingPatternTerminalDraft(this.processingEncodingType == ProcessingPatternEncodingType.NORMAL ? null : this.processingDraftSync);
   }

   private List<GenericStack> snapshotProcessingInputs() {
      return snapshotProcessingInventory(this.tianshuHost.getLogic().getEncodedInputInv());
   }

   private List<GenericStack> snapshotProcessingOutputs() {
      return snapshotProcessingInventory(this.tianshuHost.getLogic().getEncodedOutputInv());
   }

   private static List<GenericStack> snapshotProcessingInventory(ConfigInventory inventory) {
      ArrayList<GenericStack> result = new ArrayList<>(inventory.size());

      for (int i = 0; i < inventory.size(); i++) {
         result.add(inventory.getStack(i));
      }

      return result;
   }

   private static boolean validDirections(int[] directions) {
      for (int direction : directions) {
         if (direction < 0 || direction > 6) {
            return false;
         }
      }

      return true;
   }

   private static boolean validSlots(int[] slots, int slotCount) {
      boolean[] seen = new boolean[slotCount];

      for (int slot : slots) {
         if (slot < 0 || slot >= slotCount || seen[slot]) {
            return false;
         }

         seen[slot] = true;
      }

      return true;
   }

   public void clear() {
      if (this.isClientSide()) {
         TianshuRecipeTransferContext.clear(this);
         this.clearClientUploadSelectionState();
      }

      this.resetProcessingEncodingType();
      super.clear();
   }

   public List<AppEngSlot> getClosedLoopMemberSlots() {
      return this.closedLoopMemberSlots;
   }

   public List<AppEngSlot> getClosedLoopOutputSlots() {
      return this.closedLoopOutputSlots;
   }

   public boolean markClosedLoopPrimaryOutput(ItemStack stack) {
      if (this.isClientSide()
         && this.tianshuMode == TianshuEncodingMode.CLOSED_LOOP
         && stack != null
         && !stack.m_41619_()
         && !this.closedLoopOutputSlots.isEmpty()
         && GenericStack.fromItemStack(stack) != null) {
         ((TianshuPatternEncodingTermMenu.ClosedLoopOutputSlot)this.closedLoopOutputSlots.get(0)).setFilterTo(stack);
         return true;
      } else {
         return false;
      }
   }

   public boolean hasClosedLoopPrimaryOutputMark() {
      return this.getMarkedClosedLoopPrimaryOutput() != null;
   }

   public FakeSlot getGlobalReserveMarkSlot() {
      return this.globalReserveMarkSlot;
   }

   public void requestClosedLoopResultPage(ClosedLoopResultPage.Kind kind, int offset) {
      if (this.isClientSide() && kind != null) {
         PacketSender.sendToServer(new RequestClosedLoopResultPagePacket(this.f_38840_, kind, offset));
      }
   }

   public void sendClosedLoopResultPage(ServerPlayer player, ClosedLoopResultPage.Kind kind, int offset) {
      if (this.isServerSide() && player != null && kind != null) {
         List<GenericStack> source = kind == ClosedLoopResultPage.Kind.EXTERNAL_INPUTS ? this.closedLoopExternalInputs : this.closedLoopSeeds;
         PacketSender.sendToPlayer(
            player, new ClosedLoopResultPagePacket(this.f_38840_, ClosedLoopResultPage.from(this.closedLoopResultRevision, kind, source, offset))
         );
      }
   }

   public void receiveClosedLoopResultPage(ClosedLoopResultPage page) {
      if (this.isClientSide() && page != null && page.revision() >= this.closedLoopResultRevision) {
         this.closedLoopResultPages.put(page.kind(), page);
      }
   }

   @Nullable
   public ClosedLoopResultPage getClosedLoopResultPage(ClosedLoopResultPage.Kind kind, int offset) {
      ClosedLoopResultPage page = this.closedLoopResultPages.get(kind);
      return page != null && page.revision() == this.closedLoopResultRevision && page.offset() == offset ? page : null;
   }

   public long getClosedLoopMemberCopies(int slot) {
      return slot >= 0 && slot < this.closedLoopMemberCopies.length ? this.closedLoopMemberCopies[slot] : 0L;
   }

   public int getClosedLoopOutputRole(int slot) {
      return slot >= 0 && slot < this.closedLoopOutputRoles.length ? this.closedLoopOutputRoles[slot] : 0;
   }

   public void setClosedLoopMemberCopies(int slot, long copies) {
      if (this.isClientSide()) {
         this.sendClientAction("setClosedLoopMemberCopies", new TianshuPatternEncodingTermMenu.ClosedLoopMemberEdit(slot, copies));
      } else {
         this.setClosedLoopMemberCopiesServer(new TianshuPatternEncodingTermMenu.ClosedLoopMemberEdit(slot, copies));
      }
   }

   private void setClosedLoopMemberCopiesServer(TianshuPatternEncodingTermMenu.ClosedLoopMemberEdit edit) {
      if (this.isServerSide()
         && this.tianshuMode == TianshuEncodingMode.CLOSED_LOOP
         && edit != null
         && edit.slot() >= 0
         && edit.slot() < 27
         && edit.copies() >= 1L
         && edit.copies() != Long.MAX_VALUE) {
         if (!this.closedLoopMemberInventory.getStackInSlot(edit.slot()).m_41619_()) {
            this.closedLoopMemberCopies[edit.slot()] = edit.copies();
            this.closedLoopDraftRepresentsEncoded = false;
            this.closedLoopDraftDirty = true;
            this.m_38946_();
         }
      }
   }

   public void moveClosedLoopMember(int slot, int direction) {
      if (this.isClientSide()) {
         this.sendClientAction("moveClosedLoopMember", new TianshuPatternEncodingTermMenu.ClosedLoopMemberMove(slot, direction));
      } else {
         this.moveClosedLoopMemberServer(new TianshuPatternEncodingTermMenu.ClosedLoopMemberMove(slot, direction));
      }
   }

   private void moveClosedLoopMemberServer(TianshuPatternEncodingTermMenu.ClosedLoopMemberMove move) {
      if (this.isServerSide() && this.tianshuMode == TianshuEncodingMode.CLOSED_LOOP && move != null && (move.direction() == -1 || move.direction() == 1)) {
         int source = move.slot();
         int target = source + move.direction();
         if (source >= 0 && source < 27 && target >= 0 && target < 27) {
            this.closedLoopBulkUpdating = true;

            try {
               ItemStack left = this.closedLoopMemberInventory.getStackInSlot(source).m_41777_();
               ItemStack right = this.closedLoopMemberInventory.getStackInSlot(target).m_41777_();
               this.closedLoopMemberInventory.setItemDirect(source, right);
               this.closedLoopMemberInventory.setItemDirect(target, left);
               long copies = this.closedLoopMemberCopies[source];
               this.closedLoopMemberCopies[source] = this.closedLoopMemberCopies[target];
               this.closedLoopMemberCopies[target] = copies;
            } finally {
               this.closedLoopBulkUpdating = false;
            }

            this.closedLoopDraftRepresentsEncoded = false;
            this.closedLoopDraftDirty = true;
            this.m_38946_();
         }
      }
   }

   public void setClosedLoopMultipliers(int execution, int stored) {
      if (this.isClientSide()) {
         this.sendClientAction("setClosedLoopMultipliers", new TianshuPatternEncodingTermMenu.ClosedLoopMultiplierEdit(execution, stored));
      } else {
         this.setClosedLoopMultipliersServer(new TianshuPatternEncodingTermMenu.ClosedLoopMultiplierEdit(execution, stored));
      }
   }

   private void setClosedLoopMultipliersServer(TianshuPatternEncodingTermMenu.ClosedLoopMultiplierEdit edit) {
      if (this.isServerSide() && this.tianshuMode == TianshuEncodingMode.CLOSED_LOOP && edit != null && edit.execution() >= 1 && edit.stored() >= 1) {
         this.closedLoopExecutionSeedMultiplier = edit.execution();
         this.closedLoopStoredTaskMultiplier = edit.stored();
         this.closedLoopSeedMultiplier = this.closedLoopExecutionSeedMultiplier;
         this.closedLoopDraftRepresentsEncoded = false;
         this.closedLoopDraftDirty = true;
         this.m_38946_();
      }
   }

   public void selectClosedLoopCandidate(int delta) {
      if (this.isClientSide()) {
         this.sendClientAction("selectClosedLoopCandidate", delta);
      } else {
         this.selectClosedLoopCandidateServer(delta);
      }
   }

   public void autoFillClosedLoop() {
      if (this.isClientSide()) {
         this.sendClientAction("autoFillClosedLoop");
      } else {
         this.autoFillClosedLoopServer();
      }
   }

   public void cycleClosedLoopOutput() {
      if (this.isClientSide()) {
         this.sendClientAction("cycleClosedLoopOutput");
      } else {
         this.cycleClosedLoopOutputServer();
      }
   }

   public boolean canCycleClosedLoopOutputs() {
      if (this.tianshuMode != TianshuEncodingMode.CLOSED_LOOP) {
         return false;
      } else {
         int outputCount = 0;

         for (int i = 0; i < 9; i++) {
            if (!this.closedLoopOutputInventory.getStackInSlot(i).m_41619_()) {
               outputCount++;
            }
         }

         return outputCount > 1;
      }
   }

   public void clearClosedLoopDraft() {
      if (this.isClientSide()) {
         this.sendClientAction("clearClosedLoopDraft");
      } else {
         this.clearClosedLoopDraftServer();
      }
   }

   private void clearClosedLoopDraftServer() {
      if (this.isServerSide() && this.tianshuMode == TianshuEncodingMode.CLOSED_LOOP) {
         this.resetClosedLoopDraft();
         this.uploadState = 0;
         this.seedRefillSync = SeedRefillSync.none();
         this.m_38946_();
      }
   }

   private void autoFillClosedLoopServer() {
      if (this.isServerSide() && this.tianshuMode == TianshuEncodingMode.CLOSED_LOOP) {
         if (!this.closedLoopCandidates.isEmpty()) {
            this.selectClosedLoopCandidateServer(1);
         } else {
            GenericStack markedOutput = this.getMarkedClosedLoopPrimaryOutput();
            if (markedOutput != null) {
               this.resetClosedLoopDraft();
               this.setClosedLoopPrimaryOutputMarker(markedOutput);
               this.refreshClosedLoops(markedOutput.what());
               this.m_38946_();
            }
         }
      }
   }

   private void cycleClosedLoopOutputServer() {
      if (this.isServerSide() && this.canCycleClosedLoopOutputs()) {
         ItemStack[] rotated = new ItemStack[9];

         for (int i = 0; i < 9; i++) {
            rotated[i] = ItemStack.f_41583_;
            if (!this.closedLoopOutputInventory.getStackInSlot(i).m_41619_()) {
               for (int offset = 1; offset < 9; offset++) {
                  ItemStack next = this.closedLoopOutputInventory.getStackInSlot((i + offset) % 9);
                  if (!next.m_41619_()) {
                     rotated[i] = next.m_41777_();
                     break;
                  }
               }
            }
         }

         this.closedLoopBulkUpdating = true;

         try {
            Arrays.fill(this.closedLoopOutputRoles, 0);

            for (int ix = 0; ix < 9; ix++) {
               this.closedLoopOutputInventory.setItemDirect(ix, rotated[ix]);
               if (!rotated[ix].m_41619_()) {
                  this.closedLoopOutputRoles[ix] = ix == 0 ? 1 : 2;
               }
            }
         } finally {
            this.closedLoopBulkUpdating = false;
         }

         GenericStack var9 = this.getMarkedClosedLoopPrimaryOutput();
         this.closedLoopMainOutput = var9 != null ? var9.what() : null;
         this.closedLoopCandidates = List.of();
         this.closedLoopCandidateCount = 0;
         this.closedLoopCandidateIndex = 0;
         this.closedLoopDraftRepresentsEncoded = false;
         this.closedLoopDraftDirty = true;
         this.uploadState = 0;
         this.seedRefillSync = SeedRefillSync.none();
         this.m_38946_();
      }
   }

   public void refillClosedLoopSeeds() {
      if (this.isClientSide()) {
         this.sendClientAction("refillClosedLoopSeeds");
      } else {
         this.refillClosedLoopSeedsServer();
      }
   }

   private void refillClosedLoopSeedsServer() {
      if (this.isServerSide() && this.tianshuMode == TianshuEncodingMode.CLOSED_LOOP) {
         TianshuSeedRefillService.RefillResult result = TianshuSeedRefillService.refillAll(this.resolveOrBindTianshu());
         this.seedRefillSync = SeedRefillSync.of(result);
         this.uploadState = 0;
         this.m_38946_();
      }
   }

   private void selectClosedLoopCandidateServer(int delta) {
      if (this.isServerSide() && !this.closedLoopCandidates.isEmpty()) {
         this.closedLoopCandidateIndex = Math.floorMod(this.closedLoopCandidateIndex + Integer.signum(delta), this.closedLoopCandidates.size());
         this.fillClosedLoopDraftFromSelectedCandidate();
         this.m_38946_();
      }
   }

   public void changeClosedLoopExecutionSeedMultiplier(int delta) {
      if (this.isClientSide()) {
         this.sendClientAction("changeClosedLoopExecutionSeedMultiplier", delta);
      } else {
         this.changeClosedLoopExecutionSeedMultiplierServer(delta);
      }
   }

   private void changeClosedLoopExecutionSeedMultiplierServer(int delta) {
      if (this.isServerSide() && delta != 0) {
         this.closedLoopExecutionSeedMultiplier = adjustPositiveMultiplier(this.closedLoopExecutionSeedMultiplier, delta);
         this.closedLoopSeedMultiplier = this.closedLoopExecutionSeedMultiplier;
         this.closedLoopDraftRepresentsEncoded = false;
         this.closedLoopDraftDirty = true;
         this.m_38946_();
      }
   }

   @Deprecated
   public void changeClosedLoopSeedMultiplier(int delta) {
      if (this.isClientSide()) {
         this.sendClientAction("changeClosedLoopSeedMultiplier", delta);
      } else {
         this.changeClosedLoopSeedMultiplierServer(delta);
      }
   }

   private void changeClosedLoopSeedMultiplierServer(int delta) {
      this.changeClosedLoopExecutionSeedMultiplierServer(delta);
   }

   public void changeClosedLoopStoredTaskMultiplier(int delta) {
      if (this.isClientSide()) {
         this.sendClientAction("changeClosedLoopStoredTaskMultiplier", delta);
      } else {
         this.changeClosedLoopStoredTaskMultiplierServer(delta);
      }
   }

   private void changeClosedLoopStoredTaskMultiplierServer(int delta) {
      if (this.isServerSide() && delta != 0) {
         this.closedLoopStoredTaskMultiplier = adjustPositiveMultiplier(this.closedLoopStoredTaskMultiplier, delta);
         this.closedLoopDraftRepresentsEncoded = false;
         this.closedLoopDraftDirty = true;
         this.m_38946_();
      }
   }

   private static int adjustPositiveMultiplier(int value, int delta) {
      return (int)Math.max(1L, Math.min(2147483647L, (long)value + (long)delta));
   }

   public void uploadEncodedPattern() {
      if (this.isClientSide()) {
         this.uploadState = 2;
         this.sendClientAction("uploadEncodedPattern", this.tianshuSelectionRevision);
      } else {
         this.uploadEncodedPatternServer(this.tianshuSelectionRevision);
      }
   }

   private void uploadEncodedPatternServer(int expectedSelectionRevision) {
      if (this.isServerSide() && expectedSelectionRevision == this.tianshuSelectionRevision) {
         this.uploadState = 2;
         ItemStack stack = this.tianshuHost.getLogic().getEncodedPatternInv().getStackInSlot(0);
         switch (TianshuPatternUploadRouting.classify(stack, this.getPlayer().m_9236_())) {
            case CLOSED_LOOP_STORAGE:
               this.uploadClosedLoopPatternServer(stack);
               break;
            case CRAFTING_ASSEMBLER:
               if (this.getPlayer() instanceof ServerPlayer player) {
                  this.uploadCraftingPatternServer(player, stack);
               } else {
                  this.finishUpload(false);
               }
               break;
            case PROCESSING_PROVIDER:
            case INVALID:
               this.finishUpload(false);
         }
      }
   }

   private void uploadClosedLoopPatternServer(ItemStack stack) {
      if (!(stack.m_41720_() instanceof ClosedLoopPatternItem item)) {
         this.finishUpload(false);
      } else {
         InternalInventory sourceInventory = this.tianshuHost.getLogic().getEncodedPatternInv();
         ItemStack removed = sourceInventory.extractItem(0, 1, false);
         if (!removed.m_41619_() && ItemStack.m_150942_(stack, removed)) {
            TianshuSupercomputerPortBlockEntity target = this.resolveOrBindTianshu();
            ClosedLoopPatternPayload payload = item.readPayload(removed, this.getPlayer().m_9236_()).orElse(null);
            ClosedLoopPatternRepository.PutResult result = ClosedLoopPatternUploadService.upload(target, payload);
            boolean success = result == ClosedLoopPatternRepository.PutResult.ADDED || result == ClosedLoopPatternRepository.PutResult.UPDATED;
            if (!success) {
               sourceInventory.addItems(removed);
            }

            this.finishUpload(success);
         } else {
            if (!removed.m_41619_()) {
               sourceInventory.addItems(removed);
            }

            this.finishUpload(false);
         }
      }
   }

   @Nullable
   private TianshuSupercomputerPortBlockEntity resolveOrBindTianshu() {
      TianshuSupercomputerPortBlockEntity resolved = this.resolveBoundTianshu();
      if (resolved == null && this.boundTianshuTarget == null) {
         List<TianshuSupercomputerPortBlockEntity> available = this.tianshuHost.getAvailableTianshu();
         if (available.isEmpty()) {
            return null;
         } else {
            TianshuSupercomputerPortBlockEntity selected = available.get(0);
            this.boundTianshuTarget = TianshuTerminalTarget.from(selected);
            this.tianshuSelectionRevision++;
            this.maintenanceEditorData = null;
            this.lastSentMaintenanceSummary = null;
            this.lastMaintenanceSummaryTick = Integer.MIN_VALUE;
            this.uploadState = 0;
            this.seedRefillSync = SeedRefillSync.none();
            return selected;
         }
      } else {
         return resolved;
      }
   }

   private void finishUpload(boolean success) {
      this.settleNetworkBlankCharge(success);
      this.uploadState = success ? 1 : 3;
      this.m_38946_();
   }

   public void requestUploadTargets() {
      if (this.isClientSide()) {
         PacketSender.sendToServer(new RequestUploadTargetsPacket(this.f_38840_));
      }
   }

   public void sendUploadTargets(ServerPlayer player) {
      if (this.isServerSide() && player != null) {
         this.refreshUploadTargetsNow();
         PacketSender.sendToPlayer(player, new UploadTargetsSyncPacket(this.f_38840_, this.uploadTargetGroups));
      }
   }

   public void receiveUploadTargets(List<TianshuUploadTargetData> targets) {
      if (this.isClientSide()) {
         this.uploadTargetGroups = targets == null ? List.of() : List.copyOf(targets);
         this.uploadTargetsRevision++;
      }
   }

   public List<TianshuUploadTargetData> getUploadTargets() {
      return this.uploadTargetGroups;
   }

   public int getUploadTargetsRevision() {
      return this.uploadTargetsRevision;
   }

   public void uploadTianshuPatternToTarget(PatternContainerGroup group) {
      if (this.isClientSide() && group != null) {
         this.uploadState = 2;
         PacketSender.sendToServer(new UploadPatternToTargetPacket(this.f_38840_, group));
      }
   }

   public void uploadTianshuPatternToTarget(ServerPlayer player, PatternContainerGroup group) {
      if (this.isServerSide() && player != null && group != null) {
         this.uploadState = 2;
         ItemStack stack = this.tianshuHost.getLogic().getEncodedPatternInv().getStackInSlot(0);
         if (TianshuPatternUploadRouting.classify(stack, this.getPlayer().m_9236_()) != TianshuPatternUploadRouting.Route.PROCESSING_PROVIDER) {
            this.finishProviderUpload(player, false);
         } else {
            IGridNode node = this.tianshuHost.getActionableNode();
            IGrid grid = node != null ? node.getGrid() : null;
            List<TianshuPatternEncodingTermMenu.BoundUploadSlot> targets = this.boundUploadTargets.get(group);
            if (grid != null && targets != null) {
               for (TianshuPatternEncodingTermMenu.BoundUploadSlot binding : targets) {
                  PatternContainer target = binding.target();

                  try {
                     if (target.getGrid() != grid || !target.isVisibleInTerminal()) {
                        continue;
                     }

                     InternalInventory inventory = target.getTerminalPatternInventory();
                     int slot = binding.slot();
                     if (slot < 0 || slot >= inventory.size() || !inventory.getStackInSlot(slot).m_41619_() || !inventory.isItemValid(slot, stack)) {
                        continue;
                     }
                  } catch (RuntimeException var12) {
                     continue;
                  }

                  this.uploadToProvider(player, target, binding.slot(), stack);
                  return;
               }

               this.finishProviderUpload(player, false);
               return;
            } else {
               this.finishProviderUpload(player, false);
            }
         }
      }
   }

   private void uploadCraftingPatternServer(ServerPlayer player, ItemStack stack) {
      this.refreshUploadTargetsNow();
      if (!this.uploadCraftingPatternToFirstTarget(player, stack, true)) {
         if (!this.uploadCraftingPatternToFirstTarget(player, stack, false)) {
            this.finishProviderUpload(player, false);
         }
      }
   }

   private boolean uploadCraftingPatternToFirstTarget(ServerPlayer player, ItemStack stack, boolean matrixTarget) {
      for (PatternContainer target : this.uploadTargets) {
         PatternContainerGroup group = target.getTerminalGroup();
         if (TianshuPatternUploadRouting.isCraftingUploadGroup(group) && TianshuPatternUploadRouting.isMatterWarpingMatrixGroup(group) == matrixTarget) {
            int free = firstFreePatternSlot(target.getTerminalPatternInventory(), stack);
            if (free >= 0) {
               this.uploadToProvider(player, target, free, stack);
               return true;
            }
         }
      }

      return false;
   }

   private void uploadToProvider(ServerPlayer player, PatternContainer selected, int selectedSlot, ItemStack stack) {
      InternalInventory sourceInventory = this.tianshuHost.getLogic().getEncodedPatternInv();
      ItemStack removed = sourceInventory.extractItem(0, 1, false);
      if (!removed.m_41619_() && ItemStack.m_150942_(stack, removed)) {
         InternalInventory targetInventory = selected.getTerminalPatternInventory();

         try {
            ItemStack remaining = targetInventory.insertItem(selectedSlot, removed, false);
            if (!remaining.m_41619_()) {
               sourceInventory.addItems(remaining);
               this.finishProviderUpload(player, false);
               return;
            }

            if (selected instanceof PatternProviderLogicHost logicHost) {
               logicHost.saveChanges();
            }

            this.finishProviderUpload(player, true);
         } catch (RuntimeException var11) {
            try {
               if (!ItemStack.m_150942_(targetInventory.getStackInSlot(selectedSlot), removed)) {
                  sourceInventory.addItems(removed);
               }
            } catch (RuntimeException var10) {
            }

            this.finishProviderUpload(player, false);
         }
      } else {
         if (!removed.m_41619_()) {
            sourceInventory.addItems(removed);
         }

         this.finishProviderUpload(player, false);
      }
   }

   private void finishProviderUpload(ServerPlayer player, boolean success) {
      this.settleNetworkBlankCharge(success);
      this.uploadState = success ? 1 : 3;
      this.refreshUploadTargetsNow();
      PacketSender.sendToPlayer(player, new UploadTargetsSyncPacket(this.f_38840_, this.uploadTargetGroups));
      this.m_38946_();
   }

   private void refreshUploadTargetsNow() {
      this.refreshUploadTargetsNow(this.tianshuHost.getLogic().getEncodedPatternInv().getStackInSlot(0));
   }

   private void refreshUploadTargetsNow(ItemStack stack) {
      this.uploadTargets = this.discoverUploadTargets();
      if (this.uploadTargets.isEmpty()) {
         this.boundUploadTargets = Map.of();
         this.uploadTargetGroups = List.of();
      } else {
         LinkedHashMap<PatternContainerGroup, TianshuPatternEncodingTermMenu.MutableUploadGroup> groups = new LinkedHashMap<>();

         for (PatternContainer target : this.uploadTargets) {
            PatternContainerGroup group = target.getTerminalGroup();
            TianshuPatternEncodingTermMenu.MutableUploadGroup summary = groups.computeIfAbsent(
               group, ignored -> new TianshuPatternEncodingTermMenu.MutableUploadGroup()
            );
            summary.providers++;
            InternalInventory inventory = target.getTerminalPatternInventory();
            if (inventory != null && stack != null && !stack.m_41619_()) {
               for (int i = 0; i < inventory.size(); i++) {
                  if (inventory.getStackInSlot(i).m_41619_() && inventory.isItemValid(i, stack)) {
                     summary.slots.add(new TianshuPatternEncodingTermMenu.BoundUploadSlot(target, i));
                     summary.availableSlots++;
                  }
               }
            }
         }

         HashMap<PatternContainerGroup, List<TianshuPatternEncodingTermMenu.BoundUploadSlot>> bindings = new HashMap<>();
         groups.forEach((groupx, summaryx) -> bindings.put(groupx, List.copyOf(summaryx.slots)));
         this.boundUploadTargets = bindings;
         this.uploadTargetGroups = groups.entrySet()
            .stream()
            .map(entry -> new TianshuUploadTargetData(entry.getKey(), entry.getValue().providers, entry.getValue().availableSlots))
            .toList();
      }
   }

   private List<PatternContainer> discoverUploadTargets() {
      return this.discoverUploadTargets(false);
   }

   private List<PatternContainer> discoverUploadTargets(boolean diagnostics) {
      IGridNode node = this.tianshuHost.getActionableNode();
      IGrid grid = GridNodeAccess.getActiveGrid(node);
      if (grid == null) {
         if (diagnostics) {
            DUPLICATE_LOG.warn("Target scan found no grid (nodePresent={})", node != null);
         }

         return List.of();
      } else {
         ArrayList<PatternContainer> found = new ArrayList<>();
         int machineClasses = 0;
         int patternContainerClasses = 0;
         int activeContainers = 0;
         int hiddenContainers = 0;
         int foreignGridContainers = 0;
         int emptyInventories = 0;

         for (Class<?> machineClass : grid.getMachineClasses()) {
            machineClasses++;
            if (PatternContainer.class.isAssignableFrom(machineClass)) {
               patternContainerClasses++;

               for (PatternContainer container : grid.getActiveMachines(machineClass)) {
                  activeContainers++;
                  if (!container.isVisibleInTerminal()) {
                     hiddenContainers++;
                  } else if (container.getGrid() != grid) {
                     foreignGridContainers++;
                  } else {
                     InternalInventory inv = container.getTerminalPatternInventory();
                     if (inv != null && inv.size() > 0) {
                        found.add(container);
                     } else {
                        emptyInventories++;
                     }
                  }
               }
            }
         }

         found.sort(
            Comparator.<PatternContainer, String>comparing(host -> host.getTerminalGroup().name().getString())
               .thenComparingLong(PatternContainer::getTerminalSortOrder)
         );
         if (diagnostics) {
            DUPLICATE_LOG.debug(
               "Target scan: machineClasses={}, patternContainerClasses={}, activeContainers={}, eligibleTargets={}, hidden={}, foreignGrid={}, missingOrEmptyInventory={}",
               new Object[]{machineClasses, patternContainerClasses, activeContainers, found.size(), hiddenContainers, foreignGridContainers, emptyInventories}
            );
         }

         return List.copyOf(found);
      }
   }

   private static int firstFreePatternSlot(InternalInventory inventory, ItemStack stack) {
      for (int i = 0; i < inventory.size(); i++) {
         if (inventory.getStackInSlot(i).m_41619_() && inventory.isItemValid(i, stack)) {
            return i;
         }
      }

      return -1;
   }

   private void refreshDerivedConfiguration() {
      ItemStack source = this.tianshuHost.getLogic().getEncodedPatternInv().getStackInSlot(0);
      if (!this.refundableEncodedPattern.m_41619_()
         && !ItemStack.m_150942_(this.refundableEncodedPattern, source)
         && (!source.m_41619_() || this.uploadState != 2)) {
         this.refundableEncodedPattern = ItemStack.f_41583_;
      }

      if (!ItemStack.m_41728_(this.configuredSource, source)) {
         boolean wasEncodedClosedLoop = this.configuredSource.m_41720_() instanceof ClosedLoopPatternItem;
         this.configuredSource = source.m_41777_();
         this.encodedClosedLoop = source.m_41720_() instanceof ClosedLoopPatternItem;
         if (!source.m_41619_()) {
            this.uploadState = 0;
            this.seedRefillSync = SeedRefillSync.none();
            this.resetProcessingEncodingType();
         }

         if (!source.m_41619_() && source.m_41720_() instanceof ClosedLoopPatternItem) {
            if (source.m_41720_() instanceof ClosedLoopPatternItem closedLoopItem) {
               this.selectInsertedPatternMode(TianshuEncodingMode.CLOSED_LOOP);
               this.resetClosedLoopDraft();
               ClosedLoopPatternPayload payload = closedLoopItem.readPayload(source, this.getPlayer().m_9236_()).orElse(null);
               if (payload != null) {
                  this.closedLoopExecutionSeedMultiplier = payload.executionSeedMultiplier();
                  this.closedLoopSeedMultiplier = this.closedLoopExecutionSeedMultiplier;
                  this.closedLoopStoredTaskMultiplier = payload.storedTaskMultiplier();
                  this.fillClosedLoopDraft(payload);
                  this.closedLoopDraftRepresentsEncoded = true;
               } else {
                  this.closedLoopDraftStatus = ClosedLoopDraftStatus.MEMBER_UNDECODABLE;
                  this.closedLoopEncodeState = 1;
               }
            }
         } else {
            if (wasEncodedClosedLoop) {
               this.closedLoopDraftRepresentsEncoded = false;
               this.closedLoopDraftDirty = true;
            }

            if (!source.m_41619_()) {
               this.restoreInsertedProcessingPattern(source);
            }
         }
      }
   }

   private void restoreInsertedProcessingPattern(ItemStack source) {
      try {
         IPatternDetails details = PatternDetailsHelper.decodePattern(source, this.getPlayer().m_9236_());
         if (details == null) {
            return;
         }

         AdvancedAECompat.EditableProcessingPattern advanced = AdvancedAECompat.restoreForEditing(
            details, this.getProcessingInputSlots().length, this.getProcessingOutputSlots().length
         );
         if (source.m_41720_() instanceof OverloadPatternItem overloadItem) {
            EditableOverloadPatternState restored = this.conversionService
               .restoreEditableState(overloadItem, source, new Ae2PlainPatternResolver(this.getPlayer().m_9236_()))
               .orElse(null);
            if (restored != null && this.replaceProcessingInventories(restored.parsedPattern())) {
               ProcessingPatternEncodingType.AdvancedConfig advancedConfig = advanced == null
                  ? null
                  : new ProcessingPatternEncodingType.AdvancedConfig(advanced.directions());
               ProcessingPatternEncodingType.OverloadConfig overloadConfig = restoreOverloadConfig(
                  restored.encodedPattern(), this.getProcessingInputSlots().length, this.getProcessingOutputSlots().length
               );
               this.selectInsertedPatternMode(TianshuEncodingMode.PROCESSING);
               this.setProcessingDraft(this.snapshotProcessingInputs(), this.snapshotProcessingOutputs(), advancedConfig, overloadConfig);
               this.persistProcessingDraft();
               return;
            }

            return;
         }

         if (advanced != null && this.replaceProcessingInventories(advanced.inputs(), advanced.outputs())) {
            this.selectInsertedPatternMode(TianshuEncodingMode.PROCESSING);
            this.setProcessingDraft(
               this.snapshotProcessingInputs(), this.snapshotProcessingOutputs(), new ProcessingPatternEncodingType.AdvancedConfig(advanced.directions()), null
            );
            this.persistProcessingDraft();
         }
      } catch (RuntimeException var8) {
      }
   }

   private boolean replaceProcessingInventories(ParsedPatternDefinition pattern) {
      List<GenericStack> inputs = nullableStackList(this.tianshuHost.getLogic().getEncodedInputInv().size());
      List<GenericStack> outputs = nullableStackList(this.tianshuHost.getLogic().getEncodedOutputInv().size());

      for (ParsedPatternInput input : pattern.inputs()) {
         if (input.slotIndex() >= inputs.size()) {
            return false;
         }

         GenericStack stack = GenericStack.fromItemStack(input.stack());
         if (stack == null) {
            return false;
         }

         inputs.set(input.slotIndex(), stack);
      }

      for (ParsedPatternOutput output : pattern.outputs()) {
         if (output.slotIndex() >= outputs.size()) {
            return false;
         }

         GenericStack stack = GenericStack.fromItemStack(output.stack());
         if (stack == null) {
            return false;
         }

         outputs.set(output.slotIndex(), stack);
      }

      return this.replaceProcessingInventories(inputs, outputs);
   }

   private boolean replaceProcessingInventories(List<GenericStack> inputs, List<GenericStack> outputs) {
      ConfigInventory inputInventory = this.tianshuHost.getLogic().getEncodedInputInv();
      ConfigInventory outputInventory = this.tianshuHost.getLogic().getEncodedOutputInv();
      if (inputs.size() <= inputInventory.size() && outputs.size() <= outputInventory.size()) {
         inputInventory.clear();
         outputInventory.clear();

         for (int i = 0; i < inputs.size(); i++) {
            inputInventory.setStack(i, inputs.get(i));
         }

         for (int i = 0; i < outputs.size(); i++) {
            outputInventory.setStack(i, outputs.get(i));
         }

         return true;
      } else {
         return false;
      }
   }

   private static List<GenericStack> nullableStackList(int size) {
      return new ArrayList<>(Collections.nCopies(Math.max(0, size), null));
   }

   private static ProcessingPatternEncodingType.OverloadConfig restoreOverloadConfig(EncodedOverloadPattern pattern, int inputSlots, int outputSlots) {
      for (OverloadPatternSlot slot : pattern.inputSlots()) {
         checkedPatternSlot(slot.slotIndex(), inputSlots);
      }

      for (OverloadPatternSlot slot : pattern.outputSlots()) {
         checkedPatternSlot(slot.slotIndex(), outputSlots);
      }

      int[] idOnlyInputs = pattern.inputSlots()
         .stream()
         .filter(slot -> slot.matchMode() == MatchMode.ID_ONLY)
         .mapToInt(slot -> checkedPatternSlot(slot.slotIndex(), inputSlots))
         .toArray();
      int[] idOnlyOutputs = pattern.outputSlots()
         .stream()
         .filter(slot -> slot.matchMode() == MatchMode.ID_ONLY)
         .mapToInt(slot -> checkedPatternSlot(slot.slotIndex(), outputSlots))
         .toArray();
      return new ProcessingPatternEncodingType.OverloadConfig(idOnlyInputs, idOnlyOutputs);
   }

   private static int checkedPatternSlot(int slot, int slotCount) {
      if (slot >= 0 && slot < slotCount) {
         return slot;
      } else {
         throw new IllegalArgumentException("overload slot is outside the terminal draft");
      }
   }

   private void selectInsertedPatternMode(TianshuEncodingMode mode) {
      this.tianshuMode = mode;
      this.tianshuHost.setTianshuEncodingMode(mode);
      if (mode.ae2Mode() != null) {
         this.tianshuHost.getLogic().setMode(mode.ae2Mode());
         super.setMode(mode.ae2Mode());
      }
   }

   private void refreshClosedLoops(AEKey primaryOutput) {
      IGridNode node = this.tianshuHost.getActionableNode();
      IGrid grid = GridNodeAccess.getActiveGrid(node);
      if (primaryOutput != null) {
         this.closedLoopMainOutput = primaryOutput;
         if (grid == null) {
            this.closedLoopDraftStatus = ClosedLoopDraftStatus.NO_CANDIDATE;
         } else {
            ClosedLoopDiscoveryService.DiscoveryResult discovery = ClosedLoopDiscoveryService.discoverDetailed(
               grid.getCraftingService(), this.getPlayer().m_9236_(), primaryOutput
            );
            this.closedLoopCandidates = discovery.candidates();
            this.closedLoopCandidateCount = this.closedLoopCandidates.size();
            if (this.closedLoopCandidates.isEmpty() && discovery.rejectedUndecodablePattern()) {
               this.closedLoopDraftStatus = ClosedLoopDraftStatus.MEMBER_UNDECODABLE;
               this.closedLoopEncodeState = 1;
            } else if (this.closedLoopCandidates.isEmpty()) {
               this.closedLoopDraftStatus = ClosedLoopDraftStatus.NO_CANDIDATE;
            }

            this.fillClosedLoopDraftFromSelectedCandidate();
         }
      }
   }

   private void fillClosedLoopDraftFromSelectedCandidate() {
      if (!this.closedLoopCandidates.isEmpty()) {
         int index = Math.max(0, Math.min(this.closedLoopCandidateIndex, this.closedLoopCandidates.size() - 1));
         this.fillClosedLoopDraft(this.closedLoopCandidates.get(index).payload());
         this.closedLoopDraftRepresentsEncoded = false;
      }
   }

   @Nullable
   private GenericStack getMarkedClosedLoopPrimaryOutput() {
      GenericStack output = GenericStack.fromItemStack(this.closedLoopOutputInventory.getStackInSlot(0));
      return output != null && output.what() != null ? output : null;
   }

   private void setClosedLoopPrimaryOutputMarker(GenericStack output) {
      if (output != null && output.what() != null) {
         this.closedLoopBulkUpdating = true;

         try {
            clearInventory(this.closedLoopOutputInventory);
            Arrays.fill(this.closedLoopOutputRoles, 0);
            this.closedLoopOutputInventory.setItemDirect(0, GenericStack.wrapInItemStack(output));
            this.closedLoopOutputRoles[0] = 1;
            this.closedLoopMainOutput = output.what();
         } finally {
            this.closedLoopBulkUpdating = false;
         }
      }
   }

   private void onClosedLoopPrimaryOutputMarked() {
      if (this.isServerSide() && !this.closedLoopBulkUpdating && this.tianshuMode == TianshuEncodingMode.CLOSED_LOOP) {
         GenericStack marked = GenericStack.fromItemStack(this.closedLoopOutputInventory.getStackInSlot(0));
         this.closedLoopBulkUpdating = true;

         try {
            for (int i = 1; i < 9; i++) {
               this.closedLoopOutputInventory.setItemDirect(i, ItemStack.f_41583_);
            }
         } finally {
            this.closedLoopBulkUpdating = false;
         }

         Arrays.fill(this.closedLoopOutputRoles, 0);
         this.closedLoopMainOutput = marked != null ? marked.what() : null;
         if (this.closedLoopMainOutput != null) {
            this.closedLoopOutputRoles[0] = 1;
         }

         this.closedLoopCandidates = List.of();
         this.closedLoopCandidateCount = 0;
         this.closedLoopCandidateIndex = 0;
         this.closedLoopDraftRepresentsEncoded = false;
         this.closedLoopDraftDirty = true;
         this.uploadState = 0;
         this.seedRefillSync = SeedRefillSync.none();
      }
   }

   private void resetClosedLoopDraft() {
      this.closedLoopBulkUpdating = true;

      try {
         clearInventory(this.closedLoopMemberInventory);
         clearInventory(this.closedLoopOutputInventory);
      } finally {
         this.closedLoopBulkUpdating = false;
      }

      Arrays.fill(this.closedLoopMemberCopies, 0L);
      Arrays.fill(this.closedLoopOutputRoles, 0);
      this.closedLoopCandidates = List.of();
      this.closedLoopDraftMembers = List.of();
      this.closedLoopMainOutput = null;
      this.closedLoopCandidateCount = 0;
      this.closedLoopCandidateIndex = 0;
      this.closedLoopEncodeState = 0;
      this.closedLoopDraftStatus = ClosedLoopDraftStatus.EMPTY;
      this.closedLoopPreparedPayload = null;
      this.setClosedLoopComputedResults(List.of(), List.of());
      this.closedLoopDraftDirty = false;
      this.closedLoopDraftRepresentsEncoded = false;
   }

   private void fillClosedLoopDraft(ClosedLoopPatternPayload payload) {
      if (payload != null) {
         this.closedLoopBulkUpdating = true;

         try {
            clearInventory(this.closedLoopMemberInventory);
            clearInventory(this.closedLoopOutputInventory);
            Arrays.fill(this.closedLoopMemberCopies, 0L);
            Arrays.fill(this.closedLoopOutputRoles, 0);
            int memberCount = Math.min(27, payload.memberPatterns().size());

            for (int i = 0; i < memberCount; i++) {
               ClosedLoopMemberPattern member = payload.memberPatterns().get(i);
               ItemStack stack = member.pattern().toItemStack();
               if (!stack.m_41619_()) {
                  this.closedLoopMemberInventory.setItemDirect(i, stack.m_255036_(1));
                  this.closedLoopMemberCopies[i] = member.copiesPerCycle();
               }
            }

            int outputCount = Math.min(9, payload.netOutputs().size());

            for (int ix = 0; ix < outputCount; ix++) {
               this.closedLoopOutputInventory.setItemDirect(ix, GenericStack.wrapInItemStack(payload.netOutputs().get(ix)));
               this.closedLoopOutputRoles[ix] = ix == 0 ? 1 : 2;
            }

            this.closedLoopMainOutput = payload.netOutputs().isEmpty() ? null : payload.netOutputs().get(0).what();
            this.closedLoopDraftMembers = List.copyOf(payload.memberPatterns());
         } finally {
            this.closedLoopBulkUpdating = false;
         }

         this.closedLoopPreparedPayload = null;
         this.setClosedLoopComputedResults(List.of(), List.of());
         this.closedLoopDraftDirty = true;
      }
   }

   private static void clearInventory(InternalInventory inventory) {
      if (inventory != null) {
         for (int i = 0; i < inventory.size(); i++) {
            inventory.setItemDirect(i, ItemStack.f_41583_);
         }
      }
   }

   private void rebuildClosedLoopDraft() {
      if (this.isServerSide() && this.tianshuMode == TianshuEncodingMode.CLOSED_LOOP) {
         this.closedLoopDraftDirty = false;
         ArrayList<ClosedLoopMemberPattern> draft = new ArrayList<>();

         for (int i = 0; i < 27; i++) {
            ItemStack stack = this.closedLoopMemberInventory.getStackInSlot(i);
            if (stack.m_41619_()) {
               this.closedLoopMemberCopies[i] = 0L;
            } else {
               if (!PatternDetailsHelper.isEncodedPattern(stack) || this.isExecutionMemberReference(stack)) {
                  this.setClosedLoopInvalid(ClosedLoopDraftStatus.MEMBER_UNDECODABLE);
                  return;
               }

               long copies = this.closedLoopMemberCopies[i];
               if (copies < 1L) {
                  copies = 1L;
               }

               this.closedLoopMemberCopies[i] = copies;

               try {
                  draft.add(new ClosedLoopMemberPattern(SourcePatternSnapshot.fromItemStack(stack), copies));
               } catch (RuntimeException var7) {
                  this.setClosedLoopInvalid(ClosedLoopDraftStatus.MEMBER_UNDECODABLE);
                  return;
               }
            }
         }

         this.closedLoopDraftMembers = List.copyOf(draft);
         if (draft.isEmpty()) {
            this.clearClosedLoopComputedResults();
            this.closedLoopDraftStatus = this.closedLoopCandidates.isEmpty() ? ClosedLoopDraftStatus.NO_CANDIDATE : ClosedLoopDraftStatus.EMPTY;
         } else if (!ClosedLoopPatternAnalyzer.isMinimalIntegerRatio(draft.stream().mapToLong(ClosedLoopMemberPattern::copiesPerCycle).toArray())) {
            this.setClosedLoopInvalid(ClosedLoopDraftStatus.NON_MINIMAL_COPIES);
         } else {
            GenericStack markedPrimary = this.getMarkedClosedLoopPrimaryOutput();
            if (markedPrimary == null) {
               this.clearClosedLoopComputedResults();
               this.closedLoopDraftStatus = ClosedLoopDraftStatus.MISSING_PRIMARY_OUTPUT;
               this.closedLoopEncodeState = 2;
            } else {
               List<AEKey> preferredOutputOrder = this.snapshotClosedLoopOutputKeys();
               this.closedLoopMainOutput = markedPrimary.what();
               ClosedLoopPatternAuthoringService.Result authored = ClosedLoopPatternAuthoringService.createFromDraft(
                  draft, this.closedLoopMainOutput, this.closedLoopExecutionSeedMultiplier, this.closedLoopStoredTaskMultiplier, this.getPlayer().m_9236_()
               );
               if (!authored.valid()) {
                  this.clearClosedLoopComputedResults();
                  this.setClosedLoopInvalid(mapAuthoringStatus(authored));
               } else {
                  ClosedLoopPatternPayload payload = authored.payload();
                  List<GenericStack> orderedOutputs = orderClosedLoopOutputs(payload.netOutputs(), preferredOutputOrder);
                  if (!orderedOutputs.equals(payload.netOutputs())) {
                     payload = new ClosedLoopPatternPayload(
                        payload.memberPatterns(),
                        payload.seeds(),
                        payload.externalInputs(),
                        orderedOutputs,
                        payload.executionSeedMultiplier(),
                        payload.storedTaskMultiplier(),
                        payload.enabled()
                     );
                  }

                  this.writeOutputCandidates(payload.netOutputs());
                  this.closedLoopPreparedPayload = payload;
                  this.fillClosedLoopComputedResults(payload);
                  this.closedLoopDraftStatus = this.closedLoopDraftRepresentsEncoded ? ClosedLoopDraftStatus.ENCODED : ClosedLoopDraftStatus.VALID;
                  this.closedLoopEncodeState = 0;
               }
            }
         }
      }
   }

   private List<AEKey> snapshotClosedLoopOutputKeys() {
      ArrayList<AEKey> result = new ArrayList<>(9);

      for (int i = 0; i < 9; i++) {
         GenericStack output = GenericStack.fromItemStack(this.closedLoopOutputInventory.getStackInSlot(i));
         if (output != null && output.what() != null && !result.contains(output.what())) {
            result.add(output.what());
         }
      }

      return result;
   }

   private static List<GenericStack> orderClosedLoopOutputs(List<GenericStack> analyzedOutputs, List<AEKey> preferredOrder) {
      if (analyzedOutputs.isEmpty()) {
         return List.of();
      } else {
         LinkedHashMap<AEKey, GenericStack> remaining = new LinkedHashMap<>();

         for (GenericStack output : analyzedOutputs) {
            remaining.put(output.what(), output);
         }

         ArrayList<GenericStack> ordered = new ArrayList<>(analyzedOutputs.size());
         GenericStack primary = analyzedOutputs.get(0);
         ordered.add(primary);
         remaining.remove(primary.what());

         for (AEKey key : preferredOrder) {
            GenericStack output = remaining.remove(key);
            if (output != null) {
               ordered.add(output);
            }
         }

         ordered.addAll(remaining.values());
         return List.copyOf(ordered);
      }
   }

   private void writeOutputCandidates(List<GenericStack> outputs) {
      this.closedLoopBulkUpdating = true;

      try {
         clearInventory(this.closedLoopOutputInventory);
         Arrays.fill(this.closedLoopOutputRoles, 0);
         int count = Math.min(9, outputs.size());

         for (int i = 0; i < count; i++) {
            GenericStack output = outputs.get(i);
            this.closedLoopOutputInventory.setItemDirect(i, GenericStack.wrapInItemStack(output));
            this.closedLoopOutputRoles[i] = i == 0 ? 1 : 2;
         }

         this.closedLoopMainOutput = count == 0 ? null : outputs.get(0).what();
      } finally {
         this.closedLoopBulkUpdating = false;
      }
   }

   private void fillClosedLoopComputedResults(ClosedLoopPatternPayload payload) {
      this.setClosedLoopComputedResults(payload.externalInputs(), payload.seeds());
   }

   private void clearClosedLoopComputedResults() {
      this.closedLoopPreparedPayload = null;
      this.setClosedLoopComputedResults(List.of(), List.of());
   }

   private void setClosedLoopComputedResults(List<GenericStack> externalInputs, List<GenericStack> seeds) {
      List<GenericStack> nextExternalInputs = copyClosedLoopResults(externalInputs);
      List<GenericStack> nextSeeds = copyClosedLoopResults(seeds);
      boolean changed = !nextExternalInputs.equals(this.closedLoopExternalInputs) || !nextSeeds.equals(this.closedLoopSeeds);
      this.closedLoopExternalInputs = nextExternalInputs;
      this.closedLoopSeeds = nextSeeds;
      this.closedLoopExternalInputCount = nextExternalInputs.size();
      this.closedLoopSeedInputCount = nextSeeds.size();
      if (changed) {
         this.closedLoopResultRevision++;
      }
   }

   private static List<GenericStack> copyClosedLoopResults(List<GenericStack> source) {
      if (source != null && !source.isEmpty()) {
         ArrayList<GenericStack> result = new ArrayList<>(Math.min(243, source.size()));

         for (GenericStack entry : source) {
            if (entry != null && entry.what() != null && entry.amount() > 0L) {
               result.add(entry);
               if (result.size() == 243) {
                  break;
               }
            }
         }

         return List.copyOf(result);
      } else {
         return List.of();
      }
   }

   private void setClosedLoopInvalid(ClosedLoopDraftStatus status) {
      this.clearClosedLoopComputedResults();
      this.closedLoopDraftStatus = status == null ? ClosedLoopDraftStatus.INVALID_OUTPUT_MARKING : status;
      this.closedLoopEncodeState = 2;
   }

   private static ClosedLoopDraftStatus mapAuthoringStatus(@Nullable ClosedLoopPatternAuthoringService.Result result) {
      if (result == null) {
         return ClosedLoopDraftStatus.NOT_BALANCED;
      } else {
         return switch (result.status()) {
            case MEMBER_UNDECODABLE -> ClosedLoopDraftStatus.MEMBER_UNDECODABLE;
            case TOO_MANY_MEMBERS -> ClosedLoopDraftStatus.TOO_MANY_MEMBERS;
            case NON_MINIMAL_COPIES -> ClosedLoopDraftStatus.NON_MINIMAL_COPIES;
            case INVALID_SEED_ROUTING -> ClosedLoopDraftStatus.INVALID_SEED_ROUTING;
            case INVALID_MARKING -> ClosedLoopDraftStatus.INVALID_OUTPUT_MARKING;
            case NOT_BALANCED -> ClosedLoopDraftStatus.NOT_BALANCED;
            case VALID -> ClosedLoopDraftStatus.NOT_BALANCED;
         };
      }
   }

   private void refreshClosedLoopDraftSync() {
      ArrayList<Long> copies = new ArrayList<>(27);

      for (long value : this.closedLoopMemberCopies) {
         copies.add(value);
      }

      ArrayList<Integer> roles = new ArrayList<>(9);

      for (int value : this.closedLoopOutputRoles) {
         roles.add(value);
      }

      this.closedLoopDraftSync = new ClosedLoopDraftSync(copies, roles);
   }

   private void restoreClosedLoopDraft(@Nullable ClosedLoopTerminalDraft draft) {
      ItemStack source = this.tianshuHost.getLogic().getEncodedPatternInv().getStackInSlot(0);
      if (draft != null && (!(source.m_41720_() instanceof ClosedLoopPatternItem) || ItemStack.m_41728_(source, draft.source()))) {
         this.configuredSource = source.m_41777_();
         this.encodedClosedLoop = source.m_41720_() instanceof ClosedLoopPatternItem;
         this.closedLoopBulkUpdating = true;

         try {
            for (int i = 0; i < 27; i++) {
               this.closedLoopMemberInventory.setItemDirect(i, draft.members().get(i).m_41777_());
               this.closedLoopMemberCopies[i] = draft.memberCopies().get(i);
            }

            int primaryIndex = -1;

            for (int i = 0; i < 9; i++) {
               if (draft.outputRoles().get(i) == 1 && !draft.outputs().get(i).m_41619_()) {
                  primaryIndex = i;
                  break;
               }
            }

            if (primaryIndex < 0 && !draft.outputs().get(0).m_41619_()) {
               primaryIndex = 0;
            }

            clearInventory(this.closedLoopOutputInventory);
            Arrays.fill(this.closedLoopOutputRoles, 0);
            int outputSlot = 0;
            if (primaryIndex >= 0) {
               this.closedLoopOutputInventory.setItemDirect(outputSlot, draft.outputs().get(primaryIndex).m_41777_());
               this.closedLoopOutputRoles[outputSlot++] = 1;
            }

            for (int ix = 0; ix < 9 && outputSlot < 9; ix++) {
               if (ix != primaryIndex && !draft.outputs().get(ix).m_41619_()) {
                  this.closedLoopOutputInventory.setItemDirect(outputSlot, draft.outputs().get(ix).m_41777_());
                  this.closedLoopOutputRoles[outputSlot++] = 2;
               }
            }
         } finally {
            this.closedLoopBulkUpdating = false;
         }

         this.closedLoopExecutionSeedMultiplier = draft.executionSeedMultiplier();
         this.closedLoopSeedMultiplier = this.closedLoopExecutionSeedMultiplier;
         this.closedLoopStoredTaskMultiplier = draft.storedTaskMultiplier();
         this.closedLoopDraftRepresentsEncoded = draft.representsEncodedPattern();
         GenericStack var10 = this.getMarkedClosedLoopPrimaryOutput();
         this.closedLoopMainOutput = var10 != null ? var10.what() : null;
         this.closedLoopDraftDirty = draft.members().stream().anyMatch(stack -> !stack.m_41619_());
      }
   }

   private void persistClosedLoopDraft() {
      ItemStack source = this.tianshuHost.getLogic().getEncodedPatternInv().getStackInSlot(0);
      boolean hasMember = false;

      for (int i = 0; i < 27; i++) {
         if (!this.closedLoopMemberInventory.getStackInSlot(i).m_41619_()) {
            hasMember = true;
            break;
         }
      }

      boolean hasOutputMark = this.getMarkedClosedLoopPrimaryOutput() != null;
      if (!hasMember && !hasOutputMark && !(source.m_41720_() instanceof ClosedLoopPatternItem)) {
         this.tianshuHost.setClosedLoopTerminalDraft(null);
      } else {
         ArrayList<ItemStack> members = new ArrayList<>(27);

         for (int ix = 0; ix < 27; ix++) {
            members.add(this.closedLoopMemberInventory.getStackInSlot(ix).m_41777_());
         }

         ArrayList<Long> copies = new ArrayList<>(27);

         for (long copiesPerCycle : this.closedLoopMemberCopies) {
            copies.add(copiesPerCycle);
         }

         ArrayList<ItemStack> outputs = new ArrayList<>(9);

         for (int ix = 0; ix < 9; ix++) {
            outputs.add(this.closedLoopOutputInventory.getStackInSlot(ix).m_41777_());
         }

         ArrayList<Integer> roles = new ArrayList<>(9);

         for (int role : this.closedLoopOutputRoles) {
            roles.add(role);
         }

         this.tianshuHost
            .setClosedLoopTerminalDraft(
               new ClosedLoopTerminalDraft(
                  source,
                  members,
                  copies,
                  outputs,
                  roles,
                  this.closedLoopExecutionSeedMultiplier,
                  this.closedLoopStoredTaskMultiplier,
                  this.closedLoopDraftRepresentsEncoded
               )
            );
      }
   }

   private boolean isExecutionMemberReference(ItemStack stack) {
      if (stack.m_41720_() instanceof ClosedLoopPatternItem item && item.readExecutionMember(stack) >= 0) {
         return true;
      }

      return false;
   }

   public void requestMaintenanceEditor(AEKey key) {
      if (this.isClientSide()) {
         PacketSender.sendToServer(new OpenMaintenanceEditorPacket(this.f_38840_, this.tianshuSelectionRevision, key));
      }
   }

   public void openMaintenanceEditor(int expectedSelectionRevision, AEKey key) {
      if (this.isServerSide() && expectedSelectionRevision == this.tianshuSelectionRevision && this.getPlayer() instanceof ServerPlayer serverPlayer) {
         TianshuSupercomputerPortBlockEntity var7 = this.resolveBoundTianshu();
         if (key != null && var7 != null && var7.getFunctionProfile().supportsInventoryMaintenance()) {
            TianshuInventoryMaintenanceService maintenance = var7.getInventoryMaintenance();
            IGrid grid = var7.getGrid();
            if (maintenance != null) {
               if (maintenance.repository().get(key) != null || grid != null && MaintenanceRequestability.isRequestable(grid.getCraftingService(), key)) {
                  this.sendMaintenanceEditorData(serverPlayer, key);
               } else {
                  serverPlayer.m_5661_(Component.m_237115_("ae2lt.tianshu.maintenance.unsupported"), true);
               }
            }
         }
      }
   }

   public void setMaintainableView(boolean enabled) {
      this.maintainableView = enabled;
      if (this.isClientSide()) {
         this.sendClientAction("setMaintainableView", enabled);
      } else {
         this.setMaintainableViewServer(enabled);
      }
   }

   public void setMaintainableViewTemporarily(boolean enabled) {
      this.maintainableView = enabled;
      if (this.isClientSide()) {
         this.sendClientAction("setMaintainableViewTemporarily", enabled);
      } else {
         this.setMaintainableViewTemporarilyServer(enabled);
      }
   }

   private void setMaintainableViewServer(boolean enabled) {
      this.applyMaintainableViewServer(enabled, true);
   }

   private void setMaintainableViewTemporarilyServer(boolean enabled) {
      this.applyMaintainableViewServer(enabled, false);
   }

   private void applyMaintainableViewServer(boolean enabled, boolean persist) {
      if (this.isServerSide()) {
         this.maintainableView = enabled;
         if (persist) {
            this.tianshuHost.setMaintainableView(enabled);
         }

         this.getConfigManager().putSetting(Settings.VIEW_MODE, ViewItems.ALL);
         this.m_38946_();
      }
   }

   protected boolean showsCraftables() {
      return this.maintainableView || super.showsCraftables();
   }

   private void sendMaintenanceSummaryIfNeeded() {
      if (this.getPlayer() instanceof ServerPlayer player) {
         if (this.lastMaintenanceSummaryTick == Integer.MIN_VALUE || player.f_19797_ - this.lastMaintenanceSummaryTick >= 20) {
            this.lastMaintenanceSummaryTick = this.getPlayer().f_19797_;
            TianshuSupercomputerPortBlockEntity target = this.resolveBoundTianshu();
            LinkedHashMap<AEKey, MaintenanceSummarySyncPacket.Entry> summaries = new LinkedHashMap<>();
            boolean overflow = false;
            if (target != null && target.getFunctionProfile().supportsInventoryMaintenance()) {
               TianshuInventoryMaintenanceService service = target.getInventoryMaintenance();
               if (service != null) {
                  IGrid grid = target.getGrid();
                  KeyCounter available = grid != null ? grid.getStorageService().getInventory().getAvailableStacks() : null;
                  ICraftingService crafting = grid != null ? grid.getCraftingService() : null;
                  if (service.repository().size() > 2048 || service.reservedStock().size() > 2048) {
                     overflow = true;
                  }

                  List<ReservedStockRepository.Entry> globalReservations = service.reservedStock().reservations(2048);
                  LinkedHashSet<AEKey> directlyReservedKeys = new LinkedHashSet<>();

                  for (ReservedStockRepository.Entry reserve : globalReservations) {
                     directlyReservedKeys.add(reserve.key());
                  }

                  for (InventoryMaintenanceRule rule : service.repository().rules(2048)) {
                     if (summaries.size() >= 2048 && !summaries.containsKey(rule.key())) {
                        overflow = true;
                        break;
                     }

                     boolean ruleReserveOverflow = service.reservedStock(rule.id()).size() > 2048;
                     long storedAmount = available != null ? Math.max(0L, available.get(rule.key())) : 0L;
                     boolean craftable = MaintenanceRequestability.isRequestable(crafting, rule.key());
                     summaries.put(
                        rule.key(),
                        new MaintenanceSummarySyncPacket.Entry(
                           rule.key(),
                           true,
                           maintenanceSummaryStatus(rule, service.status(rule.id()), grid != null, craftable),
                           storedAmount,
                           rule.lowerThreshold(),
                           rule.upperThreshold(),
                           rule.amountPerJob(),
                           service.reservedStock().reserve(rule.key()),
                           service.reservedStock().matchMode(rule.key()),
                           directlyReservedKeys.contains(rule.key()),
                           craftable,
                           ruleReserveOverflow
                        )
                     );
                  }

                  for (ReservedStockRepository.Entry reserve : globalReservations) {
                     if (summaries.size() >= 2048 && !summaries.containsKey(reserve.key())) {
                        overflow = true;
                        break;
                     }

                     long storedAmount = available != null ? Math.max(0L, available.get(reserve.key())) : 0L;
                     boolean craftable = MaintenanceRequestability.isRequestable(crafting, reserve.key());
                     MaintenanceSummarySyncPacket.Entry existing = summaries.get(reserve.key());
                     summaries.put(
                        reserve.key(),
                        existing == null
                           ? new MaintenanceSummarySyncPacket.Entry(
                              reserve.key(),
                              false,
                              InventoryMaintenanceStatus.IDLE,
                              storedAmount,
                              0L,
                              0L,
                              0L,
                              reserve.amount(),
                              reserve.mode(),
                              true,
                              craftable,
                              false
                           )
                           : new MaintenanceSummarySyncPacket.Entry(
                              existing.key(),
                              existing.ruleConfigured(),
                              existing.status(),
                              existing.storedAmount(),
                              existing.lowerThreshold(),
                              existing.upperThreshold(),
                              existing.amountPerJob(),
                              reserve.amount(),
                              reserve.mode(),
                              true,
                              existing.craftable(),
                              existing.ruleReserveOverflow()
                           )
                     );
                  }
               }
            }

            List<MaintenanceSummarySyncPacket.Entry> snapshot = List.copyOf(summaries.values());
            if (this.lastSentMaintenanceSummary == null
               || this.lastSentMaintenanceSummaryOverflow != overflow
               || !this.lastSentMaintenanceSummary.equals(snapshot)) {
               this.lastSentMaintenanceSummary = snapshot;
               this.lastSentMaintenanceSummaryOverflow = overflow;
               this.maintenanceSummaryRevision++;
               PacketSender.sendToPlayer(
                  player, new MaintenanceSummarySyncPacket(this.f_38840_, this.tianshuSelectionRevision, this.maintenanceSummaryRevision, overflow, snapshot)
               );
            }
         }
      }
   }

   public void receiveMaintenanceSummary(int selectionRevision, long revision, boolean overflow, List<MaintenanceSummarySyncPacket.Entry> entries) {
      if (this.isClientSide() && revision > this.receivedMaintenanceSummaryRevision) {
         if (selectionRevision >= this.tianshuSelectionRevision) {
            this.receivedMaintenanceSummaryRevision = revision;
            this.maintenanceSummarySelectionRevision = selectionRevision;
            this.maintenanceSummaryOverflow = overflow;
            this.maintenanceSummary = entries != null ? List.copyOf(entries) : List.of();
         }
      }
   }

   public boolean isMaintenanceSummaryOverflow() {
      return this.maintenanceSummaryOverflow;
   }

   public long getMaintenanceSummaryRevision() {
      return this.receivedMaintenanceSummaryRevision;
   }

   public Map<AEKey, MaintenanceSummarySyncPacket.Entry> getMaintenanceSummary() {
      LinkedHashMap<AEKey, MaintenanceSummarySyncPacket.Entry> result = new LinkedHashMap<>();

      for (MaintenanceSummarySyncPacket.Entry entry : this.maintenanceSummary) {
         result.put(entry.key(), entry);
      }

      return Map.copyOf(result);
   }

   @Nullable
   public MaintenanceSummarySyncPacket.Entry getMaintenanceSummaryEntry(AEKey key) {
      if (key == null) {
         return null;
      } else {
         for (MaintenanceSummarySyncPacket.Entry entry : this.maintenanceSummary) {
            if (key.equals(entry.key())) {
               return entry;
            }
         }

         return null;
      }
   }

   public void runMaintenanceAction(UUID ruleId, boolean cancel) {
      if (this.isClientSide() && ruleId != null) {
         this.sendClientAction("maintenanceAction", new TianshuPatternEncodingTermMenu.MaintenanceAction(this.tianshuSelectionRevision, ruleId, cancel));
      }
   }

   private void maintenanceActionServer(TianshuPatternEncodingTermMenu.MaintenanceAction action) {
      if (this.isServerSide() && action != null && action.selectionRevision() == this.tianshuSelectionRevision) {
         TianshuSupercomputerPortBlockEntity target = this.resolveBoundTianshu();
         TianshuInventoryMaintenanceService service = target != null ? target.getInventoryMaintenance() : null;
         if (service != null && service.repository().getById(action.ruleId()) != null) {
            if (action.cancel()) {
               service.cancelRuleTask(action.ruleId());
            } else {
               service.retryNow(action.ruleId());
            }

            this.lastMaintenanceSummaryTick = Integer.MIN_VALUE;
            this.m_38946_();
         }
      }
   }

   public void sendGlobalReserve(AEKey key, long amount, ReservedStockMatchMode mode) {
      if (this.isClientSide() && key != null && mode != null) {
         PacketSender.sendToServer(new SaveGlobalReservePacket(this.f_38840_, this.tianshuSelectionRevision, key, amount, mode));
      }
   }

   public void saveGlobalReserve(SaveGlobalReservePacket packet) {
      if (this.isServerSide() && packet != null && packet.amount() >= -1L && packet.selectionRevision() == this.tianshuSelectionRevision) {
         TianshuSupercomputerPortBlockEntity target = this.resolveBoundTianshu();
         if (target != null && target.getFunctionProfile().supportsInventoryMaintenance()) {
            TianshuInventoryMaintenanceService maintenance = target.getInventoryMaintenance();
            if (maintenance != null) {
               if (packet.amount() != 0L && maintenance.reservedStock().size() > 2048) {
                  this.getPlayer().m_5661_(Component.m_237110_("ae2lt.tianshu.maintenance.too_large", new Object[]{2048}), true);
               } else if (packet.amount() == 0L || maintenance.reservedStock().reserve(packet.key()) != 0L || maintenance.reservedStock().size() < 2048) {
                  setGlobalReserveFromEditor(maintenance, packet.key(), packet.mode(), packet.amount());
                  this.lastMaintenanceSummaryTick = Integer.MIN_VALUE;
                  this.m_38946_();
               }
            }
         }
      }
   }

   private static void setGlobalReserveFromEditor(TianshuInventoryMaintenanceService maintenance, AEKey key, ReservedStockMatchMode mode, long amount) {
      ReservedStockRepository.Entry direct = maintenance.reservedStock()
         .reservations()
         .stream()
         .filter(entry -> entry.key().equals(key))
         .findFirst()
         .orElse(null);
      if (direct != null && (amount == 0L || direct.mode() != mode)) {
         maintenance.setMaintenanceWideReservedStock(key, direct.mode(), 0L);
      }

      if (amount != 0L || direct == null) {
         maintenance.setMaintenanceWideReservedStock(key, mode, amount);
      }
   }

   private static void setRuleReserveFromEditor(
      TianshuInventoryMaintenanceService maintenance, UUID ruleId, AEKey key, ReservedStockMatchMode mode, long amount
   ) {
      ReservedStockRepository repository = maintenance.reservedStock(ruleId);
      ReservedStockRepository.Entry direct = repository.reservations().stream().filter(entry -> entry.key().equals(key)).findFirst().orElse(null);
      if (direct != null && (amount == 0L || direct.mode() != mode)) {
         maintenance.setReservedStock(ruleId, key, direct.mode(), 0L);
      }

      if (amount != 0L || direct == null) {
         maintenance.setReservedStock(ruleId, key, mode, amount);
      }
   }

   private void sendMaintenanceEditorData(ServerPlayer player, AEKey key) {
      TianshuSupercomputerPortBlockEntity target = this.resolveBoundTianshu();
      if (target != null) {
         TianshuInventoryMaintenanceService maintenance = target.getInventoryMaintenance();
         if (maintenance != null) {
            InventoryMaintenanceRule rule = maintenance.repository().get(key);
            IGrid grid = GridNodeAccess.getActiveGrid(this.tianshuHost.getActionableNode());
            KeyCounter available = grid != null ? grid.getStorageService().getInventory().getAvailableStacks() : null;
            List<MaintenanceTopologyService.Entry> topology = grid != null ? MaintenanceTopologyService.build(grid.getCraftingService(), key) : List.of();
            boolean recoveryPage = topology.size() > 2048;
            ReservedStockRepository global = maintenance.reservedStock();
            ReservedStockRepository local = rule != null ? maintenance.reservedStock(rule.id()) : null;
            LinkedHashMap<AEKey, MaintenanceTopologyService.Entry> topologyByKey = new LinkedHashMap<>();

            for (MaintenanceTopologyService.Entry entry : topology) {
               topologyByKey.putIfAbsent(entry.key(), entry);
            }

            LinkedHashMap<AEKey, MaintenanceEditorData.TopologyEntry> topologyData = new LinkedHashMap<>();
            if (local != null) {
               if (local.size() > 2048) {
                  recoveryPage = true;
               }

               for (ReservedStockRepository.Entry saved : local.reservations(2048)) {
                  MaintenanceTopologyService.Entry topologyEntry = topologyByKey.get(saved.key());
                  topologyData.put(saved.key(), maintenanceEditorEntry(saved.key(), topologyEntry, available, global, local));
               }
            }

            for (MaintenanceTopologyService.Entry entry : topology) {
               if (!topologyData.containsKey(entry.key())) {
                  if (topologyData.size() >= 2048) {
                     recoveryPage = true;
                     break;
                  }

                  topologyData.put(entry.key(), maintenanceEditorEntry(entry.key(), entry, available, global, local));
               }
            }

            List<MaintenanceVariantService.Variant> allVariants = maintenance.variants(key);
            if (allVariants.size() > 2048) {
               recoveryPage = true;
            }

            List<MaintenanceEditorData.VariantEntry> variants = allVariants.stream()
               .limit(2048L)
               .map(variant -> new MaintenanceEditorData.VariantEntry(variant.key(), variant.storedAmount(), variant.craftable()))
               .toList();
            long currentStock = available != null ? Math.max(0L, available.get(key)) : 0L;
            boolean craftable = grid != null && MaintenanceRequestability.isRequestable(grid.getCraftingService(), key);
            InventoryMaintenanceStatus editorStatus = rule != null
               ? maintenanceSummaryStatus(rule, maintenance.status(rule.id()), grid != null, craftable)
               : InventoryMaintenanceStatus.IDLE;
            MaintenanceEditorData data = new MaintenanceEditorData(
               key,
               rule != null ? rule.id() : null,
               rule != null ? rule.lowerThreshold() : 0L,
               rule != null ? rule.upperThreshold() : 64L,
               rule != null ? rule.amountPerJob() : 64L,
               rule == null || rule.enabled(),
               editorStatus,
               currentStock,
               craftable,
               recoveryPage,
               List.copyOf(topologyData.values()),
               variants
            );
            PacketSender.sendToPlayer(player, new MaintenanceEditorSyncPacket(this.f_38840_, this.tianshuSelectionRevision, data));
         }
      }
   }

   private static MaintenanceEditorData.TopologyEntry maintenanceEditorEntry(
      AEKey key,
      @Nullable MaintenanceTopologyService.Entry topology,
      @Nullable KeyCounter available,
      ReservedStockRepository global,
      @Nullable ReservedStockRepository local
   ) {
      return new MaintenanceEditorData.TopologyEntry(
         key,
         topology != null ? topology.depth() : 0,
         topology != null && topology.craftable(),
         available != null ? Math.max(0L, available.get(key)) : 0L,
         global.reserve(key),
         global.matchMode(key),
         local != null ? local.reserve(key) : 0L,
         local != null ? local.matchMode(key) : ReservedStockMatchMode.EXACT
      );
   }

   private static InventoryMaintenanceStatus maintenanceSummaryStatus(
      InventoryMaintenanceRule rule, InventoryMaintenanceStatus runtimeStatus, boolean online, boolean craftable
   ) {
      if (!rule.enabled()) {
         return InventoryMaintenanceStatus.DISABLED;
      } else if (!online) {
         return InventoryMaintenanceStatus.OFFLINE;
      } else {
         InventoryMaintenanceStatus status = runtimeStatus != null ? runtimeStatus : InventoryMaintenanceStatus.IDLE;
         return !craftable && status != InventoryMaintenanceStatus.CRAFTING && status != InventoryMaintenanceStatus.CANCELLING
            ? InventoryMaintenanceStatus.MISSING_PATTERN
            : status;
      }
   }

   public void receiveMaintenanceEditorData(int selectionRevision, MaintenanceEditorData data) {
      if (this.isClientSide() && data != null && selectionRevision >= this.tianshuSelectionRevision) {
         this.maintenanceEditorSelectionRevision = selectionRevision;
         this.maintenanceEditorData = data;
         this.maintenanceEditorRevision++;
      }
   }

   @Nullable
   public MaintenanceEditorData getMaintenanceEditorData() {
      return this.maintenanceEditorData;
   }

   public int getMaintenanceEditorRevision() {
      return this.maintenanceEditorRevision;
   }

   public void sendMaintenanceSave(SaveMaintenanceRulePacket packet) {
      if (this.isClientSide() && packet != null) {
         PacketSender.sendToServer(packet);
      }
   }

   public void saveMaintenanceRule(SaveMaintenanceRulePacket packet) {
      if (this.isServerSide()
         && packet != null
         && packet.selectionRevision() == this.tianshuSelectionRevision
         && this.getPlayer() instanceof ServerPlayer player) {
         TianshuSupercomputerPortBlockEntity target = this.resolveBoundTianshu();
         if (target != null && target.getFunctionProfile().supportsInventoryMaintenance()) {
            TianshuInventoryMaintenanceService service = target.getInventoryMaintenance();
            if (service != null) {
               InventoryMaintenanceRule existing = service.repository().get(packet.target());
               if (existing == null && packet.expectedRuleId() != null || existing != null && !existing.id().equals(packet.expectedRuleId())) {
                  this.sendMaintenanceEditorData(player, packet.target());
               } else if (packet.delete()) {
                  if (existing != null && service.removeRule(existing.id())) {
                     this.lastMaintenanceSummaryTick = Integer.MIN_VALUE;
                  }

                  this.sendMaintenanceEditorData(player, packet.target());
               } else {
                  LinkedHashSet<AEKey> editedReserveKeys = new LinkedHashSet<>();

                  for (SaveMaintenanceRulePacket.ReserveEdit edit : packet.reserves()) {
                     if (edit == null
                        || edit.key() == null
                        || edit.globalMode() == null
                        || edit.ruleMode() == null
                        || edit.globalAmount() < -1L
                        || edit.ruleAmount() < -1L
                        || !editedReserveKeys.add(edit.key())) {
                        this.sendMaintenanceEditorData(player, packet.target());
                        return;
                     }
                  }

                  if (existing == null) {
                     IGrid grid = target.getGrid();
                     if (grid == null || !MaintenanceRequestability.isRequestable(grid.getCraftingService(), packet.target())) {
                        player.m_5661_(Component.m_237115_("ae2lt.tianshu.maintenance.unsupported"), true);
                        return;
                     }
                  }

                  if (service.repository().size() > 2048) {
                     player.m_5661_(Component.m_237110_("ae2lt.tianshu.maintenance.too_large", new Object[]{2048}), true);
                     this.sendMaintenanceEditorData(player, packet.target());
                  } else if (packet.lower() < 0L || packet.upper() < packet.lower() || packet.amountPerJob() <= 0L) {
                     this.sendMaintenanceEditorData(player, packet.target());
                  } else if (existing == null && service.repository().size() >= 2048) {
                     player.m_5661_(Component.m_237110_("ae2lt.tianshu.maintenance.too_large", new Object[]{2048}), true);
                     this.sendMaintenanceEditorData(player, packet.target());
                  } else {
                     UUID ruleId = existing != null ? existing.id() : UUID.randomUUID();
                     InventoryMaintenanceRule rule = new InventoryMaintenanceRule(
                        ruleId,
                        packet.target(),
                        packet.lower(),
                        packet.upper(),
                        packet.amountPerJob(),
                        packet.enabled(),
                        existing != null && existing.replenishing(),
                        existing != null ? existing.activeCraftingId() : null
                     );
                     InventoryMaintenanceRepository.PutResult result = service.putRule(rule);
                     if (result == InventoryMaintenanceRepository.PutResult.ADDED || result == InventoryMaintenanceRepository.PutResult.UPDATED) {
                        this.lastMaintenanceSummaryTick = Integer.MIN_VALUE;

                        for (SaveMaintenanceRulePacket.ReserveEdit editx : packet.reserves()) {
                           setGlobalReserveFromEditor(service, editx.key(), editx.globalMode(), editx.globalAmount());
                           setRuleReserveFromEditor(service, ruleId, editx.key(), editx.ruleMode(), editx.ruleAmount());
                        }
                     }

                     this.sendMaintenanceEditorData(player, packet.target());
                  }
               }
            }
         }
      }
   }

   public void encode() {
      if (this.isClientSide()) {
         this.beginClientEncoding(TianshuUploadTriggerClient.shouldTrigger(), false);
      } else {
         this.encodeServerWithOptions(false);
      }
   }

   public void encodeAndUploadDirectly() {
      if (this.isClientSide()) {
         this.beginClientEncoding(true, true);
      }
   }

   public void refreshPolymorphRecipe() {
      if (this.isClientSide()) {
         this.sendClientAction("polyeng$selectRecipe");
      }
   }

   private void beginClientEncoding(boolean triggerUpload, boolean directUpload) {
      TianshuRecipeTransferContext.beginEncoding(this, this.tianshuHost.getLogic().getEncodedPatternInv().getStackInSlot(0));
      this.pendingTriggeredUpload = triggerUpload;
      this.pendingDirectUpload = triggerUpload && directUpload;
      this.pendingTriggeredUploadUntil = this.getPlayer().f_19797_ + 200;
      this.expectedTriggeredUploadAck = this.triggeredUploadAck;
      this.directUploadTargetsRequested = false;
      this.sendClientAction("encodeTianshu", triggerUpload && AE2LTClientConfig.interceptDuplicatePatternEncoding());
   }

   private void encodeServerWithOptions(Boolean interceptDuplicateUpload) {
      if (this.isServerSide()) {
         boolean interceptDuplicates = Boolean.TRUE.equals(interceptDuplicateUpload);
         if (this.tianshuMode.isAe2Mode()) {
            InternalInventory encodedInventory = this.tianshuHost.getLogic().getEncodedPatternInv();
            boolean carriesNetworkBlank = this.isRefundableEncodedPattern(encodedInventory.getStackInSlot(0));
            boolean stagedNetworkBlank = false;
            this.ae2EncodingInProgress = true;

            try {
               stagedNetworkBlank = this.stageNetworkBlankPattern();

               try (ExtendedAEPlusEncodingCompat.Suppression ignored = ExtendedAEPlusEncodingCompat.suppressAutomaticUpload(this)) {
                  super.encode();
               }

               this.applyConfiguredProcessingConversion();
            } finally {
               this.ae2EncodingInProgress = false;
               if (stagedNetworkBlank) {
                  this.returnStagedBlankPatternToNetwork();
               }
            }

            ItemStack encoded = encodedInventory.getStackInSlot(0);
            if (TianshuPatternUploadRouting.isValidEncodingResult(encoded, this.getPlayer().m_9236_())) {
               if (stagedNetworkBlank || carriesNetworkBlank) {
                  this.refundableEncodedPattern = encoded.m_41777_();
               }

               if (this.shouldInterceptDuplicateEncoding(encoded, interceptDuplicates)) {
                  this.rollbackRefundableEncodedPattern();
                  this.notifyDuplicateEncodingIntercepted();
                  this.m_38946_();
                  return;
               }

               this.triggeredUploadAck++;
            }

            this.m_38946_();
         } else {
            ItemStack result = this.encodeDerivedPattern();
            if (result != null && !result.m_41619_()) {
               if (this.shouldInterceptDuplicateEncoding(result, interceptDuplicates)) {
                  this.notifyDuplicateEncodingIntercepted();
                  return;
               }

               InternalInventory encodedInventory = this.tianshuHost.getLogic().getEncodedPatternInv();
               boolean carriesNetworkBlank = this.isRefundableEncodedPattern(encodedInventory.getStackInSlot(0));
               boolean stagedNetworkBlank = false;
               if (encodedInventory.getStackInSlot(0).m_41619_()) {
                  stagedNetworkBlank = this.stageNetworkBlankPattern();
                  if (!stagedNetworkBlank) {
                     return;
                  }
               }

               if (this.tianshuMode == TianshuEncodingMode.CLOSED_LOOP) {
                  this.closedLoopEncodeState = 0;
               }

               encodedInventory.setItemDirect(0, result);
               if (stagedNetworkBlank || carriesNetworkBlank) {
                  this.refundableEncodedPattern = result.m_41777_();
               }

               if (TianshuPatternUploadRouting.isValidEncodingResult(result, this.getPlayer().m_9236_())) {
                  this.triggeredUploadAck++;
               }

               this.m_38946_();
            }
         }
      }
   }

   private boolean shouldInterceptDuplicateEncoding(ItemStack candidate, boolean enabled) {
      if (enabled && candidate != null && !candidate.m_41619_()) {
         TianshuPatternUploadRouting.Route route = TianshuPatternUploadRouting.classify(candidate, this.getPlayer().m_9236_());
         if (route == TianshuPatternUploadRouting.Route.INVALID) {
            return false;
         } else {
            String candidateDescription = PatternEncodingDuplicateFilter.describeStack(candidate);
            DUPLICATE_LOG.debug(
               "Check start: player={}, route={}, candidate={}", new Object[]{this.getPlayer().m_36316_().getName(), route, candidateDescription}
            );
            this.uploadTargets = this.discoverUploadTargets(true);
            Level level = this.getPlayer().m_9236_();

            for (int targetIndex = 0; targetIndex < this.uploadTargets.size(); targetIndex++) {
               PatternContainer target = this.uploadTargets.get(targetIndex);
               InternalInventory inventory = target.getTerminalPatternInventory();
               PatternEncodingDuplicateFilter.CheckResult result = PatternEncodingDuplicateFilter.checkEquivalentPattern(inventory, candidate, level);
               if (DUPLICATE_LOG.isDebugEnabled()) {
                  DUPLICATE_LOG.debug(
                     "Target {}: type={}, group={}, slots={}, occupiedScanned={}, undecodable={}, duplicate={}, matchedSlot={}, method={}, stored={}",
                     new Object[]{
                        targetIndex,
                        target.getClass().getName(),
                        target.getTerminalGroup().name().getString(),
                        inventory.size(),
                        result.occupiedSlots(),
                        result.undecodableSlots(),
                        result.duplicate(),
                        result.matchedSlot(),
                        result.matchMethod(),
                        result.duplicate() ? "matched" : PatternEncodingDuplicateFilter.describeOccupiedStacks(inventory, 8)
                     }
                  );
               }

               if (result.duplicate()) {
                  DUPLICATE_LOG.debug(
                     "Check result: BLOCK candidate={} target={} slot={} method={}",
                     new Object[]{candidateDescription, targetIndex, result.matchedSlot(), result.matchMethod()}
                  );
                  return true;
               }
            }

            DUPLICATE_LOG.debug(
               "Check result: ALLOW candidate={} because no equivalent pattern was found across {} eligible targets",
               candidateDescription,
               this.uploadTargets.size()
            );
            return false;
         }
      } else {
         return false;
      }
   }

   private void notifyDuplicateEncodingIntercepted() {
      if (this.getPlayer() instanceof ServerPlayer player) {
         player.m_5661_(Component.m_237115_("ae2lt.tianshu.encode.duplicate_blocked"), false);
      }
   }

   private boolean isConnectedToNetwork() {
      return GridNodeAccess.getActiveGrid(this.getNetworkNode()) != null;
   }

   private boolean stageNetworkBlankPattern() {
      InternalInventory encodedInventory = this.tianshuHost.getLogic().getEncodedPatternInv();
      if (encodedInventory.getStackInSlot(0).m_41619_() && this.isConnectedToNetwork()) {
         AEItemKey blankPatternKey = AEItemKey.of(AEItems.BLANK_PATTERN.m_5456_());
         IActionSource actionSource = this.getActionSource();
         long available = this.storage.extract(blankPatternKey, 1L, Actionable.SIMULATE, actionSource);
         if (available <= 0L) {
            this.notifyEncodingFailure("ae2lt.tianshu.encode.missing_blank");
            return false;
         } else {
            long poweredAvailable = StorageHelper.poweredExtraction(this.powerSource, this.storage, blankPatternKey, 1L, actionSource, Actionable.SIMULATE);
            if (poweredAvailable <= 0L) {
               this.notifyEncodingFailure("ae2lt.tianshu.encode.insufficient_power");
               return false;
            } else {
               long extracted = StorageHelper.poweredExtraction(this.powerSource, this.storage, blankPatternKey, 1L, actionSource);
               if (extracted <= 0L) {
                  this.notifyEncodingFailure("ae2lt.tianshu.encode.extraction_failed");
                  return false;
               } else {
                  encodedInventory.setItemDirect(0, AEItems.BLANK_PATTERN.stack((int)extracted));
                  return true;
               }
            }
         }
      } else {
         return false;
      }
   }

   private void notifyEncodingFailure(String translationKey) {
      if (this.getPlayer() instanceof ServerPlayer player) {
         player.m_5661_(Component.m_237115_(translationKey), false);
      }
   }

   private void returnStagedBlankPatternToNetwork() {
      if (this.isServerSide() && this.isConnectedToNetwork()) {
         InternalInventory encodedInventory = this.tianshuHost.getLogic().getEncodedPatternInv();
         ItemStack stack = encodedInventory.getStackInSlot(0);
         if (AEItems.BLANK_PATTERN.isSameAs(stack)) {
            AEItemKey blankPatternKey = AEItemKey.of(AEItems.BLANK_PATTERN.m_5456_());
            long inserted = StorageHelper.poweredInsert(this.powerSource, this.storage, blankPatternKey, (long)stack.m_41613_(), this.getActionSource());
            if (inserted > 0L) {
               ItemStack remainder = stack.m_41777_();
               remainder.m_41774_((int)inserted);
               encodedInventory.setItemDirect(0, remainder);
            }
         }
      }
   }

   private boolean isRefundableEncodedPattern(ItemStack stack) {
      return stack != null && !stack.m_41619_() && !this.refundableEncodedPattern.m_41619_() && ItemStack.m_150942_(this.refundableEncodedPattern, stack);
   }

   private boolean rollbackRefundableEncodedPattern() {
      if (this.isServerSide() && this.isConnectedToNetwork()) {
         InternalInventory encodedInventory = this.tianshuHost.getLogic().getEncodedPatternInv();
         ItemStack current = encodedInventory.getStackInSlot(0);
         if (!this.isRefundableEncodedPattern(current)) {
            this.refundableEncodedPattern = ItemStack.f_41583_;
            return false;
         } else {
            ItemStack debt = this.refundableEncodedPattern.m_41777_();
            ItemStack removed = encodedInventory.extractItem(0, 1, false);
            if (!removed.m_41619_() && ItemStack.m_150942_(debt, removed)) {
               AEItemKey blankPatternKey = AEItemKey.of(AEItems.BLANK_PATTERN.m_5456_());
               long inserted = StorageHelper.poweredInsert(this.powerSource, this.storage, blankPatternKey, 1L, this.getActionSource());
               if (inserted == 1L) {
                  this.refundableEncodedPattern = ItemStack.f_41583_;
                  return true;
               } else {
                  encodedInventory.addItems(removed);
                  this.refundableEncodedPattern = debt;
                  return false;
               }
            } else {
               if (!removed.m_41619_()) {
                  encodedInventory.addItems(removed);
               }

               this.refundableEncodedPattern = ItemStack.f_41583_;
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private void settleNetworkBlankCharge(boolean uploadSucceeded) {
      if (uploadSucceeded) {
         this.refundableEncodedPattern = ItemStack.f_41583_;
      } else {
         this.rollbackRefundableEncodedPattern();
      }
   }

   private void returnLegacyBlankPatternsToNetwork() {
      if (this.isServerSide() && this.isConnectedToNetwork()) {
         InternalInventory blankInventory = this.tianshuHost.getLogic().getBlankPatternInv();
         ItemStack stack = blankInventory.getStackInSlot(0);
         if (AEItems.BLANK_PATTERN.isSameAs(stack)) {
            AEItemKey blankPatternKey = AEItemKey.of(AEItems.BLANK_PATTERN.m_5456_());
            long inserted = StorageHelper.poweredInsert(this.powerSource, this.storage, blankPatternKey, (long)stack.m_41613_(), this.getActionSource());
            if (inserted > 0L) {
               ItemStack remainder = stack.m_41777_();
               remainder.m_41774_((int)inserted);
               blankInventory.setItemDirect(0, remainder);
            }
         }
      }
   }

   private ItemStack encodeDerivedPattern() {
      return this.tianshuMode != TianshuEncodingMode.CLOSED_LOOP ? ItemStack.f_41583_ : this.encodeSelectedClosedLoopCandidate();
   }

   private void applyConfiguredProcessingConversion() {
      if (this.tianshuMode == TianshuEncodingMode.PROCESSING && this.processingEncodingType != ProcessingPatternEncodingType.NORMAL) {
         InternalInventory inventory = this.tianshuHost.getLogic().getEncodedPatternInv();
         ItemStack source = inventory.getStackInSlot(0);
         if (!source.m_41619_()) {
            ItemStack converted = this.convertConfiguredProcessingPattern(source, this.getAdvancedEncodingConfig(), this.getOverloadEncodingConfig());
            if (converted != null && !converted.m_41619_()) {
               inventory.setItemDirect(0, converted);
            }
         }
      }
   }

   @Nullable
   private ItemStack convertConfiguredProcessingPattern(
      ItemStack source,
      @Nullable ProcessingPatternEncodingType.AdvancedConfig advancedConfig,
      @Nullable ProcessingPatternEncodingType.OverloadConfig overloadConfig
   ) {
      ItemStack converted = source;
      if (advancedConfig != null) {
         converted = this.convertToAdvanced(source, advancedConfig);
         if (converted == null || converted.m_41619_()) {
            return null;
         }
      }

      if (overloadConfig != null) {
         converted = this.convertToOverload(converted, overloadConfig);
         if (converted == null || converted.m_41619_()) {
            return null;
         }
      }

      return converted;
   }

   @Nullable
   private ItemStack convertToAdvanced(ItemStack source, @Nullable ProcessingPatternEncodingType.AdvancedConfig config) {
      if (config == null) {
         return null;
      } else {
         int slotCount = this.getProcessingInputSlots().length;
         ArrayList<Integer> sides = new ArrayList<>(slotCount);

         for (int i = 0; i < slotCount; i++) {
            sides.add(config.direction(i));
         }

         return AdvancedAECompat.encodeWithDirections(source, this.getPlayer().m_9236_(), sides);
      }
   }

   @Nullable
   private ItemStack convertToOverload(ItemStack source, @Nullable ProcessingPatternEncodingType.OverloadConfig config) {
      if (config == null) {
         return null;
      } else {
         try {
            EditableOverloadPatternState editable = this.conversionService
               .resolveEditableSource(source, new Ae2PlainPatternResolver(this.getPlayer().m_9236_()))
               .orElse(null);
            if (editable == null) {
               return null;
            } else {
               ParsedPatternDefinition parsed = editable.parsedPattern();
               EncodedOverloadPattern.Builder builder = EncodedOverloadPattern.builder();

               for (ParsedPatternInput input : parsed.inputs()) {
                  builder.input(input.slotIndex(), config.isInputIdOnly(input.slotIndex()) ? MatchMode.ID_ONLY : MatchMode.STRICT);
               }

               for (ParsedPatternOutput output : parsed.outputs()) {
                  builder.output(output.slotIndex(), config.isOutputIdOnly(output.slotIndex()) ? MatchMode.ID_ONLY : MatchMode.STRICT);
               }

               return this.conversionService.createOverloadPatternStack((OverloadPatternItem)ModItems.OVERLOAD_PATTERN.get(), parsed, builder.build());
            }
         } catch (RuntimeException var8) {
            return null;
         }
      }
   }

   private ItemStack encodeSelectedClosedLoopCandidate() {
      if (this.closedLoopDraftDirty) {
         this.rebuildClosedLoopDraft();
      }

      if (this.closedLoopPreparedPayload == null
         || this.closedLoopDraftStatus != ClosedLoopDraftStatus.VALID && this.closedLoopDraftStatus != ClosedLoopDraftStatus.ENCODED) {
         this.closedLoopEncodeState = this.closedLoopDraftStatus == ClosedLoopDraftStatus.MEMBER_UNDECODABLE ? 1 : 2;
         this.m_38946_();
         return ItemStack.f_41583_;
      } else {
         return ((ClosedLoopPatternItem)ModItems.CLOSED_LOOP_PATTERN.get()).createStack(this.closedLoopPreparedPayload, this.getPlayer().m_9236_().m_9598_());
      }
   }

   private static record BoundUploadSlot(PatternContainer target, int slot) {
   }

   public static record ClosedLoopMemberEdit(int slot, long copies) {
   }

   public static record ClosedLoopMemberMove(int slot, int direction) {
   }

   private final class ClosedLoopMemberSlot extends FakeSlot {
      private ClosedLoopMemberSlot(AppEngInternalInventory inventory, int slot) {
         super(inventory, slot);
         this.setHideAmount(true);
      }

      public void m_5852_(ItemStack stack) {
         super.m_5852_(stack.m_41619_() ? ItemStack.f_41583_ : stack.m_255036_(1));
      }

      public boolean m_5857_(ItemStack stack) {
         return this.canUseAsClosedLoopMember(stack);
      }

      public boolean canSetFilterTo(ItemStack stack) {
         return stack.m_41619_() || this.canUseAsClosedLoopMember(stack);
      }

      public void setFilterTo(ItemStack stack) {
         if (this.canSetFilterTo(stack)) {
            super.setFilterTo(stack);
         }
      }

      private boolean canUseAsClosedLoopMember(ItemStack stack) {
         return stack != null
            && !stack.m_41619_()
            && PatternDetailsHelper.isEncodedPattern(stack)
            && !TianshuPatternEncodingTermMenu.this.isExecutionMemberReference(stack);
      }
   }

   public static record ClosedLoopMultiplierEdit(int execution, int stored) {
   }

   private final class ClosedLoopOutputSlot extends FakeSlot {
      private ClosedLoopOutputSlot(AppEngInternalInventory inventory) {
         super(inventory, 0);
      }

      public void m_5852_(ItemStack stack) {
         if (this.canSetFilterTo(stack)) {
            GenericStack marked = stack == null ? null : GenericStack.fromItemStack(stack);
            super.m_5852_(marked == null ? ItemStack.f_41583_ : GenericStack.wrapInItemStack(marked.what(), 1L));
            TianshuPatternEncodingTermMenu.this.onClosedLoopPrimaryOutputMarked();
         }
      }

      public boolean m_5857_(ItemStack stack) {
         return this.canSetFilterTo(stack);
      }

      public boolean canSetFilterTo(ItemStack stack) {
         return stack == null || stack.m_41619_() || GenericStack.fromItemStack(stack) != null;
      }
   }

   private static final class ClosedLoopReadonlySlot extends AppEngSlot {
      private ClosedLoopReadonlySlot(AppEngInternalInventory inventory, int slot) {
         super(inventory, slot);
         this.setNotDraggable();
      }

      public boolean m_5857_(ItemStack stack) {
         return false;
      }

      public boolean m_8010_(Player player) {
         return false;
      }

      public ItemStack m_6201_(int amount) {
         return ItemStack.f_41583_;
      }
   }

   public static record MaintenanceAction(int selectionRevision, UUID ruleId, boolean cancel) {
   }

   private static final class MutableUploadGroup {
      final List<TianshuPatternEncodingTermMenu.BoundUploadSlot> slots = new ArrayList<>();
      int providers;
      int availableSlots;
   }
}
