package com.moakiee.ae2lt.client.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;

public class AE2Button extends Button {
   public AE2Button(int x, int y, int width, int height, Component message, OnPress onPress) {
      super(x, y, width, height, message, onPress, f_252438_);
   }

   public AE2Button(Component message, OnPress onPress) {
      this(0, 0, 0, 0, message, onPress);
   }
}
