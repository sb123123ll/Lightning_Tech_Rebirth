package com.moakiee.ae2lt.grid;

import appeng.api.networking.IGridNode;
import com.moakiee.ae2lt.blockentity.WirelessOverloadedControllerBlockEntity;
import com.moakiee.ae2lt.grid.wirelesslink.WirelessLinkRegistry;
import com.moakiee.ae2lt.me.GridNodeAccess;
import com.moakiee.ae2lt.network.SyncFrequencyDetailPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

public final class WirelessFrequencyManager extends SavedData {
   private static final String DATA_NAME = "ae2lt_wireless_frequencies";
   private final Int2ObjectOpenHashMap<WirelessFrequency> frequencies = new Int2ObjectOpenHashMap();
   private final Int2ObjectOpenHashMap<WirelessFrequencyManager.TransmitterEntry> transmitters = new Int2ObjectOpenHashMap();
   private final FrequencyDeviceIndex<WirelessFrequencyManager.DeviceEntry> devices = new FrequencyDeviceIndex<>();
   private final Map<Integer, List<WirelessFrequencyManager.TransmitterListener>> listeners = new HashMap<>();
   private final List<WirelessFrequencyManager.DeviceListener> deviceListeners = new ArrayList<>();
   private final Set<Integer> pendingDeviceNotifications = new HashSet<>();
   private int uniqueId = 0;
   @Nullable
   private static WirelessFrequencyManager instance;

   private WirelessFrequencyManager() {
   }

   private WirelessFrequencyManager(CompoundTag tag) {
      this.read(tag);
   }

   public static void onServerStart(MinecraftServer server) {
      ServerLevel overworld = server.m_129783_();
      instance = (WirelessFrequencyManager)overworld.m_8895_()
         .m_164861_(WirelessFrequencyManager::new, WirelessFrequencyManager::new, "ae2lt_wireless_frequencies");
      instance.addDeviceListener(freqId -> SyncFrequencyDetailPacket.broadcastConnectionsTo(server, freqId));
   }

   public static void onServerStop() {
      if (instance != null) {
         instance.listeners.clear();
         instance.deviceListeners.clear();
         instance.pendingDeviceNotifications.clear();
      }

      instance = null;
   }

   @Nullable
   public static WirelessFrequencyManager get() {
      return instance;
   }

   public static void flushPendingDeviceNotifications() {
      if (instance != null) {
         instance.flushDeviceListeners();
      }
   }

   @Nullable
   public WirelessFrequency createFrequency(ServerPlayer creator, String name, int color, FrequencySecurityLevel security, String password) {
      do {
         this.uniqueId++;
         if (this.uniqueId < 0) {
            this.uniqueId = 1;
         }
      } while (this.frequencies.containsKey(this.uniqueId));

      WirelessFrequency freq = new WirelessFrequency(this.uniqueId, name, color, creator, security, password);
      this.frequencies.put(freq.getId(), freq);
      this.m_77762_();
      return freq;
   }

   public boolean deleteFrequency(int id, MinecraftServer server) {
      WirelessFrequency removed = (WirelessFrequency)this.frequencies.remove(id);
      if (removed != null) {
         WirelessLinkRegistry linkRegistry = WirelessLinkRegistry.get();
         if (linkRegistry != null) {
            linkRegistry.removeFrequencyLinks(id);
         }

         WirelessFrequencyManager.TransmitterEntry txEntry = (WirelessFrequencyManager.TransmitterEntry)this.transmitters.get(id);
         if (txEntry != null && server != null) {
            ServerLevel txLevel = server.m_129880_(txEntry.dimension());
            BlockEntity be = txLevel == null ? null : getLoadedBlockEntity(txLevel, txEntry.pos());
            if (be instanceof WirelessFrequencyManager.WirelessTransmitterNodeProvider provider
               && provider.getTransmitterFrequencyId() == id
               && be instanceof WirelessOverloadedControllerBlockEntity ctrl) {
               ctrl.clearFrequency();
            }
         }

         this.transmitters.remove(id);
         this.devices.clearFrequency(id);
         this.fireListeners(id, false);
         this.queueDeviceListeners(id);
         this.m_77762_();
         return true;
      } else {
         return false;
      }
   }

