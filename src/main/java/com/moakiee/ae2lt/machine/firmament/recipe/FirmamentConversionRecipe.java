package com.moakiee.ae2lt.machine.firmament.recipe;

import com.google.gson.JsonObject;
import com.moakiee.ae2lt.registry.ModRecipeTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class FirmamentConversionRecipe implements Recipe<FirmamentConversionRecipeInput> {
   private static final Codec<List<FirmamentConversionIngredient>> INPUTS_CODEC = FirmamentConversionIngredient.CODEC.codec().listOf();
   private static final Codec<Integer> POSITIVE_PROCESS_TIME_CODEC = Codec.intRange(1, Integer.MAX_VALUE);
   private static final Codec<List<ItemStack>> OUTPUTS_CODEC = ItemStack.f_41582_.listOf();
   private ResourceLocation id;
   private final int priority;
   private final List<FirmamentConversionIngredient> inputs;
   private final List<ItemStack> results;
   private final int processTime;
   private final int totalInputCount;

   public FirmamentConversionRecipe(int priority, List<FirmamentConversionIngredient> inputs, ItemStack result, int processTime) {
      this(priority, inputs, List.of(result), processTime);
   }

   public FirmamentConversionRecipe(int priority, List<FirmamentConversionIngredient> inputs, List<ItemStack> results, int processTime) {
      Objects.requireNonNull(inputs, "inputs");
      Objects.requireNonNull(results, "results");
      if (inputs.isEmpty() || inputs.size() > 3) {
         throw new IllegalArgumentException("inputs must contain 1 to 3 entries");
      } else if (results.isEmpty() || results.size() > 4) {
         throw new IllegalArgumentException("results must contain 1 to 4 entries");
      } else if (results.stream().anyMatch(ItemStack::m_41619_)) {
         throw new IllegalArgumentException("results cannot contain empty stacks");
      } else if (processTime <= 0) {
         throw new IllegalArgumentException("processTime must be positive");
      } else {
         this.priority = priority;
         this.inputs = List.copyOf(inputs);
         this.results = results.stream().<ItemStack>map(ItemStack::m_41777_).toList();
         this.processTime = processTime;
         this.totalInputCount = this.inputs.stream().mapToInt(FirmamentConversionIngredient::count).sum();
      }
   }

   public int priority() {
      return this.priority;
   }

   public List<FirmamentConversionIngredient> inputs() {
      return this.inputs;
   }

   public ItemStack getResultStack() {
      return this.results.get(0).m_41777_();
   }

   public List<ItemStack> getResultStacks() {
      return this.results.stream().<ItemStack>map(ItemStack::m_41777_).toList();
   }

   public int processTime() {
      return this.processTime;
   }

   public int totalInputCount() {
      return this.totalInputCount;
   }

   public boolean matches(FirmamentConversionRecipeInput input, Level level) {
      return this.planMatch(input).isPresent();
   }

   public Optional<FirmamentConversionRecipeMatch> planMatch(FirmamentConversionRecipeInput input) {
      List<FirmamentConversionRecipeInput.SlotStack> slotStacks = input.slotStacks();
      if (!slotStacks.isEmpty() && slotStacks.size() <= 3) {
         int[] slotFlexibility = new int[slotStacks.size()];
         List<List<Integer>> rawMatches = new ArrayList<>(this.inputs.size());

         for (FirmamentConversionIngredient requirement : this.inputs) {
            List<Integer> matchingSlots = new ArrayList<>();
            int availableCount = 0;

            for (int slotIndex = 0; slotIndex < slotStacks.size(); slotIndex++) {
               FirmamentConversionRecipeInput.SlotStack slotStack = slotStacks.get(slotIndex);
               if (requirement.ingredient().test(slotStack.stack())) {
                  matchingSlots.add(slotIndex);
                  availableCount += slotStack.stack().m_41613_();
                  slotFlexibility[slotIndex]++;
               }
            }

            if (availableCount < requirement.count()) {
               return Optional.empty();
            }

            rawMatches.add(matchingSlots);
         }

         List<FirmamentConversionRecipe.RequirementState> requirements = new ArrayList<>(this.inputs.size());

         for (int requirementIndex = 0; requirementIndex < this.inputs.size(); requirementIndex++) {
            FirmamentConversionIngredient requirement = this.inputs.get(requirementIndex);
            List<Integer> matchingSlots = rawMatches.get(requirementIndex);
            matchingSlots.sort(
               Comparator.<Integer>comparingInt(slotIndexx -> slotFlexibility[slotIndexx])
                  .thenComparing(Comparator.<Integer>comparingInt(slotIndexx -> slotStacks.get(slotIndexx).stack().m_41613_()).reversed())
            );
            requirements.add(new FirmamentConversionRecipe.RequirementState(requirement.count(), matchingSlots.stream().mapToInt(Integer::intValue).toArray()));
         }

         requirements.sort(
            Comparator.comparingInt(FirmamentConversionRecipe.RequirementState::matchingSlotCount)
               .thenComparing(Comparator.comparingInt(FirmamentConversionRecipe.RequirementState::count).reversed())
         );
         int[] remainingCounts = slotStacks.stream().mapToInt(slotStackx -> slotStackx.stack().m_41613_()).toArray();
         int[] slotConsumptions = new int[3];
         return !this.allocateRequirement(0, requirements, slotStacks, remainingCounts, slotConsumptions)
            ? Optional.empty()
            : Optional.of(new FirmamentConversionRecipeMatch(slotConsumptions));
      } else {
         return Optional.empty();
      }
   }

   public ItemStack assemble(FirmamentConversionRecipeInput input, RegistryAccess registries) {
      return this.getResultStack();
   }

   public boolean m_8004_(int width, int height) {
      return true;
   }

   public ItemStack m_8043_(RegistryAccess registries) {
      return this.getResultStack();
   }

   public ResourceLocation m_6423_() {
      return this.id;
   }

   public void setId(ResourceLocation id) {
      this.id = id;
   }

   public NonNullList<Ingredient> m_7527_() {
      NonNullList<Ingredient> ingredients = NonNullList.m_122779_();

      for (FirmamentConversionIngredient input : this.inputs) {
         ingredients.add(input.ingredient());
      }

      return ingredients;
   }

   public RecipeSerializer<?> m_7707_() {
      return (RecipeSerializer<?>)ModRecipeTypes.FIRMAMENT_CONVERSION_SERIALIZER.get();
   }

   public RecipeType<?> m_6671_() {
      return (RecipeType<?>)ModRecipeTypes.FIRMAMENT_CONVERSION_TYPE.get();
   }

   public boolean m_142505_() {
      return this.inputs.isEmpty()
         || this.results.isEmpty()
         || this.results.stream().anyMatch(ItemStack::m_41619_)
         || this.processTime <= 0
         || this.inputs.stream().anyMatch(input -> input.ingredient().m_43947_());
   }

   private List<ItemStack> rawResults() {
      return this.results;
   }

   private boolean allocateRequirement(
      int requirementIndex,
      List<FirmamentConversionRecipe.RequirementState> requirements,
      List<FirmamentConversionRecipeInput.SlotStack> slotStacks,
      int[] remainingCounts,
      int[] slotConsumptions
   ) {
      if (requirementIndex >= requirements.size()) {
         return true;
      } else {
         FirmamentConversionRecipe.RequirementState requirement = requirements.get(requirementIndex);
         return this.allocateAcrossSlots(requirementIndex, requirements, requirement, slotStacks, 0, requirement.count(), remainingCounts, slotConsumptions);
      }
   }

   private boolean allocateAcrossSlots(
      int requirementIndex,
      List<FirmamentConversionRecipe.RequirementState> requirements,
      FirmamentConversionRecipe.RequirementState requirement,
      List<FirmamentConversionRecipeInput.SlotStack> slotStacks,
      int slotCursor,
      int needed,
      int[] remainingCounts,
      int[] slotConsumptions
   ) {
      if (needed == 0) {
         return this.allocateRequirement(requirementIndex + 1, requirements, slotStacks, remainingCounts, slotConsumptions);
      } else if (slotCursor >= requirement.matchingSlots.length) {
         return false;
      } else if (this.remainingCapacity(requirement.matchingSlots, slotCursor, remainingCounts) < needed) {
         return false;
      } else {
         int slotIndex = requirement.matchingSlots[slotCursor];
         int maxTake = Math.min(needed, remainingCounts[slotIndex]);
         int machineSlot = slotStacks.get(slotIndex).slot();

         for (int take = maxTake; take >= 0; take--) {
            if (take > 0) {
               remainingCounts[slotIndex] -= take;
               slotConsumptions[machineSlot] += take;
            }

            if (this.allocateAcrossSlots(
               requirementIndex, requirements, requirement, slotStacks, slotCursor + 1, needed - take, remainingCounts, slotConsumptions
            )) {
               return true;
            }

            if (take > 0) {
               slotConsumptions[machineSlot] -= take;
               remainingCounts[slotIndex] += take;
            }
         }

         return false;
      }
   }

   private int remainingCapacity(int[] matchingSlots, int startIndex, int[] remainingCounts) {
      int total = 0;

      for (int index = startIndex; index < matchingSlots.length; index++) {
         total += remainingCounts[matchingSlots[index]];
      }

      return total;
   }

   private static final class RequirementState {
      private final int count;
      private final int[] matchingSlots;

      private RequirementState(int count, int[] matchingSlots) {
         this.count = count;
         this.matchingSlots = matchingSlots;
      }

      private int count() {
         return this.count;
      }

      private int matchingSlotCount() {
         return this.matchingSlots.length;
      }
   }

   public static final class Serializer implements RecipeSerializer<FirmamentConversionRecipe> {
      private static final MapCodec<FirmamentConversionRecipe> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
                  Codec.INT.optionalFieldOf("priority", 0).forGetter(FirmamentConversionRecipe::priority),
                  FirmamentConversionRecipe.INPUTS_CODEC.fieldOf("inputs").forGetter(FirmamentConversionRecipe::inputs),
                  FirmamentConversionRecipe.OUTPUTS_CODEC.fieldOf("results").forGetter(FirmamentConversionRecipe::rawResults),
                  FirmamentConversionRecipe.POSITIVE_PROCESS_TIME_CODEC.fieldOf("processTime").forGetter(FirmamentConversionRecipe::processTime)
               )
               .apply(instance, FirmamentConversionRecipe::new)
      );

      public FirmamentConversionRecipe fromJson(ResourceLocation id, JsonObject json) {
         FirmamentConversionRecipe recipe = (FirmamentConversionRecipe)CODEC.codec().parse(JsonOps.INSTANCE, json).resultOrPartial(error -> {
            throw new IllegalArgumentException("Failed to parse firmament conversion recipe " + id + ": " + error);
         }).orElseThrow();
         recipe.setId(id);
         return recipe;
      }

      public FirmamentConversionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
         int priority = buf.m_130242_();
         int inputCount = buf.m_130242_();
         List<FirmamentConversionIngredient> inputs = new ArrayList<>(inputCount);

         for (int i = 0; i < inputCount; i++) {
            inputs.add(new FirmamentConversionIngredient(Ingredient.m_43940_(buf), buf.m_130242_()));
         }

         int resultCount = buf.m_130242_();
         List<ItemStack> results = new ArrayList<>(resultCount);

         for (int i = 0; i < resultCount; i++) {
            results.add(buf.m_130267_());
         }

         int processTime = buf.m_130242_();
         FirmamentConversionRecipe recipe = new FirmamentConversionRecipe(priority, inputs, results, processTime);
         recipe.setId(id);
         return recipe;
      }

      public void toNetwork(FriendlyByteBuf buf, FirmamentConversionRecipe recipe) {
         buf.m_130130_(recipe.priority);
         buf.m_130130_(recipe.inputs.size());

         for (FirmamentConversionIngredient input : recipe.inputs) {
            input.ingredient().m_43923_(buf);
            buf.m_130130_(input.count());
         }

         buf.m_130130_(recipe.results.size());

         for (ItemStack result : recipe.results) {
            buf.m_130055_(result);
         }

         buf.m_130130_(recipe.processTime);
      }
   }
}
