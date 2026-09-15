package com.moakiee.ae2lt.client;

import appeng.client.gui.Icon;
import appeng.client.gui.me.common.TerminalSettingsScreen;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.VerticalButtonBar;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import com.moakiee.ae2lt.mixin.client.AEBaseScreenAccessor;
import com.moakiee.ae2lt.mixin.client.VerticalButtonBarAccessor;
import com.moakiee.ae2lt.registry.ModItems;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent.Init.Post;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt",
   value = {Dist.CLIENT}
)
public final class TianshuTerminalSettingsHook {
   private TianshuTerminalSettingsHook() {
   }

   @SubscribeEvent
   public static void addTianshuSettingsTab(Post event) {
      if (event.getScreen() instanceof TerminalSettingsScreen settings && settings.m_6262_() instanceof TianshuPatternEncodingTermMenu) {
         VerticalButtonBar toolbar = ((AEBaseScreenAccessor)settings).ae2lt$getVerticalToolbar();

         for (Button existing : ((VerticalButtonBarAccessor)toolbar).ae2lt$getButtons()) {
            if (existing instanceof TianshuTerminalSettingsHook.TianshuSettingsButton) {
               return;
            }
         }

         TianshuTerminalSettingsHook.TianshuSettingsButton button = new TianshuTerminalSettingsHook.TianshuSettingsButton(
            ignored -> ((AEBaseScreenAccessor)settings).ae2lt$switchToScreen(new TianshuTerminalSettingsScreen(settings))
         );
         toolbar.add(button);
         event.addListener(button);
         return;
      }
   }

   private static final class TianshuSettingsButton extends IconButton {
      private TianshuSettingsButton(OnPress onPress) {
         super(onPress);
         this.setDisableBackground(true);
      }

      protected Icon getIcon() {
         return Icon.TOOLBAR_BUTTON_BACKGROUND;
      }

      protected Item getItemOverlay() {
         return (Item)ModItems.TIANSHU_PATTERN_ENCODING_TERMINAL.get();
      }

      public List<Component> getTooltipMessage() {
         return List.of(Component.m_237115_("ae2lt.tianshu.settings.title"));
      }
   }
}