   public boolean isFrequencyValid(int freqId) {
      return freqId > 0 && this.frequencies.containsKey(freqId);
   }

   public void markModified() {
      this.m_77762_();
   }

   @Nullable
   public WirelessFrequency getFrequency(int id) {
      return (WirelessFrequency)this.frequencies.get(id);
   }

   public Collection<WirelessFrequency> getAllFrequencies() {
      return this.frequencies.values();
   }

   public boolean canRegisterTransmitter(int freqId, ResourceKey<Level> dimension, BlockPos pos) {
      WirelessFrequencyManager.TransmitterEntry existing = (WirelessFrequencyManager.TransmitterEntry)this.transmitters.get(freqId);
      return existing == null || existing.dimension().equals(dimension) && existing.pos().equals(pos);
   }

   public boolean registerTransmitter(int freqId, ResourceKey<Level> dimension, BlockPos pos, @Nullable IGridNode node, boolean advanced) {
      if (!this.canRegisterTransmitter(freqId, dimension, pos)) {
         return false;
      } else {
         this.transmitters.put(freqId, new WirelessFrequencyManager.TransmitterEntry(dimension, pos, node, advanced));
         this.m_77762_();
         this.fireListeners(freqId, true);
         return true;
      }
   }

   public void unregisterTransmitter(int freqId) {
      if (this.transmitters.remove(freqId) != null) {
         this.m_77762_();
         this.fireListeners(freqId, false);
      }
   }

   public void updateNode(int freqId, @Nullable IGridNode node) {
      WirelessFrequencyManager.TransmitterEntry entry = (WirelessFrequencyManager.TransmitterEntry)this.transmitters.get(freqId);
      if (entry != null) {
         this.transmitters.put(freqId, new WirelessFrequencyManager.TransmitterEntry(entry.dimension(), entry.pos(), node, entry.advanced()));
      }
   }

   @Nullable
   public WirelessFrequencyManager.TransmitterEntry findTransmitter(int freqId) {
      return (WirelessFrequencyManager.TransmitterEntry)this.transmitters.get(freqId);
   }

   @Nullable
   public IGridNode resolveNode(int freqId, MinecraftServer server) {
      WirelessFrequencyManager.TransmitterEntry entry = (WirelessFrequencyManager.TransmitterEntry)this.transmitters.get(freqId);
      if (entry == null) {
         return null;
      } else {
         ServerLevel targetLevel = server.m_129880_(entry.dimension());
         if (targetLevel == null) {
            return liveCachedNode(entry);
         } else {
            LevelChunk chunk = targetLevel.m_7726_().m_7131_(entry.pos().m_123341_() >> 4, entry.pos().m_123343_() >> 4);
            if (chunk == null) {
               return liveCachedNode(entry);
            } else if (chunk.m_7702_(entry.pos()) instanceof WirelessFrequencyManager.WirelessTransmitterNodeProvider provider) {
               IGridNode node = provider.getWirelessGridNode();
               if (GridNodeAccess.getGridIfPresent(node) == null) {
                  node = null;
               }

               this.updateNode(freqId, node);
               return node;
            } else {
               return null;
            }
         }
      }
   }

   @Nullable
   private static IGridNode liveCachedNode(WirelessFrequencyManager.TransmitterEntry entry) {
      IGridNode node = entry.cachedNode();
      return GridNodeAccess.getGridIfPresent(node) != null ? node : null;
   }

   public boolean isAdvancedTransmitter(int freqId) {
      WirelessFrequencyManager.TransmitterEntry entry = (WirelessFrequencyManager.TransmitterEntry)this.transmitters.get(freqId);
      return entry != null && entry.advanced();
   }

   @Nullable
   public IGridNode resolveAdvancedNode(int freqId, MinecraftServer server) {
      return this.isAdvancedTransmitter(freqId) ? this.resolveNode(freqId, server) : null;
   }

   public void addListener(int freqId, WirelessFrequencyManager.TransmitterListener listener) {
      this.listeners.computeIfAbsent(freqId, k -> new ArrayList<>()).add(listener);
   }

