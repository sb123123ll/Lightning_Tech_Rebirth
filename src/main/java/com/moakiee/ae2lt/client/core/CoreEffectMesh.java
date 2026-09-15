package com.moakiee.ae2lt.client.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;

final class CoreEffectMesh {
   private static final int SPHERE_LONGITUDES = 24;
   private static final int SPHERE_LATITUDES = 12;
   private static final int RING_SEGMENTS = 48;

   private CoreEffectMesh() {
   }

   static void sphere(PoseStack stack, VertexConsumer consumer, float radius, float r, float g, float b, float alpha) {
      for (int latitude = 0; latitude < 12; latitude++) {
         double theta0 = (-Math.PI / 2) + Math.PI * (double)latitude / 12.0;
         double theta1 = (-Math.PI / 2) + Math.PI * (double)(latitude + 1) / 12.0;

         for (int longitude = 0; longitude < 24; longitude++) {
            double phi0 = (Math.PI * 2) * (double)longitude / 24.0;
            double phi1 = (Math.PI * 2) * (double)(longitude + 1) / 24.0;
            CoreEffectMesh.Vertex a = sphereVertex(theta0, phi0, radius);
            CoreEffectMesh.Vertex b0 = sphereVertex(theta1, phi0, radius);
            CoreEffectMesh.Vertex c = sphereVertex(theta1, phi1, radius);
            CoreEffectMesh.Vertex d = sphereVertex(theta0, phi1, radius);
            triangle(stack, consumer, a, b0, c, r, g, b, alpha);
            triangle(stack, consumer, a, c, d, r, g, b, alpha);
         }
      }
   }

   static void ringBand(
      PoseStack stack, VertexConsumer consumer, float innerRadius, float outerRadius, float halfThickness, float r, float g, float b, float alpha
   ) {
      for (int segment = 0; segment < 48; segment++) {
         double angle0 = (double)segment / 48.0 * Math.PI * 2.0;
         double angle1 = (double)(segment + 1) / 48.0 * Math.PI * 2.0;
         float cos0 = (float)Math.cos(angle0);
         float sin0 = (float)Math.sin(angle0);
         float cos1 = (float)Math.cos(angle1);
         float sin1 = (float)Math.sin(angle1);
         quad(
            stack,
            consumer,
            ringVertex(angle0, innerRadius, halfThickness, 0.0F, 1.0F, 0.0F),
            ringVertex(angle0, outerRadius, halfThickness, 0.0F, 1.0F, 0.0F),
            ringVertex(angle1, outerRadius, halfThickness, 0.0F, 1.0F, 0.0F),
            ringVertex(angle1, innerRadius, halfThickness, 0.0F, 1.0F, 0.0F),
            r,
            g,
            b,
            alpha
         );
         quad(
            stack,
            consumer,
            ringVertex(angle1, innerRadius, -halfThickness, 0.0F, -1.0F, 0.0F),
            ringVertex(angle1, outerRadius, -halfThickness, 0.0F, -1.0F, 0.0F),
            ringVertex(angle0, outerRadius, -halfThickness, 0.0F, -1.0F, 0.0F),
            ringVertex(angle0, innerRadius, -halfThickness, 0.0F, -1.0F, 0.0F),
            r,
            g,
            b,
            alpha
         );
         quad(
            stack,
            consumer,
            ringVertex(angle0, outerRadius, -halfThickness, cos0, 0.0F, sin0),
            ringVertex(angle1, outerRadius, -halfThickness, cos1, 0.0F, sin1),
            ringVertex(angle1, outerRadius, halfThickness, cos1, 0.0F, sin1),
            ringVertex(angle0, outerRadius, halfThickness, cos0, 0.0F, sin0),
            r,
            g,
            b,
            alpha
         );
         quad(
            stack,
            consumer,
            ringVertex(angle1, innerRadius, -halfThickness, -cos1, 0.0F, -sin1),
            ringVertex(angle0, innerRadius, -halfThickness, -cos0, 0.0F, -sin0),
            ringVertex(angle0, innerRadius, halfThickness, -cos0, 0.0F, -sin0),
            ringVertex(angle1, innerRadius, halfThickness, -cos1, 0.0F, -sin1),
            r,
            g,
            b,
            alpha
         );
      }
   }

