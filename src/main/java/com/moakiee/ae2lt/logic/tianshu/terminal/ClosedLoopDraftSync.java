package com.moakiee.ae2lt.logic.tianshu.terminal;

import appeng.menu.guisync.PacketWritable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

public record ClosedLoopDraftSync(List<Long> memberCopies, List<Integer> outputRoles) implements PacketWritable {
   public static final int MEMBER_SLOTS = 27;
   public static final int OUTPUT_SLOTS = 9;

   public ClosedLoopDraftSync(List<Long> memberCopies, List<Integer> outputRoles) {
      memberCopies = List.copyOf(memberCopies);
      outputRoles = List.copyOf(outputRoles);
      if (memberCopies.size() == 27 && outputRoles.size() == 9) {
         for (long copies : memberCopies) {
            if (copies < 0L) {
               throw new IllegalArgumentException("negative member copies");
            }
         }

         for (int role : outputRoles) {
            if (role < 0 || role > 2) {
               throw new IllegalArgumentException("invalid output role");
            }
         }

         this.memberCopies = memberCopies;
         this.outputRoles = outputRoles;
      } else {
         throw new IllegalArgumentException("invalid closed-loop draft sync dimensions");
      }
   }

   public ClosedLoopDraftSync(FriendlyByteBuf data) {
      this(readCopies(data), readRoles(data));
   }

   public static ClosedLoopDraftSync empty() {
      return new ClosedLoopDraftSync(Collections.nCopies(27, 0L), Collections.nCopies(9, 0));
   }

   public long copies(int slot) {
      return slot >= 0 && slot < this.memberCopies.size() ? this.memberCopies.get(slot) : 0L;
   }

   public int outputRole(int slot) {
      return slot >= 0 && slot < this.outputRoles.size() ? this.outputRoles.get(slot) : 0;
   }

   public void writeToPacket(FriendlyByteBuf data) {
      for (long copies : this.memberCopies) {
         data.m_130103_(copies);
      }

      for (int role : this.outputRoles) {
         data.m_130130_(role);
      }
   }

   private static List<Long> readCopies(FriendlyByteBuf data) {
      ArrayList<Long> result = new ArrayList<>(27);

      for (int i = 0; i < 27; i++) {
         result.add(data.m_130258_());
      }

      return result;
   }

   private static List<Integer> readRoles(FriendlyByteBuf data) {
      ArrayList<Integer> result = new ArrayList<>(9);

      for (int i = 0; i < 9; i++) {
         result.add(data.m_130242_());
      }

      return result;
   }
}
