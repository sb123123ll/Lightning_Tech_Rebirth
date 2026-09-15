package com.moakiee.ae2lt.logic;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import com.moakiee.ae2lt.overload.runtime.model.MatchMode;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadPatternDetails;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadedProviderOnlyPatternDetails;
import com.moakiee.ae2lt.util.MixinReflectionSupport;
import com.moakiee.thunderbolt.core.crafting.pattern.IWrappedPatternDetails;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

public final class AdvancedAECompat {
   @Nullable
   private static final Class<?> ADV_PATTERN_DETAILS_CLASS = MixinReflectionSupport.findClassSafe("net.pedroksl.advanced_ae.common.patterns.AdvPatternDetails");
   @Nullable
   private static final Class<?> ADV_PROCESSING_PATTERN_CLASS = MixinReflectionSupport.findClassSafe(
      "net.pedroksl.advanced_ae.common.patterns.AdvProcessingPattern"
   );
   @Nullable
   private static final Class<?> ADV_ENCODER_CLASS = MixinReflectionSupport.findClassSafe("net.pedroksl.advanced_ae.common.patterns.AdvPatternDetailsEncoder");
   @Nullable
   private static final Method DIRECTIONAL_INPUTS_SET_METHOD = MixinReflectionSupport.findDeclaredMethodSafe(ADV_PATTERN_DETAILS_CLASS, "directionalInputsSet");
   @Nullable
   private static final Method GET_DIRECTION_SIDE_FOR_INPUT_KEY_METHOD = MixinReflectionSupport.findDeclaredMethodSafe(
      ADV_PATTERN_DETAILS_CLASS, "getDirectionSideForInputKey", AEKey.class
   );
   @Nullable
   private static final Method ADV_GET_SPARSE_INPUTS_METHOD = findSparseAccessor(ADV_PROCESSING_PATTERN_CLASS, "getSparseInputs");
   @Nullable
   private static final Method ADV_GET_SPARSE_OUTPUTS_METHOD = findSparseAccessor(ADV_PROCESSING_PATTERN_CLASS, "getSparseOutputs");
   @Nullable
   private static final Method ADV_GET_DIRECTION_MAP_METHOD = findSparseAccessor(ADV_PROCESSING_PATTERN_CLASS, "getDirectionMap");
   @Nullable
   private static final Method ADV_ENCODE_METHOD = findEncodeMethod();
   private static Boolean loaded;

   public static boolean isLoaded() {
      if (loaded == null) {
         loaded = ModList.get().isLoaded("advanced_ae")
            && ADV_PATTERN_DETAILS_CLASS != null
            && DIRECTIONAL_INPUTS_SET_METHOD != null
            && GET_DIRECTION_SIDE_FOR_INPUT_KEY_METHOD != null;
      }

      return loaded;
   }

   public static boolean canEncode() {
      return isLoaded() && ADV_ENCODE_METHOD != null;
   }

