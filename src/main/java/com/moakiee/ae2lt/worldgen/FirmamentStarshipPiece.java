package com.moakiee.ae2lt.worldgen;

import com.moakiee.ae2lt.blockentity.FirmamentConversionCoreBlockEntity;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.registry.ModStructureTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public final class FirmamentStarshipPiece extends TemplateStructurePiece {
   public FirmamentStarshipPiece(StructureTemplateManager structureTemplateManager, ResourceLocation template, BlockPos position, Rotation rotation) {
      this(structureTemplateManager, template, position, rotation, BlockPos.f_121853_);
   }

   public FirmamentStarshipPiece(
      StructureTemplateManager structureTemplateManager, ResourceLocation template, BlockPos position, Rotation rotation, BlockPos rotationPivot
   ) {
      super(
         (StructurePieceType)ModStructureTypes.FIRMAMENT_STARSHIP_PIECE.get(),
         0,
         structureTemplateManager,
         template,
         template.toString(),
         makeSettings(rotation, rotationPivot),
         position
      );
   }

   public FirmamentStarshipPiece(StructureTemplateManager structureTemplateManager, CompoundTag tag) {
      super(
         (StructurePieceType)ModStructureTypes.FIRMAMENT_STARSHIP_PIECE.get(),
         tag,
         structureTemplateManager,
         location -> makeSettings(readRotation(tag), readRotationPivot(tag))
      );
   }

   private static StructurePlaceSettings makeSettings(Rotation rotation, BlockPos rotationPivot) {
      return new StructurePlaceSettings().m_74392_(false).m_74383_(BlockIgnoreProcessor.f_74048_).m_74385_(rotationPivot).m_74379_(rotation);
   }

   protected void m_183620_(StructurePieceSerializationContext context, CompoundTag tag) {
      super.m_183620_(context, tag);
      tag.m_128359_("Rot", this.f_73657_.m_74404_().m_7912_());
      BlockPos rotationPivot = this.f_73657_.m_74407_();
      tag.m_128405_("RPX", rotationPivot.m_123341_());
      tag.m_128405_("RPY", rotationPivot.m_123342_());
      tag.m_128405_("RPZ", rotationPivot.m_123343_());
   }

   private static Rotation readRotation(CompoundTag tag) {
      String serializedName = tag.m_128461_("Rot");

      for (Rotation rotation : Rotation.values()) {
         if (rotation.m_7912_().equals(serializedName)) {
            return rotation;
         }
      }

      return Rotation.NONE;
   }

   private static BlockPos readRotationPivot(CompoundTag tag) {
      return tag.m_128441_("RPX") && tag.m_128441_("RPY") && tag.m_128441_("RPZ")
         ? new BlockPos(tag.m_128451_("RPX"), tag.m_128451_("RPY"), tag.m_128451_("RPZ"))
         : BlockPos.f_121853_;
   }

   public void m_213694_(
      WorldGenLevel level,
      StructureManager structureManager,
      ChunkGenerator chunkGenerator,
      RandomSource random,
      BoundingBox chunkBB,
      ChunkPos chunkPos,
      BlockPos pivot
   ) {
      super.m_213694_(level, structureManager, chunkGenerator, random, chunkBB, chunkPos, pivot);

      for (StructureBlockInfo blockInfo : this.f_73656_.m_74603_(this.f_73658_, this.f_73657_, (Block)ModBlocks.FIRMAMENT_CONVERSION_CORE.get())) {
         if (level.m_7702_(blockInfo.f_74675_()) instanceof FirmamentConversionCoreBlockEntity core) {
            core.initializeNaturalLoot(random);
         }
      }
   }

   protected void m_213704_(String markerId, BlockPos position, ServerLevelAccessor level, RandomSource random, BoundingBox chunkBB) {
   }
}
