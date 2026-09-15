package com.moakiee.ae2lt.integration.jade;

import com.moakiee.ae2lt.api.frequency.FrequencyBindingHost;
import com.moakiee.ae2lt.grid.FrequencyDisplayName;
import com.moakiee.ae2lt.grid.WirelessFrequency;
import com.moakiee.ae2lt.grid.WirelessFrequencyManager;
import com.moakiee.ae2lt.grid.wirelesslink.WirelessLinkRegistry;
import com.moakiee.ae2lt.grid.wirelesslink.WirelessLinkState;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.BlockHitResult;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public final class FrequencyCardWirelessNodeJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
   private static final ResourceLocation UID = new ResourceLocation("ae2lt", "frequency_card_wireless_node");
   private static final String TAG_LINK = "AE2LTFrequencyCardWirelessNode";
   private static final String TAG_STATE = "State";
   private static final String TAG_FREQUENCY_NAME = "FrequencyName";

   public ResourceLocation getUid() {
      return UID;
   }

   public void appendServerData(CompoundTag data, BlockAccessor accessor) {
      if (accessor.getLevel() instanceof ServerLevel level) {
         FrequencyCardWirelessNodeJadeProvider.DisplayData displayData = inspectNativeHost(accessor);
         if (displayData == null) {
            BlockHitResult hitResult = (BlockHitResult)accessor.getHitResult();
            WirelessLinkRegistry.TargetLinkInspection inspection = WirelessLinkRegistry.get(level.m_7654_())
               .inspectTarget(level, accessor.getPosition(), accessor.getSide(), hitResult == null ? null : hitResult.m_82450_());
            displayData = displayData(inspection);
         }

         if (displayData != null) {
            CompoundTag linkData = new CompoundTag();
            linkData.m_128359_("State", displayData.state().name());
            linkData.m_128359_("FrequencyName", resolveFrequencyNames(displayData.frequencyIds()));
            data.m_128365_("AE2LTFrequencyCardWirelessNode", linkData);
         }
      }
   }

   private static FrequencyCardWirelessNodeJadeProvider.DisplayData inspectNativeHost(BlockAccessor accessor) {
      if (accessor.getBlockEntity() instanceof FrequencyBindingHost host && host.getFrequencyId() > 0) {
         return new FrequencyCardWirelessNodeJadeProvider.DisplayData(
            host.isFrequencyConnected()
               ? FrequencyCardWirelessNodeJadeProvider.DisplayState.CONNECTED
               : FrequencyCardWirelessNodeJadeProvider.DisplayState.PENDING,
            List.of(host.getFrequencyId())
         );
      }

      return null;
   }

   private static FrequencyCardWirelessNodeJadeProvider.DisplayData displayData(WirelessLinkRegistry.TargetLinkInspection inspection) {
      if (!inspection.isPresent()) {
         return null;
      } else {
         boolean connected = inspection.liveVirtualEntrance();
         boolean pending = false;
         boolean conflict = false;
         LinkedHashSet<Integer> frequencyIds = new LinkedHashSet<>();

         for (WirelessLinkRegistry.InspectedTargetLink link : inspection.links()) {
            frequencyIds.add(Integer.valueOf(link.frequencyId()));
            connected |= link.state() == WirelessLinkState.CONNECTED;
            pending |= isPending(link.state());
            conflict |= link.state() == WirelessLinkState.CLUSTER_FREQUENCY_CONFLICT;
         }

         FrequencyCardWirelessNodeJadeProvider.DisplayState state = conflict
            ? FrequencyCardWirelessNodeJadeProvider.DisplayState.CONFLICT
            : (
               connected
                  ? FrequencyCardWirelessNodeJadeProvider.DisplayState.CONNECTED
                  : (pending ? FrequencyCardWirelessNodeJadeProvider.DisplayState.PENDING : FrequencyCardWirelessNodeJadeProvider.DisplayState.INACTIVE)
            );
         return new FrequencyCardWirelessNodeJadeProvider.DisplayData(state, List.copyOf(frequencyIds));
      }
   }

   private static boolean isPending(WirelessLinkState state) {
      return state == WirelessLinkState.PENDING_TARGET_CHUNK || state == WirelessLinkState.PENDING_TRANSMITTER || state == WirelessLinkState.TARGET_NOT_READY;
   }

   private static String resolveFrequencyNames(List<Integer> frequencyIds) {
      WirelessFrequencyManager manager = WirelessFrequencyManager.get();
      ArrayList<String> names = new ArrayList<>(frequencyIds.size());

      for (int frequencyId : frequencyIds) {
         WirelessFrequency frequency = manager == null ? null : manager.getFrequency(frequencyId);
         String name = frequency == null ? "" : frequency.getName();
         names.add(FrequencyDisplayName.of(frequencyId, name));
      }

      return String.join(" / ", names);
   }

   public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
      CompoundTag serverData = accessor.getServerData();
      if (serverData.m_128425_("AE2LTFrequencyCardWirelessNode", 10)) {
         String stateName = serverData.m_128469_("AE2LTFrequencyCardWirelessNode").m_128461_("State");

         FrequencyCardWirelessNodeJadeProvider.DisplayState state;
         try {
            state = FrequencyCardWirelessNodeJadeProvider.DisplayState.valueOf(stateName);
         } catch (IllegalArgumentException var9) {
            return;
         }

         String frequencyName = serverData.m_128469_("AE2LTFrequencyCardWirelessNode").m_128461_("FrequencyName");

         Component status = switch (state) {
            case CONNECTED -> Component.m_237115_("jade.ae2lt.frequency_card_wireless_node.state.connected").m_130940_(ChatFormatting.GREEN);
            case PENDING -> Component.m_237115_("jade.ae2lt.frequency_card_wireless_node.state.pending").m_130940_(ChatFormatting.YELLOW);
            case CONFLICT -> Component.m_237115_("jade.ae2lt.frequency_card_wireless_node.state.conflict").m_130940_(ChatFormatting.RED);
            case INACTIVE -> Component.m_237115_("jade.ae2lt.frequency_card_wireless_node.state.inactive").m_130940_(ChatFormatting.GRAY);
         };
         tooltip.add(Component.m_237110_("jade.ae2lt.frequency_card_wireless_node", new Object[]{frequencyName, status}).m_130940_(ChatFormatting.AQUA));
      }
   }

   private static record DisplayData(FrequencyCardWirelessNodeJadeProvider.DisplayState state, List<Integer> frequencyIds) {
   }

   private static enum DisplayState {
      CONNECTED,
      PENDING,
      CONFLICT,
      INACTIVE;
   }
}
