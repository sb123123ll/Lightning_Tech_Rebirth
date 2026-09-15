package com.moakiee.ae2lt.logic;

import appeng.api.orientation.RelativeSide;
import appeng.util.SettingsFrom;
import com.moakiee.ae2lt.machine.common.LightningCollapseMatrixHost;
import java.util.EnumSet;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class MemoryCardConfigSupport {
   private static final String TAG_MACHINE_CONFIG = "AE2LTMachineConfig";
   private static final String TAG_AUTO_EXPORT = "AutoExport";
   private static final String TAG_ALLOWED_OUTPUTS = "AllowedOutputs";
   private static final String TAG_MATRIX_COUNT = "LightningCollapseMatrixCount";

   private MemoryCardConfigSupport() {
   }

   public static void writeCustomTag(CompoundTag output, CompoundTag tag) {
      if (!tag.m_128456_()) {
         output.m_128365_("AE2LTMachineConfig", tag.m_6426_());
      }
   }

   @Nullable
   public static CompoundTag readCustomTag(CompoundTag input) {
      return !input.m_128425_("AE2LTMachineConfig", 10) ? null : input.m_128469_("AE2LTMachineConfig").m_6426_();
   }

   public static void exportMemoryCardSettings(SettingsFrom mode, CompoundTag output, Consumer<CompoundTag> writer) {
      if (mode == SettingsFrom.MEMORY_CARD) {
         CompoundTag tag = new CompoundTag();
         writer.accept(tag);
         writeCustomTag(output, tag);
      }
   }

   public static void importMemoryCardSettings(SettingsFrom mode, CompoundTag input, Consumer<CompoundTag> reader) {
      if (mode == SettingsFrom.MEMORY_CARD) {
         CompoundTag tag = readCustomTag(input);
         if (tag != null) {
            reader.accept(tag);
         }
      }
   }

   public static void exportAutoExportSettings(
      SettingsFrom mode, CompoundTag output, boolean autoExport, EnumSet<RelativeSide> allowedOutputs, Consumer<CompoundTag> extraWriter
   ) {
      exportMemoryCardSettings(mode, output, tag -> {
         tag.m_128379_("AutoExport", autoExport);
         writeRelativeSideSet(tag, "AllowedOutputs", allowedOutputs);
         extraWriter.accept(tag);
      });
   }

   public static void importAutoExportSettings(
      SettingsFrom mode,
      CompoundTag input,
      Consumer<Boolean> autoExportSetter,
      Consumer<EnumSet<RelativeSide>> allowedOutputsSetter,
      Consumer<CompoundTag> extraReader,
      Runnable afterImport
   ) {
      importMemoryCardSettings(mode, input, tag -> {
         ifBoolean(tag, "AutoExport", autoExportSetter);
         if (tag.m_128441_("AllowedOutputs")) {
            allowedOutputsSetter.accept(readRelativeSideSet(tag, "AllowedOutputs"));
         }

         extraReader.accept(tag);
         afterImport.run();
      });
   }

   public static void writeRelativeSideSet(CompoundTag tag, String key, EnumSet<RelativeSide> sides) {
      if (sides != null) {
         ListTag list = new ListTag();

         for (RelativeSide side : sides) {
            list.add(StringTag.m_129297_(side.name()));
         }

         tag.m_128365_(key, list);
      }
   }

   public static EnumSet<RelativeSide> readRelativeSideSet(CompoundTag tag, String key) {
      EnumSet<RelativeSide> result = EnumSet.noneOf(RelativeSide.class);
      if (!tag.m_128425_(key, 9)) {
         return result;
      } else {
         ListTag list = tag.m_128437_(key, 8);

         for (int i = 0; i < list.size(); i++) {
            try {
               result.add(RelativeSide.valueOf(list.m_128778_(i)));
            } catch (IllegalArgumentException var6) {
            }
         }

         return result;
      }
   }

   public static void writeDirection(CompoundTag tag, String key, @Nullable Direction direction) {
      if (direction != null) {
         tag.m_128344_(key, (byte)direction.m_122411_());
      }
   }

   @Nullable
   public static Direction readDirection(CompoundTag tag, String key) {
      if (!tag.m_128441_(key)) {
         return null;
      } else {
         int idx = tag.m_128445_(key);
         return idx >= 0 && idx < 6 ? Direction.m_122376_(idx) : null;
      }
   }

   public static <E extends Enum<E>> void writeEnum(CompoundTag tag, String key, @Nullable E value) {
      if (value != null) {
         tag.m_128359_(key, value.name());
      }
   }

   public static <E extends Enum<E>> E readEnum(CompoundTag tag, String key, Class<E> type, E fallback) {
      if (!tag.m_128441_(key)) {
         return fallback;
      } else {
         try {
            return Enum.valueOf(type, tag.m_128461_(key));
         } catch (IllegalArgumentException var5) {
            return fallback;
         }
      }
   }

   public static void ifBoolean(CompoundTag tag, String key, Consumer<Boolean> setter) {
      if (tag.m_128441_(key)) {
         setter.accept(tag.m_128471_(key));
      }
   }

   public static void writeMatrixCount(CompoundTag tag, LightningCollapseMatrixHost host) {
      tag.m_128405_("LightningCollapseMatrixCount", host.getInstalledMatrixCount());
   }

   public static void restoreMatrixCount(CompoundTag tag, @Nullable Player player, LightningCollapseMatrixHost host) {
      if (tag.m_128441_("LightningCollapseMatrixCount")) {
         int missing = host.restoreMatricesFromMemoryCard(player, tag.m_128451_("LightningCollapseMatrixCount"));
         if (missing > 0 && player != null && !player.m_9236_().m_5776_()) {
            player.m_213846_(Component.m_237110_("message.ae2lt.memory_card.missing_matrices", new Object[]{missing}));
         }
      }
   }
}