   public void removeListener(int freqId, WirelessFrequencyManager.TransmitterListener listener) {
      List<WirelessFrequencyManager.TransmitterListener> list = this.listeners.get(freqId);
      if (list != null) {
         list.remove(listener);
         if (list.isEmpty()) {
            this.listeners.remove(freqId);
         }
      }
   }

   private void fireListeners(int freqId, boolean available) {
      List<WirelessFrequencyManager.TransmitterListener> list = this.listeners.get(freqId);
      if (list != null && !list.isEmpty()) {
         for (WirelessFrequencyManager.TransmitterListener listener : List.copyOf(list)) {
            listener.onTransmitterChanged(freqId, available);
         }
      }
   }

   public void registerDevice(int freqId, WirelessFrequencyManager.DeviceEntry entry) {
      if (freqId > 0) {
         if (this.devices.put(freqId, entry.dimension().m_135782_().toString(), entry.pos().m_121878_(), entry)) {
            this.m_77762_();
            this.queueDeviceListeners(freqId);
         }
      }
   }

   public void unregisterDevice(int freqId, ResourceKey<Level> dim, BlockPos pos) {
      if (freqId > 0) {
         if (this.devices.remove(freqId, dim.m_135782_().toString(), pos.m_121878_())) {
            this.m_77762_();
            this.queueDeviceListeners(freqId);
         }
      }
   }

   public List<WirelessFrequencyManager.DeviceEntry> getDevices(int freqId) {
      return this.devices.get(freqId);
   }

   public void addDeviceListener(WirelessFrequencyManager.DeviceListener l) {
      this.deviceListeners.add(l);
   }

   private void queueDeviceListeners(int freqId) {
      this.pendingDeviceNotifications.add(freqId);
   }

   private void flushDeviceListeners() {
      if (!this.pendingDeviceNotifications.isEmpty() && !this.deviceListeners.isEmpty()) {
         List<Integer> dirtyFrequencies = List.copyOf(this.pendingDeviceNotifications);
         this.pendingDeviceNotifications.clear();

         for (int freqId : dirtyFrequencies) {
            this.fireDeviceListeners(freqId);
         }
      }
   }

   private void fireDeviceListeners(int freqId) {
      if (!this.deviceListeners.isEmpty()) {
         for (WirelessFrequencyManager.DeviceListener l : List.copyOf(this.deviceListeners)) {
            l.onDevicesChanged(freqId);
         }
      }
   }

   @Nullable
   private static BlockEntity getLoadedBlockEntity(ServerLevel level, BlockPos pos) {
      LevelChunk chunk = level.m_7726_().m_7131_(pos.m_123341_() >> 4, pos.m_123343_() >> 4);
      return chunk == null ? null : chunk.m_7702_(pos);
   }

   private void read(CompoundTag root) {
      this.uniqueId = root.m_128451_("uniqueId");
      ListTag freqList = root.m_128437_("frequencies", 10);

      for (int i = 0; i < freqList.size(); i++) {
         WirelessFrequency freq = new WirelessFrequency();
         freq.readFromTag(freqList.m_128728_(i), (byte)1);
         if (freq.getId() > 0) {
            this.frequencies.put(freq.getId(), freq);
         }
      }

      ListTag txList = root.m_128437_("transmitters", 10);

      for (int ix = 0; ix < txList.size(); ix++) {
         CompoundTag entry = txList.m_128728_(ix);
         int freqId = entry.m_128451_("freqId");
         ResourceKey<Level> dimKey = ResourceKey.m_135785_(Registries.f_256858_, ResourceLocation.m_135820_(entry.m_128461_("dim")));
         BlockPos pos = BlockPos.m_122022_(entry.m_128454_("pos"));
         boolean adv = entry.m_128471_("advanced");
         this.transmitters.put(freqId, new WirelessFrequencyManager.TransmitterEntry(dimKey, pos, null, adv));
      }

      ListTag devList = root.m_128437_("devices", 10);

      for (int ix = 0; ix < devList.size(); ix++) {
         CompoundTag entry = devList.m_128728_(ix);
         int freqId = entry.m_128451_("freqId");
         ResourceKey<Level> dimKey = ResourceKey.m_135785_(Registries.f_256858_, ResourceLocation.m_135820_(entry.m_128461_("dim")));
         BlockPos pos = BlockPos.m_122022_(entry.m_128454_("pos"));
         boolean ctrl = entry.m_128471_("controller");
         boolean adv = entry.m_128471_("advanced");
         String deviceName = entry.m_128441_("name") ? entry.m_128461_("name") : WirelessFrequencyManager.DeviceEntry.defaultDeviceName(ctrl, adv);
         this.devices.put(freqId, dimKey.m_135782_().toString(), pos.m_121878_(), new WirelessFrequencyManager.DeviceEntry(dimKey, pos, ctrl, adv, deviceName));
      }
   }

