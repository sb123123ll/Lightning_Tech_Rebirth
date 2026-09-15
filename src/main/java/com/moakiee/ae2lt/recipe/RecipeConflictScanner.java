package com.moakiee.ae2lt.recipe;

import com.moakiee.ae2lt.lightning.LightningTransformRecipe;
import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.CrystalCatalyzerRecipe;
import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.Mode;
import com.moakiee.ae2lt.machine.firmament.recipe.FirmamentConversionRecipe;
import com.moakiee.ae2lt.machine.lightningassembly.recipe.LightningAssemblyRecipe;
import com.moakiee.ae2lt.machine.lightningchamber.recipe.LightningSimulationRecipe;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipe;
import com.moakiee.ae2lt.registry.ModRecipeTypes;
import com.moakiee.ae2lt.util.RecipeManagerByTypeAccess;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.fluids.FluidStack;

public final class RecipeConflictScanner {
   public static final long INPUT_SCALE = 8192L;

   private RecipeConflictScanner() {
   }

   public static List<ResourceLocation> scan(RecipeManager recipeManager) {
      TreeSet<ResourceLocation> conflicts = new TreeSet<>(Comparator.comparing(ResourceLocation::toString));
      scanPool(
         RecipeManagerByTypeAccess.byType(recipeManager, (RecipeType)ModRecipeTypes.LIGHTNING_TRANSFORM_TYPE.get())
            .entrySet()
            .stream()
            .map(RecipeConflictScanner::fromLightningTransform)
            .toList(),
         conflicts
      );
      scanPool(
         RecipeManagerByTypeAccess.byType(recipeManager, (RecipeType)ModRecipeTypes.FIRMAMENT_CONVERSION_TYPE.get())
            .entrySet()
            .stream()
            .map(RecipeConflictScanner::fromFirmamentConversion)
            .toList(),
         conflicts
      );
      scanPool(
         RecipeManagerByTypeAccess.byType(recipeManager, (RecipeType)ModRecipeTypes.LIGHTNING_SIMULATION_TYPE.get())
            .entrySet()
            .stream()
            .map(RecipeConflictScanner::fromLightningSimulation)
            .toList(),
         conflicts
      );
      scanPool(
         RecipeManagerByTypeAccess.byType(recipeManager, (RecipeType)ModRecipeTypes.LIGHTNING_ASSEMBLY_TYPE.get())
            .entrySet()
            .stream()
            .map(RecipeConflictScanner::fromLightningAssembly)
            .toList(),
         conflicts
      );
      scanPool(
         RecipeManagerByTypeAccess.byType(recipeManager, (RecipeType)ModRecipeTypes.OVERLOAD_PROCESSING_TYPE.get())
            .entrySet()
            .stream()
            .map(RecipeConflictScanner::fromOverloadProcessing)
            .toList(),
         conflicts
      );
      Map<ResourceLocation, CrystalCatalyzerRecipe> catalyzerRecipes = RecipeManagerByTypeAccess.byType(
         recipeManager, (RecipeType)ModRecipeTypes.CRYSTAL_CATALYZER_TYPE.get()
      );

      for (Mode mode : Mode.values()) {
         scanPool(
            catalyzerRecipes.entrySet().stream().filter(entry -> entry.getValue().mode() == mode).map(RecipeConflictScanner::fromCrystalCatalyzer).toList(),
            conflicts
         );
      }

      return List.copyOf(conflicts);
   }

   private static RecipeConflictScanner.RecipeRequirements fromLightningTransform(Entry<ResourceLocation, LightningTransformRecipe> entry) {
      return new RecipeConflictScanner.RecipeRequirements(
         entry.getKey(),
         entry.getValue().inputs().stream().map(input -> new RecipeConflictScanner.ItemRequirement(input.ingredient(), (long)input.count())).toList(),
         List.of()
      );
   }

   private static RecipeConflictScanner.RecipeRequirements fromFirmamentConversion(Entry<ResourceLocation, FirmamentConversionRecipe> entry) {
      return new RecipeConflictScanner.RecipeRequirements(
         entry.getKey(),
         entry.getValue().inputs().stream().map(input -> new RecipeConflictScanner.ItemRequirement(input.ingredient(), (long)input.count())).toList(),
         List.of()
      );
   }

   private static RecipeConflictScanner.RecipeRequirements fromLightningSimulation(Entry<ResourceLocation, LightningSimulationRecipe> entry) {
      return new RecipeConflictScanner.RecipeRequirements(
         entry.getKey(),
         entry.getValue().inputs().stream().map(input -> new RecipeConflictScanner.ItemRequirement(input.ingredient(), (long)input.count())).toList(),
         List.of()
      );
   }

   private static RecipeConflictScanner.RecipeRequirements fromLightningAssembly(Entry<ResourceLocation, LightningAssemblyRecipe> entry) {
      return new RecipeConflictScanner.RecipeRequirements(
         entry.getKey(),
         entry.getValue().inputs().stream().map(input -> new RecipeConflictScanner.ItemRequirement(input.ingredient(), (long)input.count())).toList(),
         List.of()
      );
   }

