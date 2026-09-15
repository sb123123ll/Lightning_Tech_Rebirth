package com.moakiee.ae2lt.lightning.strike;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;

public record StructureRequirement(BlockPos offset, Block block, boolean consume) {
   public static StructureRequirement fromJson(JsonObject json) {
      JsonArray offsetArray = GsonHelper.m_13933_(json, "offset");
      if (offsetArray.size() != 3) {
         throw new JsonSyntaxException("Structure requirement offset must contain exactly 3 integers");
      } else {
         Block block = (Block)BuiltInRegistries.f_256975_
            .m_6612_(ResourceLocation.m_135820_(GsonHelper.m_13906_(json, "block")))
            .orElseThrow(() -> new JsonSyntaxException("Unknown block id: " + GsonHelper.m_13906_(json, "block")));
         return new StructureRequirement(
            new BlockPos(
               GsonHelper.m_13897_(offsetArray.get(0), "offset[0]"),
               GsonHelper.m_13897_(offsetArray.get(1), "offset[1]"),
               GsonHelper.m_13897_(offsetArray.get(2), "offset[2]")
            ),
            block,
            GsonHelper.m_13855_(json, "consume", false)
         );
      }
   }

   public static StructureRequirement fromNetwork(FriendlyByteBuf buffer) {
      BlockPos offset = buffer.m_130135_();
      Block block = (Block)BuiltInRegistries.f_256975_
         .m_6612_(buffer.m_130281_())
         .orElseThrow(() -> new IllegalStateException("Received unknown block id in lightning strike recipe"));
      return new StructureRequirement(offset, block, buffer.readBoolean());
   }

   public void toNetwork(FriendlyByteBuf buffer) {
      buffer.m_130064_(this.offset);
      buffer.m_130085_(BuiltInRegistries.f_256975_.m_7981_(this.block));
      buffer.writeBoolean(this.consume);
   }
}
