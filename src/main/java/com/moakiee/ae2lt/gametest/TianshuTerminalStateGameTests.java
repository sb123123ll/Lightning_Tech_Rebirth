package com.moakiee.ae2lt.gametest;

import appeng.api.parts.IPartItem;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import com.moakiee.ae2lt.integration.ae2wtlib.Ae2wtlibIntegration;
import com.moakiee.ae2lt.integration.ae2wtlib.TianshuWTItem;
import com.moakiee.ae2lt.integration.ae2wtlib.TianshuWTMenuHost;
import com.moakiee.ae2lt.integration.ae2wtlib.WirelessTerminalFrequencyLink;
import com.moakiee.ae2lt.item.OverloadedFrequencyCardData;
import com.moakiee.ae2lt.item.OverloadedFrequencyCardItem;
import com.moakiee.ae2lt.logic.tianshu.terminal.ClosedLoopTerminalDraft;
import com.moakiee.ae2lt.logic.tianshu.terminal.ProcessingPatternEncodingType;
import com.moakiee.ae2lt.logic.tianshu.terminal.ProcessingPatternTerminalDraft;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuEncodingMode;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuPatternTerminalHost;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuWirelessPatternEncodingTermMenuHost;
import com.moakiee.ae2lt.part.TianshuPatternEncodingTerminalPart;
import com.moakiee.ae2lt.registry.ModItems;
import com.mojang.authlib.GameProfile;
import de.mari_023.ae2wtlib.AE2wtlib;
import de.mari_023.ae2wtlib.terminal.WTMenuHost;
import de.mari_023.ae2wtlib.wut.WTDefinition;
import de.mari_023.ae2wtlib.wut.WUTHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("ae2lt")
@PrefixGameTestTemplate(false)
public final class TianshuTerminalStateGameTests {
   private TianshuTerminalStateGameTests() {
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty"
   )
   public static void wiredHostPreservesLegacyState(GameTestHelper helper) {
      helper.m_177245_(BlockPos.f_121853_, AEBlocks.CABLE_BUS.block());
      CableBusBlockEntity bus = (CableBusBlockEntity)helper.m_177347_(BlockPos.f_121853_);
      TianshuPatternEncodingTerminalPart part = new TianshuPatternEncodingTerminalPart((IPartItem<?>)ModItems.TIANSHU_PATTERN_ENCODING_TERMINAL.get());
      part.setPartHostInfo(Direction.NORTH, bus, bus);
      populate(part);
      part.getLogic().setSubstitution(true);
      CompoundTag saved = new CompoundTag();
      part.writeToNBT(saved);
      helper.m_246336_(saved.m_128461_("TianshuEncodingMode").equals("CLOSED_LOOP"), "Wired terminals must retain their original root NBT keys");
      helper.m_277053_(saved.m_128441_("patternEncodingLogic"), "Do not migrate wired state into item NBT");
      TianshuPatternEncodingTerminalPart restored = new TianshuPatternEncodingTerminalPart((IPartItem<?>)ModItems.TIANSHU_PATTERN_ENCODING_TERMINAL.get());
      restored.setPartHostInfo(Direction.SOUTH, bus, bus);
      restored.readFromNBT(saved.m_6426_());
      assertPopulated(helper, restored);
      helper.m_246336_(restored.getLogic().isSubstitution(), "Native pattern settings must survive reload");
      helper.m_246336_(restored.getLogic().getBlankPatternInv().getSlotLimit(0) == 0, "Physical blank-pattern storage must remain disabled");
      restored.setClosedLoopTerminalDraft(null);
      restored.setProcessingPatternTerminalDraft(null);
      restored.writeToNBT(saved);
      helper.m_277053_(saved.m_128441_("ClosedLoopDraft"), "Cleared closed-loop drafts must be removed");
      helper.m_277053_(saved.m_128441_("ProcessingDraft"), "Cleared processing drafts must be removed");
      helper.m_177412_();
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty"
   )
   public static void wirelessHostsFollowOptionalDependency(GameTestHelper helper) {
      if (!ModList.get().isLoaded("ae2wtlib")) {
         ResourceLocation id = new ResourceLocation("ae2lt", "wireless_tianshu_pattern_encoding_terminal");
         helper.m_277053_(ForgeRegistries.ITEMS.containsKey(id), "The wireless item must stay absent without AE2WTLib");
         helper.m_277053_(ForgeRegistries.MENU_TYPES.containsKey(id), "The wireless menu must stay absent without AE2WTLib");
      } else {
         TianshuTerminalStateGameTests.WirelessHosts.verify(helper);
      }

      helper.m_177412_();
   }

