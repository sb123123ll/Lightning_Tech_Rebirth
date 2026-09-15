package com.moakiee.ae2lt.integration.recipeviewer.multiblock;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public record MultiblockStructureRecipe(
   ResourceLocation id,
   Component title,
   int sizeX,
   int sizeY,
   int sizeZ,
   List<MultiblockStructureRecipe.Cell> cells,
   List<MultiblockStructureRecipe.MaterialEntry> materials,
   List<ItemStack> focusStacks
) {
   public MultiblockStructureRecipe(
      ResourceLocation id,
      Component title,
      int sizeX,
      int sizeY,
      int sizeZ,
      List<MultiblockStructureRecipe.Cell> cells,
      List<MultiblockStructureRecipe.MaterialEntry> materials,
      List<ItemStack> focusStacks
   ) {
      Objects.requireNonNull(id);
      Objects.requireNonNull(title);
      if (sizeX > 0 && sizeY > 0 && sizeZ > 0) {
         cells = List.copyOf(cells);
         materials = List.copyOf(materials);
         focusStacks = copyStacks(focusStacks);
         this.id = id;
         this.title = title;
         this.sizeX = sizeX;
         this.sizeY = sizeY;
         this.sizeZ = sizeZ;
         this.cells = cells;
         this.materials = materials;
         this.focusStacks = focusStacks;
      } else {
         throw new IllegalArgumentException("Multiblock dimensions must be positive");
      }
   }

   public static MultiblockStructureRecipe create(
      ResourceLocation id,
      Component title,
      int sizeX,
      int sizeY,
      int sizeZ,
      List<MultiblockStructureRecipe.Cell> cells,
      List<MultiblockStructureRecipe.MaterialSpec> materialOrder
   ) {
      List<MultiblockStructureRecipe.Cell> cellCopy = List.copyOf(cells);
      ArrayList<MultiblockStructureRecipe.MaterialEntry> materials = new ArrayList<>(materialOrder.size());

      for (MultiblockStructureRecipe.MaterialSpec spec : materialOrder) {
         int count = 0;

         for (MultiblockStructureRecipe.Cell cell : cellCopy) {
            if (cell.state().m_60713_(spec.block())) {
               count++;
            }
         }

         if (count > 0) {
            materials.add(new MultiblockStructureRecipe.MaterialEntry(spec.block(), count, spec.note()));
         }
      }

      LinkedHashSet<Block> focusBlocks = new LinkedHashSet<>();

      for (MultiblockStructureRecipe.Cell cellx : cellCopy) {
         if (!cellx.state().m_60795_()) {
            focusBlocks.add(cellx.state().m_60734_());
         }

         for (Block alternative : cellx.alternatives()) {
            if (alternative != Blocks.f_50016_) {
               focusBlocks.add(alternative);
            }
         }
      }

      List<ItemStack> focusStacks = focusBlocks.stream().<ItemStack>map(ItemStack::new).toList();
      return new MultiblockStructureRecipe(id, title, sizeX, sizeY, sizeZ, cellCopy, materials, focusStacks);
   }

   private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
      return stacks.stream().<ItemStack>map(ItemStack::m_41777_).toList();
   }

   public static record Cell(BlockPos localPos, BlockState state, Component role, List<Block> alternatives, List<Component> rules, boolean shell) {
      public Cell(BlockPos localPos, BlockState state, Component role, List<Block> alternatives, List<Component> rules, boolean shell) {
         Objects.requireNonNull(localPos);
         Objects.requireNonNull(state);
         Objects.requireNonNull(role);
         alternatives = List.copyOf(alternatives);
         rules = List.copyOf(rules);
         if (alternatives.isEmpty()) {
            throw new IllegalArgumentException("A structure cell must have at least one alternative");
         } else {
            this.localPos = localPos;
            this.state = state;
            this.role = role;
            this.alternatives = alternatives;
            this.rules = rules;
            this.shell = shell;
         }
      }
   }

   public static record MaterialEntry(Block block, int count, Component note) {
      public MaterialEntry(Block block, int count, Component note) {
         Objects.requireNonNull(block);
         Objects.requireNonNull(note);
         if (count <= 0) {
            throw new IllegalArgumentException("Material count must be positive");
         } else {
            this.block = block;
            this.count = count;
            this.note = note;
         }
      }
   }

   public static record MaterialSpec(Block block, Component note) {
      public MaterialSpec(Block block, Component note) {
         Objects.requireNonNull(block);
         Objects.requireNonNull(note);
         this.block = block;
         this.note = note;
      }

      public static MultiblockStructureRecipe.MaterialSpec of(Block block) {
         return new MultiblockStructureRecipe.MaterialSpec(block, Component.m_237119_());
      }

      public static MultiblockStructureRecipe.MaterialSpec of(Block block, Component note) {
         return new MultiblockStructureRecipe.MaterialSpec(block, note);
      }
   }
}