   static void octahedron(PoseStack stack, VertexConsumer consumer, float radius, float r, float g, float b, float alpha) {
      CoreEffectMesh.Vertex top = new CoreEffectMesh.Vertex(0.0F, radius, 0.0F, 0.0F, 1.0F, 0.0F);
      CoreEffectMesh.Vertex bottom = new CoreEffectMesh.Vertex(0.0F, -radius, 0.0F, 0.0F, -1.0F, 0.0F);
      CoreEffectMesh.Vertex east = new CoreEffectMesh.Vertex(radius, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F);
      CoreEffectMesh.Vertex west = new CoreEffectMesh.Vertex(-radius, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F);
      CoreEffectMesh.Vertex south = new CoreEffectMesh.Vertex(0.0F, 0.0F, radius, 0.0F, 0.0F, 1.0F);
      CoreEffectMesh.Vertex north = new CoreEffectMesh.Vertex(0.0F, 0.0F, -radius, 0.0F, 0.0F, -1.0F);
      triangle(stack, consumer, top, south, east, r, g, b, alpha);
      triangle(stack, consumer, top, west, south, r, g, b, alpha);
      triangle(stack, consumer, top, north, west, r, g, b, alpha);
      triangle(stack, consumer, top, east, north, r, g, b, alpha);
      triangle(stack, consumer, bottom, east, south, r, g, b, alpha);
      triangle(stack, consumer, bottom, south, west, r, g, b, alpha);
      triangle(stack, consumer, bottom, west, north, r, g, b, alpha);
      triangle(stack, consumer, bottom, north, east, r, g, b, alpha);
   }

   static void cube(PoseStack stack, VertexConsumer consumer, float halfSize, float r, float g, float b, float alpha) {
      float low = -halfSize;
      quad(
         stack,
         consumer,
         new CoreEffectMesh.Vertex(low, low, halfSize, 0.0F, 0.0F, 1.0F),
         new CoreEffectMesh.Vertex(halfSize, low, halfSize, 0.0F, 0.0F, 1.0F),
         new CoreEffectMesh.Vertex(halfSize, halfSize, halfSize, 0.0F, 0.0F, 1.0F),
         new CoreEffectMesh.Vertex(low, halfSize, halfSize, 0.0F, 0.0F, 1.0F),
         r,
         g,
         b,
         alpha
      );
      quad(
         stack,
         consumer,
         new CoreEffectMesh.Vertex(halfSize, low, low, 0.0F, 0.0F, -1.0F),
         new CoreEffectMesh.Vertex(low, low, low, 0.0F, 0.0F, -1.0F),
         new CoreEffectMesh.Vertex(low, halfSize, low, 0.0F, 0.0F, -1.0F),
         new CoreEffectMesh.Vertex(halfSize, halfSize, low, 0.0F, 0.0F, -1.0F),
         r,
         g,
         b,
         alpha
      );
      quad(
         stack,
         consumer,
         new CoreEffectMesh.Vertex(halfSize, low, halfSize, 1.0F, 0.0F, 0.0F),
         new CoreEffectMesh.Vertex(halfSize, low, low, 1.0F, 0.0F, 0.0F),
         new CoreEffectMesh.Vertex(halfSize, halfSize, low, 1.0F, 0.0F, 0.0F),
         new CoreEffectMesh.Vertex(halfSize, halfSize, halfSize, 1.0F, 0.0F, 0.0F),
         r,
         g,
         b,
         alpha
      );
      quad(
         stack,
         consumer,
         new CoreEffectMesh.Vertex(low, low, low, -1.0F, 0.0F, 0.0F),
         new CoreEffectMesh.Vertex(low, low, halfSize, -1.0F, 0.0F, 0.0F),
         new CoreEffectMesh.Vertex(low, halfSize, halfSize, -1.0F, 0.0F, 0.0F),
         new CoreEffectMesh.Vertex(low, halfSize, low, -1.0F, 0.0F, 0.0F),
         r,
         g,
         b,
         alpha
      );
      quad(
         stack,
         consumer,
         new CoreEffectMesh.Vertex(low, halfSize, halfSize, 0.0F, 1.0F, 0.0F),
         new CoreEffectMesh.Vertex(halfSize, halfSize, halfSize, 0.0F, 1.0F, 0.0F),
         new CoreEffectMesh.Vertex(halfSize, halfSize, low, 0.0F, 1.0F, 0.0F),
         new CoreEffectMesh.Vertex(low, halfSize, low, 0.0F, 1.0F, 0.0F),
         r,
         g,
         b,
         alpha
      );
      quad(
         stack,
         consumer,
         new CoreEffectMesh.Vertex(low, low, low, 0.0F, -1.0F, 0.0F),
         new CoreEffectMesh.Vertex(halfSize, low, low, 0.0F, -1.0F, 0.0F),
         new CoreEffectMesh.Vertex(halfSize, low, halfSize, 0.0F, -1.0F, 0.0F),
         new CoreEffectMesh.Vertex(low, low, halfSize, 0.0F, -1.0F, 0.0F),
         r,
         g,
         b,
         alpha
      );
   }

