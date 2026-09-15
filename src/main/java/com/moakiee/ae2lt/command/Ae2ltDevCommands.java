package com.moakiee.ae2lt.command;

import com.moakiee.ae2lt.item.ResearchNoteItem;
import com.moakiee.ae2lt.logic.research.ResearchNoteData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.loading.FMLEnvironment;

@EventBusSubscriber(
   modid = "ae2lt"
)
public final class Ae2ltDevCommands {
   private Ae2ltDevCommands() {
   }

   @SubscribeEvent
   public static void registerCommands(RegisterCommandsEvent event) {
      if (!FMLEnvironment.production) {
         register(event.getDispatcher());
      }
   }

   private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.m_82127_("ae2lt")
            .then(Commands.m_82127_("giveRitual").executes(context -> giveHeldRitualItems((CommandSourceStack)context.getSource())))
      );
   }

   private static int giveHeldRitualItems(CommandSourceStack source) throws CommandSyntaxException {
      ServerPlayer player = source.m_81375_();
      ItemStack held = player.m_21205_();
      if (!ResearchNoteItem.isUsableGeneratedNote(held)) {
         source.m_81352_(Component.m_237113_("主手需要持有一张已生成且尚未完成的研究笔记。"));
         return 0;
      } else {
         ResearchNoteData note = ResearchNoteItem.getData(held);
         if (note == null) {
            source.m_81352_(Component.m_237113_("无法读取主手研究笔记。"));
            return 0;
         } else {
            List<ItemStack> ritualItems = new ArrayList<>(note.recipeItems().size());

            for (ResourceLocation itemId : note.recipeItems()) {
               Item item = BuiltInRegistries.f_257033_.m_6612_(itemId).orElse(Items.f_41852_);
               if (item == Items.f_41852_) {
                  source.m_81352_(Component.m_237113_("研究材料不存在：" + itemId));
                  return 0;
               }

               ritualItems.add(new ItemStack(item));
            }

            for (ItemStack ritualItem : ritualItems) {
               if (!player.m_36356_(ritualItem)) {
                  player.m_36176_(ritualItem, false);
               }
            }

            source.m_288197_(() -> Component.m_237113_("已按笔记顺序给予 9 项仪式材料。"), false);
            return ritualItems.size();
         }
      }
   }
}