   @GameTest(
      templateNamespace = "minecraft",
      m_177046_ = "bastion/mobs/empty"
   )
   public static void legacyHostRetainsNestedViewCellStorage(GameTestHelper helper) {
      FakePlayer player = FakePlayerFactory.get(helper.m_177100_(), new GameProfile(UUID.randomUUID(), "LegacyTerminal"));
      ItemStack stack = AEItems.WIRELESS_TERMINAL.stack();
      player.m_150109_().m_6836_(0, stack);
      TianshuWirelessPatternEncodingTermMenuHost host = new TianshuWirelessPatternEncodingTermMenuHost(player, 0, stack, (p, menu) -> {
      });
      host.getViewCellStorage().setItemDirect(0, AEItems.VIEW_CELL.stack());
      populate(host);
      TianshuWirelessPatternEncodingTermMenuHost restored = new TianshuWirelessPatternEncodingTermMenuHost(player, 0, stack, (p, menu) -> {
      });
      assertPopulated(helper, restored);
      helper.m_246336_(
         restored.getViewCellStorage().getStackInSlot(0).m_150930_(AEItems.VIEW_CELL.m_5456_()), "The compatibility host must still restore its own view cells"
      );
      helper.m_246336_(stack.m_41698_("patternEncodingLogic").m_128441_("viewcells"), "Legacy view cells must retain their nested location");
      helper.m_277053_(stack.m_41784_().m_128441_("viewcells"), "Do not migrate legacy view cells to AE2WTLib's root location");
      player.m_150109_().m_6211_();
      helper.m_177412_();
   }

   private static void populate(TianshuPatternTerminalHost host) {
      host.setTianshuEncodingMode(TianshuEncodingMode.CLOSED_LOOP);
      host.setMaintainableView(true);
      host.setClosedLoopTerminalDraft(closedLoopDraft());
      host.setProcessingPatternTerminalDraft(processingDraft());
   }

   private static void assertPopulated(GameTestHelper helper, TianshuPatternTerminalHost host) {
      helper.m_246336_(host.getTianshuEncodingMode() == TianshuEncodingMode.CLOSED_LOOP, "Mode must survive reload");
      helper.m_246336_(host.isMaintainableView(), "View selection must survive reload");
      helper.m_246336_(
         ClosedLoopTerminalDraft.sameState(closedLoopDraft(), host.getClosedLoopTerminalDraft()),
         "Closed-loop stacks, roles and multipliers must survive reload"
      );
      helper.m_246336_(
         ProcessingPatternTerminalDraft.sameState(processingDraft(), host.getProcessingPatternTerminalDraft()), "Processing configuration must survive reload"
      );
   }

   private static ClosedLoopTerminalDraft closedLoopDraft() {
      ArrayList<ItemStack> members = new ArrayList<>(Collections.nCopies(27, ItemStack.f_41583_));
      members.set(2, new ItemStack(Items.f_41905_, 3));
      ArrayList<Long> copies = new ArrayList<>(Collections.nCopies(27, 0L));
      copies.set(2, 5L);
      ArrayList<ItemStack> outputs = new ArrayList<>(Collections.nCopies(9, ItemStack.f_41583_));
      outputs.set(1, new ItemStack(Items.f_42594_, 7));
      ArrayList<Integer> roles = new ArrayList<>(Collections.nCopies(9, 0));
      roles.set(1, 2);
      return new ClosedLoopTerminalDraft(new ItemStack(Items.f_42516_), members, copies, outputs, roles, 3, 4, true);
   }

   private static ProcessingPatternTerminalDraft processingDraft() {
      return ProcessingPatternTerminalDraft.configured(
         Collections.nCopies(3, null),
         Collections.nCopies(2, null),
         new ProcessingPatternEncodingType.AdvancedConfig(new int[]{2, 0, 6}),
         new ProcessingPatternEncodingType.OverloadConfig(new int[]{2}, new int[]{1})
      );
   }

   private static final class WirelessHosts {
      private static void verify(GameTestHelper helper) {
         TianshuWTItem item = Ae2wtlibIntegration.terminal();
         helper.m_246336_(
            item == ModItems.TIANSHU_WIRELESS_PATTERN_ENCODING_TERMINAL.get(), "Item registration and the terminal definition must share an instance"
         );
         String name = "tianshu_pattern_encoding";
         helper.m_246336_(((WTDefinition)WUTHandler.wirelessTerminals.get(name)).item() == item, "WUT must use the registered item instance");
         ItemStack universal = new ItemStack(AE2wtlib.UNIVERSAL_TERMINAL);

         for (String installed : WUTHandler.terminalNames) {
            universal.m_41784_().m_128379_(installed, true);
         }

         universal.m_41784_().m_128359_("currentTerminal", name);

         for (ItemStack stack : List.of(new ItemStack(item), universal)) {
            verifyStack(helper, stack, name);
         }
      }

