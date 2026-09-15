package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.item.ResearchNoteItem;
import com.moakiee.ae2lt.network.ResearchNoteClientBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen.WrittenBookAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;

public final class ResearchNoteClientBootstrap {
   private static boolean installed;

   private ResearchNoteClientBootstrap() {
   }

   public static void install() {
      if (!installed) {
         ResearchNoteClientBridge.install(new ResearchNoteClientBridge.Hooks() {
            @Override
            public void open(ItemStack book) {
               if (book.m_41720_() instanceof ResearchNoteItem && ResearchNoteItem.isGenerated(book) && WrittenBookItem.m_43471_(book.m_41783_())) {
                  Minecraft.m_91087_().m_91152_(new BookViewScreen(new WrittenBookAccess(book)));
               }
            }
         });
         installed = true;
      }
   }
}
