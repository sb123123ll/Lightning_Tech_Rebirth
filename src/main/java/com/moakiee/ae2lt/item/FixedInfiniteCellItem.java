package com.moakiee.ae2lt.item;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.items.storage.StorageCellTooltipComponent;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.logic.advancement.ProgressionAdvancementService;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.registry.ModFumos;
import com.moakiee.ae2lt.registry.ModItems;
import com.moakiee.ae2lt.util.ItemStackTagSupport;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

public final class FixedInfiniteCellItem extends AE2LTItem {
   private static final String TAG_SEED = "CellSeed";
   private static final String TAG_TYPE = "CellType";
   private static final String TAG_RESULT_CONSUMED = "ResultConsumed";
   private static final String TAG_WORLD_SEED = "WorldSeed";
   private static final String BASE_KEY = "item.ae2lt.mysterious_cell";
   private static final String TOOLTIP_KEY = "tooltip.ae2lt.mysterious_cell";

   public FixedInfiniteCellItem(Properties properties) {
      super(properties.m_41487_(1));
   }

   public void m_6883_(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
      super.m_6883_(stack, level, entity, slotId, isSelected);
      if (!level.f_46443_ && entity instanceof ServerPlayer player) {
         ProgressionAdvancementService.inspectMysteriousCell(player, stack);
      }
   }

   public static void setSeed(ItemStack stack, UUID seed) {
      ItemStackTagSupport.updateTag(stack, tag -> tag.m_128362_("CellSeed", seed));
   }

