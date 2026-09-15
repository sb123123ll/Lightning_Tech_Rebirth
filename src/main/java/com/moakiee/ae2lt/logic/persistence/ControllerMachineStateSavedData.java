package com.moakiee.ae2lt.logic.persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class ControllerMachineStateSavedData extends SavedData {
   private static final String DATA_NAME = "ae2lt_controller_machine_states";
   private static final String TAG_ENTRIES = "Entries";
   private static final String TAG_TYPE = "Type";
   private static final String TAG_ID = "Id";
   private static final String TAG_STATE = "State";
   private final Map<ControllerMachineStateSavedData.MachineKey, CompoundTag> states = new HashMap<>();
   private final Map<ControllerMachineStateSavedData.MachineKey, Supplier<CompoundTag>> deferredStateSnapshots = new HashMap<>();
   private final Map<ControllerMachineStateSavedData.MachineKey, ControllerMachineStateSavedData.Owner> owners = new HashMap<>();

   public static ControllerMachineStateSavedData get(ServerLevel level) {
      return (ControllerMachineStateSavedData)level.m_7654_()
         .m_129783_()
         .m_8895_()
         .m_164861_(ControllerMachineStateSavedData::load, ControllerMachineStateSavedData::new, "ae2lt_controller_machine_states");
   }

   public boolean hasState(ControllerMachineStateSavedData.MachineType type, UUID id) {
      return type != null && id != null && this.states.containsKey(new ControllerMachineStateSavedData.MachineKey(type, id));
   }

   public CompoundTag getState(ControllerMachineStateSavedData.MachineType type, UUID id) {
      if (type != null && id != null) {
         CompoundTag state = this.states.get(new ControllerMachineStateSavedData.MachineKey(type, id));
         return state == null ? new CompoundTag() : state.m_6426_();
      } else {
         return new CompoundTag();
      }
   }

   public void setState(ControllerMachineStateSavedData.MachineType type, UUID id, CompoundTag state) {
      if (type != null && id != null && state != null) {
         this.setOwnedState(type, id, state.m_6426_());
      }
   }

   public void setOwnedState(ControllerMachineStateSavedData.MachineType type, UUID id, CompoundTag state) {
      if (type != null && id != null && state != null) {
         ControllerMachineStateSavedData.MachineKey key = new ControllerMachineStateSavedData.MachineKey(type, id);
         this.deferredStateSnapshots.remove(key);
         if (!state.equals(this.states.get(key))) {
            this.states.put(key, state);
            this.m_77762_();
         }
      }
   }

   public void deferStateSnapshot(ControllerMachineStateSavedData.MachineType type, UUID id, Supplier<CompoundTag> snapshotSupplier) {
      if (type != null && id != null && snapshotSupplier != null) {
         this.deferredStateSnapshots.put(new ControllerMachineStateSavedData.MachineKey(type, id), snapshotSupplier);
         this.m_77762_();
      }
   }

   public void cancelDeferredStateSnapshot(ControllerMachineStateSavedData.MachineType type, UUID id) {
      if (type != null && id != null) {
         this.deferredStateSnapshots.remove(new ControllerMachineStateSavedData.MachineKey(type, id));
      }
   }

   private void materializeDeferredStateSnapshots() {
      HashMap<ControllerMachineStateSavedData.MachineKey, Supplier<CompoundTag>> pending = new HashMap<>(this.deferredStateSnapshots);

      for (Entry<ControllerMachineStateSavedData.MachineKey, Supplier<CompoundTag>> entry : pending.entrySet()) {
         CompoundTag state = entry.getValue().get();
         this.deferredStateSnapshots.remove(entry.getKey(), entry.getValue());
         if (state != null && !state.equals(this.states.get(entry.getKey()))) {
            this.states.put(entry.getKey(), state);
         }
      }
   }

   public boolean claim(ControllerMachineStateSavedData.MachineType type, UUID id, ServerLevel level, BlockPos pos) {
      return level != null && pos != null ? this.claim(type, id, level.m_46472_().m_135782_().toString(), pos.m_121878_()) : false;
   }

   public boolean claim(ControllerMachineStateSavedData.MachineType type, UUID id, String dimension, long position) {
      if (type != null && id != null && dimension != null) {
         ControllerMachineStateSavedData.MachineKey key = new ControllerMachineStateSavedData.MachineKey(type, id);
         ControllerMachineStateSavedData.Owner owner = new ControllerMachineStateSavedData.Owner(dimension, position);
         ControllerMachineStateSavedData.Owner current = this.owners.get(key);
         if (current != null && !current.equals(owner)) {
            return false;
         } else {
            this.owners.put(key, owner);
            return true;
         }
      } else {
         return false;
      }
   }

   public void release(ControllerMachineStateSavedData.MachineType type, UUID id, ServerLevel level, BlockPos pos) {
      if (level != null && pos != null) {
         this.release(type, id, level.m_46472_().m_135782_().toString(), pos.m_121878_());
      }
   }

   public void release(ControllerMachineStateSavedData.MachineType type, UUID id, String dimension, long position) {
      if (type != null && id != null && dimension != null) {
         this.owners.remove(new ControllerMachineStateSavedData.MachineKey(type, id), new ControllerMachineStateSavedData.Owner(dimension, position));
      }
   }

   public boolean isOwner(ControllerMachineStateSavedData.MachineType type, UUID id, ServerLevel level, BlockPos pos) {
      return type != null && id != null && level != null && pos != null
         ? new ControllerMachineStateSavedData.Owner(level.m_46472_().m_135782_().toString(), pos.m_121878_())
            .equals(this.owners.get(new ControllerMachineStateSavedData.MachineKey(type, id)))
         : false;
   }

   public CompoundTag m_7176_(CompoundTag tag) {
      this.materializeDeferredStateSnapshots();
      ListTag entries = new ListTag();

      for (Entry<ControllerMachineStateSavedData.MachineKey, CompoundTag> entry : this.states.entrySet()) {
         CompoundTag entryTag = new CompoundTag();
         entryTag.m_128359_("Type", entry.getKey().type().name());
         entryTag.m_128362_("Id", entry.getKey().id());
         entryTag.m_128365_("State", entry.getValue().m_6426_());
         entries.add(entryTag);
      }

      tag.m_128365_("Entries", entries);
      return tag;
   }

   public static ControllerMachineStateSavedData load(CompoundTag tag) {
      ControllerMachineStateSavedData data = new ControllerMachineStateSavedData();
      ListTag entries = tag.m_128437_("Entries", 10);

      for (int i = 0; i < entries.size(); i++) {
         CompoundTag entry = entries.m_128728_(i);
         if (entry.m_128403_("Id") && entry.m_128425_("State", 10)) {
            try {
               ControllerMachineStateSavedData.MachineType type = ControllerMachineStateSavedData.MachineType.valueOf(entry.m_128461_("Type"));
               data.states.put(new ControllerMachineStateSavedData.MachineKey(type, entry.m_128342_("Id")), entry.m_128469_("State").m_6426_());
            } catch (IllegalArgumentException var6) {
            }
         }
      }

      return data;
   }

   private static record MachineKey(ControllerMachineStateSavedData.MachineType type, UUID id) {
      private MachineKey(ControllerMachineStateSavedData.MachineType type, UUID id) {
         Objects.requireNonNull(type, "type");
         Objects.requireNonNull(id, "id");
         this.type = type;
         this.id = id;
      }
   }

   public static enum MachineType {
      TIANSHU,
      MATRIX;
   }

   private static record Owner(String dimension, long position) {
   }
}
