package com.moakiee.ae2lt.lightning;

import com.moakiee.ae2lt.recipe.RecipeContainerInput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class LightningTransformRecipeInput extends RecipeContainerInput {
   private final List<LightningTransformRecipeInput.GroupedStack> groupedStacks;
   private final List<ItemStack> displayStacks;

   private LightningTransformRecipeInput(List<LightningTransformRecipeInput.GroupedStack> groupedStacks) {
      this.groupedStacks = List.copyOf(groupedStacks);
      this.displayStacks = this.groupedStacks.stream().map(LightningTransformRecipeInput.GroupedStack::asDisplayStack).toList();
   }

   public static LightningTransformRecipeInput fromEntities(Collection<ItemEntity> itemEntities) {
      Map<LightningTransformRecipeInput.ItemStackKey, LightningTransformRecipeInput.MutableGroup> grouped = new LinkedHashMap<>();

      for (ItemEntity itemEntity : itemEntities) {
         ItemStack stack = itemEntity.m_32055_();
         if (!stack.m_41619_()) {
            LightningTransformRecipeInput.ItemStackKey key = new LightningTransformRecipeInput.ItemStackKey(stack);
            LightningTransformRecipeInput.MutableGroup group = grouped.computeIfAbsent(
               key, ignored -> new LightningTransformRecipeInput.MutableGroup(stack.m_255036_(1))
            );
            group.add(itemEntity, stack.m_41613_(), itemEntity.m_20182_());
         }
      }

      return new LightningTransformRecipeInput(grouped.values().stream().map(LightningTransformRecipeInput.MutableGroup::freeze).toList());
   }

   public List<LightningTransformRecipeInput.GroupedStack> groupedStacks() {
      return this.groupedStacks;
   }

   @Override
   public boolean m_7983_() {
      return this.groupedStacks.isEmpty();
   }

   @Override
   public ItemStack m_8020_(int index) {
      return this.displayStacks.get(index);
   }

   @Override
   public int size() {
      return this.displayStacks.size();
   }

   public static final class GroupedStack {
      private final ItemStack stack;
      private final int totalCount;
      private final List<LightningTransformRecipeInput.ParticipantStack> participants;

      private GroupedStack(ItemStack stack, int totalCount, List<LightningTransformRecipeInput.ParticipantStack> participants) {
         this.stack = stack.m_255036_(1);
         this.totalCount = totalCount;
         this.participants = List.copyOf(participants);
      }

      public ItemStack stack() {
         return this.stack;
      }

      public int totalCount() {
         return this.totalCount;
      }

      public List<LightningTransformRecipeInput.ParticipantStack> participants() {
         return this.participants;
      }

      public ItemStack asDisplayStack() {
         return this.stack.m_255036_(this.totalCount);
      }
   }

   private static final class ItemStackKey {
      private final ItemStack stack;
      private final int hash;

      private ItemStackKey(ItemStack stack) {
         this.stack = stack.m_255036_(1);
         this.hash = Objects.hash(this.stack.m_41720_(), this.stack.m_41783_());
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else {
            return other instanceof LightningTransformRecipeInput.ItemStackKey itemStackKey ? ItemStack.m_150942_(this.stack, itemStackKey.stack) : false;
         }
      }

      @Override
      public int hashCode() {
         return this.hash;
      }
   }

   private static final class MutableGroup {
      private final ItemStack stack;
      private final List<LightningTransformRecipeInput.ParticipantStack> participants = new ArrayList<>();
      private int totalCount;

      private MutableGroup(ItemStack stack) {
         this.stack = stack;
      }

      private void add(ItemEntity itemEntity, int count, Vec3 position) {
         this.participants.add(new LightningTransformRecipeInput.ParticipantStack(itemEntity, count, position));
         this.totalCount += count;
      }

      private LightningTransformRecipeInput.GroupedStack freeze() {
         return new LightningTransformRecipeInput.GroupedStack(this.stack, this.totalCount, this.participants);
      }
   }

   public static final class ParticipantStack {
      private final ItemEntity itemEntity;
      private final int count;
      private final Vec3 position;

      private ParticipantStack(ItemEntity itemEntity, int count, Vec3 position) {
         this.itemEntity = Objects.requireNonNull(itemEntity, "itemEntity");
         this.count = count;
         this.position = Objects.requireNonNull(position, "position");
      }

      public ItemEntity itemEntity() {
         return this.itemEntity;
      }

      public int count() {
         return this.count;
      }

      public Vec3 position() {
         return this.position;
      }
   }
}