   private static RecipeConflictScanner.RecipeRequirements fromOverloadProcessing(Entry<ResourceLocation, OverloadProcessingRecipe> entry) {
      OverloadProcessingRecipe recipe = entry.getValue();
      List<RecipeConflictScanner.FluidRequirement> fluidRequirements = recipe.fluidInput().isEmpty()
         ? List.of()
         : List.of(new RecipeConflictScanner.FluidRequirement(recipe.fluidInput()));
      return new RecipeConflictScanner.RecipeRequirements(
         entry.getKey(),
         recipe.itemInputs().stream().map(input -> new RecipeConflictScanner.ItemRequirement(input.ingredient(), (long)input.count())).toList(),
         fluidRequirements
      );
   }

   private static RecipeConflictScanner.RecipeRequirements fromCrystalCatalyzer(Entry<ResourceLocation, CrystalCatalyzerRecipe> entry) {
      CrystalCatalyzerRecipe recipe = entry.getValue();
      List<RecipeConflictScanner.ItemRequirement> itemRequirements = recipe.catalyst()
         .map(ingredient -> List.of(new RecipeConflictScanner.ItemRequirement(ingredient, (long)recipe.catalystCount())))
         .orElseGet(List::of);
      return new RecipeConflictScanner.RecipeRequirements(entry.getKey(), itemRequirements, List.of());
   }

   private static void scanPool(List<RecipeConflictScanner.RecipeRequirements> recipes, TreeSet<ResourceLocation> conflicts) {
      for (int targetIndex = 0; targetIndex < recipes.size(); targetIndex++) {
         RecipeConflictScanner.RecipeRequirements target = recipes.get(targetIndex);
         List<RecipeConflictScanner.ItemRequirement> itemSupplies = new ArrayList<>();
         List<RecipeConflictScanner.FluidRequirement> fluidSupplies = new ArrayList<>();

         for (int sourceIndex = 0; sourceIndex < recipes.size(); sourceIndex++) {
            if (sourceIndex != targetIndex) {
               RecipeConflictScanner.RecipeRequirements source = recipes.get(sourceIndex);
               source.items().forEach(requirement -> itemSupplies.add(requirement.scaled()));
               source.fluids().forEach(requirement -> fluidSupplies.add(requirement.scaled()));
            }
         }

         if (canCoverItems(itemSupplies, target.items()) && canCoverFluids(fluidSupplies, target.fluids())) {
            conflicts.add(target.id());
         }
      }
   }

   static boolean canCoverItems(List<RecipeConflictScanner.ItemRequirement> supplies, List<RecipeConflictScanner.ItemRequirement> requirements) {
      return canCover(
         supplies.stream().map(RecipeConflictScanner.ItemRequirement::count).toList(),
         requirements.stream().map(RecipeConflictScanner.ItemRequirement::count).toList(),
         (supplyIndex, requirementIndex) -> ingredientsOverlap(supplies.get(supplyIndex).ingredient(), requirements.get(requirementIndex).ingredient())
      );
   }

   private static boolean canCoverFluids(List<RecipeConflictScanner.FluidRequirement> supplies, List<RecipeConflictScanner.FluidRequirement> requirements) {
      return canCover(
         supplies.stream().map(RecipeConflictScanner.FluidRequirement::amount).toList(),
         requirements.stream().map(RecipeConflictScanner.FluidRequirement::amount).toList(),
         (supplyIndex, requirementIndex) -> supplies.get(supplyIndex).stack().isFluidStackIdentical(requirements.get(requirementIndex).stack())
      );
   }

   static boolean canCover(List<Long> supplyCapacities, List<Long> requirementAmounts, RecipeConflictScanner.EdgePredicate edgePredicate) {
      long totalDemand = 0L;

      for (long amount : requirementAmounts) {
         totalDemand = Math.addExact(totalDemand, amount);
      }

      if (totalDemand == 0L) {
         return true;
      } else if (supplyCapacities.isEmpty()) {
         return false;
      } else {
         int source = 0;
         int firstSupply = 1;
         int firstRequirement = firstSupply + supplyCapacities.size();
         int sink = firstRequirement + requirementAmounts.size();
         RecipeConflictScanner.CapacityFlow flow = new RecipeConflictScanner.CapacityFlow(sink + 1);

         for (int supplyIndex = 0; supplyIndex < supplyCapacities.size(); supplyIndex++) {
            flow.addEdge(source, firstSupply + supplyIndex, supplyCapacities.get(supplyIndex));

            for (int requirementIndex = 0; requirementIndex < requirementAmounts.size(); requirementIndex++) {
               if (edgePredicate.test(supplyIndex, requirementIndex)) {
                  flow.addEdge(firstSupply + supplyIndex, firstRequirement + requirementIndex, totalDemand);
               }
            }
         }

         for (int requirementIndexx = 0; requirementIndexx < requirementAmounts.size(); requirementIndexx++) {
            flow.addEdge(firstRequirement + requirementIndexx, sink, requirementAmounts.get(requirementIndexx));
         }

         return flow.maxFlow(source, sink, totalDemand) == totalDemand;
      }
   }