   public static boolean isDirectional(IPatternDetails pattern) {
      IPatternDetails unwrapped = unwrap(pattern);
      Object advPattern = asAdvPatternDetails(unwrapped);
      if (isLoaded() && advPattern != null) {
         if (MixinReflectionSupport.invokeMethodSafe(DIRECTIONAL_INPUTS_SET_METHOD, advPattern, "read AdvancedAE directional inputs") instanceof Boolean directional
            && directional) {
            return true;
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean samePatternSemantics(@Nullable IPatternDetails stored, @Nullable IPatternDetails candidate) {
      if (isLoaded() && stored != null && candidate != null) {
         IPatternDetails storedUnwrapped = unwrap(stored);
         IPatternDetails candidateUnwrapped = unwrap(candidate);
         if (ADV_PROCESSING_PATTERN_CLASS != null
            && ADV_PROCESSING_PATTERN_CLASS.isInstance(storedUnwrapped)
            && ADV_PROCESSING_PATTERN_CLASS.isInstance(candidateUnwrapped)
            && ADV_GET_SPARSE_INPUTS_METHOD != null
            && ADV_GET_SPARSE_OUTPUTS_METHOD != null
            && ADV_GET_DIRECTION_MAP_METHOD != null) {
            List<GenericStack> storedInputs = asStackList(
               MixinReflectionSupport.invokeMethodSafe(ADV_GET_SPARSE_INPUTS_METHOD, storedUnwrapped, "read AdvancedAE sparse inputs")
            );
            List<GenericStack> candidateInputs = asStackList(
               MixinReflectionSupport.invokeMethodSafe(ADV_GET_SPARSE_INPUTS_METHOD, candidateUnwrapped, "read AdvancedAE sparse inputs")
            );
            List<GenericStack> storedOutputs = asStackList(
               MixinReflectionSupport.invokeMethodSafe(ADV_GET_SPARSE_OUTPUTS_METHOD, storedUnwrapped, "read AdvancedAE sparse outputs")
            );
            List<GenericStack> candidateOutputs = asStackList(
               MixinReflectionSupport.invokeMethodSafe(ADV_GET_SPARSE_OUTPUTS_METHOD, candidateUnwrapped, "read AdvancedAE sparse outputs")
            );
            Object storedDirections = MixinReflectionSupport.invokeMethodSafe(ADV_GET_DIRECTION_MAP_METHOD, storedUnwrapped, "read AdvancedAE direction map");
            Object candidateDirections = MixinReflectionSupport.invokeMethodSafe(
               ADV_GET_DIRECTION_MAP_METHOD, candidateUnwrapped, "read AdvancedAE direction map"
            );
            return Objects.equals(storedInputs, candidateInputs)
               && Objects.equals(storedOutputs, candidateOutputs)
               && Objects.equals(storedDirections, candidateDirections);
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Nullable
   public static Direction getDirectionForKey(IPatternDetails pattern, AEKey key) {
      IPatternDetails unwrapped = unwrap(pattern);
      Object advPattern = asAdvPatternDetails(unwrapped);
      if (advPattern == null) {
         return null;
      } else {
         Direction direct = MixinReflectionSupport.invokeMethodSafe(GET_DIRECTION_SIDE_FOR_INPUT_KEY_METHOD, advPattern, "read AdvancedAE input direction", key) instanceof Direction direction
            ? direction
            : null;
         if (direct != null) {
            return direct;
         } else {
            if (pattern instanceof OverloadedProviderOnlyPatternDetails overload && key instanceof AEItemKey itemKey) {
               IInput[] sourceInputs = unwrapped.getInputs();

               for (OverloadPatternDetails.InputSlot input : overload.overloadPatternDetailsView().inputs()) {
                  if (input.matchMode() == MatchMode.ID_ONLY
                     && input.template().m_41720_() == itemKey.getItem()
                     && input.slotIndex() >= 0
                     && input.slotIndex() < sourceInputs.length) {
                     for (GenericStack possible : sourceInputs[input.slotIndex()].getPossibleInputs()) {
                        Object resolved = MixinReflectionSupport.invokeMethodSafe(
                           GET_DIRECTION_SIDE_FOR_INPUT_KEY_METHOD, advPattern, "read AdvancedAE input direction", possible.what()
                        );
                        if (resolved instanceof Direction) {
                           return (Direction)resolved;
                        }
                     }
                  }
               }
            }

            return null;
         }
      }
   }

   private static IPatternDetails unwrap(IPatternDetails pattern) {
      IPatternDetails current = pattern;

      for (int depth = 0; depth < 8 && current instanceof IWrappedPatternDetails; depth++) {
         IWrappedPatternDetails wrapped = (IWrappedPatternDetails)current;
         IPatternDetails next = wrapped.wrappedPatternDetails();
         if (next == null || next == current) {
            break;
         }

         current = next;
      }

      return current;
   }

   @Nullable
   public static ItemStack encodeAnySide(ItemStack source, Level level) {
      return encodeWithDirections(source, level, List.of());
   }

   @Nullable
   public static ItemStack encodeWithDirections(ItemStack source, Level level, List<Integer> configuredSides) {
      if (canEncode() && source != null && !source.m_41619_() && level != null) {
         IPatternDetails details = PatternDetailsHelper.decodePattern(source, level);
         List<GenericStack> inputs;
         List<GenericStack> outputs;
         if (details instanceof AEProcessingPattern processing) {
            inputs = Arrays.asList(processing.getSparseInputs());
            outputs = Arrays.asList(processing.getSparseOutputs());
         } else {
            if (ADV_PROCESSING_PATTERN_CLASS == null
               || !ADV_PROCESSING_PATTERN_CLASS.isInstance(details)
               || ADV_GET_SPARSE_INPUTS_METHOD == null
               || ADV_GET_SPARSE_OUTPUTS_METHOD == null) {
               return null;
            }

            Object sparseInputs = MixinReflectionSupport.invokeMethodSafe(ADV_GET_SPARSE_INPUTS_METHOD, details, "read AdvancedAE sparse inputs");
            Object sparseOutputs = MixinReflectionSupport.invokeMethodSafe(ADV_GET_SPARSE_OUTPUTS_METHOD, details, "read AdvancedAE sparse outputs");
            inputs = asStackList(sparseInputs);
            outputs = asStackList(sparseOutputs);
            if (inputs == null || outputs == null) {
               return null;
            }
         }

         HashMap<AEKey, Direction> directions = new HashMap<>();

         for (int i = 0; i < inputs.size(); i++) {
            GenericStack input = inputs.get(i);
            if (input != null) {
               int encoded = i < configuredSides.size() ? configuredSides.get(i) : 0;
               Direction direction = encoded > 0 && encoded <= Direction.values().length ? Direction.values()[encoded - 1] : null;
               directions.putIfAbsent(input.what(), direction);
            }
         }

         return MixinReflectionSupport.invokeMethodSafe(
               ADV_ENCODE_METHOD,
               null,
               "encode AdvancedAE processing pattern",
               inputs.toArray(GenericStack[]::new),
               outputs.toArray(GenericStack[]::new),
               directions
            ) instanceof ItemStack stack
            ? stack
            : null;
      } else {
         return null;
      }
   }

   @Nullable
   public static AdvancedAECompat.EditableProcessingPattern restoreForEditing(IPatternDetails pattern, int maxInputSlots, int maxOutputSlots) {
      if (isLoaded() && pattern != null && maxInputSlots >= 0 && maxOutputSlots >= 0) {
         IPatternDetails unwrapped = unwrap(pattern);
         if (ADV_PROCESSING_PATTERN_CLASS != null
            && ADV_PROCESSING_PATTERN_CLASS.isInstance(unwrapped)
            && ADV_GET_SPARSE_INPUTS_METHOD != null
            && ADV_GET_SPARSE_OUTPUTS_METHOD != null) {
            Object sparseInputs = MixinReflectionSupport.invokeMethodSafe(ADV_GET_SPARSE_INPUTS_METHOD, unwrapped, "read AdvancedAE sparse inputs");
            Object sparseOutputs = MixinReflectionSupport.invokeMethodSafe(ADV_GET_SPARSE_OUTPUTS_METHOD, unwrapped, "read AdvancedAE sparse outputs");
            List<GenericStack> sparseInputList = asStackList(sparseInputs);
            List<GenericStack> sparseOutputList = asStackList(sparseOutputs);
            if (sparseInputList != null && sparseOutputList != null) {
               if (sparseInputList.size() <= maxInputSlots && sparseOutputList.size() <= maxOutputSlots) {
                  List<GenericStack> inputs = nullableCopy(sparseInputList);
                  List<GenericStack> outputs = nullableCopy(sparseOutputList);
                  int[] directions = new int[maxInputSlots];

                  for (int slot = 0; slot < inputs.size(); slot++) {
                     GenericStack input = inputs.get(slot);
                     if (input != null && input.what() != null) {
                        directions[slot] = MixinReflectionSupport.invokeMethodSafe(
                              GET_DIRECTION_SIDE_FOR_INPUT_KEY_METHOD, unwrapped, "read AdvancedAE input direction", input.what()
                           ) instanceof Direction direction
                           ? direction.ordinal() + 1
                           : 0;
                     }
                  }

                  return new AdvancedAECompat.EditableProcessingPattern(inputs, outputs, directions);
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static List<GenericStack> nullableCopy(List<GenericStack> source) {
      return source != null && !source.isEmpty() ? Collections.unmodifiableList(new ArrayList<>(source)) : List.of();
   }

   @Nullable
   private static List<GenericStack> asStackList(@Nullable Object sparse) {
      if (sparse instanceof List<?> list) {
         ArrayList<GenericStack> result = new ArrayList<>(list.size());

         for (Object element : list) {
            result.add(element instanceof GenericStack stack ? stack : null);
         }

         return result;
      } else {
         return sparse instanceof GenericStack[] array ? new ArrayList<>(Arrays.asList(array)) : null;
      }
   }

   @Nullable
   private static Method findSparseAccessor(@Nullable Class<?> owner, String name) {
      if (owner == null) {
         return null;
      } else {
         try {
            Method method = owner.getMethod(name);
            method.setAccessible(true);
            return method;
         } catch (Exception var3) {
            return null;
         }
      }
   }

   @Nullable
   private static Method findEncodeMethod() {
      if (ADV_ENCODER_CLASS == null) {
         return null;
      } else {
         try {
            for (Method method : ADV_ENCODER_CLASS.getDeclaredMethods()) {
               if (method.getName().equals("encodeProcessingPattern")
                  && Modifier.isStatic(method.getModifiers())
                  && Arrays.equals((Object[])method.getParameterTypes(), (Object[])(new Class[]{GenericStack[].class, GenericStack[].class, HashMap.class}))) {
                  method.setAccessible(true);
                  return method;
               }
            }
         } catch (Exception var4) {
         }

         return null;
      }
   }

   private AdvancedAECompat() {
   }

   @Nullable
   private static Object asAdvPatternDetails(IPatternDetails pattern) {
      return isLoaded() && ADV_PATTERN_DETAILS_CLASS != null && ADV_PATTERN_DETAILS_CLASS.isInstance(pattern) ? pattern : null;
   }

   public static record EditableProcessingPattern(List<GenericStack> inputs, List<GenericStack> outputs, int[] directions) {
      public EditableProcessingPattern(List<GenericStack> inputs, List<GenericStack> outputs, int[] directions) {
         inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
         outputs = Collections.unmodifiableList(new ArrayList<>(outputs));
         directions = (int[])directions.clone();
         this.inputs = inputs;
         this.outputs = outputs;
         this.directions = directions;
      }

      public int[] directions() {
         return (int[])this.directions.clone();
      }
   }
}
