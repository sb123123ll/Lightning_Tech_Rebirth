package com.moakiee.ae2lt.integration.jade;

import com.moakiee.ae2lt.blockentity.FirmamentConversionCoreBlockEntity;
import com.moakiee.ae2lt.registry.ModBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class FirmamentConversionCoreJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
   private static final ResourceLocation UID = new ResourceLocation("ae2lt", "firmament_conversion_core");
   private static final String TAG_INSIDE_STARSHIP = "InsideStarship";
   private static final String TAG_INACTIVE_CORE_OUTPUT = "InactiveCoreOutput";
   private static final String TAG_PROGRESS = "Progress";
   private static final String TAG_PROCESS_TIME = "ProcessTime";

   public ResourceLocation getUid() {
      return UID;
   }

   public void appendServerData(CompoundTag data, BlockAccessor accessor) {
      if (accessor.getBlockEntity() instanceof FirmamentConversionCoreBlockEntity core) {
         data.m_128379_("InsideStarship", core.isInsideFirmamentStarship());
         data.m_128379_("InactiveCoreOutput", core.hasInactiveSpiritCoreOutput());
         data.m_128405_("Progress", core.getProgress());
         data.m_128405_("ProcessTime", core.getProcessTime());
      }
   }

   public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
      if (accessor.getBlockState().m_60713_((Block)ModBlocks.FIRMAMENT_CONVERSION_CORE.get())) {
         CompoundTag data = accessor.getServerData();
         if (data.m_128441_("InsideStarship") && !data.m_128471_("InsideStarship")) {
            tooltip.add(Component.m_237115_("jade.ae2lt.firmament_conversion_core.invalid_structure"));
         } else {
            if (data.m_128471_("InactiveCoreOutput")) {
               tooltip.add(Component.m_237115_("jade.ae2lt.firmament_conversion_core.inactive_core_output"));
            }

            int processTime = data.m_128451_("ProcessTime");
            if (processTime <= 0) {
               tooltip.add(Component.m_237115_("jade.ae2lt.firmament_conversion_core.idle"));
            } else {
               int progress = data.m_128451_("Progress");
               int percent = Math.min(100, progress * 100 / processTime);
               tooltip.add(Component.m_237110_("jade.ae2lt.firmament_conversion_core.progress", new Object[]{percent}));
            }
         }
      }
   }
}
