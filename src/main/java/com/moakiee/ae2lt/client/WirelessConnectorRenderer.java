package com.moakiee.ae2lt.client;

import appeng.client.render.overlay.OverlayRenderType;
import com.moakiee.ae2lt.api.patternprovider.WirelessPatternProviderHost;
import com.moakiee.ae2lt.blockentity.OverloadedInterfaceBlockEntity;
import com.moakiee.ae2lt.blockentity.OverloadedPowerSupplyBlockEntity;
import com.moakiee.ae2lt.item.OverloadedWirelessConnectorItem;
import com.moakiee.ae2lt.logic.WirelessConnectorTargetHelper;
import com.moakiee.ae2lt.logic.wireless.support.WirelessConnectionRef;
import com.moakiee.ae2lt.util.ItemStackTagSupport;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.joml.Matrix4f;

@EventBusSubscriber(
   modid = "ae2lt",
   value = {Dist.CLIENT}
)
public class WirelessConnectorRenderer {
   private static final int COLOR_PREVIEW = 1627389696;
   private static final int COLOR_CONNECTED = 1610645759;
   private static final int COLOR_PREVIEW_LINE = -1056964864;
   private static final int COLOR_HOST = -2147450625;
   private static final int COLOR_HOST_SELECTED = -2130706688;
   private static final int COLOR_LINE = -1073708801;
   private static final int SCAN_RANGE = 64;
   private static final int RESCAN_INTERVAL_TICKS = 4;
   private static final List<BlockPos> cachedHostPositions = new ArrayList<>();
   private static long lastScanTick = -1L;
   private static ResourceKey<Level> lastScanDimension = null;
   private static final Set<BlockPos> scratchConnectionSet = new HashSet<>();
   private static final String TAG_SELECTED = "SelectedProvider";
   private static final String TAG_DIM = "Dim";
   private static final String TAG_POS = "Pos";
   private static final String TAG_HOST_TYPE = "HostType";

