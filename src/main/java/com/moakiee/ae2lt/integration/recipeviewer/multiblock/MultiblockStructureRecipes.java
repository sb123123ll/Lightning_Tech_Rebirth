package com.moakiee.ae2lt.integration.recipeviewer.multiblock;

import com.moakiee.ae2lt.logic.craft.MatrixMultiblockRole;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockTemplate;
import com.moakiee.ae2lt.logic.tianshu.TianshuMultiblockRole;
import com.moakiee.ae2lt.logic.tianshu.TianshuMultiblockTemplate;
import com.moakiee.ae2lt.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class MultiblockStructureRecipes {
   private static final BlockPos MATRIX_DEFAULT_PATTERN = new BlockPos(1, 1, 1);
   private static final BlockPos MATRIX_DEFAULT_PORT = new BlockPos(6, 5, 3);
   private static final BlockPos MATRIX_DEFAULT_THREAD = MatrixMultiblockTemplate.entries()
      .stream()
      .filter(entry -> entry.role() == MatrixMultiblockRole.CRAFTING_BAY)
      .map(MatrixMultiblockTemplate.Entry::localPos)
      .filter(pos -> !pos.equals(MatrixMultiblockTemplate.CRAFTING_CENTER_LOCAL))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Matrix template has no peripheral crafting slot"));
   private static final BlockPos TIANSHU_DEFAULT_PARALLEL = new BlockPos(2, 2, 2);

   public static List<MultiblockStructureRecipe> all() {
      return List.of(matrix(), tianshu());
   }

   private static MultiblockStructureRecipe matrix() {
      Block casing = (Block)ModBlocks.MATTER_WARPING_MATRIX_CASING.get();
      Block frame = (Block)ModBlocks.MATTER_WARPING_MATRIX_CONSTRAINT_FRAME.get();
      Block glass = (Block)ModBlocks.MATTER_WARPING_MATRIX_GLASS.get();
      Block controller = (Block)ModBlocks.MATTER_WARPING_MATRIX_CONTROLLER.get();
      Block port = (Block)ModBlocks.MATTER_WARPING_MATRIX_PORT.get();
      Block patternT1 = (Block)ModBlocks.MATTER_WARPING_MATRIX_PATTERN_STORAGE_T1.get();
      Block patternT2 = (Block)ModBlocks.MATTER_WARPING_MATRIX_PATTERN_STORAGE_T2.get();
      Block blank = (Block)ModBlocks.TIANSHU_BLANK_UNIT.get();
      Block threadT1 = (Block)ModBlocks.MATTER_WARPING_MATRIX_THREAD_UNIT_T1.get();
      List<Block> mainCores = List.of(
         (Block)ModBlocks.MATTER_WARPING_MATRIX_STABLE_MAIN_CORE.get(),
         (Block)ModBlocks.MATTER_WARPING_MATRIX_QUANTUM_MAIN_CORE.get(),
         (Block)ModBlocks.MATTER_WARPING_MATRIX_OVERLOAD_MAIN_CORE.get(),
         (Block)ModBlocks.MATTER_WARPING_MATRIX_MULTIDIMENSIONAL_MAIN_CORE.get()
      );
      List<Block> peripheralUnits = List.of(
         blank,
         threadT1,
         (Block)ModBlocks.MATTER_WARPING_MATRIX_THREAD_UNIT_T2.get(),
         (Block)ModBlocks.TIANSHU_AMPLIFIER_UNIT.get(),
         (Block)ModBlocks.MATTER_WARPING_MATRIX_THERMAL_CONTROL_UNIT_T1.get(),
         (Block)ModBlocks.MATTER_WARPING_MATRIX_THERMAL_CONTROL_UNIT_T2.get()
      );
      Component casingRole = role("casing");
      Component frameRole = role("constraint_frame");
      Component glassRole = role("glass");
      Component controllerRole = role("controller");
      Component portRole = role("port_candidate");
      Component patternRole = role("pattern_bay");
      Component mainCoreRole = role("main_core");
      Component peripheralUnitRole = role("peripheral_unit_slot");
      Component portRule = rule("matrix_port");
      Component patternRule = rule("matrix_pattern");
      Component mainCoreRule = rule("matrix_main_core");
      Component peripheralUnitRule = rule("matrix_peripheral_unit");
      Component amplifierRule = rule("matrix_amplifier");
      ArrayList<MultiblockStructureRecipe.Cell> cells = new ArrayList<>();

      for (MatrixMultiblockTemplate.Entry entry : MatrixMultiblockTemplate.entries()) {
         BlockPos pos = entry.localPos();
         MatrixMultiblockRole role = entry.role();
         switch (role) {
            case EMPTY:
            default:
               break;
            case CASING:
               cells.add(cell(pos, casing, casingRole, List.of(casing), true));
               break;
            case CONSTRAINT_FRAME:
               cells.add(cell(pos, frame, frameRole, List.of(frame), true));
               break;
            case GLASS:
               cells.add(cell(pos, glass, glassRole, List.of(glass), true));
               break;
            case CONTROLLER:
               cells.add(cell(pos, facing(controller, Direction.EAST), controllerRole, List.of(controller), List.of(), false));
               break;
            case PORT_CANDIDATE: {
               Block displayed = pos.equals(MATRIX_DEFAULT_PORT) ? port : frame;
               cells.add(cell(pos, displayed, portRole, List.of(port, frame), List.of(portRule), false));
               break;
            }
            case PATTERN_BAY: {
               Block displayed = pos.equals(MATRIX_DEFAULT_PATTERN) ? patternT1 : Blocks.f_50016_;
               cells.add(cell(pos, displayed, patternRole, List.of(Blocks.f_50016_, patternT1, patternT2), List.of(patternRule), false));
               break;
            }
            case CRAFTING_BAY:
               if (pos.equals(MatrixMultiblockTemplate.CRAFTING_CENTER_LOCAL)) {
                  cells.add(cell(pos, mainCores.get(0), mainCoreRole, mainCores, List.of(mainCoreRule), false));
               } else {
                  Block displayed = isDefaultMatrixThreadPosition(pos) ? threadT1 : blank;
                  cells.add(cell(pos, displayed, peripheralUnitRole, peripheralUnits, List.of(peripheralUnitRule, amplifierRule), false));
               }
         }
      }

      List<MultiblockStructureRecipe.MaterialSpec> materialOrder = List.of(
         material(casing),
         material(frame, portRule),
         material(glass),
         material(controller),
         material(port, portRule),
         material(patternT1, patternRule),
         material(mainCores.get(0), mainCoreRule),
         material(blank, peripheralUnitRule),
         material(threadT1, peripheralUnitRule)
      );
      return MultiblockStructureRecipe.create(
         id("tianshu_matter_warping_matrix"), Component.m_237115_("jei.ae2lt.multiblock.matrix"), 7, 11, 7, cells, materialOrder
      );
   }

   private static MultiblockStructureRecipe tianshu() {
      Block casing = (Block)ModBlocks.TIANSHU_SUPERCOMPUTER_CASING.get();
      Block cooling = (Block)ModBlocks.PHASE_CHANGE_COOLING_UNIT.get();
      Block glass = (Block)ModBlocks.TIANSHU_SUPERCOMPUTER_GLASS.get();
      Block controller = (Block)ModBlocks.TIANSHU_SUPERCOMPUTER_CONTROLLER.get();
      Block port = (Block)ModBlocks.TIANSHU_SUPERCOMPUTER_PORT.get();
      Block blank = (Block)ModBlocks.TIANSHU_BLANK_UNIT.get();
      Block storage = (Block)ModBlocks.STORAGE_SUPERCOMPUTING_UNIT.get();
      Block parallel = (Block)ModBlocks.PARALLEL_SUPERCOMPUTING_UNIT.get();
      Block amplifier = (Block)ModBlocks.TIANSHU_AMPLIFIER_UNIT.get();
      Block patternStorage = (Block)ModBlocks.CLOSED_LOOP_PATTERN_STORAGE.get();
      Block seedStorage = (Block)ModBlocks.CLOSED_LOOP_SEED_STORAGE.get();
      List<Block> coolingPositionBlocks = List.of(cooling, patternStorage, seedStorage);
      List<Block> peripheralUnits = List.of(blank, storage, parallel, amplifier);
      List<Block> mainCores = List.of(
         (Block)ModBlocks.BASELINE_SUPERCOMPUTING_UNIT.get(),
         (Block)ModBlocks.QUANTUM_SUPERCOMPUTING_UNIT.get(),
         (Block)ModBlocks.OVERLOAD_SUPERCOMPUTING_UNIT.get(),
         (Block)ModBlocks.MULTIDIMENSIONAL_SUPERCOMPUTING_UNIT.get()
      );
      Component casingRole = role("casing");
      Component coolingRole = role("cooling");
      Component glassRole = role("glass");
      Component controllerRole = role("controller");
      Component portRole = role("port_candidate");
      Component mainCoreRole = role("main_core");
      Component peripheralRole = role("peripheral_unit");
      Component portRule = rule("tianshu_port");
      Component coolingRule = rule("tianshu_cooling");
      Component mainCoreRule = rule("tianshu_main_core");
      Component peripheralRule = rule("tianshu_peripheral");
      ArrayList<MultiblockStructureRecipe.Cell> cells = new ArrayList<>();

      for (int y = 0; y < 7; y++) {
         for (int z = 0; z < 7; z++) {
            for (int x = 0; x < 7; x++) {
               BlockPos pos = new BlockPos(x, y, z);
               TianshuMultiblockRole role = TianshuMultiblockTemplate.roleAt(pos);
               switch (role) {
                  case IGNORED:
                  default:
                     break;
                  case CASING:
                     cells.add(cell(pos, casing, casingRole, List.of(casing), true));
                     break;
                  case COOLING:
                     cells.add(cell(pos, cooling, coolingRole, coolingPositionBlocks, List.of(coolingRule), true));
                     break;
                  case GLASS:
                     cells.add(cell(pos, glass, glassRole, List.of(glass), true));
                     break;
                  case CONTROLLER:
                     cells.add(cell(pos, facing(controller, Direction.WEST), controllerRole, List.of(controller), List.of(), false));
                     break;
                  case PORT_CANDIDATE:
                     Block displayed = pos.equals(TianshuMultiblockTemplate.LOWER_PORT) ? port : cooling;
                     cells.add(cell(pos, displayed, portRole, List.of(port, cooling, patternStorage, seedStorage), List.of(portRule), false));
                     break;
                  case CORE_RESERVED:
                     if (pos.equals(new BlockPos(3, 3, 3))) {
                        cells.add(cell(pos, mainCores.get(0), mainCoreRole, mainCores, List.of(mainCoreRule), false));
                     } else {
                        Block displayed = isDefaultTianshuParallelPosition(pos) ? parallel : blank;
                        cells.add(cell(pos, displayed, peripheralRole, peripheralUnits, List.of(peripheralRule), false));
                     }
               }
            }
         }
      }

      List<MultiblockStructureRecipe.MaterialSpec> materialOrder = List.of(
         material(casing),
         material(cooling, coolingRule),
         material(glass),
         material(controller),
         material(port, portRule),
         material(mainCores.get(0), mainCoreRule),
         material(blank, peripheralRule),
         material(parallel, peripheralRule)
      );
      return MultiblockStructureRecipe.create(id("tianshu_supercomputer"), Component.m_237115_("jei.ae2lt.multiblock.tianshu"), 7, 7, 7, cells, materialOrder);
   }

   private static MultiblockStructureRecipe.Cell cell(BlockPos pos, Block block, Component role, List<Block> alternatives, boolean shell) {
      return cell(pos, block.m_49966_(), role, alternatives, List.of(), shell);
   }

   private static MultiblockStructureRecipe.Cell cell(BlockPos pos, Block block, Component role, List<Block> alternatives, List<Component> rules, boolean shell) {
      return cell(pos, block.m_49966_(), role, alternatives, rules, shell);
   }

   private static MultiblockStructureRecipe.Cell cell(
      BlockPos pos, BlockState state, Component role, List<Block> alternatives, List<Component> rules, boolean shell
   ) {
      return new MultiblockStructureRecipe.Cell(pos.m_7949_(), state, role, alternatives, rules, shell);
   }

   private static BlockState facing(Block block, Direction direction) {
      BlockState state = block.m_49966_();
      return state.m_61138_(HorizontalDirectionalBlock.f_54117_) ? (BlockState)state.m_61124_(HorizontalDirectionalBlock.f_54117_, direction) : state;
   }

   private static Component role(String path) {
      return Component.m_237115_("jei.ae2lt.multiblock.role." + path);
   }

   static boolean isDefaultMatrixThreadPosition(BlockPos pos) {
      return MATRIX_DEFAULT_THREAD.equals(pos);
   }

   static boolean isDefaultTianshuParallelPosition(BlockPos pos) {
      return TIANSHU_DEFAULT_PARALLEL.equals(pos);
   }

   private static Component rule(String path) {
      return Component.m_237115_("jei.ae2lt.multiblock.rule." + path);
   }

   private static MultiblockStructureRecipe.MaterialSpec material(Block block) {
      return MultiblockStructureRecipe.MaterialSpec.of(block);
   }

   private static MultiblockStructureRecipe.MaterialSpec material(Block block, Component note) {
      return MultiblockStructureRecipe.MaterialSpec.of(block, note);
   }

   private static ResourceLocation id(String path) {
      return new ResourceLocation("ae2lt", path);
   }

   private MultiblockStructureRecipes() {
   }
}