   public CompoundTag m_7176_(CompoundTag root) {
      root.m_128405_("uniqueId", this.uniqueId);
      ListTag freqList = new ListTag();
      ObjectIterator txList = this.frequencies.values().iterator();

      while (txList.hasNext()) {
         WirelessFrequency freq = (WirelessFrequency)txList.next();
         CompoundTag tag = new CompoundTag();
         freq.writeToTag(tag, (byte)1);
         freqList.add(tag);
      }

      root.m_128365_("frequencies", freqList);
      ListTag txListx = new ListTag();
      ObjectIterator var8 = this.transmitters.int2ObjectEntrySet().iterator();

      while (var8.hasNext()) {
         Entry<WirelessFrequencyManager.TransmitterEntry> e = (Entry<WirelessFrequencyManager.TransmitterEntry>)var8.next();
         CompoundTag tag = new CompoundTag();
         tag.m_128405_("freqId", e.getIntKey());
         tag.m_128359_("dim", ((WirelessFrequencyManager.TransmitterEntry)e.getValue()).dimension().m_135782_().toString());
         tag.m_128356_("pos", ((WirelessFrequencyManager.TransmitterEntry)e.getValue()).pos().m_121878_());
         tag.m_128379_("advanced", ((WirelessFrequencyManager.TransmitterEntry)e.getValue()).advanced());
         txListx.add(tag);
      }

      root.m_128365_("transmitters", txListx);
      ListTag devList = new ListTag();
      this.devices.forEach((freqId, d) -> {
         CompoundTag tagx = new CompoundTag();
         tagx.m_128405_("freqId", freqId);
         tagx.m_128359_("dim", d.dimension().m_135782_().toString());
         tagx.m_128356_("pos", d.pos().m_121878_());
         tagx.m_128379_("controller", d.isController());
         tagx.m_128379_("advanced", d.advanced());
         tagx.m_128359_("name", d.deviceName());
         devList.add(tagx);
      });
      root.m_128365_("devices", devList);
      return root;
   }

   public static record DeviceEntry(ResourceKey<Level> dimension, BlockPos pos, boolean isController, boolean advanced, String deviceName) {
      public DeviceEntry(ResourceKey<Level> dimension, BlockPos pos, boolean isController, boolean advanced) {
         this(dimension, pos, isController, advanced, defaultDeviceName(isController, advanced));
      }

      private static String defaultDeviceName(boolean isController, boolean advanced) {
         if (isController) {
            return advanced ? "block.ae2lt.advanced_wireless_overloaded_controller" : "block.ae2lt.wireless_overloaded_controller";
         } else {
            return "block.ae2lt.wireless_receiver";
         }
      }
   }

   @FunctionalInterface
   public interface DeviceListener {
      void onDevicesChanged(int var1);
   }

   public static record TransmitterEntry(ResourceKey<Level> dimension, BlockPos pos, @Nullable IGridNode cachedNode, boolean advanced) {
   }

   @FunctionalInterface
   public interface TransmitterListener {
      void onTransmitterChanged(int var1, boolean var2);
   }

   public interface WirelessTransmitterNodeProvider {
      @Nullable
      IGridNode getWirelessGridNode();

      int getTransmitterFrequencyId();
   }
}
