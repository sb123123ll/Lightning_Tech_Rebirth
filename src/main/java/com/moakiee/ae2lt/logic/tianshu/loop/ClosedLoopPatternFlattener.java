package com.moakiee.ae2lt.logic.tianshu.loop;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import com.moakiee.ae2lt.item.ClosedLoopPatternItem;
import com.moakiee.ae2lt.overload.runtime.pattern.SourcePatternSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ClosedLoopPatternFlattener {
   public static final int MAX_NESTING_DEPTH = 8;
   static final int MAX_EXPANDED_NODES = 64;

   public static ClosedLoopPatternFlattener.Result flatten(List<ClosedLoopMemberPattern> draftMembers, Level level) {
      return level == null
         ? failure(ClosedLoopPatternFlattener.Status.INVALID_INPUT)
         : flatten(draftMembers, (ClosedLoopPatternFlattener.MemberResolver)(snapshot -> resolve(snapshot, level)));
   }

   static ClosedLoopPatternFlattener.Result flatten(List<ClosedLoopMemberPattern> draftMembers, ClosedLoopPatternFlattener.MemberResolver resolver) {
      if (draftMembers != null && !draftMembers.isEmpty() && resolver != null) {
         ClosedLoopPatternFlattener.ExpansionState state = new ClosedLoopPatternFlattener.ExpansionState(resolver);

         for (ClosedLoopMemberPattern member : draftMembers) {
            if (member == null) {
               return failure(ClosedLoopPatternFlattener.Status.INVALID_INPUT);
            }

            state.expand(member, 1L, 0);
            if (state.failure != null) {
               return failure(state.failure);
            }
         }

         if (state.leaves.isEmpty()) {
            return failure(ClosedLoopPatternFlattener.Status.INVALID_INPUT);
         } else if (!ClosedLoopPatternAnalyzer.isMinimalIntegerRatio(state.leaves.stream().mapToLong(leafx -> leafx.totalCopies).toArray())) {
            return failure(ClosedLoopPatternFlattener.Status.NON_MINIMAL_COPIES);
         } else {
            ArrayList<ClosedLoopPatternFlattener.LeafMember> flattened = new ArrayList<>(state.leaves.size());

            for (ClosedLoopPatternFlattener.RawLeaf leaf : state.leaves) {
               flattened.add(new ClosedLoopPatternFlattener.LeafMember(leaf.details, leaf.snapshot, leaf.totalCopies));
            }

            return new ClosedLoopPatternFlattener.Result(ClosedLoopPatternFlattener.Status.VALID, flattened);
         }
      } else {
         return failure(ClosedLoopPatternFlattener.Status.INVALID_INPUT);
      }
   }

   private static ClosedLoopPatternFlattener.ResolvedMember resolve(SourcePatternSnapshot snapshot, Level level) {
      try {
         ItemStack stack = snapshot.toItemStack();
         if (stack.m_41619_()) {
            return null;
         } else if (stack.m_41720_() instanceof ClosedLoopPatternItem item) {
            if (item.readExecutionMember(stack) >= 0) {
               return ClosedLoopPatternFlattener.ResolvedMember.executionReference();
            } else {
               ClosedLoopPatternPayload payload = item.readPayload(stack, level).orElse(null);
               return payload != null ? ClosedLoopPatternFlattener.ResolvedMember.macro(payload) : null;
            }
         } else {
            IPatternDetails details = PatternDetailsHelper.decodePattern(stack, level);
            if (details == null) {
               return null;
            } else {
               return details instanceof TianshuClosedLoopPatternDetails nested
                  ? ClosedLoopPatternFlattener.ResolvedMember.macro(nested.closedLoopPayload())
                  : ClosedLoopPatternFlattener.ResolvedMember.leaf(details);
            }
         }
      } catch (RuntimeException var5) {
         return null;
      }
   }

   private static ClosedLoopPatternFlattener.Result failure(ClosedLoopPatternFlattener.Status status) {
      return new ClosedLoopPatternFlattener.Result(status, List.of());
   }

   private ClosedLoopPatternFlattener() {
   }

   private static final class ExpansionState {
      private final ClosedLoopPatternFlattener.MemberResolver resolver;
      private final List<ClosedLoopPatternFlattener.RawLeaf> leaves = new ArrayList<>();
      private final Set<ClosedLoopPatternPayload> activePayloads = Collections.newSetFromMap(new IdentityHashMap<>());
      private int expandedNodes;
      private ClosedLoopPatternFlattener.Status failure;

      private ExpansionState(ClosedLoopPatternFlattener.MemberResolver resolver) {
         this.resolver = resolver;
      }

      private void expand(ClosedLoopMemberPattern member, long parentScale, int depth) {
         if (this.failure == null) {
            if (++this.expandedNodes > 64) {
               this.failure = ClosedLoopPatternFlattener.Status.TOO_MANY_MEMBERS;
            } else {
               long totalScale;
               try {
                  totalScale = Math.multiplyExact(parentScale, member.copiesPerCycle());
               } catch (ArithmeticException var16) {
                  this.failure = ClosedLoopPatternFlattener.Status.ARITHMETIC_OVERFLOW;
                  return;
               }

               ClosedLoopPatternFlattener.ResolvedMember resolved;
               try {
                  resolved = this.resolver.resolve(member.pattern());
               } catch (RuntimeException var15) {
                  this.failure = ClosedLoopPatternFlattener.Status.MEMBER_UNDECODABLE;
                  return;
               }

               if (resolved == null) {
                  this.failure = ClosedLoopPatternFlattener.Status.MEMBER_UNDECODABLE;
               } else if (resolved.executionMember()) {
                  this.failure = ClosedLoopPatternFlattener.Status.EXECUTION_MEMBER_REFERENCE;
               } else if (resolved.nestedPayload() == null) {
                  if (this.leaves.size() >= 27) {
                     this.failure = ClosedLoopPatternFlattener.Status.TOO_MANY_MEMBERS;
                  } else {
                     this.leaves.add(new ClosedLoopPatternFlattener.RawLeaf(resolved.details(), member.pattern(), totalScale));
                  }
               } else if (depth >= 8) {
                  this.failure = ClosedLoopPatternFlattener.Status.NESTING_TOO_DEEP;
               } else {
                  ClosedLoopPatternPayload payload = resolved.nestedPayload();
                  if (!this.activePayloads.add(payload)) {
                     this.failure = ClosedLoopPatternFlattener.Status.CYCLIC_REFERENCE;
                  } else {
                     try {
                        Iterator var9 = payload.memberPatterns().iterator();

                        do {
                           if (!var9.hasNext()) {
                              return;
                           }

                           ClosedLoopMemberPattern nestedMember = (ClosedLoopMemberPattern)var9.next();
                           this.expand(nestedMember, totalScale, depth + 1);
                        } while (this.failure == null);
                     } finally {
                        this.activePayloads.remove(payload);
                     }
                  }
               }
            }
         }
      }
   }

   public static record LeafMember(IPatternDetails details, SourcePatternSnapshot snapshot, long totalCopies) {
      public LeafMember(IPatternDetails details, SourcePatternSnapshot snapshot, long totalCopies) {
         Objects.requireNonNull(details, "details");
         Objects.requireNonNull(snapshot, "snapshot");
         if (totalCopies < 1L) {
            throw new IllegalArgumentException("invalid closed-loop leaf copies");
         } else {
            this.details = details;
            this.snapshot = snapshot;
            this.totalCopies = totalCopies;
         }
      }
   }

   @FunctionalInterface
   interface MemberResolver {
      ClosedLoopPatternFlattener.ResolvedMember resolve(SourcePatternSnapshot var1);
   }

   private static record RawLeaf(IPatternDetails details, SourcePatternSnapshot snapshot, long totalCopies) {
   }

   static record ResolvedMember(IPatternDetails details, ClosedLoopPatternPayload nestedPayload, boolean executionMember) {
      ResolvedMember(IPatternDetails details, ClosedLoopPatternPayload nestedPayload, boolean executionMember) {
         if (executionMember) {
            if (details != null || nestedPayload != null) {
               throw new IllegalArgumentException("execution member resolution is exclusive");
            }
         } else if (details == null == (nestedPayload == null)) {
            throw new IllegalArgumentException("resolved member must be exactly one kind");
         }

         this.details = details;
         this.nestedPayload = nestedPayload;
         this.executionMember = executionMember;
      }

      static ClosedLoopPatternFlattener.ResolvedMember leaf(IPatternDetails details) {
         return new ClosedLoopPatternFlattener.ResolvedMember(Objects.requireNonNull(details, "details"), null, false);
      }

      static ClosedLoopPatternFlattener.ResolvedMember macro(ClosedLoopPatternPayload payload) {
         return new ClosedLoopPatternFlattener.ResolvedMember(null, Objects.requireNonNull(payload, "payload"), false);
      }

      static ClosedLoopPatternFlattener.ResolvedMember executionReference() {
         return new ClosedLoopPatternFlattener.ResolvedMember(null, null, true);
      }
   }

   public static record Result(ClosedLoopPatternFlattener.Status status, List<ClosedLoopPatternFlattener.LeafMember> members) {
      public Result(ClosedLoopPatternFlattener.Status status, List<ClosedLoopPatternFlattener.LeafMember> members) {
         status = Objects.requireNonNull(status, "status");
         members = List.copyOf(members);
         if (status == ClosedLoopPatternFlattener.Status.VALID && members.isEmpty()) {
            throw new IllegalArgumentException("valid flatten result requires members");
         } else if (status != ClosedLoopPatternFlattener.Status.VALID && !members.isEmpty()) {
            throw new IllegalArgumentException("failed flatten result must not expose members");
         } else {
            this.status = status;
            this.members = members;
         }
      }

      public boolean valid() {
         return this.status == ClosedLoopPatternFlattener.Status.VALID;
      }
   }

   public static enum Status {
      VALID,
      INVALID_INPUT,
      MEMBER_UNDECODABLE,
      EXECUTION_MEMBER_REFERENCE,
      NESTING_TOO_DEEP,
      CYCLIC_REFERENCE,
      TOO_MANY_MEMBERS,
      NON_MINIMAL_COPIES,
      ARITHMETIC_OVERFLOW;
   }
}
