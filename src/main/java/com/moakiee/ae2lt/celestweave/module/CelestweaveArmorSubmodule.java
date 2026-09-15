package com.moakiee.ae2lt.celestweave.module;

import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import com.moakiee.ae2lt.device.module.OverloadDeviceSubmodule;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import org.jetbrains.annotations.Nullable;

public interface CelestweaveArmorSubmodule extends OverloadDeviceSubmodule {
   @Override
   String id();

   String nameKey();

   String descriptionKey();

   boolean defaultEnabled();

   default Component name() {
      return Component.m_237115_(this.nameKey());
   }

   default Component description() {
      return Component.m_237115_(this.descriptionKey());
   }

   default Component buttonLabel(boolean enabled) {
      return Component.m_237110_(enabled ? "ae2lt.celestweave.feature.toggle.on" : "ae2lt.celestweave.feature.toggle.off", new Object[]{this.name()});
   }

   default void onInstalled(@Nullable Player player, Dist dist, ItemStack armor) {
   }

   default void onUninstalled(@Nullable Player player, Dist dist, ItemStack armor) {
   }

   default void onActivated(@Nullable Player player, Dist dist, ItemStack armor) {
   }

   default void onDeactivated(@Nullable Player player, Dist dist, ItemStack armor) {
   }

   default int getMaxInstallAmount() {
      return 0;
   }

   default String installGroupId() {
      return this.id();
   }

   default Set<String> installGroupIds() {
      String groupId = this.installGroupId();
      return groupId != null && !groupId.isBlank() ? Set.of(groupId) : Set.of();
   }

   default int tickActive(@Nullable Player player, Dist dist, ItemStack armor) {
      return 0;
   }

   default CompoundTag loadData(ItemStack armor, CompoundTag data) {
      return data.m_6426_();
   }

   default CompoundTag saveData(ItemStack armor, CompoundTag data) {
      return data.m_6426_();
   }

   default CompoundTag getData(ItemStack armor) {
      return CelestweaveArmorState.getSubmoduleData(armor, this);
   }

   default void setData(ItemStack armor, CompoundTag data) {
      CelestweaveArmorState.setSubmoduleData(armor, this, data);
   }

   default boolean isInstalled(ItemStack armor, Provider registries) {
      return CelestweaveArmorState.isSubmoduleInstalled(armor, registries, this.id());
   }

   @Deprecated(
      forRemoval = false
   )
   @Override
   default boolean isInstalled(ItemStack armor) {
      return CelestweaveArmorState.isSubmoduleInstalled(armor, this.id());
   }

   @Override
   default boolean isActive(ItemStack armor) {
      return CelestweaveArmorState.isSubmoduleRuntimeActive(armor, this.id());
   }

   List<CelestweaveArmorSubmoduleConfig> getConfigs(ItemStack var1);

   boolean setConfig(ItemStack var1, String var2, @Nullable Tag var3);

   List<CelestweaveArmorSubmoduleOptionUi> getConfigUI(ItemStack var1);

   @Deprecated(
      forRemoval = false
   )
   default List<CelestweaveArmorSubmoduleOptionUi> getOptionUI(ItemStack armor) {
      return this.getConfigUI(armor);
   }
}
