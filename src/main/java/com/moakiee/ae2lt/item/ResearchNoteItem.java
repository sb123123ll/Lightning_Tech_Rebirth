package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.logic.research.ResearchNoteData;
import com.moakiee.ae2lt.logic.research.ResearchNoteGenerator;
import com.moakiee.ae2lt.network.OpenResearchNotePacket;
import com.moakiee.ae2lt.network.PacketSender;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class ResearchNoteItem extends AE2LTItem {
   private static final String[] ORDER_MARKERS = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9"};
   private static final String BOOK_TITLE_KEY = "ae2lt.research_note.book.title";
   private static final String BOOK_AUTHOR_KEY = "ae2lt.research_note.book.author";

   public ResearchNoteItem(Properties properties) {
      super(properties.m_41487_(16));
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      ItemStack heldStack = player.m_21120_(hand);
      ResearchNoteData data = ResearchNoteData.read(heldStack);
      if (level.m_5776_()) {
         return InteractionResultHolder.m_19092_(heldStack, true);
      } else if (data == null) {
         if (!ResearchNoteGenerator.hasValidPool()) {
            player.m_5661_(Component.m_237115_("ae2lt.research_note.error.invalid_pool").m_130940_(ChatFormatting.RED), true);
            return InteractionResultHolder.m_19100_(heldStack);
         } else {
            ResearchNoteData generated = ResearchNoteGenerator.generate((ServerLevel)level);
            ItemStack generatedStack = new ItemStack(this);
            applyGeneratedState(generatedStack, generated);
            heldStack.m_41774_(1);
            if (!player.m_36356_(generatedStack)) {
               player.m_36176_(generatedStack, false);
            }

            player.m_36246_(Stats.f_12982_.m_12902_(this));
            return InteractionResultHolder.m_19092_(player.m_21120_(hand), false);
         }
      } else {
         applyGeneratedState(heldStack, data);
         if (player instanceof ServerPlayer serverPlayer) {
            PacketSender.sendToPlayer(serverPlayer, new OpenResearchNotePacket(heldStack.m_41777_()));
         }

         player.m_36246_(Stats.f_12982_.m_12902_(this));
         return InteractionResultHolder.m_19092_(heldStack, false);
      }
   }

   @Override
   public void appendHoverText(ItemStack stack, AE2LTItem.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      ResearchNoteData data = ResearchNoteData.read(stack);
      if (data == null) {
         tooltipComponents.add(Component.m_237115_("ae2lt.research_note.tooltip.blank").m_130940_(ChatFormatting.GRAY));
         tooltipComponents.add(Component.m_237115_("ae2lt.research_note.tooltip.open_hint").m_130940_(ChatFormatting.DARK_GRAY));
      } else {
         tooltipComponents.add(
            Component.m_237110_("ae2lt.research_note.tooltip.goal", new Object[]{data.goal().getDisplayName()}).m_130940_(ChatFormatting.GOLD)
         );
         tooltipComponents.add(
            Component.m_237115_(data.consumed() ? "ae2lt.research_note.tooltip.completed" : "ae2lt.research_note.tooltip.generated")
               .m_130940_(data.consumed() ? ChatFormatting.RED : ChatFormatting.GRAY)
         );
      }
   }

   public static boolean isUsableGeneratedNote(ItemStack stack) {
      return stack.m_41720_() instanceof ResearchNoteItem && isGenerated(stack) && !ResearchNoteData.isConsumed(stack);
   }

   public static boolean isGenerated(ItemStack stack) {
      return ResearchNoteData.read(stack) != null;
   }

   @Nullable
   public static ResearchNoteData getData(ItemStack stack) {
      return ResearchNoteData.read(stack);
   }

   public static void applyGeneratedState(ItemStack stack, ResearchNoteData data) {
      data.writeTo(stack);
      CompoundTag tag = stack.m_41784_();
      tag.m_128359_("title", Component.m_237110_("ae2lt.research_note.book.title", new Object[]{data.shortCode()}).getString());
      tag.m_128359_("author", Component.m_237115_("ae2lt.research_note.book.author").getString());
      tag.m_128405_("generation", 0);
      tag.m_128379_("resolved", true);
      tag.m_128365_("pages", createBookPages(data));
   }

   private static ListTag createBookPages(ResearchNoteData data) {
      ListTag pages = new ListTag();

      for (Component page : buildPages(data)) {
         pages.add(StringTag.m_129297_(Serializer.m_130703_(page)));
      }

      return pages;
   }

   private static List<Component> buildPages(ResearchNoteData data) {
      List<Component> pages = new ArrayList<>(5);
      pages.add(buildCoverPage(data));
      pages.add(Component.m_237110_("ae2lt.research_note.page.intro", new Object[]{data.goal().getDisplayName()}));
      pages.add(buildRecipePage(data, 0, 5));
      pages.add(buildRecipePage(data, 5, 9));
      MutableComponent warningPage = Component.m_237119_().m_7220_(Component.m_237115_("ae2lt.research_note.page.warning"));
      if (data.consumed()) {
         warningPage = warningPage.m_7220_(Component.m_237113_("\n\n"))
            .m_7220_(Component.m_237115_("ae2lt.research_note.page.completed").m_130944_(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}));
      }

      pages.add(warningPage);
      return pages;
   }

   private static Component buildCoverPage(ResearchNoteData data) {
      return Component.m_237110_("ae2lt.research_note.page.cover", new Object[]{data.shortCode()})
         .m_7220_(Component.m_237113_("\n"))
         .m_7220_(Component.m_237115_("ae2lt.research_note.page.author"))
         .m_7220_(Component.m_237113_("\n"))
         .m_7220_(Component.m_237110_("ae2lt.research_note.page.goal_line", new Object[]{data.goal().getDisplayName()}));
   }

   private static MutableComponent buildRecipePage(ResearchNoteData data, int startInclusive, int endExclusive) {
      MutableComponent page = Component.m_237119_();

      for (int i = startInclusive; i < Math.min(endExclusive, data.descriptionKeys().size()); i++) {
         if (i > startInclusive) {
            page = page.m_7220_(Component.m_237113_("\n"));
         }

         page = page.m_7220_(Component.m_237113_(ORDER_MARKERS[i] + ". ")).m_7220_(Component.m_237115_(data.descriptionKeys().get(i)));
      }

      return page;
   }
}
