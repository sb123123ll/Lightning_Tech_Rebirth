package com.moakiee.ae2lt.grid.wirelesslink;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridMultiblock;
import appeng.api.networking.IGridNode;
import java.lang.reflect.Method;
import java.util.Iterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public final class MultiblockLinkReadiness {
   private static final String FORMED_PROPERTY = "formed";

   private MultiblockLinkReadiness() {
   }

   public static boolean canKeepVirtualConnection(IGridNode node) {
      Boolean formedState = getRelevantFormedState(node);
      if (Boolean.FALSE.equals(formedState)) {
         return false;
      } else if (!node.hasFlag(GridFlags.MULTIBLOCK)) {
         return true;
      } else {
         IGridMultiblock multiblock = (IGridMultiblock)node.getService(IGridMultiblock.class);
         return multiblock != null && containsSelf(multiblock.getMultiblockNodes(), node);
      }
   }

   public static boolean isKnownMultiblockAffectedByChange(IGridNode node, BlockPos changedPos) {
      Object owner = getOwner(node);
      if (!(owner instanceof BlockEntity blockEntity)) {
         return false;
      } else {
         ResourceLocation blockId = BuiltInRegistries.f_256975_.m_7981_(currentBlockState(blockEntity).m_60734_());
         if (!node.hasFlag(GridFlags.MULTIBLOCK) && !isKnownFormedStateMultiblock(owner.getClass().getName(), blockId)) {
            return false;
         } else {
            Object cluster = invokeNoArg(owner, "getCluster");
            if (cluster == null) {
               return false;
            } else {
               BlockPos min = invokeBlockPos(cluster, "getBoundsMin");
               BlockPos max = invokeBlockPos(cluster, "getBoundsMax");
               return min != null && max != null && contains(min, max, changedPos);
            }
         }
      }
   }

   public static void refreshAfterVirtualConnectionRemoved(IGridNode node) {
      Object owner = getOwner(node);
      if (owner instanceof BlockEntity blockEntity) {
         BlockState state = currentBlockState(blockEntity);
         ResourceLocation blockId = BuiltInRegistries.f_256975_.m_7981_(state.m_60734_());
         if (readBooleanProperty(state, "formed") != null
            && (node.hasFlag(GridFlags.MULTIBLOCK) || isKnownFormedStateMultiblock(owner.getClass().getName(), blockId))) {
            invokeUpdateSubType(blockEntity);
         }
      }
   }

   static boolean containsSelf(Iterator<?> nodes, Object self) {
      while (nodes.hasNext()) {
         if (nodes.next() == self) {
            return true;
         }
      }

      return false;
   }

   static boolean contains(BlockPos min, BlockPos max, BlockPos pos) {
      int minX = Math.min(min.m_123341_(), max.m_123341_());
      int minY = Math.min(min.m_123342_(), max.m_123342_());
      int minZ = Math.min(min.m_123343_(), max.m_123343_());
      int maxX = Math.max(min.m_123341_(), max.m_123341_());
      int maxY = Math.max(min.m_123342_(), max.m_123342_());
      int maxZ = Math.max(min.m_123343_(), max.m_123343_());
      return pos.m_123341_() >= minX
         && pos.m_123341_() <= maxX
         && pos.m_123342_() >= minY
         && pos.m_123342_() <= maxY
         && pos.m_123343_() >= minZ
         && pos.m_123343_() <= maxZ;
   }

   static boolean isKnownFormedStateMultiblock(String ownerClassName, String blockNamespace, String blockPath) {
      return KnownFormedStateMultiblocks.matches(ownerClassName, blockNamespace, blockPath);
   }

   private static boolean isKnownFormedStateMultiblock(String ownerClassName, ResourceLocation blockId) {
      return blockId != null && isKnownFormedStateMultiblock(ownerClassName, blockId.m_135827_(), blockId.m_135815_());
   }

   private static Boolean getRelevantFormedState(IGridNode node) {
      Object owner = getOwner(node);
      if (owner instanceof BlockEntity blockEntity) {
         BlockState state = currentBlockState(blockEntity);
         Boolean formed = readBooleanProperty(state, "formed");
         if (formed == null) {
            return null;
         } else {
            ResourceLocation blockId = BuiltInRegistries.f_256975_.m_7981_(state.m_60734_());
            return !node.hasFlag(GridFlags.MULTIBLOCK) && !isKnownFormedStateMultiblock(owner.getClass().getName(), blockId) ? null : formed;
         }
      } else {
         return null;
      }
   }

   private static Object getOwner(IGridNode node) {
      try {
         return node.getOwner();
      } catch (RuntimeException var2) {
         return null;
      }
   }

   private static Object invokeNoArg(Object target, String methodName) {
      Method method = findMethod(target.getClass(), methodName);
      if (method == null) {
         return null;
      } else {
         try {
            method.setAccessible(true);
            return method.invoke(target);
         } catch (RuntimeException | ReflectiveOperationException var4) {
            return null;
         }
      }
   }

   private static BlockPos invokeBlockPos(Object target, String methodName) {
      return invokeNoArg(target, methodName) instanceof BlockPos pos ? pos : null;
   }

   private static BlockState currentBlockState(BlockEntity blockEntity) {
      Level level = blockEntity.m_58904_();
      return level == null ? blockEntity.m_58900_() : level.m_8055_(blockEntity.m_58899_());
   }

   private static Boolean readBooleanProperty(BlockState state, String propertyName) {
      for (Property<?> property : state.m_61147_()) {
         if (property instanceof BooleanProperty booleanProperty && property.m_61708_().equals(propertyName)) {
            return (Boolean)state.m_61143_(booleanProperty);
         }
      }

      return null;
   }

   private static void invokeUpdateSubType(BlockEntity blockEntity) {
      Method updateSubType = findMethod(blockEntity.getClass(), "updateSubType", boolean.class);
      if (updateSubType != null) {
         try {
            updateSubType.setAccessible(true);
            updateSubType.invoke(blockEntity, false);
         } catch (RuntimeException | ReflectiveOperationException var3) {
         }
      }
   }

   private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
      for (Class<?> current = type; current != null; current = current.getSuperclass()) {
         try {
            return current.getDeclaredMethod(name, parameterTypes);
         } catch (NoSuchMethodException var5) {
         }
      }

      return null;
   }
}