   private static boolean ingredientsOverlap(Ingredient left, Ingredient right) {
      for (ItemStack stack : left.m_43908_()) {
         if (right.test(stack)) {
            return true;
         }
      }

      for (ItemStack stackx : right.m_43908_()) {
         if (left.test(stackx)) {
            return true;
         }
      }

      return false;
   }

   private static long scale(long amount) {
      return Math.multiplyExact(amount, 8192L);
   }

   private static final class CapacityFlow {
      private final List<List<RecipeConflictScanner.CapacityFlow.Edge>> graph;
      private int[] levels;
      private int[] cursors;

      private CapacityFlow(int nodeCount) {
         this.graph = new ArrayList<>(nodeCount);

         for (int i = 0; i < nodeCount; i++) {
            this.graph.add(new ArrayList<>());
         }
      }

      private void addEdge(int from, int to, long capacity) {
         RecipeConflictScanner.CapacityFlow.Edge forward = new RecipeConflictScanner.CapacityFlow.Edge(to, this.graph.get(to).size(), capacity);
         RecipeConflictScanner.CapacityFlow.Edge reverse = new RecipeConflictScanner.CapacityFlow.Edge(from, this.graph.get(from).size(), 0L);
         this.graph.get(from).add(forward);
         this.graph.get(to).add(reverse);
      }

      private long maxFlow(int source, int sink, long limit) {
         long total = 0L;

         while (total < limit && this.buildLevels(source, sink)) {
            this.cursors = new int[this.graph.size()];

            long pushed;
            while (total < limit && (pushed = this.push(source, sink, limit - total)) > 0L) {
               total += pushed;
            }
         }

         return total;
      }

      private boolean buildLevels(int source, int sink) {
         this.levels = new int[this.graph.size()];
         Arrays.fill(this.levels, -1);
         this.levels[source] = 0;
         ArrayDeque<Integer> queue = new ArrayDeque<>();
         queue.add(source);

         while (!queue.isEmpty()) {
            int node = queue.removeFirst();

            for (RecipeConflictScanner.CapacityFlow.Edge edge : this.graph.get(node)) {
               if (edge.capacity > 0L && this.levels[edge.to] < 0) {
                  this.levels[edge.to] = this.levels[node] + 1;
                  queue.addLast(edge.to);
               }
            }
         }

         return this.levels[sink] >= 0;
      }

      private long push(int node, int sink, long available) {
         if (node == sink) {
            return available;
         } else {
            for (List<RecipeConflictScanner.CapacityFlow.Edge> edges = this.graph.get(node); this.cursors[node] < edges.size(); this.cursors[node]++) {
               RecipeConflictScanner.CapacityFlow.Edge edge = edges.get(this.cursors[node]);
               if (edge.capacity > 0L && this.levels[edge.to] == this.levels[node] + 1) {
                  long pushed = this.push(edge.to, sink, Math.min(available, edge.capacity));
                  if (pushed > 0L) {
                     edge.capacity -= pushed;
                     this.graph.get(edge.to).get(edge.reverseIndex).capacity += pushed;
                     return pushed;
                  }
               }
            }

            return 0L;
         }
      }

      private static final class Edge {
         private final int to;
         private final int reverseIndex;
         private long capacity;

         private Edge(int to, int reverseIndex, long capacity) {
            this.to = to;
            this.reverseIndex = reverseIndex;
            this.capacity = capacity;
         }
      }
   }

   @FunctionalInterface
   interface EdgePredicate {
      boolean test(int var1, int var2);
   }

   private static record FluidRequirement(FluidStack stack, long amount) {
      private FluidRequirement(FluidStack stack) {
         this(new FluidStack(stack, 1), (long)stack.getAmount());
      }

      private FluidRequirement(FluidStack stack, long amount) {
         if (!stack.isEmpty() && amount > 0L) {
            this.stack = stack;
            this.amount = amount;
         } else {
            throw new IllegalArgumentException("fluid requirement must be non-empty and positive");
         }
      }

      private RecipeConflictScanner.FluidRequirement scaled() {
         return new RecipeConflictScanner.FluidRequirement(this.stack, RecipeConflictScanner.scale(this.amount));
      }
   }

   static record ItemRequirement(Ingredient ingredient, long count) {
      ItemRequirement(Ingredient ingredient, long count) {
         if (count <= 0L) {
            throw new IllegalArgumentException("count must be positive");
         } else {
            this.ingredient = ingredient;
            this.count = count;
         }
      }

      RecipeConflictScanner.ItemRequirement scaled() {
         return new RecipeConflictScanner.ItemRequirement(this.ingredient, RecipeConflictScanner.scale(this.count));
      }
   }

   private static record RecipeRequirements(
      ResourceLocation id, List<RecipeConflictScanner.ItemRequirement> items, List<RecipeConflictScanner.FluidRequirement> fluids
   ) {
   }
}
