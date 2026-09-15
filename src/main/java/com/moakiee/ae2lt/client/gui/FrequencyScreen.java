package com.moakiee.ae2lt.client.gui;

import appeng.client.gui.Icon;
import appeng.client.gui.style.Color;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.TabButton;
import appeng.client.gui.widgets.TabButton.Style;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.SwitchGuisPacket;
import com.moakiee.ae2lt.client.ClientFrequencyCache;
import com.moakiee.ae2lt.grid.FrequencyAccessLevel;
import com.moakiee.ae2lt.grid.FrequencySecurityLevel;
import com.moakiee.ae2lt.menu.FrequencyMenu;
import com.moakiee.ae2lt.network.ChangeMemberPacket;
import com.moakiee.ae2lt.network.CreateFrequencyPacket;
import com.moakiee.ae2lt.network.DeleteFrequencyPacket;
import com.moakiee.ae2lt.network.EditFrequencyPacket;
import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.network.SelectFrequencyPacket;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class FrequencyScreen extends AbstractContainerScreen<FrequencyMenu> {
   private static final int GUI_WIDTH = 195;
   private static final int GUI_HEIGHT = 157;
   private static final int TAB_WIDTH = 22;
   private static final int TAB_HEIGHT = 22;
   private static final int TAB_STEP = 22;
   private static final int ITEMS_PER_PAGE_LIST = 5;
   private static final int ITEMS_PER_PAGE_SEARCH = 4;
   private static final int LIST_ROW_HEIGHT = 21;
   private static final int LIST_ROW_BUTTON_HEIGHT = 20;
   private static final int LIST_ROW_FIRST_Y = 38;
   private static final int LIST_ROW_X = 9;
   private static final int LIST_ROW_WIDTH = 160;
   private static final int ROW_SPRITE_WIDTH = 160;
   private static final int ROW_SPRITE_HEIGHT = 20;
   private static final int ROW_SPRITE_IDLE_V = 158;
   private static final int ROW_SPRITE_HOVER_V = 180;
   private static final int SCROLLBAR_X = 175;
   private static final int SCROLLBAR_Y = 38;
   private static final int SCROLLBAR_WIDTH = 12;
   private static final int SCROLLBAR_HANDLE_HEIGHT = 15;
   private static final ResourceLocation BG_HOME = new ResourceLocation("ae2lt", "textures/gui/wireless_overloaded_home.png");
   private static final ResourceLocation BG_SELECTION = new ResourceLocation("ae2lt", "textures/gui/wireless_overloaded_selection.png");
   private static final ResourceLocation BG_LIST = new ResourceLocation("ae2lt", "textures/gui/wireless_overloaded_list.png");
   private static final ResourceLocation BG_FORM = new ResourceLocation("ae2lt", "textures/gui/wireless_overloaded_form.png");
   private static final int TEXTURE_SIZE = 256;
   private static final int AE2_TEXT_TITLE = 0;
   private static final int AE2_TEXT_BODY = 0;
   private static final int AE2_TEXT_MUTED = 4210752;
   private static final int INPUT_HEIGHT = 12;
   private static final int INPUT_MAX_WIDTH = 128;
   private static final ScreenStyle AE2_STYLE = buildAe2Style();
   private FrequencyNavigationTab currentTab = FrequencyNavigationTab.TAB_HOME;
   private int selectionScroll = 0;
   private int memberScroll = 0;
   private int connectionScroll = 0;
   private FrequencyScreen.ScrollbarWidget currentScrollbar;
   private final List<AbstractWidget> currentRowButtons = new ArrayList<>();
   private AETextField nameField;
   private AETextField passwordField;
   private FrequencySecurityLevel editSecurity = FrequencySecurityLevel.PRIVATE;
   private int editColor = 2003199;
   private int lastCacheRevision = -1;
   private int lastFreqId = Integer.MIN_VALUE;
   private boolean lastAutoConnect;
   private UUID popupMemberUUID;
   private String popupMemberName = "";
   private FrequencyAccessLevel popupMemberAccess = FrequencyAccessLevel.USER;
   private boolean popupIsStranger = false;
   private Component inlineError = null;
   private long inlineErrorExpiresAt = 0L;
   private static final long INLINE_ERROR_DURATION_MS = 4000L;
   private final List<FrequencyScreen.FittedTextTooltip> fittedTextTooltips = new ArrayList<>();
   private boolean deleteConfirmOpen = false;
   private static final int[] PRESET_COLORS = new int[]{
      7018472, 4456684, 13311, 44031, 65497, 65280, 7864064, 16776960, 16746496, 16711680, 16711786, 15466732, 8355711, 16777215
   };
   private static final String PASSWORD_SENTINEL = "••••••••";
   private boolean settingsPasswordPristine = false;
   private int passwordPromptFreqId = 0;
   private String passwordPromptFreqName = "";
   private boolean passwordPromptLocksScreen = false;
   private AETextField passwordPromptField;
   private String selectionSearchQuery = "";
   private static final ResourceLocation TAB_ICON_HOME = new ResourceLocation("ae2lt", "textures/gui/buttons/menu.png");
   private static final ResourceLocation TAB_ICON_SELECTION = new ResourceLocation("ae2lt", "textures/gui/buttons/frequency_select.png");
   private static final ResourceLocation TAB_ICON_CONNECTION = new ResourceLocation("ae2lt", "textures/gui/buttons/frequency_connect.png");
   private static final ResourceLocation TAB_ICON_MEMBER = new ResourceLocation("ae2lt", "textures/gui/buttons/frequency_member.png");
   private static final ResourceLocation TAB_ICON_CREATE = new ResourceLocation("ae2lt", "textures/gui/buttons/frequency_add.png");

   private static ScreenStyle buildAe2Style() {
      ScreenStyle style = new ScreenStyle();

      try {
         Field paletteField = ScreenStyle.class.getDeclaredField("palette");
         paletteField.setAccessible(true);
         Map<PaletteColor, Color> palette = (Map<PaletteColor, Color>)paletteField.get(style);
         palette.put(PaletteColor.DEFAULT_TEXT_COLOR, new Color(64, 64, 64, 255));
         palette.put(PaletteColor.MUTED_TEXT_COLOR, new Color(127, 127, 127, 255));
         palette.put(PaletteColor.SELECTION_COLOR, new Color(120, 170, 255, 120));
         palette.put(PaletteColor.TEXTFIELD_TEXT, new Color(255, 255, 255, 255));
         palette.put(PaletteColor.TEXTFIELD_PLACEHOLDER, new Color(96, 96, 96, 255));
         palette.put(PaletteColor.TEXTFIELD_SELECTION, new Color(120, 170, 255, 120));
         palette.put(PaletteColor.TEXTFIELD_ERROR, new Color(200, 70, 70, 255));
         palette.put(PaletteColor.ERROR, new Color(200, 70, 70, 255));
         return style;
      } catch (ReflectiveOperationException var3) {
         throw new IllegalStateException("Failed to populate AE2 ScreenStyle palette for FrequencyScreen", var3);
      }
   }

   public void showInlineError(Component message) {
      this.inlineError = message;
      this.inlineErrorExpiresAt = System.currentTimeMillis() + 4000L;
   }

   private FrequencyMenu freqMenu() {
      return (FrequencyMenu)this.m_6262_();
   }

   private int token() {
      return ((FrequencyMenu)this.m_6262_()).f_38840_;
   }

   public FrequencyScreen(FrequencyMenu menu, Inventory playerInv, Component title) {
      super(menu, playerInv, title);
      this.f_97726_ = 195;
      this.f_97727_ = 157;
   }

   protected void m_7856_() {
      super.m_7856_();
      this.lastCacheRevision = ClientFrequencyCache.revision();
      this.lastFreqId = this.freqMenu().getCurrentFrequencyId();
      this.lastAutoConnect = this.freqMenu().isAutoConnect();
      this.initTabWidgets();
   }

   public void m_181908_() {
      super.m_181908_();
      int rev = ClientFrequencyCache.revision();
      int fid = this.freqMenu().getCurrentFrequencyId();
      boolean auto = this.freqMenu().isAutoConnect();
      if (rev != this.lastCacheRevision || fid != this.lastFreqId || auto != this.lastAutoConnect) {
         this.lastCacheRevision = rev;
         this.lastFreqId = fid;
         this.lastAutoConnect = auto;
         this.initTabWidgets();
      }
   }

   private AETextField makeAe2Field(int x, int y, int width, int height) {
      AETextField field = new AETextField(AE2_STYLE, this.f_96547_, x, y, width, height);
      field.m_94182_(false);
      return field;
   }

   private void initTabWidgets() {
      this.m_169413_();
      this.currentRowButtons.clear();
      this.currentScrollbar = null;
      this.closePopup();
      int x0 = this.f_97735_;
      int y0 = this.f_97736_;
      this.buildTopTabs(x0, y0, false);
      if (this.freqMenu().hasParentMenu()) {
         Component backTooltip = Component.m_237115_(this.freqMenu().isCardMode() ? "ae2lt.gui.button.return_to_terminal" : "ae2lt.gui.button.back");
         FrequencyScreen.HoverableTabButton backButton = new FrequencyScreen.HoverableTabButton(
            Icon.ARROW_LEFT, null, backTooltip, btn -> NetworkHandler.instance().sendToServer(SwitchGuisPacket.returnToParentMenu())
         );
         backButton.setStyle(Style.BOX);
         backButton.m_252865_(x0 - 22 + 6);
         backButton.m_253211_(y0 - 22);
         backButton.m_257544_(Tooltip.m_257550_(backTooltip));
         this.m_142416_(backButton);
      }

      if (this.passwordPromptFreqId > 0) {
         ClientFrequencyCache.CachedFrequency freq = ClientFrequencyCache.getFrequency(this.passwordPromptFreqId);
         if (freq == null || !this.needsPasswordUnlock(freq)) {
            this.passwordPromptFreqId = 0;
            this.passwordPromptLocksScreen = false;
         }
      }

      if (this.passwordPromptFreqId == 0) {
         this.checkAutoPasswordPrompt();
      }

      if (this.passwordPromptFreqId > 0) {
         this.buildPasswordPromptWidgets(x0, y0);
      } else if (this.deleteConfirmOpen) {
         this.buildDeleteConfirmWidgets(x0, y0);
      } else {
         switch (this.currentTab) {
            case TAB_HOME:
               this.initHomeTab(x0, y0);
               break;
            case TAB_SELECTION:
               this.initSelectionTab(x0, y0);
               break;
            case TAB_CONNECTION:
               this.initConnectionsTab(x0, y0);
               break;
            case TAB_MEMBER:
               this.initMembersTab(x0, y0);
               break;
            case TAB_CREATE:
               this.initCreateTab(x0, y0);
               break;
            case TAB_SETTING:
               this.initSettingsTab(x0, y0);
         }
      }
   }

   private void buildDeleteConfirmWidgets(int x0, int y0) {
      this.m_142416_(new AE2Button(x0 + 22, y0 + 92, 58, 16, Component.m_237115_("ae2lt.gui.button.cancel"), btn -> {
         this.deleteConfirmOpen = false;
         this.scheduleRebuild();
      }));
      this.m_142416_(
         new AE2Button(x0 + 96, y0 + 92, 58, 16, Component.m_237115_("ae2lt.gui.button.confirm").m_6881_().m_130940_(ChatFormatting.DARK_RED), btn -> {
            int freqId = this.freqMenu().getCurrentFrequencyId();
            if (freqId > 0) {
               NetworkInit.sendToServer(new DeleteFrequencyPacket(this.token(), freqId));
            }

            this.deleteConfirmOpen = false;
            this.switchTab(FrequencyNavigationTab.TAB_SELECTION);
         })
      );
   }

   private boolean needsPasswordUnlock(ClientFrequencyCache.CachedFrequency freq) {
      if (freq.security() != FrequencySecurityLevel.ENCRYPTED) {
         return false;
      } else {
         Minecraft mc = Minecraft.m_91087_();
         if (mc.f_91074_ == null) {
            return false;
         } else {
            UUID me = mc.f_91074_.m_20148_();

            for (ClientFrequencyCache.CachedMember m : ClientFrequencyCache.getMembers(freq.id())) {
               if (m.uuid().equals(me)) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   private void checkAutoPasswordPrompt() {
      int currentId = this.freqMenu().getCurrentFrequencyId();
      if (currentId > 0) {
         ClientFrequencyCache.CachedFrequency freq = ClientFrequencyCache.getFrequency(currentId);
         if (freq != null) {
            if (this.needsPasswordUnlock(freq)) {
               this.passwordPromptFreqId = currentId;
               this.passwordPromptFreqName = freq.name();
               this.passwordPromptLocksScreen = true;
            }
         }
      }
   }

   private void buildPasswordPromptWidgets(int x0, int y0) {
      int fieldX = x0 + (this.f_97726_ - 128) / 2;
      this.passwordPromptField = this.makeAe2Field(fieldX, y0 + 68, 128, 12);
      this.passwordPromptField.m_94199_(16);
      this.passwordPromptField.setPlaceholder(Component.m_237115_("ae2lt.gui.frequency.password"));
      this.m_142416_(this.passwordPromptField);
      this.m_142416_(new AE2Button(x0 + 22, y0 + 92, 58, 16, Component.m_237115_("ae2lt.gui.button.cancel"), btn -> this.cancelPasswordPrompt()));
      this.m_142416_(new AE2Button(x0 + 96, y0 + 92, 58, 16, Component.m_237115_("ae2lt.gui.button.submit"), btn -> this.submitPasswordPrompt()));
   }

   private void submitPasswordPrompt() {
      String pw = this.passwordPromptField == null ? "" : this.passwordPromptField.m_94155_();
      int freqId = this.passwordPromptFreqId;
      if (freqId > 0) {
         NetworkInit.sendToServer(new SelectFrequencyPacket(this.token(), this.freqMenu().getBlockPos(), freqId, pw));
      }
   }

   private void cancelPasswordPrompt() {
      boolean closeAll = this.passwordPromptLocksScreen;
      this.passwordPromptFreqId = 0;
      this.passwordPromptLocksScreen = false;
      if (closeAll) {
         Minecraft.m_91087_().execute(this::m_7379_);
      } else {
         this.scheduleRebuild();
      }
   }

   private void buildTopTabs(int x0, int y0, boolean popup) {
      for (int i = 0; i < FrequencyNavigationTab.VALUES.length; i++) {
         FrequencyNavigationTab tab = FrequencyNavigationTab.VALUES[i];
         int bx = tabButtonX(x0, tab, i);
         int by = y0 - 22;
         ResourceLocation customIcon = customIconFor(tab);
         Icon baseIcon = customIcon != null ? null : iconFor(tab);
         Component tooltip = Component.m_237115_(tab.getTranslationKey());
         OnPress onPress = popup ? btn -> {
            this.closePopup();
            this.switchTab(tab);
         } : btn -> this.switchTab(tab);
         FrequencyScreen.HoverableTabButton button = new FrequencyScreen.HoverableTabButton(baseIcon, customIcon, tooltip, onPress);
         button.setStyle(Style.BOX);
         button.m_252865_(bx);
         button.m_253211_(by);
         button.m_257544_(Tooltip.m_257550_(tooltip));
         this.m_142416_(button);
      }
   }

   private static Icon iconFor(FrequencyNavigationTab tab) {
      return switch (tab) {
         case TAB_SETTING -> Icon.WRENCH;
         default -> null;
      };
   }

   private static ResourceLocation customIconFor(FrequencyNavigationTab tab) {
      return switch (tab) {
         case TAB_HOME -> TAB_ICON_HOME;
         case TAB_SELECTION -> TAB_ICON_SELECTION;
         case TAB_CONNECTION -> TAB_ICON_CONNECTION;
         case TAB_MEMBER -> TAB_ICON_MEMBER;
         case TAB_CREATE -> TAB_ICON_CREATE;
         case TAB_SETTING -> null;
      };
   }

   private void switchTab(FrequencyNavigationTab tab) {
      this.currentTab = tab;
      this.selectionScroll = 0;
      this.memberScroll = 0;
      this.connectionScroll = 0;
      this.selectionSearchQuery = "";
      this.deleteConfirmOpen = false;
      this.scheduleRebuild();
   }

   private void scheduleRebuild() {
      Minecraft.m_91087_().execute(this::initTabWidgets);
   }

   private void initHomeTab(int x0, int y0) {
      if (this.freqMenu().isCardMode()) {
         boolean auto = this.freqMenu().isAutoConnect();
         this.m_142416_(
            new AE2Button(
               x0 + 8,
               y0 + 124,
               88,
               18,
               Component.m_237115_(auto ? "ae2lt.gui.button.auto_connect_on" : "ae2lt.gui.button.auto_connect_off"),
               btn -> this.freqMenu().clientToggleAutoConnect()
            )
         );
         this.m_142416_(
            new AE2Button(
               x0 + 99,
               y0 + 124,
               88,
               18,
               Component.m_237115_("ae2lt.gui.button.disconnect"),
               btn -> NetworkInit.sendToServer(new SelectFrequencyPacket(this.token(), this.freqMenu().getBlockPos(), -1, ""))
            )
         );
      } else {
         this.m_142416_(
            new AE2Button(
               x0 + 49,
               y0 + 124,
               96,
               18,
               Component.m_237115_("ae2lt.gui.button.disconnect"),
               btn -> NetworkInit.sendToServer(new SelectFrequencyPacket(this.token(), this.freqMenu().getBlockPos(), -1, ""))
            )
         );
      }
   }

   private void initSelectionTab(int x0, int y0) {
      int searchX = x0 + (this.f_97726_ - 128) / 2;
      AETextField searchField = this.makeAe2Field(searchX, y0 + 124, 128, 12);
      searchField.m_94199_(32);
      searchField.m_94144_(this.selectionSearchQuery);
      searchField.m_94151_(value -> {
         if (!value.equals(this.selectionSearchQuery)) {
            this.selectionSearchQuery = value;
            this.selectionScroll = 0;
            this.scheduleRebuild();
         }
      });
      searchField.setPlaceholder(Component.m_237115_("ae2lt.gui.frequency.search"));
      this.m_142416_(searchField);
      int total = this.filteredFrequencies().size();
      this.selectionScroll = clampScroll(this.selectionScroll, total, 4);
      this.currentScrollbar = new FrequencyScreen.ScrollbarWidget(x0 + 175, y0 + 38, 84, total, 4, this.selectionScroll, offset -> {
         this.selectionScroll = offset;
         this.rebuildSelectionRows();
      });
      this.m_142416_(this.currentScrollbar);
      this.rebuildSelectionRows();
   }

   private List<ClientFrequencyCache.CachedFrequency> filteredFrequencies() {
      List<ClientFrequencyCache.CachedFrequency> all = ClientFrequencyCache.getAllFrequenciesSorted();
      String q = this.selectionSearchQuery.trim().toLowerCase(Locale.ROOT);
      return q.isEmpty() ? all : all.stream().filter(f -> f.name().toLowerCase(Locale.ROOT).contains(q)).toList();
   }

   private static int clampScroll(int v, int total, int visible) {
      return Math.max(0, Math.min(v, Math.max(0, total - visible)));
   }

   private void rebuildSelectionRows() {
      this.clearRowButtons();
      List<ClientFrequencyCache.CachedFrequency> freqs = this.filteredFrequencies();
      int x0 = this.f_97735_;
      int y0 = this.f_97736_;
      int start = this.selectionScroll;
      int end = Math.min(start + 4, freqs.size());

      for (int i = start; i < end; i++) {
         ClientFrequencyCache.CachedFrequency f = freqs.get(i);
         int row = i - start;
         int by = y0 + 38 + row * 21;

         String label = switch (f.security()) {
            case ENCRYPTED -> f.name() + " [" + Component.m_237115_("ae2lt.gui.security.encrypted").getString() + "]";
            case PRIVATE -> f.name() + " [" + Component.m_237115_("ae2lt.gui.security.private").getString() + "]";
            default -> f.name();
         };
         Component display = Component.m_237113_(label)
            .m_6270_(net.minecraft.network.chat.Style.f_131099_.m_131148_(TextColor.m_131266_(f.color() & 16777215)));
         FrequencyScreen.RowSpriteButton btn = new FrequencyScreen.RowSpriteButton(x0 + 9, by, 160, 20, display, b -> {
            if (this.needsPasswordUnlock(f)) {
               this.passwordPromptFreqId = f.id();
               this.passwordPromptFreqName = f.name();
               this.passwordPromptLocksScreen = false;
               this.scheduleRebuild();
            } else {
               NetworkInit.sendToServer(new SelectFrequencyPacket(this.token(), this.freqMenu().getBlockPos(), f.id(), ""));
            }
         });
         this.currentRowButtons.add(btn);
         this.m_142416_(btn);
      }
   }

   private void clearRowButtons() {
      for (AbstractWidget b : this.currentRowButtons) {
         this.m_169411_(b);
      }

      this.currentRowButtons.clear();
   }

   private void initConnectionsTab(int x0, int y0) {
      int currentId = this.freqMenu().getCurrentFrequencyId();
      if (currentId > 0) {
         int total = ClientFrequencyCache.getConnections(currentId).size();
         this.connectionScroll = clampScroll(this.connectionScroll, total, 5);
         this.currentScrollbar = new FrequencyScreen.ScrollbarWidget(
            x0 + 175, y0 + 38, 105, total, 5, this.connectionScroll, offset -> this.connectionScroll = offset
         );
         this.m_142416_(this.currentScrollbar);
      }
   }

   private void initMembersTab(int x0, int y0) {
      int currentId = this.freqMenu().getCurrentFrequencyId();
      if (currentId > 0) {
         int total = this.collectMemberRows().size();
         this.memberScroll = clampScroll(this.memberScroll, total, 5);
         this.currentScrollbar = new FrequencyScreen.ScrollbarWidget(x0 + 175, y0 + 38, 105, total, 5, this.memberScroll, offset -> {
            this.memberScroll = offset;
            this.rebuildMemberRows();
         });
         this.m_142416_(this.currentScrollbar);
         this.rebuildMemberRows();
      }
   }

   private List<FrequencyScreen.MemberRow> collectMemberRows() {
      int currentId = this.freqMenu().getCurrentFrequencyId();
      if (currentId <= 0) {
         return List.of();
      } else {
         List<ClientFrequencyCache.CachedMember> members = ClientFrequencyCache.getMembers(currentId);
         List<FrequencyScreen.MemberRow> rows = new ArrayList<>();
         Set<UUID> memberIds = new HashSet<>();

         for (ClientFrequencyCache.CachedMember m : members) {
            rows.add(new FrequencyScreen.MemberRow(true, m.uuid(), m.name(), m.access()));
            memberIds.add(m.uuid());
         }

         ClientPacketListener connection = Minecraft.m_91087_().m_91403_();
         if (connection != null) {
            List<FrequencyScreen.StrangerRow> strangers = new ArrayList<>();

            for (PlayerInfo info : connection.m_105142_()) {
               UUID id = info.m_105312_().getId();
               if (id != null && !memberIds.contains(id)) {
                  strangers.add(new FrequencyScreen.StrangerRow(id, info.m_105312_().getName()));
               }
            }

            strangers.sort(Comparator.comparing(sx -> sx.name.toLowerCase()));

            for (FrequencyScreen.StrangerRow s : strangers) {
               rows.add(new FrequencyScreen.MemberRow(false, s.uuid(), s.name(), FrequencyAccessLevel.BLOCKED));
            }
         }

         return rows;
      }
   }

   private void rebuildMemberRows() {
      this.clearRowButtons();
      List<FrequencyScreen.MemberRow> rows = this.collectMemberRows();
      int x0 = this.f_97735_;
      int y0 = this.f_97736_;
      int start = this.memberScroll;
      int end = Math.min(start + 5, rows.size());

      for (int i = start; i < end; i++) {
         FrequencyScreen.MemberRow entry = rows.get(i);
         int row = i - start;
         int by = y0 + 38 + row * 21;
         if (entry.isMember()) {
            StringBuilder text = new StringBuilder(entry.name());
            if (this.isSelf(entry.uuid())) {
               text.append(' ').append(Component.m_237115_("ae2lt.gui.member.you").getString());
            }

            text.append(" [").append(accessLabel(entry.access())).append(']');
            Component display = Component.m_237113_(text.toString()).m_130940_(entry.access().getFormatting());
            FrequencyScreen.RowSpriteButton rowBtn = new FrequencyScreen.RowSpriteButton(
               x0 + 9, by, 160, 20, display, b -> this.openMemberPopup(entry.uuid(), entry.name(), entry.access())
            );
            if (this.isSelf(entry.uuid())) {
               rowBtn.f_93623_ = false;
            }

            this.currentRowButtons.add(rowBtn);
            this.m_142416_(rowBtn);
         } else {
            String shortCode = Component.m_237115_("ae2lt.gui.member.stranger_short").getString();
            Component display = Component.m_237113_(entry.name() + " [" + shortCode + "]").m_130940_(ChatFormatting.DARK_GRAY);
            FrequencyScreen.RowSpriteButton btn = new FrequencyScreen.RowSpriteButton(
               x0 + 9, by, 160, 20, display, b -> this.openStrangerPopup(entry.uuid(), entry.name())
            );
            this.currentRowButtons.add(btn);
            this.m_142416_(btn);
         }
      }
   }

   private void openMemberPopup(UUID uuid, String name, FrequencyAccessLevel access) {
      this.popupMemberUUID = uuid;
      this.popupMemberName = name;
      this.popupMemberAccess = access;
      this.popupIsStranger = false;
      Minecraft.m_91087_().execute(this::rebuildMemberPopupWidgets);
   }

   private void openStrangerPopup(UUID uuid, String name) {
      this.popupMemberUUID = uuid;
      this.popupMemberName = name;
      this.popupMemberAccess = FrequencyAccessLevel.USER;
      this.popupIsStranger = true;
      Minecraft.m_91087_().execute(this::rebuildMemberPopupWidgets);
   }

   private void closePopup() {
      this.popupMemberUUID = null;
      this.popupIsStranger = false;
   }

   private void rebuildMemberPopupWidgets() {
      this.m_169413_();
      int x0 = this.f_97735_;
      int y0 = this.f_97736_;
      this.buildTopTabs(x0, y0, true);
      int px = x0 + 20;
      int py = y0 + 40;
      int pw = 136;
      FrequencyAccessLevel myAccess = this.selfAccess();
      FrequencyAccessLevel targetLevel = this.popupIsStranger ? FrequencyAccessLevel.BLOCKED : this.popupMemberAccess;
      boolean targetIsOwner = targetLevel == FrequencyAccessLevel.OWNER;
      boolean isSelfTarget = !this.popupIsStranger && this.isSelf(this.popupMemberUUID);
      boolean ownerActingOnSelf = targetIsOwner && isSelfTarget;
      boolean canUser = !ownerActingOnSelf
         && targetLevel != FrequencyAccessLevel.USER
         && myAccess.canActOnLevel(FrequencyAccessLevel.higher(targetLevel, FrequencyAccessLevel.USER));
      AE2Button btnUser = new AE2Button(px, py, pw, 16, Component.m_237115_("ae2lt.gui.member.set_user"), btn -> this.sendMember((byte)0));
      btnUser.f_93623_ = canUser;
      this.m_142416_(btnUser);
      boolean canAdmin = !ownerActingOnSelf
         && targetLevel != FrequencyAccessLevel.ADMIN
         && myAccess.canActOnLevel(FrequencyAccessLevel.higher(targetLevel, FrequencyAccessLevel.ADMIN));
      AE2Button btnAdmin = new AE2Button(px, py + 20, pw, 16, Component.m_237115_("ae2lt.gui.member.set_admin"), btn -> this.sendMember((byte)1));
      btnAdmin.f_93623_ = canAdmin;
      this.m_142416_(btnAdmin);
      boolean canRemove = !ownerActingOnSelf && !this.popupIsStranger && myAccess.canActOnLevel(targetLevel);
      Component removeLabel = Component.m_237115_("ae2lt.gui.member.remove");
      if (canRemove) {
         removeLabel = removeLabel.m_6881_().m_130940_(ChatFormatting.DARK_RED);
      }

      AE2Button btnRemove = new AE2Button(px, py + 40, pw, 16, removeLabel, btn -> this.sendMember((byte)2));
      btnRemove.f_93623_ = canRemove;
      this.m_142416_(btnRemove);
      boolean canSetOwner = !targetIsOwner && myAccess.canActOnLevel(FrequencyAccessLevel.OWNER);
      Component setOwnerLabel = Component.m_237115_("ae2lt.gui.member.set_owner");
      if (canSetOwner) {
         setOwnerLabel = setOwnerLabel.m_6881_().m_130940_(ChatFormatting.GOLD);
      }

      AE2Button btnSetOwner = new AE2Button(px, py + 60, pw, 16, setOwnerLabel, btn -> this.sendMember((byte)3));
      btnSetOwner.f_93623_ = canSetOwner;
      this.m_142416_(btnSetOwner);
      this.m_142416_(new AE2Button(px + 36, py + 84, 64, 16, Component.m_237115_("ae2lt.gui.button.cancel"), btn -> {
         this.closePopup();
         this.scheduleRebuild();
      }));
   }

   private void sendMember(byte type) {
      int currentId = this.freqMenu().getCurrentFrequencyId();
      if (currentId > 0 && this.popupMemberUUID != null) {
         NetworkInit.sendToServer(new ChangeMemberPacket(this.token(), currentId, this.popupMemberUUID, type));
         this.closePopup();
         this.scheduleRebuild();
      }
   }

   private void initCreateTab(int x0, int y0) {
      int fieldX = x0 + (this.f_97726_ - 128) / 2;
      this.nameField = this.makeAe2Field(fieldX, y0 + 30, 128, 12);
      this.nameField.m_94199_(24);
      this.nameField.setPlaceholder(Component.m_237115_("ae2lt.gui.frequency.name"));
      this.m_142416_(this.nameField);
      this.passwordField = this.makeAe2Field(fieldX, y0 + 68, 128, 12);
      this.passwordField.m_94199_(16);
      this.passwordField.setPlaceholder(Component.m_237115_("ae2lt.gui.frequency.password"));
      this.passwordField.m_94194_(this.editSecurity == FrequencySecurityLevel.ENCRYPTED);
      this.m_142416_(this.passwordField);
      this.m_142416_(new AE2Button(x0 + 80, y0 + 50, 96, 14, getSecurityLabel(this.editSecurity), btn -> {
         this.editSecurity = FrequencySecurityLevel.VALUES[(this.editSecurity.ordinal() + 1) % FrequencySecurityLevel.VALUES.length];
         this.passwordField.m_94194_(this.editSecurity == FrequencySecurityLevel.ENCRYPTED);
         btn.m_93666_(getSecurityLabel(this.editSecurity));
      }));

      for (int i = 0; i < PRESET_COLORS.length; i++) {
         int c = PRESET_COLORS[i];
         int cx = x0 + 42 + i % 7 * 16;
         int cy = y0 + 90 + i / 7 * 16;
         this.m_142416_(Button.m_253074_(Component.m_237113_(" "), btn -> this.editColor = c).m_252987_(cx, cy, 14, 14).m_253136_());
      }

      this.m_142416_(
         new AE2Button(
            x0 + 67,
            y0 + 138,
            60,
            16,
            Component.m_237115_("ae2lt.gui.button.create"),
            btn -> {
               if (!this.nameField.m_94155_().isBlank()) {
                  NetworkInit.sendToServer(
                     new CreateFrequencyPacket(this.token(), this.nameField.m_94155_(), this.editColor, this.editSecurity, this.passwordField.m_94155_())
                  );
                  this.switchTab(FrequencyNavigationTab.TAB_SELECTION);
               }
            }
         )
      );
   }

   private void initSettingsTab(int x0, int y0) {
      ClientFrequencyCache.CachedFrequency freq = ClientFrequencyCache.getFrequency(this.freqMenu().getCurrentFrequencyId());
      if (freq != null) {
         boolean isManager = this.hasManagerAccess();
         boolean isOwner = this.hasOwnerAccess();
         this.editSecurity = freq.security();
         this.editColor = freq.color();
         int fieldX = x0 + (this.f_97726_ - 128) / 2;
         this.nameField = this.makeAe2Field(fieldX, y0 + 30, 128, 12);
         this.nameField.m_94199_(24);
         this.nameField.setPlaceholder(Component.m_237115_("ae2lt.gui.frequency.name"));
         this.nameField.m_94144_(freq.name());
         this.nameField.m_94186_(isManager);
         this.m_142416_(this.nameField);
         this.passwordField = new FrequencyScreen.PristinePasswordField(fieldX, y0 + 68, 128, 12);
         this.passwordField.m_94199_(16);
         this.passwordField.setPlaceholder(Component.m_237115_("ae2lt.gui.frequency.password"));
         this.passwordField.m_94194_(this.editSecurity == FrequencySecurityLevel.ENCRYPTED);
         if (this.editSecurity == FrequencySecurityLevel.ENCRYPTED) {
            this.passwordField.m_94144_("••••••••");
            this.settingsPasswordPristine = true;
         } else {
            this.settingsPasswordPristine = false;
         }

         this.passwordField.m_94186_(isOwner);
         this.m_142416_(this.passwordField);
         AE2Button securityBtn = new AE2Button(x0 + 80, y0 + 50, 96, 14, getSecurityLabel(this.editSecurity), btn -> {
            this.editSecurity = FrequencySecurityLevel.VALUES[(this.editSecurity.ordinal() + 1) % FrequencySecurityLevel.VALUES.length];
            this.passwordField.m_94194_(this.editSecurity == FrequencySecurityLevel.ENCRYPTED);
            this.passwordField.m_94144_("");
            this.settingsPasswordPristine = false;
            btn.m_93666_(getSecurityLabel(this.editSecurity));
         });
         securityBtn.f_93623_ = isOwner;
         this.m_142416_(securityBtn);
         AE2Button applyBtn = new AE2Button(x0 + 103, y0 + 138, 60, 16, Component.m_237115_("ae2lt.gui.button.apply"), btn -> {
            String pw = this.settingsPasswordPristine ? "" : this.passwordField.m_94155_();
            NetworkInit.sendToServer(new EditFrequencyPacket(this.token(), freq.id(), this.nameField.m_94155_(), this.editColor, this.editSecurity, pw));
         });
         applyBtn.f_93623_ = isManager;
         this.m_142416_(applyBtn);
         if (isOwner) {
            this.m_142416_(
               new AE2Button(x0 + 31, y0 + 138, 60, 16, Component.m_237115_("ae2lt.gui.button.delete").m_6881_().m_130940_(ChatFormatting.DARK_RED), btn -> {
                  this.deleteConfirmOpen = true;
                  this.scheduleRebuild();
               })
            );
         } else {
            this.m_142416_(
               new AE2Button(x0 + 31, y0 + 138, 60, 16, Component.m_237115_("ae2lt.gui.button.leave").m_6881_().m_130940_(ChatFormatting.DARK_GRAY), btn -> {
                  Minecraft mc = Minecraft.m_91087_();
                  if (mc.f_91074_ != null) {
                     NetworkInit.sendToServer(new SelectFrequencyPacket(this.token(), this.freqMenu().getBlockPos(), -1, ""));
                     NetworkInit.sendToServer(new ChangeMemberPacket(this.token(), freq.id(), mc.f_91074_.m_20148_(), (byte)2));
                     this.switchTab(FrequencyNavigationTab.TAB_SELECTION);
                  }
               })
            );
         }
      }
   }

   protected void m_7286_(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
      g.m_280163_(backgroundTextureForTab(this.currentTab), this.f_97735_, this.f_97736_, 0.0F, 0.0F, 195, 157, 256, 256);
      if (this.popupMemberUUID != null) {
         this.drawMemberPopupPanel(g);
      }

      if (this.deleteConfirmOpen) {
         this.drawDeleteConfirmPanel(g);
      }

      if (this.passwordPromptFreqId > 0) {
         this.drawPasswordPromptPanel(g);
      }
   }

   private static ResourceLocation backgroundTextureForTab(FrequencyNavigationTab tab) {
      return switch (tab) {
         case TAB_HOME -> BG_HOME;
         case TAB_SELECTION -> BG_SELECTION;
         case TAB_CONNECTION, TAB_MEMBER -> BG_LIST;
         case TAB_CREATE, TAB_SETTING -> BG_FORM;
      };
   }

   private void drawPasswordPromptPanel(GuiGraphics g) {
      int px0 = this.f_97735_ + 16;
      int py0 = this.f_97736_ + 50;
      int pw = 144;
      int ph = 66;
      g.m_280509_(px0, py0, px0 + pw, py0 + ph, -3420972);
      g.m_280509_(px0, py0, px0 + pw, py0 + 1, -10855829);
      g.m_280509_(px0, py0, px0 + 1, py0 + ph, -10855829);
      g.m_280509_(px0 + pw - 1, py0, px0 + pw, py0 + ph, -1);
      g.m_280509_(px0, py0 + ph - 1, px0 + pw, py0 + ph, -1);
   }

   private void drawDeleteConfirmPanel(GuiGraphics g) {
      int px0 = this.f_97735_ + 16;
      int py0 = this.f_97736_ + 50;
      int pw = 144;
      int ph = 66;
      g.m_280509_(px0, py0, px0 + pw, py0 + ph, -3420972);
      g.m_280509_(px0, py0, px0 + pw, py0 + 1, -10855829);
      g.m_280509_(px0, py0, px0 + 1, py0 + ph, -10855829);
      g.m_280509_(px0 + pw - 1, py0, px0 + pw, py0 + ph, -1);
      g.m_280509_(px0, py0 + ph - 1, px0 + pw, py0 + ph, -1);
   }

   private void drawMemberPopupPanel(GuiGraphics g) {
      int px0 = this.f_97735_ + 10;
      int py0 = this.f_97736_ + 26;
      int pw = 156;
      int ph = 120;
      g.m_280509_(px0, py0, px0 + pw, py0 + ph, -3420972);
      g.m_280509_(px0, py0, px0 + pw, py0 + 1, -10855829);
      g.m_280509_(px0, py0, px0 + 1, py0 + ph, -10855829);
      g.m_280509_(px0 + pw - 1, py0, px0 + pw, py0 + ph, -1);
      g.m_280509_(px0, py0 + ph - 1, px0 + pw, py0 + ph, -1);
   }

   private static int tabButtonX(int originX, FrequencyNavigationTab tab, int index) {
      return tab == FrequencyNavigationTab.TAB_CREATE ? originX + 195 - 22 - 2 : originX + 8 + index * 22;
   }

   protected void m_280003_(GuiGraphics g, int mouseX, int mouseY) {
      this.fittedTextTooltips.clear();
      this.drawFlatCentered(g, Component.m_237115_(this.currentTab.getTranslationKey()), this.f_97726_ / 2, 6, 0);
      if (this.popupMemberUUID == null && !this.deleteConfirmOpen && this.passwordPromptFreqId == 0) {
         switch (this.currentTab) {
            case TAB_HOME:
               this.renderHomeLabels(g);
               break;
            case TAB_SELECTION:
               this.renderSelectionLabels(g);
               break;
            case TAB_CONNECTION:
               this.renderConnectionLabels(g);
               break;
            case TAB_MEMBER:
               this.renderMemberLabels(g);
               break;
            case TAB_CREATE:
               this.renderCreateLabels(g);
               break;
            case TAB_SETTING:
               this.renderSettingLabels(g);
         }
      }

      if (this.deleteConfirmOpen) {
         this.drawFlatCentered(g, Component.m_237115_("ae2lt.gui.delete_confirm.title").m_6881_().m_130940_(ChatFormatting.DARK_RED), this.f_97726_ / 2, 60, 0);
         this.drawFlatCentered(g, Component.m_237115_("ae2lt.gui.delete_confirm.hint"), this.f_97726_ / 2, 74, 4210752);
      }

      if (this.passwordPromptFreqId > 0) {
         this.drawFlatCenteredFitted(
            g, Component.m_237110_("ae2lt.gui.password_prompt.title", new Object[]{this.passwordPromptFreqName}), this.f_97726_ / 2, 58, 136, 0
         );
      }

      if (this.popupMemberUUID != null) {
         Component header;
         if (this.popupIsStranger) {
            String code = Component.m_237115_("ae2lt.gui.member.stranger_short").getString();
            header = Component.m_237113_(this.popupMemberName + " [" + code + "]").m_130940_(ChatFormatting.DARK_GRAY);
         } else {
            header = Component.m_237113_(this.popupMemberName)
               .m_130940_(this.popupMemberAccess.getFormatting())
               .m_6881_()
               .m_7220_(Component.m_237113_(" [" + accessLabel(this.popupMemberAccess) + "]").m_130940_(ChatFormatting.DARK_GRAY));
         }

         this.drawFlatCenteredFitted(g, header, this.f_97726_ / 2, 30, 148, 0);
      }

      if (this.inlineError != null) {
         if (System.currentTimeMillis() < this.inlineErrorExpiresAt) {
            int maxTextW = this.f_97726_ - 16;
            String full = this.inlineError.getString();
            String fitted = FittingText.fit(full, maxTextW, this.f_96547_::m_92895_);
            Component display = (Component)(full.equals(fitted) ? this.inlineError : Component.m_237113_(fitted).m_6270_(this.inlineError.m_7383_()));
            int textW = this.f_96547_.m_92852_(display);
            int bgW = Math.min(this.f_97726_ - 8, textW + 8);
            int bgX = (this.f_97726_ - bgW) / 2;
            int bgY = this.f_97727_ - 14;
            g.m_280509_(bgX, bgY, bgX + bgW, bgY + 12, -803200984);
            g.m_280509_(bgX, bgY, bgX + bgW, bgY + 1, -3127224);
            this.drawFlatCentered(g, display, this.f_97726_ / 2, bgY + 2, -19276);
            if (!full.equals(fitted)) {
               this.addFittedTooltip(bgX, bgY, bgW, 12, this.inlineError);
            }
         } else {
            this.inlineError = null;
         }
      }
   }

   private void drawFlat(GuiGraphics g, Component text, int x, int y, int color) {
      g.m_280614_(this.f_96547_, text, x, y, color, false);
   }

   private void drawFlat(GuiGraphics g, String text, int x, int y, int color) {
      g.m_280056_(this.f_96547_, text, x, y, color, false);
   }

   private void drawFlatCentered(GuiGraphics g, Component text, int centerX, int y, int color) {
      int w = this.f_96547_.m_92852_(text);
      g.m_280614_(this.f_96547_, text, centerX - w / 2, y, color, false);
   }

   private void drawFlatFitted(GuiGraphics g, Component text, int x, int y, int maxWidth, int color) {
      String full = text.getString();
      String fitted = FittingText.fit(full, maxWidth, this.f_96547_::m_92895_);
      Component display = (Component)(full.equals(fitted) ? text : Component.m_237113_(fitted).m_6270_(text.m_7383_()));
      g.m_280614_(this.f_96547_, display, x, y, color, false);
      if (!full.equals(fitted)) {
         this.addFittedTooltip(x, y, Math.min(maxWidth, this.f_96547_.m_92852_(display)), 9, text);
      }
   }

   private void drawFlatFitted(GuiGraphics g, String text, int x, int y, int maxWidth, int color) {
      String fitted = FittingText.fit(text, maxWidth, this.f_96547_::m_92895_);
      g.m_280056_(this.f_96547_, fitted, x, y, color, false);
      if (!text.equals(fitted)) {
         this.addFittedTooltip(x, y, Math.min(maxWidth, this.f_96547_.m_92895_(fitted)), 9, Component.m_237113_(text));
      }
   }

   private void drawFlatCenteredFitted(GuiGraphics g, Component text, int centerX, int y, int maxWidth, int color) {
      String full = text.getString();
      String fitted = FittingText.fit(full, maxWidth, this.f_96547_::m_92895_);
      Component display = (Component)(full.equals(fitted) ? text : Component.m_237113_(fitted).m_6270_(text.m_7383_()));
      int w = this.f_96547_.m_92852_(display);
      int x = centerX - w / 2;
      g.m_280614_(this.f_96547_, display, x, y, color, false);
      if (!full.equals(fitted)) {
         this.addFittedTooltip(x, y, Math.min(maxWidth, w), 9, text);
      }
   }

   private void addFittedTooltip(int x, int y, int width, int height, Component text) {
      this.fittedTextTooltips.add(new FrequencyScreen.FittedTextTooltip(new Rect2i(this.f_97735_ + x, this.f_97736_ + y, width, height), text));
   }

   private void renderHomeLabels(GuiGraphics g) {
      int y = 43;
      ClientFrequencyCache.CachedFrequency freq = ClientFrequencyCache.getFrequency(this.freqMenu().getCurrentFrequencyId());
      if (freq != null) {
         Component name = Component.m_237113_(freq.name())
            .m_6270_(net.minecraft.network.chat.Style.f_131099_.m_131148_(TextColor.m_131266_(freq.color() & 16777215)));
         Component line = Component.m_237115_("ae2lt.gui.frequency.current").m_130946_(": ").m_7220_(name);
         this.drawFlatFitted(g, line, 10, y, this.f_97726_ - 20, 0);
      } else {
         this.drawFlatFitted(g, Component.m_237115_("ae2lt.gui.frequency.none"), 10, y, this.f_97726_ - 20, 4210752);
      }

      y += 14;
      this.drawFlatFitted(
         g,
         Component.m_237115_("ae2lt.gui.home.device_type").m_130946_(": ").m_7220_(Component.m_237115_(this.freqMenu().getDeviceName())),
         10,
         y,
         this.f_97726_ - 20,
         0
      );
      y += 14;
      boolean connected = this.freqMenu().isLinkActive();
      this.drawFlatFitted(
         g,
         Component.m_237115_("ae2lt.gui.home.status")
            .m_130946_(": ")
            .m_7220_(connected ? Component.m_237115_("ae2lt.gui.home.connected") : Component.m_237115_("ae2lt.gui.home.disconnected")),
         10,
         y,
         this.f_97726_ - 20,
         0
      );
      y += 14;
      int used = this.freqMenu().getUsedChannels();
      int max = this.freqMenu().getMaxChannels();
      Component channelsValue;
      if (max < 0) {
         channelsValue = Component.m_237110_("ae2lt.gui.home.grid_channels.infinite", new Object[]{used});
      } else if (max == 0) {
         channelsValue = Component.m_237110_("ae2lt.gui.home.grid_channels.value", new Object[]{used, 0, 0});
      } else {
         int remain = Math.max(0, max - used);
         channelsValue = Component.m_237110_("ae2lt.gui.home.grid_channels.value", new Object[]{used, max, remain});
      }

      this.drawFlatFitted(g, Component.m_237115_("ae2lt.gui.home.grid_channels").m_130946_(": ").m_7220_(channelsValue), 10, y, this.f_97726_ - 20, 0);
      y += 14;
      if (this.freqMenu().isController()) {
         this.drawFlatFitted(
            g,
            Component.m_237115_("ae2lt.gui.home.cross_dimension")
               .m_130946_(": ")
               .m_7220_(
                  this.freqMenu().isAdvanced()
                     ? Component.m_237115_("ae2lt.gui.home.cross_dimension.yes")
                     : Component.m_237115_("ae2lt.gui.home.cross_dimension.no")
               ),
            10,
            y,
            this.f_97726_ - 20,
            0
         );
      }
   }

   private void renderSelectionLabels(GuiGraphics g) {
      List<ClientFrequencyCache.CachedFrequency> freqs = ClientFrequencyCache.getAllFrequenciesSorted();
      ClientFrequencyCache.CachedFrequency current = ClientFrequencyCache.getFrequency(this.freqMenu().getCurrentFrequencyId());
      Component left = Component.m_237115_("ae2lt.gui.frequency.current")
         .m_130946_(": ")
         .m_7220_(
            current != null
               ? Component.m_237113_(current.name())
                  .m_6270_(net.minecraft.network.chat.Style.f_131099_.m_131148_(TextColor.m_131266_(current.color() & 16777215)))
               : Component.m_237115_("ae2lt.gui.frequency.none").m_130940_(ChatFormatting.DARK_GRAY)
         );
      Component right = Component.m_237115_("ae2lt.gui.frequency.total").m_130946_(": " + freqs.size());
      int rightX = this.f_97726_ - 10 - this.f_96547_.m_92852_(right);
      this.drawFlatFitted(g, left, 10, 18, Math.max(20, rightX - 24), 0);
      this.drawFlat(g, right, rightX, 18, 4210752);
   }

   private void renderConnectionLabels(GuiGraphics g) {
      int currentId = this.freqMenu().getCurrentFrequencyId();
      if (currentId <= 0) {
         this.drawFlatCentered(g, Component.m_237115_("ae2lt.gui.error.no_frequency"), this.f_97726_ / 2, 40, 4210752);
      } else {
         List<ClientFrequencyCache.CachedConnection> conns = ClientFrequencyCache.getConnections(currentId);
         Component right = Component.m_237115_("ae2lt.gui.frequency.total").m_130946_(": " + conns.size());
         this.drawFlat(g, right, this.f_97726_ - 10 - this.f_96547_.m_92852_(right), 18, 4210752);
         if (conns.isEmpty()) {
            this.drawFlatCentered(g, Component.m_237115_("ae2lt.gui.connection.none"), this.f_97726_ / 2, 70, 4210752);
         } else {
            int start = this.connectionScroll;
            int end = Math.min(start + 5, conns.size());

            for (int i = start; i < end; i++) {
               ClientFrequencyCache.CachedConnection c = conns.get(i);
               int row = i - start;
               int yTop = 39 + row * 21;
               int yBot = yTop + 10;
               String typeKey = c.deviceName();
               ChatFormatting typeColor = c.controller() ? ChatFormatting.DARK_AQUA : ChatFormatting.DARK_GREEN;
               this.drawFlatFitted(g, Component.m_237115_(typeKey).m_130940_(typeColor), 9, yTop, 145, 0);
               String posStr = "(" + c.pos().m_123341_() + "," + c.pos().m_123342_() + "," + c.pos().m_123343_() + ")";
               String dim = c.dimension();
               int slash = dim.indexOf(58);
               String shortDim = slash >= 0 ? dim.substring(slash + 1) : dim;
               if (shortDim.length() > 8) {
                  shortDim = shortDim.substring(0, 8);
               }

               int dimX = 168 - this.f_96547_.m_92895_(shortDim);
               this.drawFlatFitted(g, posStr, 9, yBot, Math.max(20, dimX - 9 - 6), 4210752);
               int dotColor = c.loaded() ? -12933798 : -8421505;
               int dotY = yTop + 5;
               g.m_280509_(160, dotY, 166, dotY + 6, dotColor);
               this.drawFlat(g, shortDim, dimX, yBot, 4210752);
            }
         }
      }
   }

   private void renderMemberLabels(GuiGraphics g) {
      int currentId = this.freqMenu().getCurrentFrequencyId();
      if (currentId <= 0) {
         this.drawFlatCentered(g, Component.m_237115_("ae2lt.gui.error.no_frequency"), this.f_97726_ / 2, 40, 4210752);
      } else {
         FrequencyAccessLevel myAccess = this.selfAccess();
         Component left = Component.m_237110_(
            "ae2lt.gui.member.your_access",
            new Object[]{Component.m_237115_("ae2lt.gui.access." + myAccess.name().toLowerCase()).m_130940_(myAccess.getFormatting())}
         );
         List<ClientFrequencyCache.CachedMember> members = ClientFrequencyCache.getMembers(currentId);
         Component right = Component.m_237115_("ae2lt.gui.frequency.total").m_130946_(": " + members.size());
         int rightX = this.f_97726_ - 10 - this.f_96547_.m_92852_(right);
         this.drawFlatFitted(g, left, 10, 18, Math.max(20, rightX - 24), 0);
         this.drawFlat(g, right, rightX, 18, 4210752);
         if (members.isEmpty()) {
            this.drawFlatCentered(g, Component.m_237115_("ae2lt.gui.member.none"), this.f_97726_ / 2, 70, 4210752);
         }
      }
   }

   private void renderCreateLabels(GuiGraphics g) {
      this.drawFlat(g, Component.m_237115_("ae2lt.gui.frequency.name").m_130946_(":"), 16, 22, 4210752);
      this.drawFlat(g, Component.m_237115_("ae2lt.gui.frequency.security").m_130946_(":"), 16, 52, 4210752);
      this.drawFlat(g, Component.m_237115_("ae2lt.gui.frequency.color").m_130946_(":"), 16, 82, 4210752);

      for (int i = 0; i < PRESET_COLORS.length; i++) {
         int cx = 42 + i % 7 * 16;
         int cy = 90 + i / 7 * 16;
         int fill = PRESET_COLORS[i] | 0xFF000000;
         g.m_280509_(cx + 1, cy + 1, cx + 13, cy + 13, fill);
         if (PRESET_COLORS[i] == this.editColor) {
            int accent = -1;
            g.m_280509_(cx, cy, cx + 14, cy + 1, accent);
            g.m_280509_(cx, cy + 13, cx + 14, cy + 14, accent);
            g.m_280509_(cx, cy, cx + 1, cy + 14, accent);
            g.m_280509_(cx + 13, cy, cx + 14, cy + 14, accent);
         }
      }

      g.m_280509_(16, 124, 28, 134, this.editColor | 0xFF000000);
      if (this.nameField != null && !this.nameField.m_94155_().isBlank()) {
         this.drawFlatFitted(g, this.nameField.m_94155_(), 32, 125, this.f_97726_ - 42, this.editColor | 0xFF000000);
      }
   }

   private void renderSettingLabels(GuiGraphics g) {
      if (ClientFrequencyCache.getFrequency(this.freqMenu().getCurrentFrequencyId()) == null) {
         this.drawFlatCentered(g, Component.m_237115_("ae2lt.gui.error.no_frequency"), this.f_97726_ / 2, 40, 4210752);
      } else {
         this.drawFlat(g, Component.m_237115_("ae2lt.gui.frequency.name").m_130946_(":"), 16, 22, 4210752);
         this.drawFlat(g, Component.m_237115_("ae2lt.gui.frequency.security").m_130946_(":"), 16, 52, 4210752);
      }
   }

   private static Component getSecurityLabel(FrequencySecurityLevel level) {
      return switch (level) {
         case ENCRYPTED -> Component.m_237115_("ae2lt.gui.security.encrypted");
         case PRIVATE -> Component.m_237115_("ae2lt.gui.security.private");
         case PUBLIC -> Component.m_237115_("ae2lt.gui.security.public");
      };
   }

   public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(g);
      super.m_88315_(g, mouseX, mouseY, partialTick);
      this.m_280072_(g, mouseX, mouseY);
      this.renderFittedTextTooltip(g, mouseX, mouseY);
   }

   private void renderFittedTextTooltip(GuiGraphics g, int mouseX, int mouseY) {
      for (int i = this.fittedTextTooltips.size() - 1; i >= 0; i--) {
         FrequencyScreen.FittedTextTooltip tooltip = this.fittedTextTooltips.get(i);
         Rect2i area = tooltip.area();
         if (isWithinArea(mouseX, mouseY, area)) {
            g.m_280666_(this.f_96547_, List.of(tooltip.text()), mouseX, mouseY);
            return;
         }
      }
   }

   private static boolean isWithinArea(int mouseX, int mouseY, Rect2i area) {
      return mouseX >= area.m_110085_()
         && mouseX < area.m_110085_() + area.m_110090_()
         && mouseY >= area.m_110086_()
         && mouseY < area.m_110086_() + area.m_110091_();
   }

   private boolean isSelf(UUID uuid) {
      Minecraft mc = Minecraft.m_91087_();
      return mc.f_91074_ != null && mc.f_91074_.m_20148_().equals(uuid);
   }

   private FrequencyAccessLevel selfAccess() {
      List<ClientFrequencyCache.CachedMember> members = ClientFrequencyCache.getMembers(this.freqMenu().getCurrentFrequencyId());
      Minecraft mc = Minecraft.m_91087_();
      if (mc.f_91074_ == null) {
         return FrequencyAccessLevel.BLOCKED;
      } else {
         UUID me = mc.f_91074_.m_20148_();

         for (ClientFrequencyCache.CachedMember m : members) {
            if (m.uuid().equals(me)) {
               return m.access();
            }
         }

         ClientFrequencyCache.CachedFrequency freq = ClientFrequencyCache.getFrequency(this.freqMenu().getCurrentFrequencyId());
         return freq != null && freq.security() == FrequencySecurityLevel.PUBLIC ? FrequencyAccessLevel.USER : FrequencyAccessLevel.BLOCKED;
      }
   }

   private boolean hasManagerAccess() {
      return this.selfAccess().isManager();
   }

   private boolean hasOwnerAccess() {
      return this.selfAccess().isOwner();
   }

   private static String accessLabel(FrequencyAccessLevel a) {
      return Component.m_237115_("ae2lt.gui.access." + a.name().toLowerCase()).getString();
   }

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      return this.currentScrollbar != null && this.currentScrollbar.handleScroll(delta) ? true : super.m_6050_(mouseX, mouseY, delta);
   }

   private static record FittedTextTooltip(Rect2i area, Component text) {
   }

   private static final class HoverableTabButton extends TabButton {
      private final ResourceLocation customIcon;

      HoverableTabButton(Icon icon, ResourceLocation customIcon, Component tooltip, OnPress onPress) {
         super(icon, tooltip, onPress);
         this.customIcon = customIcon;
      }

      public boolean m_93696_() {
         return super.m_93696_() || this.m_274382_();
      }

      public void m_87963_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
         super.m_87963_(guiGraphics, mouseX, mouseY, partialTick);
         if (this.customIcon != null) {
            guiGraphics.m_280163_(this.customIcon, this.m_252754_() + 2, this.m_252907_() + 1, 0.0F, 0.0F, 16, 16, 16, 16);
         }
      }
   }

   private static record MemberRow(boolean isMember, UUID uuid, String name, FrequencyAccessLevel access) {
   }

   private final class PristinePasswordField extends AETextField {
      PristinePasswordField(int x, int y, int width, int height) {
         super(FrequencyScreen.AE2_STYLE, FrequencyScreen.this.f_96547_, x, y, width, height);
         this.m_94182_(false);
      }

      public boolean m_7933_(int key, int scan, int mods) {
         if (FrequencyScreen.this.settingsPasswordPristine) {
            FrequencyScreen.this.settingsPasswordPristine = false;
            this.m_94144_("");
         }

         return super.m_7933_(key, scan, mods);
      }

      public boolean m_5534_(char c, int mods) {
         if (FrequencyScreen.this.settingsPasswordPristine) {
            FrequencyScreen.this.settingsPasswordPristine = false;
            this.m_94144_("");
         }

         return super.m_5534_(c, mods);
      }
   }

   private final class RowSpriteButton extends Button {
      RowSpriteButton(int x, int y, int width, int height, Component message, OnPress onPress) {
         super(Button.m_253074_(message, onPress).m_252987_(x, y, width, height));
         if (!message.getString().equals(FittingText.fit(message.getString(), width - 8, FrequencyScreen.this.f_96547_::m_92895_))) {
            this.m_257544_(Tooltip.m_257550_(message));
         }
      }

      protected void m_87963_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         boolean hover = this.f_93623_ && this.m_198029_();
         int srcV = hover ? 180 : 158;
         g.m_280163_(FrequencyScreen.BG_LIST, this.m_252754_(), this.m_252907_(), 0.0F, (float)srcV, 160, 20, 256, 256);
         int fallback = this.f_93623_ ? 0 : 4210752;
         String full = this.m_6035_().getString();
         String fitted = FittingText.fit(full, this.m_5711_() - 8, FrequencyScreen.this.f_96547_::m_92895_);
         Component display = (Component)(full.equals(fitted) ? this.m_6035_() : Component.m_237113_(fitted).m_6270_(this.m_6035_().m_7383_()));
         int textY = this.m_252907_() + (this.m_93694_() - 8) / 2;
         int textX = this.m_252754_() + 4 + (this.m_5711_() - 8 - FrequencyScreen.this.f_96547_.m_92852_(display)) / 2;
         g.m_280614_(FrequencyScreen.this.f_96547_, display, textX, textY, fallback, false);
      }
   }

   private final class ScrollbarWidget extends AbstractWidget {
      private final int totalItems;
      private final int visibleItems;
      private final IntConsumer onScroll;
      private int scrollOffset;
      private boolean dragging;
      private int dragYOffset;

      ScrollbarWidget(int x, int y, int trackHeight, int totalItems, int visibleItems, int initialScroll, IntConsumer onScroll) {
         super(x, y, 12, trackHeight, Component.m_237119_());
         this.totalItems = totalItems;
         this.visibleItems = visibleItems;
         this.onScroll = onScroll;
         this.scrollOffset = this.clamp(initialScroll);
      }

      int maxOffset() {
         return Math.max(0, this.totalItems - this.visibleItems);
      }

      private int clamp(int v) {
         return Math.max(0, Math.min(this.maxOffset(), v));
      }

      private void setOffset(int v) {
         int c = this.clamp(v);
         if (c != this.scrollOffset) {
            this.scrollOffset = c;
            this.onScroll.accept(c);
         }
      }

      boolean handleScroll(double scrollY) {
         if (this.maxOffset() == 0) {
            return false;
         } else {
            int delta = -((int)Math.signum(scrollY));
            if (delta == 0) {
               return false;
            } else {
               int before = this.scrollOffset;
               this.setOffset(this.scrollOffset + delta);
               return this.scrollOffset != before;
            }
         }
      }

      protected void m_87963_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         boolean enabled = this.maxOffset() > 0;
         ResourceLocation sprite = new ResourceLocation("minecraft", "textures/gui/container/creative_inventory/tabs.png");
         int srcX = enabled ? 232 : 244;
         int availH = Math.max(0, this.m_93694_() - 15);
         int handleY = enabled ? this.m_252907_() + this.scrollOffset * availH / this.maxOffset() : this.m_252907_();
         g.m_280163_(sprite, this.m_252754_(), handleY, (float)srcX, 0.0F, 12, 15, 256, 256);
      }

      public boolean m_6375_(double mouseX, double mouseY, int button) {
         if (button != 0 || !this.f_93624_ || !this.f_93623_) {
            return false;
         } else if (!this.m_5953_(mouseX, mouseY)) {
            return false;
         } else if (this.maxOffset() == 0) {
            return true;
         } else {
            int availH = Math.max(0, this.m_93694_() - 15);
            int handleY = this.m_252907_() + this.scrollOffset * availH / this.maxOffset();
            if (mouseY < (double)handleY) {
               this.setOffset(this.scrollOffset - this.visibleItems);
            } else if (mouseY < (double)(handleY + 15)) {
               this.dragging = true;
               this.dragYOffset = (int)(mouseY - (double)handleY);
            } else {
               this.setOffset(this.scrollOffset + this.visibleItems);
            }

            this.m_93692_(true);
            return true;
         }
      }

      public boolean m_6348_(double mouseX, double mouseY, int button) {
         if (button == 0) {
            this.dragging = false;
         }

         return super.m_6348_(mouseX, mouseY, button);
      }

      protected void m_7212_(double mouseX, double mouseY, double dx, double dy) {
         if (this.dragging && this.maxOffset() != 0) {
            int availH = this.m_93694_() - 15;
            if (availH > 0) {
               double rel = (mouseY - (double)this.m_252907_() - (double)this.dragYOffset) / (double)availH;
               rel = Math.max(0.0, Math.min(1.0, rel));
               this.setOffset((int)Math.round(rel * (double)this.maxOffset()));
            }
         }
      }

      public boolean m_6050_(double mouseX, double mouseY, double delta) {
         return this.handleScroll(delta);
      }

      protected void m_168797_(NarrationElementOutput narration) {
      }
   }

   private static record StrangerRow(UUID uuid, String name) {
   }
}
