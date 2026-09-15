package com.moakiee.ae2lt.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public class EjectModeSavedData extends SavedData {
   private static final String DATA_NAME = "ae2lt_eject_registrations";
   private static final String TAG_ENTRIES = "Entries";
   private static final String TAG_I_DIM = "IDim";
   private static final String TAG_I_POS = "IPos";
   private static final String TAG_I_FACE = "IFace";
   private static final String TAG_P_DIM = "PDim";
   private static final String TAG_P_POS = "PPos";
   private final List<EjectModeSavedData.PersistentReg> entries = new ArrayList<>();

   public static EjectModeSavedData get(MinecraftServer server) {
      return (EjectModeSavedData)server.m_129783_().m_8895_().m_164861_(EjectModeSavedData::load, EjectModeSavedData::new, "ae2lt_eject_registrations");
   }

   public List<EjectModeSavedData.PersistentReg> getAll() {
      return Collections.unmodifiableList(this.entries);
   }

   public void add(EjectModeSavedData.PersistentReg reg) {
      this.entries.add(reg);
      this.m_77762_();
   }

   public void removeByIntercept(ResourceKey<Level> dim, BlockPos pos, Direction face) {
      long posL = pos.m_121878_();
      boolean changed = this.entries.removeIf(e -> e.interceptDim().equals(dim) && e.interceptPos().m_121878_() == posL && e.interceptFace() == face);
      if (changed) {
         this.m_77762_();
      }
   }

   public void removeByHost(ResourceKey<Level> hostDim, BlockPos hostPos) {
      boolean changed = this.entries.removeIf(e -> e.hostDim().equals(hostDim) && e.hostPos().equals(hostPos));
      if (changed) {
         this.m_77762_();
      }
   }

   public CompoundTag m_7176_(CompoundTag tag) {
      ListTag list = new ListTag();

      for (EjectModeSavedData.PersistentReg e : this.entries) {
         CompoundTag ct = new CompoundTag();
         ct.m_128359_("IDim", e.interceptDim().m_135782_().toString());
         ct.m_128356_("IPos", e.interceptPos().m_121878_());
         ct.m_128405_("IFace", e.interceptFace().m_122411_());
         ct.m_128359_("PDim", e.hostDim().m_135782_().toString());
         ct.m_128356_("PPos", e.hostPos().m_121878_());
         list.add(ct);
      }

      tag.m_128365_("Entries", list);
      return tag;
   }

   private static EjectModeSavedData load(CompoundTag tag) {
      EjectModeSavedData data = new EjectModeSavedData();
      if (tag.m_128425_("Entries", 9)) {
         ListTag list = tag.m_128437_("Entries", 10);

         for (int i = 0; i < list.size(); i++) {
            CompoundTag ct = list.m_128728_(i);
            ResourceKey<Level> iDim = ResourceKey.m_135785_(Registries.f_256858_, ResourceLocation.m_135820_(ct.m_128461_("IDim")));
            BlockPos iPos = BlockPos.m_122022_(ct.m_128454_("IPos"));
            Direction iFace = Direction.m_122376_(ct.m_128451_("IFace"));
            ResourceKey<Level> pDim = ResourceKey.m_135785_(Registries.f_256858_, ResourceLocation.m_135820_(ct.m_128461_("PDim")));
            BlockPos pPos = BlockPos.m_122022_(ct.m_128454_("PPos"));
            data.entries.add(new EjectModeSavedData.PersistentReg(iDim, iPos, iFace, pDim, pPos));
         }
      }

      return data;
   }

   public static record PersistentReg(
      ResourceKey<Level> interceptDim, BlockPos interceptPos, Direction interceptFace, ResourceKey<Level> hostDim, BlockPos hostPos
   ) {
   }
}
