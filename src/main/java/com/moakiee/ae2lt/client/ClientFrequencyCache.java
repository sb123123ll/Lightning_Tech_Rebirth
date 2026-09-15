package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.grid.FrequencyAccessLevel;
import com.moakiee.ae2lt.grid.FrequencyMember;
import com.moakiee.ae2lt.grid.FrequencySecurityLevel;
import com.moakiee.ae2lt.network.SyncFrequencyListPacket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(
   modid = "ae2lt",
   value = {Dist.CLIENT}
)
public final class ClientFrequencyCache {
   private static final Map<Integer, ClientFrequencyCache.CachedFrequency> cache = new HashMap<>();
   private static final Map<Integer, List<ClientFrequencyCache.CachedMember>> members = new HashMap<>();
   private static final Map<Integer, List<ClientFrequencyCache.CachedConnection>> connections = new HashMap<>();
   private static int revision = 0;

   private ClientFrequencyCache() {
   }

   public static int revision() {
      return revision;
   }

   public static void updateFromSync(List<SyncFrequencyListPacket.FrequencyEntry> entries) {
      cache.clear();

      for (SyncFrequencyListPacket.FrequencyEntry e : entries) {
         cache.put(e.id(), new ClientFrequencyCache.CachedFrequency(e.id(), e.name(), e.color(), e.ownerUUID(), e.security()));
      }

      members.keySet().retainAll(cache.keySet());
      connections.keySet().retainAll(cache.keySet());
      revision++;
   }

   public static void upsertFrequency(int id, String name, int color, UUID ownerUUID, FrequencySecurityLevel security) {
      cache.put(id, new ClientFrequencyCache.CachedFrequency(id, name, color, ownerUUID, security));
      revision++;
   }

   public static void removeFrequency(int id) {
      boolean changed = cache.remove(id) != null;
      changed |= members.remove(id) != null;
      changed |= connections.remove(id) != null;
      if (changed) {
         revision++;
      }
   }

   public static void updateMembers(int frequencyId, CompoundTag tag) {
      ListTag list = tag.m_128437_("members", 10);
      List<ClientFrequencyCache.CachedMember> result = new ArrayList<>(list.size());

      for (int i = 0; i < list.size(); i++) {
         FrequencyMember m = new FrequencyMember(list.m_128728_(i));
         result.add(new ClientFrequencyCache.CachedMember(m.getPlayerUUID(), m.getCachedName(), m.getAccessLevel()));
      }

      members.put(frequencyId, result);
      revision++;
   }

   public static void updateConnections(int frequencyId, CompoundTag tag) {
      ListTag list = tag.m_128437_("connections", 10);
      List<ClientFrequencyCache.CachedConnection> result = new ArrayList<>(list.size());

      for (int i = 0; i < list.size(); i++) {
         CompoundTag e = list.m_128728_(i);
         result.add(
            new ClientFrequencyCache.CachedConnection(
               e.m_128461_("dim"),
               BlockPos.m_122022_(e.m_128454_("pos")),
               e.m_128471_("controller"),
               e.m_128471_("advanced"),
               e.m_128471_("loaded"),
               e.m_128441_("name") ? e.m_128461_("name") : "block.ae2lt.wireless_receiver"
            )
         );
      }

      connections.put(frequencyId, result);
      revision++;
   }

   public static List<ClientFrequencyCache.CachedConnection> getConnections(int frequencyId) {
      return connections.getOrDefault(frequencyId, List.of());
   }

   @Nullable
   public static ClientFrequencyCache.CachedFrequency getFrequency(int id) {
      return cache.get(id);
   }

   public static Collection<ClientFrequencyCache.CachedFrequency> getAllFrequencies() {
      return cache.values();
   }

   public static List<ClientFrequencyCache.CachedFrequency> getAllFrequenciesSorted() {
      ArrayList<ClientFrequencyCache.CachedFrequency> list = new ArrayList<>(cache.values());
      list.sort((a, b) -> Integer.compare(a.id(), b.id()));
      return list;
   }

   public static List<ClientFrequencyCache.CachedMember> getMembers(int frequencyId) {
      return members.getOrDefault(frequencyId, List.of());
   }

   public static void clear() {
      cache.clear();
      members.clear();
      connections.clear();
      revision++;
   }

   @SubscribeEvent
   public static void onLoggingOut(LoggingOut event) {
      clear();
   }

   public static record CachedConnection(String dimension, BlockPos pos, boolean controller, boolean advanced, boolean loaded, String deviceName) {
   }

   public static record CachedFrequency(int id, String name, int color, UUID ownerUUID, FrequencySecurityLevel security) {
   }

   public static record CachedMember(UUID uuid, String name, FrequencyAccessLevel access) {
   }
}
