package com.moakiee.ae2lt.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;

public class Ae2ltRenderTypes extends RenderType {
   private static RenderType FACE_SEE_THROUGH;

   private Ae2ltRenderTypes(
      String name, VertexFormat format, Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear
   ) {
      super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
   }

   public static RenderType getFaceSeeThrough() {
      if (FACE_SEE_THROUGH == null) {
         FACE_SEE_THROUGH = m_173215_(
            "ae2lt_face_see_through",
            DefaultVertexFormat.f_166851_,
            Mode.QUADS,
            65536,
            false,
            false,
            CompositeState.m_110628_()
               .m_110685_(f_110139_)
               .m_173290_(f_110147_)
               .m_110671_(f_110153_)
               .m_110663_(f_285579_)
               .m_110687_(f_110115_)
               .m_110661_(f_110110_)
               .m_173292_(f_173104_)
               .m_110691_(false)
         );
      }

      return FACE_SEE_THROUGH;
   }
}