      private static void verifyStack(GameTestHelper helper, ItemStack stack, String terminalName) {
         FakePlayer player = FakePlayerFactory.get(helper.m_177100_(), new GameProfile(UUID.randomUUID(), "TerminalState"));
         player.m_150109_().m_6836_(0, stack);
         TianshuWTMenuHost host = new TianshuWTMenuHost(player, 0, stack, (p, menu) -> {
         });
         host.getViewCellStorage().setItemDirect(0, AEItems.VIEW_CELL.stack());
         host.getSubInventory(WTMenuHost.INV_SINGULARITY).setItemDirect(0, AEItems.QUANTUM_ENTANGLED_SINGULARITY.stack());
         host.getLogic().setSubstitution(true);
         TianshuTerminalStateGameTests.populate(host);
         CompoundTag data = stack.m_41698_("patternEncodingLogic");
         data.m_128359_("foreignState", "preserve");
         helper.m_246336_(data.m_128461_("tianshuMode").equals("CLOSED_LOOP"), "Wireless setters must save immediately using legacy keys");
         helper.m_277053_(stack.m_41784_().m_128441_("TianshuEncodingMode"), "Wireless state must not use the part's root keys");
         TianshuWTMenuHost restored = new TianshuWTMenuHost(player, null, stack, (p, menu) -> {
         });
         TianshuTerminalStateGameTests.assertPopulated(helper, restored);
         helper.m_246336_(restored.getLogic().isSubstitution(), "Native pattern settings must survive reload");
         helper.m_246336_(restored.getLogic().getBlankPatternInv().getSlotLimit(0) == 0, "Physical blank-pattern storage must remain disabled");
         helper.m_246336_(
            restored.getViewCellStorage().getStackInSlot(0).m_150930_(AEItems.VIEW_CELL.m_5456_()), "AE2WTLib must retain ownership of view-cell persistence"
         );
         helper.m_246336_(
            restored.getSubInventory(WTMenuHost.INV_SINGULARITY).getStackInSlot(0).m_150930_(AEItems.QUANTUM_ENTANGLED_SINGULARITY.m_5456_()),
            "AE2WTLib must retain ownership of singularity persistence"
         );
         restored.setClosedLoopTerminalDraft(null);
         restored.setProcessingPatternTerminalDraft(null);
         helper.m_277053_(data.m_128441_("tianshuClosedLoopDraft"), "Cleared wireless drafts must be removed");
         helper.m_277053_(data.m_128441_("tianshuProcessingDraft"), "Cleared processing drafts must be removed");
         helper.m_246336_(data.m_128461_("foreignState").equals("preserve"), "Unrelated NBT must be preserved");
         helper.m_246336_(WUTHandler.getCurrentTerminal(stack).equals(terminalName), "Saving state must not change WUT terminal selection");
         helper.m_246336_(
            restored.getMainMenuIcon().m_150930_((Item)ModItems.TIANSHU_WIRELESS_PATTERN_ENCODING_TERMINAL.get()),
            "The main menu must retain the Tianshu terminal icon"
         );
         verifyFrequencyInventory(helper, player, restored, stack);
         player.m_150109_().m_6211_();
      }

      private static void verifyFrequencyInventory(GameTestHelper helper, Player player, TianshuWTMenuHost host, ItemStack stack) {
         WirelessTerminalFrequencyLink.Resolution noCard = WirelessTerminalFrequencyLink.resolveRoute(player, stack);
         helper.m_246336_(noCard.kind() == WirelessTerminalFrequencyLink.RouteKind.NO_FREQUENCY, "An empty upgrade inventory must defer to native connections");
         IUpgradeInventory upgrades = host.getUpgrades();
         ItemStack unbound = new ItemStack((ItemLike)ModItems.OVERLOADED_FREQUENCY_CARD.get());
         upgrades.setItemDirect(0, unbound);
         helper.m_277053_(WirelessTerminalFrequencyLink.resolveRoute(player, stack).usesFrequencyRoute(), "An unbound card must defer to native connections");
         int lastSlot = upgrades.size() - 1;
         if (host.isUniversalWirelessTerminal()) {
            helper.m_246336_(lastSlot >= 2, "The multi-terminal WUT must exercise slots beyond the vanilla two-slot view");
         } else {
            helper.m_246336_(upgrades.size() == 2, "Standalone terminals must retain their native two-slot host view");
         }

         ItemStack bound = new ItemStack((ItemLike)ModItems.OVERLOADED_FREQUENCY_CARD.get());
         OverloadedFrequencyCardItem.setData(
            bound, OverloadedFrequencyCardData.empty().bindFrequency(Integer.MAX_VALUE, null, 0L, UUID.randomUUID()).withAutoConnect(false)
         );
         upgrades.setItemDirect(lastSlot, bound);
         WirelessTerminalFrequencyLink.Resolution itemRoute = WirelessTerminalFrequencyLink.resolveRoute(player, stack);
         WirelessTerminalFrequencyLink.Resolution hostRoute = WirelessTerminalFrequencyLink.resolveRoute(player, upgrades);
         helper.m_246336_(
            itemRoute.kind() == WirelessTerminalFrequencyLink.RouteKind.UNAVAILABLE,
            "Resolve the first bound card across all host slots, without adding owner/auto-connect gates"
         );
         helper.m_246336_(itemRoute.kind() == hostRoute.kind(), "Item and host views must resolve the same route");
         helper.m_277053_(itemRoute.usesFrequencyRoute(), "An unavailable frequency must retain native fallback");
      }
   }
}
