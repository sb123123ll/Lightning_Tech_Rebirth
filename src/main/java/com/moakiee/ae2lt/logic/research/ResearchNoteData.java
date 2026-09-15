package com.moakiee.ae2lt.logic.research;

import com.moakiee.ae2lt.util.ItemStackTagSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record ResearchNoteData(UUID ritualSeed, RitualGoal goal, List<ResourceLocation> recipeItems, List<String> descriptionKeys, boolean consumed) {
   public static final String TAG_RITUAL_SEED = "RitualSeed";
   public static final String TAG_GOAL = "Goal";
   public static final String TAG_RECIPE_ITEMS = "RecipeItems";
   public static final String TAG_DESCRIPTIONS = "Descriptions";
   public static final String TAG_CONSUMED = "Consumed";

   public static boolean isBlank(ItemStack stack) {
      CompoundTag tag = ItemStackTagSupport.getTagCopy(stack);
      return !tag.m_128425_("Goal", 8);
   }

   public static boolean isConsumed(ItemStack stack) {
      ResearchNoteData data = read(stack);
      return data != null && data.consumed();
   }

   @Nullable
   public static ResearchNoteData read(ItemStack stack) {
      CompoundTag tag = ItemStackTagSupport.getTagCopy(stack);
      if (!tag.m_128425_("Goal", 8)) {
         return null;
      } else {
         RitualGoal goal = RitualGoal.fromName(tag.m_128461_("Goal"));
         if (goal == null) {
            return null;
         } else {
            UUID ritualSeed;
            try {
               ritualSeed = UUID.fromString(tag.m_128461_("RitualSeed"));
            } catch (IllegalArgumentException var6) {
               return null;
            }

            List<ResourceLocation> recipeItems = readResourceLocationList(tag, "RecipeItems");
            List<String> descriptionKeys = readStringList(tag, "Descriptions");
            return recipeItems.size() == 9 && descriptionKeys.size() == recipeItems.size()
               ? new ResearchNoteData(ritualSeed, goal, List.copyOf(recipeItems), List.copyOf(descriptionKeys), tag.m_128471_("Consumed"))
               : null;
         }
      }
   }

   public void writeTo(ItemStack stack) {
      ItemStackTagSupport.updateTag(stack, tag -> {
         tag.m_128359_("RitualSeed", this.ritualSeed.toString());
         tag.m_128359_("Goal", this.goal.name());
         tag.m_128365_("RecipeItems", writeResourceLocationList(this.recipeItems));
         tag.m_128365_("Descriptions", writeStringList(this.descriptionKeys));
         tag.m_128379_("Consumed", this.consumed);
      });
   }

   public ResearchNoteData withConsumed(boolean consumed) {
      return new ResearchNoteData(this.ritualSeed, this.goal, this.recipeItems, this.descriptionKeys, consumed);
   }

   public String shortCode() {
      return this.ritualSeed.toString().replace("-", "").substring(0, 4).toUpperCase(Locale.ROOT);
   }

   private static List<ResourceLocation> readResourceLocationList(CompoundTag tag, String key) {
      List<ResourceLocation> values = new ArrayList<>();

      for (Tag element : tag.m_128437_(key, 8)) {
         if (element instanceof StringTag) {
            StringTag stringTag = (StringTag)element;
            ResourceLocation id = ResourceLocation.m_135820_(stringTag.m_7916_());
            if (id != null) {
               values.add(id);
            }
         }
      }

      return values;
   }

   private static List<String> readStringList(CompoundTag tag, String key) {
      List<String> values = new ArrayList<>();

      for (Tag element : tag.m_128437_(key, 8)) {
         if (element instanceof StringTag stringTag) {
            values.add(stringTag.m_7916_());
         }
      }

      return values;
   }

   private static ListTag writeResourceLocationList(List<ResourceLocation> values) {
      ListTag listTag = new ListTag();

      for (ResourceLocation value : values) {
         listTag.add(StringTag.m_129297_(value.toString()));
      }

      return listTag;
   }

   private static ListTag writeStringList(List<String> values) {
      ListTag listTag = new ListTag();

      for (String value : values) {
         listTag.add(StringTag.m_129297_(value));
      }

      return listTag;
   }
}