   private static CoreEffectMesh.Vertex sphereVertex(double theta, double phi, float radius) {
      float cosTheta = (float)Math.cos(theta);
      float nx = cosTheta * (float)Math.cos(phi);
      float ny = (float)Math.sin(theta);
      float nz = cosTheta * (float)Math.sin(phi);
      return new CoreEffectMesh.Vertex(nx * radius, ny * radius, nz * radius, nx, ny, nz);
   }

   private static CoreEffectMesh.Vertex ringVertex(double angle, float radius, float y, float nx, float ny, float nz) {
      return new CoreEffectMesh.Vertex(radius * (float)Math.cos(angle), y, radius * (float)Math.sin(angle), nx, ny, nz);
   }

   private static void triangle(
      PoseStack stack,
      VertexConsumer consumer,
      CoreEffectMesh.Vertex a,
      CoreEffectMesh.Vertex b,
      CoreEffectMesh.Vertex c,
      float r,
      float g,
      float blue,
      float alpha
   ) {
      vertex(stack, consumer, a, r, g, blue, alpha);
      vertex(stack, consumer, b, r, g, blue, alpha);
      vertex(stack, consumer, c, r, g, blue, alpha);
   }

   private static void quad(
      PoseStack stack,
      VertexConsumer consumer,
      CoreEffectMesh.Vertex a,
      CoreEffectMesh.Vertex b,
      CoreEffectMesh.Vertex c,
      CoreEffectMesh.Vertex d,
      float r,
      float g,
      float blue,
      float alpha
   ) {
      triangle(stack, consumer, a, b, c, r, g, blue, alpha);
      triangle(stack, consumer, a, c, d, r, g, blue, alpha);
   }

   private static void vertex(PoseStack stack, VertexConsumer consumer, CoreEffectMesh.Vertex vertex, float r, float g, float b, float alpha) {
      Pose pose = stack.m_85850_();
      consumer.m_252986_(pose.m_252922_(), vertex.x, vertex.y, vertex.z)
         .m_85950_(r, g, b, alpha)
         .m_252939_(pose.m_252943_(), vertex.nx, vertex.ny, vertex.nz)
         .m_5752_();
   }

   private static record Vertex(float x, float y, float z, float nx, float ny, float nz) {
   }
}
