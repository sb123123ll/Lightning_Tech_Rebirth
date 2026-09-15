package com.moakiee.ae2lt.api.client;

import appeng.api.config.Setting;
import com.moakiee.ae2lt.client.SettingToggleButtonAccess;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PatternProviderToolbarButtonHider {
   public static final String EXTENDED_AE_PLUS_SMART_FEATURE_BUTTON = "com.extendedae_plus.util.GuiUtil$1";
   public static final String EXPANDED_AE_MODIFY_PATTERNS_BUTTON = "lu.kolja.expandedae.client.gui.widgets.ExpActionButton";
   public static final String EXPANDED_AE_BLOCKING_SETTING = "blocking_type";
   private static final Set<String> HIDDEN_BUTTON_CLASS_NAMES = ConcurrentHashMap.newKeySet();
   private static final Set<String> HIDDEN_SETTING_NAMES = ConcurrentHashMap.newKeySet();

   public static void registerHiddenButtonClassName(String className) {
      if (className != null && !className.isBlank()) {
         HIDDEN_BUTTON_CLASS_NAMES.add(className);
      } else {
         throw new IllegalArgumentException("className must not be blank");
      }
   }

   public static boolean shouldHideToolbarButtonClassName(String className) {
      return HIDDEN_BUTTON_CLASS_NAMES.contains(className);
   }

   public static void registerHiddenSettingName(String settingName) {
      if (settingName != null && !settingName.isBlank()) {
         HIDDEN_SETTING_NAMES.add(settingName);
      } else {
         throw new IllegalArgumentException("settingName must not be blank");
      }
   }

   public static boolean shouldHideToolbarButtonSettingName(String settingName) {
      return HIDDEN_SETTING_NAMES.contains(settingName);
   }

   public static int removeHiddenToolbarButtons(List<?> buttons) {
      int previousSize = buttons.size();
      buttons.removeIf(PatternProviderToolbarButtonHider::shouldHideToolbarButton);
      return previousSize - buttons.size();
   }

   private static boolean shouldHideToolbarButton(Object button) {
      if (button == null) {
         return false;
      } else if (shouldHideToolbarButtonClassName(button.getClass().getName())) {
         return true;
      } else if (!(button instanceof SettingToggleButtonAccess accessor)) {
         return false;
      } else {
         Setting<? extends Enum<?>> setting = (Setting<? extends Enum<?>>)accessor.ae2lt$getSetting();
         return setting != null && shouldHideToolbarButtonSettingName(setting.getName());
      }
   }

   private PatternProviderToolbarButtonHider() {
   }

   static {
      registerHiddenButtonClassName("com.extendedae_plus.util.GuiUtil$1");
      registerHiddenButtonClassName("lu.kolja.expandedae.client.gui.widgets.ExpActionButton");
      registerHiddenSettingName("blocking_type");
   }
}
