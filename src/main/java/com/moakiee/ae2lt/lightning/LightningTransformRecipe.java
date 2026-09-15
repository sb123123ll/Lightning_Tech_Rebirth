package com.moakiee.ae2lt.lightning;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.moakiee.ae2lt.registry.ModRecipeTypes;
import com.moakiee.ae2lt.util.RecipeSerializationHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class LightningTransformRecipe implements Recipe<LightningTransformRecipeInput> {
   private final ResourceLocation id;
   private final int priority;
   private final List<CountedIngredient> inputs;
   private final ItemStack result;
   private final int totalInputCount;

   public LightningTransformRecipe(ResourceLocation id, int priority, List<CountedIngredient> inputs, ItemStack result) {
      this.id = Objects.requireNonNull(id, "id");
      Objects.requireNonNull(inputs, "inputs");
      Objects.requireNonNull(result, "result");
      if (inputs.isEmpty()) {
         throw new IllegalArgumentException("inputs cannot be empty");
      } else if (result.m_41619_()) {
         throw new IllegalArgumentException("result cannot be empty");
      } else {
         this.priority = priority;
         this.inputs = List.copyOf(inputs);
         this.result = result.m_41777_();
         this.totalInputCount = this.inputs.stream().mapToInt(CountedIngredient::count).sum();
      }
   }

   public int priority() {
      return this.priority;
   }

   public List<CountedIngredient> inputs() {
      return this.inputs;
   }

   public int ingredientCount() {
      return this.inputs.size();
   }

   public int totalInputCount() {
      return this.totalInputCount;
   }

   public ResourceLocation m_6423_() {
      return this.id;
   }

   public boolean matches(LightningTransformRecipeInput input, Level level) {
      return this.planMatch(input).isPresent();
   }

   public Optional<LightningTransformPlan> planMatch(LightningTransformRecipeInput input) {
      List<LightningTransformRecipeInput.GroupedStack> groupedStacks = input.groupedStacks();
      if (groupedStacks.isEmpty()) {
         return Optional.empty();
      } else {
         int[] groupFlexibility = new int[groupedStacks.size()];
         List<List<Integer>> rawMatches = new ArrayList<>(this.inputs.size());

         for (CountedIngredient countedIngredient : this.inputs) {
            List<Integer> matchingGroups = new ArrayList<>();
            int availableCount = 0;

            for (int groupIndex = 0; groupIndex < groupedStacks.size(); groupIndex++) {
               LightningTransformRecipeInput.GroupedStack groupedStack = groupedStacks.get(groupIndex);
               if (countedIngredient.ingredient().test(groupedStack.stack())) {
                  matchingGroups.add(groupIndex);
                  availableCount += groupedStack.totalCount();
                  groupFlexibility[groupIndex]++;
               }
            }

            if (availableCount < countedIngredient.count()) {
               return Optional.empty();
            }

            rawMatches.add(matchingGroups);
         }

         List<LightningTransformRecipe.RequirementState> requirements = new ArrayList<>(this.inputs.size());

         for (int inputIndex = 0; inputIndex < this.inputs.size(); inputIndex++) {
            CountedIngredient countedIngredient = this.inputs.get(inputIndex);
            List<Integer> matchingGroups = rawMatches.get(inputIndex);
            matchingGroups.sort(
               Comparator.<Integer>comparingInt(groupIndexx -> groupFlexibility[groupIndexx])
                  .thenComparing(Comparator.<Integer>comparingInt(groupIndexx -> groupedStacks.get(groupIndexx).totalCount()).reversed())
            );
            requirements.add(
               new LightningTransformRecipe.RequirementState(countedIngredient.count(), matchingGroups.stream().mapToInt(Integer::intValue).toArray())
            );
         }

         requirements.sort(
            Comparator.comparingInt(LightningTransformRecipe.RequirementState::matchingGroupCount)
               .thenComparing(Comparator.comparingInt(LightningTransformRecipe.RequirementState::count).reversed())
         );
         int[] remainingCounts = groupedStacks.stream().mapToInt(LightningTransformRecipeInput.GroupedStack::totalCount).toArray();
         int[] groupConsumptions = new int[groupedStacks.size()];
         return !this.allocateRequirement(0, requirements, remainingCounts, groupConsumptions)
            ? Optional.empty()
            : Optional.of(LightningTransformPlan.fromGroupCounts(groupedStacks, groupConsumptions));
      }
   }

   public ItemStack assemble(LightningTransformRecipeInput input, RegistryAccess registries) {
      return this.result.m_41777_();
   }

   public boolean m_8004_(int width, int height) {
      return true;
   }

   public ItemStack m_8043_(RegistryAccess registries) {
      return this.result.m_41777_();
   }

   public NonNullList<Ingredient> m_7527_() {
      NonNullList<Ingredient> ingredients = NonNullList.m_122779_();

      for (CountedIngredient input : this.inputs) {
         ingredients.add(input.ingredient());
      }

      return ingredients;
   }

   public RecipeSerializer<?> m_7707_() {
      return (RecipeSerializer<?>)ModRecipeTypes.LIGHTNING_TRANSFORM_SERIALIZER.get();
   }

   public RecipeType<?> m_6671_() {
      return (RecipeType<?>)ModRecipeTypes.LIGHTNING_TRANSFORM_TYPE.get();
   }

   public boolean m_5598_() {
      return true;
   }

   public boolean m_142505_() {
      return this.inputs.isEmpty() || this.result.m_41619_() || this.inputs.stream().anyMatch(input -> input.ingredient().m_43908_().length == 0);
   }

   private ItemStack rawResult() {
      return this.result;
   }

   private boolean allocateRequirement(
      int requirementIndex, List<LightningTransformRecipe.RequirementState> requirements, int[] remainingCounts, int[] groupConsumptions
   ) {
      if (requirementIndex >= requirements.size()) {
         return true;
      } else {
         LightningTransformRecipe.RequirementState requirement = requirements.get(requirementIndex);
         return this.allocateAcrossGroups(requirementIndex, requirements, requirement, 0, requirement.count(), remainingCounts, groupConsumptions);
      }
   }

   private boolean allocateAcrossGroups(
      int requirementIndex,
      List<LightningTransformRecipe.RequirementState> requirements,
      LightningTransformRecipe.RequirementState requirement,
      int groupCursor,
      int needed,
      int[] remainingCounts,
      int[] groupConsumptions
   ) {
      if (needed == 0) {
         return this.allocateRequirement(requirementIndex + 1, requirements, remainingCounts, groupConsumptions);
      } else if (groupCursor >= requirement.matchingGroups.length) {
         return false;
      } else if (this.remainingCapacity(requirement.matchingGroups, groupCursor, remainingCounts) < needed) {
         return false;
      } else {
         int groupIndex = requirement.matchingGroups[groupCursor];
         int maxTake = Math.min(needed, remainingCounts[groupIndex]);

         for (int take = maxTake; take >= 0; take--) {
            if (take > 0) {
               remainingCounts[groupIndex] -= take;
               groupConsumptions[groupIndex] += take;
            }

            if (this.allocateAcrossGroups(requirementIndex, requirements, requirement, groupCursor + 1, needed - take, remainingCounts, groupConsumptions)) {
               return true;
            }

            if (take > 0) {
               groupConsumptions[groupIndex] -= take;
               remainingCounts[groupIndex] += take;
            }
         }

         return false;
      }
   }

   private int remainingCapacity(int[] matchingGroups, int startIndex, int[] remainingCounts) {
      int total = 0;

      for (int index = startIndex; index < matchingGroups.length; index++) {
         total += remainingCounts[matchingGroups[index]];
      }

      return total;
   }

   private static final class RequirementState {
      private final int count;
      private final int[] matchingGroups;

      private RequirementState(int count, int[] matchingGroups) {
         this.count = count;
         this.matchingGroups = matchingGroups;
      }

      private int count() {
         return this.count;
      }

      private int matchingGroupCount() {
         return this.matchingGroups.length;
      }
   }

   public static final class Serializer implements RecipeSerializer<LightningTransformRecipe> {
      public LightningTransformRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
         int priority = GsonHelper.m_13824_(json, "priority", 0);
         JsonArray inputsJson = GsonHelper.m_13933_(json, "inputs");
         List<CountedIngredient> inputs = new ArrayList<>(inputsJson.size());

         for (JsonElement element : inputsJson) {
            inputs.add(CountedIngredient.fromJson(GsonHelper.m_13918_(element, "inputs[]")));
         }

         return new LightningTransformRecipe(recipeId, priority, inputs, RecipeSerializationHelper.itemStackFromJson(json, "result"));
      }

      public LightningTransformRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
         int priority = buffer.readInt();
         int inputCount = buffer.readInt();
         List<CountedIngredient> inputs = new ArrayList<>(inputCount);

         for (int i = 0; i < inputCount; i++) {
            inputs.add(CountedIngredient.fromNetwork(buffer));
         }

         return new LightningTransformRecipe(recipeId, priority, inputs, buffer.m_130267_());
      }

      public void toNetwork(FriendlyByteBuf buffer, LightningTransformRecipe recipe) {
         buffer.writeInt(recipe.priority());
         buffer.writeInt(recipe.inputs().size());

         for (CountedIngredient input : recipe.inputs()) {
            input.toNetwork(buffer);
         }

         buffer.m_130055_(recipe.rawResult());
      }
   }
}