   @SubscribeEvent
   public static void onRenderLevelStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
         Minecraft mc = Minecraft.m_91087_();
         LocalPlayer player = mc.f_91074_;
         if (player != null && mc.f_91073_ != null) {
            ItemStack stack = getHeldConnectorStack();
            if (!stack.m_41619_()) {
               PoseStack poseStack = event.getPoseStack();
               BufferSource buffer = mc.m_91269_().m_110104_();
               Vec3 cam = event.getCamera().m_90583_();
               WirelessConnectorRenderer.SelectedHost selectedHost = getSelectedHost(stack);
               BlockPos selectedPos = selectedHost != null ? selectedHost.pos() : null;
               ResourceKey<Level> selectedDim = selectedHost != null ? selectedHost.dimension() : null;
               String selectedHostType = selectedHost != null ? selectedHost.hostType() : null;
               boolean hasSelection = selectedPos != null && selectedDim != null && selectedHostType != null;
               boolean selectionInCurrentDimension = hasSelection && mc.f_91073_.m_46472_().equals(selectedDim);
               long selectedPosLong = selectedPos != null ? selectedPos.m_121878_() : 0L;
               boolean selectedRendered = false;
               long gameTime = mc.f_91073_.m_46467_();
               ResourceKey<Level> currentDim = mc.f_91073_.m_46472_();
               if (lastScanTick < 0L || lastScanDimension == null || !lastScanDimension.equals(currentDim) || gameTime - lastScanTick >= 4L) {
                  rescanHosts(mc.f_91073_, player.m_20183_());
                  lastScanTick = gameTime;
                  lastScanDimension = currentDim;
               }

               for (BlockPos bePos : cachedHostPositions) {
                  if (mc.f_91073_.m_46749_(bePos)) {
                     BlockEntity be = mc.f_91073_.m_7702_(bePos);
                     if (be instanceof WirelessPatternProviderHost) {
                        WirelessPatternProviderHost provider = (WirelessPatternProviderHost)be;
                        if (provider.isWirelessProvider()) {
                           boolean isSelected = hasSelection && selectionInCurrentDimension && "provider".equals(selectedHostType) && bePos.equals(selectedPos);
                           if (WirelessConnectorRenderFilter.shouldRenderHost(
                              hasSelection, selectionInCurrentDimension, selectedPosLong, selectedHostType, "provider", bePos.m_121878_()
                           )) {
                              renderProviderHost(poseStack, buffer, cam, mc.f_91073_, bePos, provider, isSelected);
                              selectedRendered |= isSelected;
                           }
                        }
                     } else if (be instanceof OverloadedInterfaceBlockEntity) {
                        OverloadedInterfaceBlockEntity iface = (OverloadedInterfaceBlockEntity)be;
                        if (iface.getInterfaceMode() == OverloadedInterfaceBlockEntity.InterfaceMode.WIRELESS) {
                           boolean isSelected = hasSelection
                              && selectionInCurrentDimension
                              && "interface".equals(selectedHostType)
                              && bePos.equals(selectedPos);
                           if (WirelessConnectorRenderFilter.shouldRenderHost(
                              hasSelection, selectionInCurrentDimension, selectedPosLong, selectedHostType, "interface", bePos.m_121878_()
                           )) {
                              renderInterfaceHost(poseStack, buffer, cam, mc.f_91073_, bePos, iface, isSelected);
                              selectedRendered |= isSelected;
                           }
                        }
                     } else if (be instanceof OverloadedPowerSupplyBlockEntity) {
                        OverloadedPowerSupplyBlockEntity powerSupply = (OverloadedPowerSupplyBlockEntity)be;
                        boolean isSelected = hasSelection
                           && selectionInCurrentDimension
                           && "power_supply".equals(selectedHostType)
                           && bePos.equals(selectedPos);
                        if (WirelessConnectorRenderFilter.shouldRenderHost(
                           hasSelection, selectionInCurrentDimension, selectedPosLong, selectedHostType, "power_supply", bePos.m_121878_()
                        )) {
                           renderPowerSupplyHost(poseStack, buffer, cam, mc.f_91073_, bePos, powerSupply, isSelected);
                           selectedRendered |= isSelected;
                        }
                     }
                  }
               }

               label195:
               if (selectionInCurrentDimension && !selectedRendered && mc.f_91073_.m_46749_(selectedPos)) {
                  BlockEntity selectedBe = mc.f_91073_.m_7702_(selectedPos);
                  if ("provider".equals(selectedHostType) && selectedBe instanceof WirelessPatternProviderHost provider && provider.isWirelessProvider()) {
                     renderProviderHost(poseStack, buffer, cam, mc.f_91073_, selectedPos, provider, true);
                     break label195;
                  }

                  if ("interface".equals(selectedHostType)
                     && selectedBe instanceof OverloadedInterfaceBlockEntity iface
                     && iface.getInterfaceMode() == OverloadedInterfaceBlockEntity.InterfaceMode.WIRELESS) {
                     renderInterfaceHost(poseStack, buffer, cam, mc.f_91073_, selectedPos, iface, true);
                     break label195;
                  }

                  if ("power_supply".equals(selectedHostType) && selectedBe instanceof OverloadedPowerSupplyBlockEntity powerSupply) {
                     renderPowerSupplyHost(poseStack, buffer, cam, mc.f_91073_, selectedPos, powerSupply, true);
                  }
               }

               label181:
               if (selectionInCurrentDimension) {
                  BlockEntity selectedBex = mc.f_91073_.m_7702_(selectedPos);
                  if ("provider".equals(selectedHostType)
                     && selectedBex instanceof WirelessPatternProviderHost selectedProvider
                     && selectedProvider.isWirelessProvider()
                     && mc.f_91077_ instanceof BlockHitResult bhr
                     && bhr.m_6662_() == Type.BLOCK
                     && !bhr.m_82425_().equals(selectedPos)
                     && mc.f_91073_.m_7702_(bhr.m_82425_()) != null) {
                     Set<BlockPos> previewTargets = WirelessConnectorTargetHelper.collectTargets(mc.f_91073_, bhr.m_82425_(), Screen.m_96637_());
                     Direction lookFace = bhr.m_82434_();
                     Set<BlockPos> existingConnections = collectConnectionsForFace(
                        selectedProvider.getConnections(), mc.f_91073_, lookFace, c -> c.dimension(), c -> c.pos(), c -> c.boundFace()
                     );
                     Iterator var54 = previewTargets.iterator();

                     while (true) {
                        if (!var54.hasNext()) {
                           break label181;
                        }

                        BlockPos lookPos = (BlockPos)var54.next();
                        if (!existingConnections.contains(lookPos)) {
                           renderFaceOverlay(poseStack, buffer, cam, lookPos, lookFace, 1627389696);
                           renderLine(poseStack, buffer, cam, selectedPos, lookPos, lookFace, -1056964864);
                        }
                     }
                  }

                  if ("interface".equals(selectedHostType)
                     && selectedBex instanceof OverloadedInterfaceBlockEntity selectedInterface
                     && mc.f_91077_ instanceof BlockHitResult bhr
                     && bhr.m_6662_() == Type.BLOCK
                     && !bhr.m_82425_().equals(selectedPos)
                     && mc.f_91073_.m_7702_(bhr.m_82425_()) != null) {
                     Set<BlockPos> previewTargets = WirelessConnectorTargetHelper.collectTargets(mc.f_91073_, bhr.m_82425_(), Screen.m_96637_());
                     Direction lookFace = bhr.m_82434_();
                     Set<BlockPos> existingConnections = collectConnectionsForFace(
                        selectedInterface.getConnections(), mc.f_91073_, lookFace, c -> c.dimension(), c -> c.pos(), c -> c.boundFace()
                     );
                     Iterator var53 = previewTargets.iterator();

                     while (true) {
                        if (!var53.hasNext()) {
                           break label181;
                        }

                        BlockPos lookPos = (BlockPos)var53.next();
                        if (!existingConnections.contains(lookPos)) {
                           renderFaceOverlay(poseStack, buffer, cam, lookPos, lookFace, 1627389696);
                           renderLine(poseStack, buffer, cam, selectedPos, lookPos, lookFace, -1056964864);
                        }
                     }
                  }

                  if ("power_supply".equals(selectedHostType)
                     && selectedBex instanceof OverloadedPowerSupplyBlockEntity selectedPowerSupply
                     && mc.f_91077_ instanceof BlockHitResult bhr
                     && bhr.m_6662_() == Type.BLOCK
                     && !bhr.m_82425_().equals(selectedPos)
                     && mc.f_91073_.m_7702_(bhr.m_82425_()) != null) {
                     Set<BlockPos> previewTargets = WirelessConnectorTargetHelper.collectTargets(mc.f_91073_, bhr.m_82425_(), Screen.m_96637_());
                     Direction lookFace = bhr.m_82434_();
                     Set<BlockPos> existingConnections = collectConnectionsForFace(
                        selectedPowerSupply.getConnections(), mc.f_91073_, lookFace, c -> c.dimension(), c -> c.pos(), c -> c.boundFace()
                     );

                     for (BlockPos lookPos : previewTargets) {
                        if (!existingConnections.contains(lookPos)) {
                           renderFaceOverlay(poseStack, buffer, cam, lookPos, lookFace, 1627389696);
                           renderLine(poseStack, buffer, cam, selectedPos, lookPos, lookFace, -1056964864);
                        }
                     }
                  }
               }

               buffer.m_109912_(Ae2ltRenderTypes.getFaceSeeThrough());
               buffer.m_109912_(OverlayRenderType.getBlockHilightFace());
               buffer.m_109912_(OverlayRenderType.getBlockHilightLine());
            }
         }
      }
   }

   private static void renderInnerCube(PoseStack poseStack, MultiBufferSource buffer, Vec3 cam, BlockPos pos, int color) {
      VertexConsumer vc = buffer.m_6299_(Ae2ltRenderTypes.getFaceSeeThrough());
      int[] c = OverlayRenderType.decomposeColor(color);
      poseStack.m_85836_();
      poseStack.m_85837_((double)pos.m_123341_() - cam.f_82479_, (double)pos.m_123342_() - cam.f_82480_, (double)pos.m_123343_() - cam.f_82481_);
      Matrix4f mat = poseStack.m_85850_().m_252922_();
      float lo = 0.25F;
      float hi = 0.75F;
      quad(vc, mat, c, lo, lo, lo, hi, lo, lo, hi, lo, hi, lo, lo, hi, 0.0F, -1.0F, 0.0F);
      quad(vc, mat, c, lo, hi, hi, hi, hi, hi, hi, hi, lo, lo, hi, lo, 0.0F, 1.0F, 0.0F);
      quad(vc, mat, c, lo, lo, lo, lo, hi, lo, hi, hi, lo, hi, lo, lo, 0.0F, 0.0F, -1.0F);
      quad(vc, mat, c, hi, lo, hi, hi, hi, hi, lo, hi, hi, lo, lo, hi, 0.0F, 0.0F, 1.0F);
      quad(vc, mat, c, lo, lo, hi, lo, hi, hi, lo, hi, lo, lo, lo, lo, -1.0F, 0.0F, 0.0F);
      quad(vc, mat, c, hi, lo, lo, hi, hi, lo, hi, hi, hi, hi, lo, hi, 1.0F, 0.0F, 0.0F);
      poseStack.m_85849_();
   }

   private static void quad(
      VertexConsumer vc,
      Matrix4f mat,
      int[] c,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      float x4,
      float y4,
      float z4,
      float nx,
      float ny,
      float nz
   ) {
      vc.m_252986_(mat, x1, y1, z1).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
      vc.m_252986_(mat, x2, y2, z2).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
      vc.m_252986_(mat, x3, y3, z3).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
      vc.m_252986_(mat, x4, y4, z4).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
   }

   private static void renderProviderHost(
      PoseStack poseStack, MultiBufferSource buffer, Vec3 cam, Level level, BlockPos hostPos, WirelessPatternProviderHost provider, boolean selected
   ) {
      renderInnerCube(poseStack, buffer, cam, hostPos, selected ? -2130706688 : -2147450625);

      for (WirelessConnectionRef conn : provider.getConnections()) {
         if (conn.dimension().equals(level.m_46472_())) {
            renderFaceOverlay(poseStack, buffer, cam, conn.pos(), conn.boundFace(), 1610645759);
            renderLine(poseStack, buffer, cam, hostPos, conn.pos(), conn.boundFace(), -1073708801);
         }
      }
   }

   private static void renderInterfaceHost(
      PoseStack poseStack, MultiBufferSource buffer, Vec3 cam, Level level, BlockPos hostPos, OverloadedInterfaceBlockEntity iface, boolean selected
   ) {
      renderInnerCube(poseStack, buffer, cam, hostPos, selected ? -2130706688 : -2147450625);

      for (OverloadedInterfaceBlockEntity.WirelessConnection conn : iface.getConnections()) {
         if (conn.dimension().equals(level.m_46472_())) {
            renderFaceOverlay(poseStack, buffer, cam, conn.pos(), conn.boundFace(), 1610645759);
            renderLine(poseStack, buffer, cam, hostPos, conn.pos(), conn.boundFace(), -1073708801);
         }
      }
   }

   private static void renderPowerSupplyHost(
      PoseStack poseStack, MultiBufferSource buffer, Vec3 cam, Level level, BlockPos hostPos, OverloadedPowerSupplyBlockEntity powerSupply, boolean selected
   ) {
      renderInnerCube(poseStack, buffer, cam, hostPos, selected ? -2130706688 : -2147450625);

      for (OverloadedPowerSupplyBlockEntity.WirelessConnection conn : powerSupply.getConnections()) {
         if (conn.dimension().equals(level.m_46472_())) {
            renderFaceOverlay(poseStack, buffer, cam, conn.pos(), conn.boundFace(), 1610645759);
            renderLine(poseStack, buffer, cam, hostPos, conn.pos(), conn.boundFace(), -1073708801);
         }
      }
   }

   private static void renderFaceOverlay(PoseStack poseStack, MultiBufferSource buffer, Vec3 cam, BlockPos pos, Direction face, int color) {
      renderFaceOverlayInternal(poseStack, buffer.m_6299_(OverlayRenderType.getBlockHilightFace()), cam, pos, face, color);
   }

   private static void renderFaceOverlayInternal(PoseStack poseStack, VertexConsumer vc, Vec3 cam, BlockPos pos, Direction face, int color) {
      int[] c = OverlayRenderType.decomposeColor(color);
      poseStack.m_85836_();
      poseStack.m_85837_((double)pos.m_123341_() - cam.f_82479_, (double)pos.m_123342_() - cam.f_82480_, (double)pos.m_123343_() - cam.f_82481_);
      Matrix4f mat = poseStack.m_85850_().m_252922_();
      float offset = 0.001F;
      float nx = (float)face.m_122429_();
      float ny = (float)face.m_122430_();
      float nz = (float)face.m_122431_();
      switch (face) {
         case DOWN:
            vc.m_252986_(mat, 0.0F, -offset, 0.0F).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            vc.m_252986_(mat, 1.0F, -offset, 0.0F).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            vc.m_252986_(mat, 1.0F, -offset, 1.0F).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            vc.m_252986_(mat, 0.0F, -offset, 1.0F).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            break;
         case UP:
            vc.m_252986_(mat, 0.0F, 1.0F + offset, 1.0F).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            vc.m_252986_(mat, 1.0F, 1.0F + offset, 1.0F).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            vc.m_252986_(mat, 1.0F, 1.0F + offset, 0.0F).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            vc.m_252986_(mat, 0.0F, 1.0F + offset, 0.0F).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            break;
         case NORTH:
            vc.m_252986_(mat, 0.0F, 0.0F, -offset).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            vc.m_252986_(mat, 0.0F, 1.0F, -offset).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            vc.m_252986_(mat, 1.0F, 1.0F, -offset).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            vc.m_252986_(mat, 1.0F, 0.0F, -offset).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            break;
         case SOUTH:
            vc.m_252986_(mat, 1.0F, 0.0F, 1.0F + offset).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            vc.m_252986_(mat, 1.0F, 1.0F, 1.0F + offset).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            vc.m_252986_(mat, 0.0F, 1.0F, 1.0F + offset).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            vc.m_252986_(mat, 0.0F, 0.0F, 1.0F + offset).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            break;
         case WEST:
            vc.m_252986_(mat, -offset, 0.0F, 1.0F).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            vc.m_252986_(mat, -offset, 1.0F, 1.0F).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            vc.m_252986_(mat, -offset, 1.0F, 0.0F).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            vc.m_252986_(mat, -offset, 0.0F, 0.0F).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            break;
         case EAST:
            vc.m_252986_(mat, 1.0F + offset, 0.0F, 0.0F).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            vc.m_252986_(mat, 1.0F + offset, 1.0F, 0.0F).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            vc.m_252986_(mat, 1.0F + offset, 1.0F, 1.0F).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
            vc.m_252986_(mat, 1.0F + offset, 0.0F, 1.0F).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
      }

      poseStack.m_85849_();
   }

   private static void renderLine(PoseStack poseStack, MultiBufferSource buffer, Vec3 cam, BlockPos from, BlockPos to, Direction face, int color) {
      VertexConsumer vc = buffer.m_6299_(OverlayRenderType.getBlockHilightLine());
      int[] c = OverlayRenderType.decomposeColor(color);
      Matrix4f mat = poseStack.m_85850_().m_252922_();
      float fx = (float)((double)from.m_123341_() + 0.5 - cam.f_82479_);
      float fy = (float)((double)from.m_123342_() + 0.5 - cam.f_82480_);
      float fz = (float)((double)from.m_123343_() + 0.5 - cam.f_82481_);
      float tx = (float)((double)to.m_123341_() + 0.5 + (double)face.m_122429_() * 0.501 - cam.f_82479_);
      float ty = (float)((double)to.m_123342_() + 0.5 + (double)face.m_122430_() * 0.501 - cam.f_82480_);
      float tz = (float)((double)to.m_123343_() + 0.5 + (double)face.m_122431_() * 0.501 - cam.f_82481_);
      float dx = tx - fx;
      float dy = ty - fy;
      float dz = tz - fz;
      float len = (float)Math.sqrt((double)(dx * dx + dy * dy + dz * dz));
      if (!(len < 1.0E-6F)) {
         float nx = dx / len;
         float ny = dy / len;
         float nz = dz / len;
         vc.m_252986_(mat, fx, fy, fz).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
         vc.m_252986_(mat, tx, ty, tz).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
      }
   }

   private static <T> Set<BlockPos> collectConnectionsForFace(
      Iterable<T> connections,
      Level level,
      Direction face,
      Function<T, ResourceKey<Level>> dimensionGetter,
      Function<T, BlockPos> posGetter,
      Function<T, Direction> faceGetter
   ) {
      scratchConnectionSet.clear();

      for (T conn : connections) {
         if (dimensionGetter.apply(conn).equals(level.m_46472_()) && faceGetter.apply(conn) == face) {
            scratchConnectionSet.add(posGetter.apply(conn));
         }
      }

      return scratchConnectionSet;
   }

   private static void rescanHosts(ClientLevel level, BlockPos playerPos) {
      cachedHostPositions.clear();
      int minCX = playerPos.m_123341_() - 64 >> 4;
      int maxCX = playerPos.m_123341_() + 64 >> 4;
      int minCZ = playerPos.m_123343_() - 64 >> 4;
      int maxCZ = playerPos.m_123343_() + 64 >> 4;

      for (int cx = minCX; cx <= maxCX; cx++) {
         for (int cz = minCZ; cz <= maxCZ; cz++) {
            if (level.m_7232_(cx, cz)) {
               LevelChunk chunk = level.m_6325_(cx, cz);

               for (BlockPos bePos : chunk.m_5928_()) {
                  BlockEntity be = chunk.m_7702_(bePos);
                  if (be instanceof WirelessPatternProviderHost
                     || be instanceof OverloadedInterfaceBlockEntity
                     || be instanceof OverloadedPowerSupplyBlockEntity) {
                     cachedHostPositions.add(bePos.m_7949_());
                  }
               }
            }
         }
      }
   }

   public static ItemStack getHeldConnectorStack() {
      LocalPlayer player = Minecraft.m_91087_().f_91074_;
      if (player == null) {
         return ItemStack.f_41583_;
      } else {
         for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.m_21120_(hand);
            if (held.m_41720_() instanceof OverloadedWirelessConnectorItem) {
               return held;
            }
         }

         return ItemStack.f_41583_;
      }
   }

   public static boolean isSelectedHost(ItemStack stack, Level level, BlockPos pos, String hostType) {
      WirelessConnectorRenderer.SelectedHost selected = getSelectedHost(stack);
      return selected != null && selected.hostType().equals(hostType) && selected.pos().equals(pos) && level.m_46472_().equals(selected.dimension());
   }

   private static WirelessConnectorRenderer.SelectedHost getSelectedHost(ItemStack stack) {
      CompoundTag tag = ItemStackTagSupport.getTagCopy(stack);
      if (!tag.m_128425_("SelectedProvider", 10)) {
         return null;
      } else {
         CompoundTag sel = tag.m_128469_("SelectedProvider");
         String dimStr = sel.m_128461_("Dim");
         return dimStr.isEmpty()
            ? null
            : new WirelessConnectorRenderer.SelectedHost(
               BlockPos.m_122022_(sel.m_128454_("Pos")),
               ResourceKey.m_135785_(Registries.f_256858_, ResourceLocation.m_135820_(dimStr)),
               sel.m_128425_("HostType", 8) ? sel.m_128461_("HostType") : "provider"
            );
      }
   }

   @SubscribeEvent
   public static void onLoggingOut(LoggingOut event) {
      cachedHostPositions.clear();
      scratchConnectionSet.clear();
      lastScanTick = -1L;
      lastScanDimension = null;
   }

   private static record SelectedHost(BlockPos pos, ResourceKey<Level> dimension, String hostType) {
   }
}
