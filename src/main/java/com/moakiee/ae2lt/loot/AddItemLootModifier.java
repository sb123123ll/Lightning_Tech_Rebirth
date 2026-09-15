package com.moakiee.ae2lt.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

public class AddItemLootModifier extends LootModifier {
   public static final Codec<AddItemLootModifier> CODEC = RecordCodecBuilder.create(
      inst -> codecStart(inst).and(ItemStack.f_41582_.fieldOf("item").forGetter(m -> m.item)).apply(inst, AddItemLootModifier::new)
   );
   private final ItemStack item;

   protected AddItemLootModifier(LootItemCondition[] conditions, ItemStack item) {
      super(conditions);
      this.item = item;
   }

   protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
      generatedLoot.add(this.item.m_41777_());
      return generatedLoot;
   }

   public Codec<? extends IGlobalLootModifier> codec() {
      return CODEC;
   }
}
