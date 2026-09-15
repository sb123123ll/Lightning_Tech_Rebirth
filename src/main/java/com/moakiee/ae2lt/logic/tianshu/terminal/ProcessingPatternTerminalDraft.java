package com.moakiee.ae2lt.logic.tianshu.terminal;

import appeng.api.stacks.GenericStack;
import appeng.menu.guisync.PacketWritable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

public record ProcessingPatternTerminalDraft(
   ProcessingPatternEncodingType type,
   List<GenericStack> inputs,
   List<GenericStack> outputs,
   @Nullable ProcessingPatternEncodingType.AdvancedConfig advancedConfig,
   @Nullable ProcessingPatternEncodingType.OverloadConfig overloadConfig
) implements PacketWritable {
   private static final int MAX_INPUT_SLOTS = 81;
   private static final int MAX_OUTPUT_SLOTS = 27;
   private static final String TAG_TYPE = "Type";
   private static final String TAG_INPUT_SIZE = "InputSize";
   private static final String TAG_OUTPUT_SIZE = "OutputSize";
   private static final String TAG_INPUTS = "Inputs";
   private static final String TAG_OUTPUTS = "Outputs";
   private static final String TAG_DIRECTIONS = "Directions";
   private static final String TAG_INPUT_ID_ONLY = "InputIdOnly";
   private static final String TAG_OUTPUT_ID_ONLY = "OutputIdOnly";
   private static final String TAG_SLOT = "Slot";
   private static final ProcessingPatternTerminalDraft EMPTY = new ProcessingPatternTerminalDraft(
      ProcessingPatternEncodingType.NORMAL, List.of(), List.of(), null, null
   );

   public ProcessingPatternTerminalDraft(
      ProcessingPatternEncodingType type,
      List<GenericStack> inputs,
      List<GenericStack> outputs,
      @Nullable ProcessingPatternEncodingType.AdvancedConfig advancedConfig,
      @Nullable ProcessingPatternEncodingType.OverloadConfig overloadConfig
   ) {
      type = Objects.requireNonNull(type, "type");
      inputs = immutableNullableCopy(inputs, "inputs", 81);
      outputs = immutableNullableCopy(outputs, "outputs", 27);
      if (type != ProcessingPatternEncodingType.fromConfigs(advancedConfig, overloadConfig)) {
         throw new IllegalArgumentException("processing draft type does not match configuration");
      } else {
         if (advancedConfig != null) {
            int[] directions = advancedConfig.directions();
            if (directions.length > inputs.size()) {
               throw new IllegalArgumentException("too many advanced directions");
            }

            for (int direction : directions) {
               if (direction < 0 || direction > 6) {
                  throw new IllegalArgumentException("invalid advanced direction");
               }
            }
         }

         if (overloadConfig != null) {
            validateSlots(overloadConfig.inputIdOnly(), inputs.size());
            validateSlots(overloadConfig.outputIdOnly(), outputs.size());
         }

         this.type = type;
         this.inputs = inputs;
         this.outputs = outputs;
         this.advancedConfig = advancedConfig;
         this.overloadConfig = overloadConfig;
      }
   }

   public ProcessingPatternTerminalDraft(FriendlyByteBuf data) {
      this(
         (ProcessingPatternEncodingType)data.m_130066_(ProcessingPatternEncodingType.class),
         readStacks(data, 81, "input size"),
         readStacks(data, 27, "output size"),
         readAdvancedConfig(data),
         readOverloadConfig(data)
      );
   }

   public static ProcessingPatternTerminalDraft empty() {
      return EMPTY;
   }

   public static ProcessingPatternTerminalDraft advanced(
      List<GenericStack> inputs, List<GenericStack> outputs, ProcessingPatternEncodingType.AdvancedConfig config
   ) {
      return new ProcessingPatternTerminalDraft(ProcessingPatternEncodingType.ADVANCED, inputs, outputs, Objects.requireNonNull(config, "config"), null);
   }

   public static ProcessingPatternTerminalDraft overload(
      List<GenericStack> inputs, List<GenericStack> outputs, ProcessingPatternEncodingType.OverloadConfig config
   ) {
      return new ProcessingPatternTerminalDraft(ProcessingPatternEncodingType.OVERLOAD, inputs, outputs, null, Objects.requireNonNull(config, "config"));
   }

   public static ProcessingPatternTerminalDraft configured(
      List<GenericStack> inputs,
      List<GenericStack> outputs,
      @Nullable ProcessingPatternEncodingType.AdvancedConfig advancedConfig,
      @Nullable ProcessingPatternEncodingType.OverloadConfig overloadConfig
   ) {
      return new ProcessingPatternTerminalDraft(
         ProcessingPatternEncodingType.fromConfigs(advancedConfig, overloadConfig), inputs, outputs, advancedConfig, overloadConfig
      );
   }

   public boolean matches(List<GenericStack> currentInputs, List<GenericStack> currentOutputs) {
      return sameStackKeys(this.inputs, currentInputs) && sameStackKeys(this.outputs, currentOutputs);
   }

   public CompoundTag write() {
      CompoundTag tag = new CompoundTag();
      tag.m_128359_("Type", this.type.name());
      tag.m_128405_("InputSize", this.inputs.size());
      tag.m_128405_("OutputSize", this.outputs.size());
      tag.m_128365_("Inputs", writeStacks(this.inputs));
      tag.m_128365_("Outputs", writeStacks(this.outputs));
      if (this.advancedConfig != null) {
         tag.m_128385_("Directions", this.advancedConfig.directions());
      }

      if (this.overloadConfig != null) {
         tag.m_128385_("InputIdOnly", this.overloadConfig.inputIdOnly());
         tag.m_128385_("OutputIdOnly", this.overloadConfig.outputIdOnly());
      }

      return tag;
   }

   @Nullable
   public static ProcessingPatternTerminalDraft read(CompoundTag tag) {
      try {
         ProcessingPatternEncodingType type = ProcessingPatternEncodingType.valueOf(tag.m_128461_("Type"));
         if (type == ProcessingPatternEncodingType.NORMAL) {
            return null;
         } else {
            int inputSize = checkedSize(tag.m_128451_("InputSize"), 81, "input size");
            int outputSize = checkedSize(tag.m_128451_("OutputSize"), 27, "output size");
            List<GenericStack> inputs = readStacks(tag.m_128437_("Inputs", 10), inputSize);
            List<GenericStack> outputs = readStacks(tag.m_128437_("Outputs", 10), outputSize);
            ProcessingPatternEncodingType.AdvancedConfig advanced = type.hasAdvanced()
               ? new ProcessingPatternEncodingType.AdvancedConfig(tag.m_128465_("Directions"))
               : null;
            ProcessingPatternEncodingType.OverloadConfig overload = type.hasOverload()
               ? new ProcessingPatternEncodingType.OverloadConfig(tag.m_128465_("InputIdOnly"), tag.m_128465_("OutputIdOnly"))
               : null;
            return configured(inputs, outputs, advanced, overload);
         }
      } catch (RuntimeException var8) {
         return null;
      }
   }

   public static boolean sameState(@Nullable ProcessingPatternTerminalDraft left, @Nullable ProcessingPatternTerminalDraft right) {
      if (left == right) {
         return true;
      } else {
         return left != null && right != null && left.type == right.type && left.inputs.equals(right.inputs) && left.outputs.equals(right.outputs)
            ? sameAdvanced(left.advancedConfig, right.advancedConfig) && sameOverload(left.overloadConfig, right.overloadConfig)
            : false;
      }
   }

   public void writeToPacket(FriendlyByteBuf data) {
      data.m_130068_(this.type);
      writeStacks(data, this.inputs);
      writeStacks(data, this.outputs);
      data.writeBoolean(this.advancedConfig != null);
      if (this.advancedConfig != null) {
         writeIntArray(data, this.advancedConfig.directions());
      }

      data.writeBoolean(this.overloadConfig != null);
      if (this.overloadConfig != null) {
         writeIntArray(data, this.overloadConfig.inputIdOnly());
         writeIntArray(data, this.overloadConfig.outputIdOnly());
      }
   }

   @Override
   public boolean equals(Object other) {
      if (other instanceof ProcessingPatternTerminalDraft draft && sameState(this, draft)) {
         return true;
      }

      return false;
   }

   @Override
   public int hashCode() {
      int result = Objects.hash(this.type, this.inputs, this.outputs);
      if (this.advancedConfig != null) {
         result = 31 * result + Arrays.hashCode(this.advancedConfig.directions());
      }

      if (this.overloadConfig != null) {
         result = 31 * result + Arrays.hashCode(this.overloadConfig.inputIdOnly());
         result = 31 * result + Arrays.hashCode(this.overloadConfig.outputIdOnly());
      }

      return result;
   }

   private static List<GenericStack> immutableNullableCopy(List<GenericStack> stacks, String name, int maxSize) {
      if (stacks != null && stacks.size() <= maxSize) {
         return Collections.unmodifiableList(new ArrayList<>(stacks));
      } else {
         throw new IllegalArgumentException("invalid processing draft " + name);
      }
   }

   private static boolean sameStackKeys(List<GenericStack> left, List<GenericStack> right) {
      if (right != null && left.size() == right.size()) {
         for (int i = 0; i < left.size(); i++) {
            GenericStack leftStack = left.get(i);
            GenericStack rightStack = right.get(i);
            if (leftStack != null && rightStack != null) {
               if (!leftStack.what().equals(rightStack.what())) {
                  return false;
               }
            } else if (leftStack != rightStack) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean sameAdvanced(
      @Nullable ProcessingPatternEncodingType.AdvancedConfig left, @Nullable ProcessingPatternEncodingType.AdvancedConfig right
   ) {
      return left == right || left != null && right != null && Arrays.equals(left.directions(), right.directions());
   }

   private static boolean sameOverload(
      @Nullable ProcessingPatternEncodingType.OverloadConfig left, @Nullable ProcessingPatternEncodingType.OverloadConfig right
   ) {
      return left == right
         || left != null && right != null && Arrays.equals(left.inputIdOnly(), right.inputIdOnly()) && Arrays.equals(left.outputIdOnly(), right.outputIdOnly());
   }

   private static void validateSlots(int[] slots, int slotCount) {
      boolean[] seen = new boolean[slotCount];

      for (int slot : slots) {
         if (slot < 0 || slot >= slotCount || seen[slot]) {
            throw new IllegalArgumentException("invalid overload slot");
         }

         seen[slot] = true;
      }
   }

   private static ListTag writeStacks(List<GenericStack> stacks) {
      ListTag result = new ListTag();

      for (int slot = 0; slot < stacks.size(); slot++) {
         GenericStack stack = stacks.get(slot);
         if (stack != null) {
            CompoundTag entry = GenericStack.writeTag(stack);
            entry.m_128405_("Slot", slot);
            result.add(entry);
         }
      }

      return result;
   }

   private static List<GenericStack> readStacks(ListTag entries, int size) {
      ArrayList<GenericStack> result = nullableStackList(size);

      for (int i = 0; i < entries.size(); i++) {
         CompoundTag entry = entries.m_128728_(i);
         int slot = entry.m_128451_("Slot");
         if (slot >= 0 && slot < size) {
            result.set(slot, GenericStack.readTag(entry));
         }
      }

      return result;
   }

   private static void writeStacks(FriendlyByteBuf data, List<GenericStack> stacks) {
      data.m_130130_(stacks.size());

      for (GenericStack stack : stacks) {
         GenericStack.writeBuffer(stack, data);
      }
   }

   private static List<GenericStack> readStacks(FriendlyByteBuf data, int maxSize, String name) {
      int size = checkedSize(data.m_130242_(), maxSize, name);
      ArrayList<GenericStack> result = new ArrayList<>(size);

      for (int i = 0; i < size; i++) {
         result.add(GenericStack.readBuffer(data));
      }

      return result;
   }

   private static void writeIntArray(FriendlyByteBuf data, int[] values) {
      data.m_130130_(values.length);

      for (int value : values) {
         data.m_130130_(value);
      }
   }

   private static int[] readIntArray(FriendlyByteBuf data, int maxSize, String name) {
      int size = checkedSize(data.m_130242_(), maxSize, name);
      int[] result = new int[size];

      for (int i = 0; i < size; i++) {
         result[i] = data.m_130242_();
      }

      return result;
   }

   @Nullable
   private static ProcessingPatternEncodingType.AdvancedConfig readAdvancedConfig(FriendlyByteBuf data) {
      return data.readBoolean() ? new ProcessingPatternEncodingType.AdvancedConfig(readIntArray(data, 81, "advanced direction count")) : null;
   }

   @Nullable
   private static ProcessingPatternEncodingType.OverloadConfig readOverloadConfig(FriendlyByteBuf data) {
      return data.readBoolean()
         ? new ProcessingPatternEncodingType.OverloadConfig(
            readIntArray(data, 81, "overload input slot count"), readIntArray(data, 27, "overload output slot count")
         )
         : null;
   }

   private static int checkedSize(int size, int maxSize, String name) {
      if (size >= 0 && size <= maxSize) {
         return size;
      } else {
         throw new IllegalArgumentException("invalid processing draft " + name);
      }
   }

   private static ArrayList<GenericStack> nullableStackList(int size) {
      ArrayList<GenericStack> result = new ArrayList<>(size);

      for (int i = 0; i < size; i++) {
         result.add(null);
      }

      return result;
   }
}
