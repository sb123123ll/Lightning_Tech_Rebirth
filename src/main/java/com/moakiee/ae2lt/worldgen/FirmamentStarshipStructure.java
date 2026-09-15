package com.moakiee.ae2lt.worldgen;

import com.moakiee.ae2lt.registry.ModStructureTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import net.minecraft.world.level.levelgen.structure.Structure.StructureSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class FirmamentStarshipStructure extends Structure {
   public static final MapCodec<FirmamentStarshipStructure> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               m_226567_(instance),
               ResourceLocation.f_135803_.fieldOf("template").forGetter(structure -> structure.template),
               Codec.intRange(16, 256).fieldOf("search_radius").forGetter(structure -> structure.searchRadius),
               Codec.intRange(4, 64).fieldOf("sample_step").forGetter(structure -> structure.sampleStep),
               Codec.intRange(1, 256).fieldOf("min_anchor_height").forGetter(structure -> structure.minAnchorHeight),
               Codec.intRange(0, 256).fieldOf("horizontal_offset").forGetter(structure -> structure.horizontalOffset),
               Codec.intRange(0, 256).fieldOf("min_y_offset").forGetter(structure -> structure.minYOffset),
               Codec.intRange(0, 256).fieldOf("max_y_offset").forGetter(structure -> structure.maxYOffset)
            )
            .apply(instance, FirmamentStarshipStructure::new)
   );
   private final ResourceLocation template;
   private final int searchRadius;
   private final int sampleStep;
   private final int minAnchorHeight;
   private final int horizontalOffset;
   private final int minYOffset;
   private final int maxYOffset;

   public FirmamentStarshipStructure(
      StructureSettings settings,
      ResourceLocation template,
      int searchRadius,
      int sampleStep,
      int minAnchorHeight,
      int horizontalOffset,
      int minYOffset,
      int maxYOffset
   ) {
      super(settings);
      this.template = template;
      this.searchRadius = searchRadius;
      this.sampleStep = sampleStep;
      this.minAnchorHeight = minAnchorHeight;
      this.horizontalOffset = horizontalOffset;
      this.minYOffset = minYOffset;
      this.maxYOffset = maxYOffset;
      if (sampleStep > searchRadius) {
         throw new IllegalArgumentException("sample_step must not be greater than search_radius");
      } else if (minYOffset > maxYOffset) {
         throw new IllegalArgumentException("min_y_offset must not be greater than max_y_offset");
      }
   }

   protected Optional<GenerationStub> m_214086_(GenerationContext context) {
      return Optional.of(this.createGenerationStub(context, this.findBestAnchorHeight(context)));
   }

   private GenerationStub createGenerationStub(GenerationContext context, int anchorHeight) {
      RandomSource random = context.f_226626_();
      int yOffset = this.minYOffset + random.m_188503_(this.maxYOffset - this.minYOffset + 1);
      ChunkPos chunkPos = context.f_226628_();
      StructureTemplate structureTemplate = context.f_226625_().m_230359_(this.template);
      FirmamentStarshipPlacement.Position desiredCenter = FirmamentStarshipPlacement.offsetFromStartChunk(
         chunkPos.m_151390_(), anchorHeight, chunkPos.m_151393_(), this.horizontalOffset, yOffset
      );
      int startY = FirmamentStarshipPlacement.clampStartY(
         desiredCenter.y(), context.f_226629_().m_141937_(), context.f_226629_().m_151558_(), structureTemplate.m_163801_().m_123342_()
      );
      FirmamentStarshipPlacement.Position center = new FirmamentStarshipPlacement.Position(desiredCenter.x(), startY, desiredCenter.z());
      FirmamentStarshipPlacement.Position origin = FirmamentStarshipPlacement.originFromCenter(
         center, structureTemplate.m_163801_().m_123341_(), structureTemplate.m_163801_().m_123343_()
      );
      BlockPos startPos = new BlockPos(origin.x(), origin.y(), origin.z());
      BlockPos rotationPivot = new BlockPos(structureTemplate.m_163801_().m_123341_() / 2, 0, structureTemplate.m_163801_().m_123343_() / 2);
      Rotation rotation = Rotation.m_221990_(random);
      return new GenerationStub(
         startPos, builder -> builder.m_142679_(new FirmamentStarshipPiece(context.f_226625_(), this.template, startPos, rotation, rotationPivot))
      );
   }

   private int findBestAnchorHeight(GenerationContext context) {
      ChunkPos chunkPos = context.f_226628_();
      int centerX = chunkPos.m_151390_();
      int centerZ = chunkPos.m_151393_();
      int bestHeight = this.minAnchorHeight;

      for (int dx = -this.searchRadius; dx <= this.searchRadius; dx += this.sampleStep) {
         for (int dz = -this.searchRadius; dz <= this.searchRadius; dz += this.sampleStep) {
            int x = centerX + dx;
            int z = centerZ + dz;
            int y = context.f_226622_().m_223235_(x, z, Types.WORLD_SURFACE_WG, context.f_226629_(), context.f_226624_());
            if (y > bestHeight) {
               bestHeight = y;
            }
         }
      }

      return bestHeight;
   }

   public StructureType<?> m_213658_() {
      return (StructureType<?>)ModStructureTypes.FIRMAMENT_STARSHIP.get();
   }
}