   public static void initializeOuterCell(ItemStack stack) {
      if (!hasType(stack)) {
         if (!hasSeed(stack)) {
            setSeed(stack, UUID.randomUUID());
         }

         if (!hasWorldSeed(stack)) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
               setWorldSeed(stack, server.m_129783_().m_7328_());
            }
         }
      }
   }

   @Nullable
   public static UUID getSeed(ItemStack stack) {
      CompoundTag tag = ItemStackTagSupport.getTagCopy(stack);
      return tag.m_128403_("CellSeed") ? tag.m_128342_("CellSeed") : null;
   }

   public static boolean hasSeed(ItemStack stack) {
      return getSeed(stack) != null;
   }

   private static void setWorldSeed(ItemStack stack, long worldSeed) {
      ItemStackTagSupport.updateTag(stack, tag -> tag.m_128356_("WorldSeed", worldSeed));
   }

   private static boolean hasWorldSeed(ItemStack stack) {
      CompoundTag tag = ItemStackTagSupport.getTagCopy(stack);
      return tag.m_128441_("WorldSeed");
   }

   private static long getWorldSeed(ItemStack stack) {
      CompoundTag tag = ItemStackTagSupport.getTagCopy(stack);
      return tag.m_128454_("WorldSeed");
   }

   public static boolean isOuterCell(ItemStack stack) {
      return !hasType(stack);
   }

   public static boolean isResultConsumed(ItemStack stack) {
      if (!isOuterCell(stack)) {
         return false;
      } else {
         CompoundTag tag = ItemStackTagSupport.getTagCopy(stack);
         return tag.m_128471_("ResultConsumed");
      }
   }

   public static void setResultConsumed(ItemStack stack, boolean consumed) {
      if (isOuterCell(stack)) {
         initializeOuterCell(stack);
         ItemStackTagSupport.updateTag(stack, tag -> tag.m_128379_("ResultConsumed", consumed));
      }
   }

   public static FixedInfiniteCellItem.CellOutcome resolveOutcome(UUID seed, long worldSeed) {
      long mixed = seed.getLeastSignificantBits() ^ worldSeed ^ seed.getMostSignificantBits() ^ Long.reverseBytes(worldSeed);
      int roll = Math.floorMod(mixed, 10000);
      int noteEnd = AE2LTCommonConfig.easterEggWeight();
      int moakieeEnd = Math.min(10000, noteEnd + 2000);
      int cystrysuEnd = Math.min(10000, moakieeEnd + 2000);
      if (roll < noteEnd) {
         return FixedInfiniteCellItem.CellOutcome.RESEARCH_NOTE;
      } else if (roll < moakieeEnd) {
         return FixedInfiniteCellItem.CellOutcome.MOAKIEE_FUMO;
      } else {
         return roll < cystrysuEnd ? FixedInfiniteCellItem.CellOutcome.CYSTRYSU_FUMO : FixedInfiniteCellItem.CellOutcome.LIGHTNING_ROD;
      }
   }

   public static FixedInfiniteCellItem.CellOutcome getOutcomeFromSeed(ItemStack stack) {
      UUID seed = getSeed(stack);
      if (seed == null) {
         return FixedInfiniteCellItem.CellOutcome.LIGHTNING_ROD;
      } else {
         long worldSeed;
         if (hasWorldSeed(stack)) {
            worldSeed = getWorldSeed(stack);
         } else {
            worldSeed = 0L;
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
               worldSeed = server.m_129783_().m_7328_();
            }
         }

         return resolveOutcome(seed, worldSeed);
      }
   }

   public static void setType(ItemStack stack, byte type) {
      ItemStackTagSupport.updateTag(stack, tag -> tag.m_128344_("CellType", type));
   }

   public static boolean hasType(ItemStack stack) {
      CompoundTag tag = ItemStackTagSupport.getTagCopy(stack);
      return tag.m_128441_("CellType");
   }

   public static byte getType(ItemStack stack) {
      CompoundTag tag = ItemStackTagSupport.getTagCopy(stack);
      return tag.m_128445_("CellType");
   }

   public static AEKey getEffectiveKey(ItemStack stack) {
      return (AEKey)(hasType(stack)
         ? FixedInfiniteCellItem.CellOutcome.fromTypeId(getType(stack)).displayKey()
         : AEItemKey.of(createDisplayedResultStack(stack)));
   }

   public static ItemStack createDisplayedResultStack(ItemStack stack) {
      return createDisplayedResultStack(getOutcomeFromSeed(stack));
   }

   public static ItemStack createDisplayedResultStack(FixedInfiniteCellItem.CellOutcome outcome) {
      if (outcome == FixedInfiniteCellItem.CellOutcome.RESEARCH_NOTE) {
         return new ItemStack((ItemLike)ModItems.RESEARCH_NOTE.get());
      } else {
         ItemStack innerStack = new ItemStack((ItemLike)ModItems.MYSTERIOUS_CELL.get());
         setType(innerStack, outcome.typeId());
         return innerStack;
      }
   }

   public Component m_7626_(ItemStack stack) {
      return hasType(stack)
         ? Component.m_237115_("item.ae2lt.mysterious_cell." + FixedInfiniteCellItem.CellOutcome.fromTypeId(getType(stack)).suffix())
         : Component.m_237115_("item.ae2lt.mysterious_cell");
   }

   @Override
   public void appendHoverText(ItemStack stack, AE2LTItem.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      if (!hasType(stack)) {
         if (hasSeed(stack)) {
            if (isResultConsumed(stack)) {
               tooltipComponents.add(Component.m_237115_("tooltip.ae2lt.mysterious_cell.consumed").m_130940_(ChatFormatting.DARK_GRAY));
               tooltipComponents.add(Component.m_237115_("tooltip.ae2lt.mysterious_cell.consumed.hint").m_130940_(ChatFormatting.DARK_GRAY));
            } else {
               String suffix = getOutcomeFromSeed(stack).suffix();
               tooltipComponents.add(Component.m_237115_("tooltip.ae2lt.mysterious_cell." + suffix).m_130940_(ChatFormatting.GREEN));
               tooltipComponents.add(Component.m_237115_("tooltip.ae2lt.mysterious_cell.hint.once").m_130940_(ChatFormatting.GRAY));
            }
         }
      }
   }

   public Optional<TooltipComponent> m_142422_(ItemStack stack) {
      if (!hasType(stack) && !hasSeed(stack)) {
         return Optional.empty();
      } else if (isResultConsumed(stack)) {
         return Optional.empty();
      } else {
         AEKey key = getEffectiveKey(stack);
         long amount = hasType(stack) ? 2147483647L * (long)key.getAmountPerUnit() : (long)key.getAmountPerUnit();
         List<GenericStack> content = Collections.singletonList(new GenericStack(key, amount));
         return Optional.of(new StorageCellTooltipComponent(List.of(), content, false, true));
      }
   }

   public static enum CellOutcome {
      LIGHTNING_ROD((byte)0, "lightning_rod"),
      HIGH_VOLTAGE((byte)1, "high_voltage"),
      EXTREME_HIGH_VOLTAGE((byte)2, "extreme_high_voltage"),
      LIGHTNING_COLLAPSE_MATRIX((byte)3, "lightning_collapse_matrix"),
      RESEARCH_NOTE((byte)4, "research_note"),
      MOAKIEE_FUMO((byte)5, "moakiee_fumo"),
      CYSTRYSU_FUMO((byte)6, "cystrysu_fumo");

      private final byte typeId;
      private final String suffix;

      private CellOutcome(byte typeId, String suffix) {
         this.typeId = typeId;
         this.suffix = suffix;
      }

      public byte typeId() {
         return this.typeId;
      }

      public String suffix() {
         return this.suffix;
      }

      public static FixedInfiniteCellItem.CellOutcome fromTypeId(byte id) {
         return switch (id) {
            case 1 -> HIGH_VOLTAGE;
            case 2 -> EXTREME_HIGH_VOLTAGE;
            case 3 -> LIGHTNING_COLLAPSE_MATRIX;
            case 4 -> RESEARCH_NOTE;
            case 5 -> MOAKIEE_FUMO;
            case 6 -> CYSTRYSU_FUMO;
            default -> LIGHTNING_ROD;
         };
      }

      public AEKey displayKey() {
         return (AEKey)(switch (this) {
            case LIGHTNING_ROD -> AEItemKey.of(Items.f_151041_);
            case HIGH_VOLTAGE -> LightningKey.HIGH_VOLTAGE;
            case EXTREME_HIGH_VOLTAGE -> LightningKey.EXTREME_HIGH_VOLTAGE;
            case LIGHTNING_COLLAPSE_MATRIX -> AEItemKey.of((ItemLike)ModItems.LIGHTNING_COLLAPSE_MATRIX.get());
            case RESEARCH_NOTE -> AEItemKey.of((ItemLike)ModItems.RESEARCH_NOTE.get());
            case MOAKIEE_FUMO -> AEItemKey.of((ItemLike)ModFumos.MOAKIEE_FUMO_ITEM.get());
            case CYSTRYSU_FUMO -> AEItemKey.of((ItemLike)ModFumos.CYSTRYSU_FUMO_ITEM.get());
         });
      }
   }
}
