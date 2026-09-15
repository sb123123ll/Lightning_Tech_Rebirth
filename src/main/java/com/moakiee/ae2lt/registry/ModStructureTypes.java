package com.moakiee.ae2lt.registry;

import com.moakiee.ae2lt.worldgen.FirmamentStarshipPiece;
import com.moakiee.ae2lt.worldgen.FirmamentStarshipStructure;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModStructureTypes {
   public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.f_256938_, "ae2lt");
   public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.f_256786_, "ae2lt");
   public static final RegistryObject<StructureType<FirmamentStarshipStructure>> FIRMAMENT_STARSHIP = STRUCTURE_TYPES.register(
      "firmament_starship", () -> new StructureType<FirmamentStarshipStructure>() {
            public Codec<FirmamentStarshipStructure> m_226884_() {
               return FirmamentStarshipStructure.CODEC.codec();
            }
         }
   );
   public static final RegistryObject<StructurePieceType> FIRMAMENT_STARSHIP_PIECE = STRUCTURE_PIECES.register(
      "firmament_starship_piece", () -> (context, tag) -> new FirmamentStarshipPiece(context.f_226956_(), tag)
   );

   private ModStructureTypes() {
   }
}
