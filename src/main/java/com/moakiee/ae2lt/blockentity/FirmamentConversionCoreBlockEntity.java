package com.moakiee.ae2lt.blockentity;

import com.moakiee.ae2lt.machine.firmament.FirmamentConversionAutomationInventory;
import com.moakiee.ae2lt.machine.firmament.FirmamentConversionInventory;
import com.moakiee.ae2lt.machine.firmament.recipe.FirmamentConversionLockedRecipe;
import com.moakiee.ae2lt.machine.firmament.recipe.FirmamentConversionRecipeCandidate;
import com.moakiee.ae2lt.machine.firmament.recipe.FirmamentConversionRecipeService;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModItems;
import com.moakiee.ae2lt.util.NativeStackDropHelper;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

public class FirmamentConversionCoreBlockEntity extends BlockEntity {
   private static final ResourceLocation FIRMAMENT_STARSHIP_ID = new ResourceLocation("ae2lt", "firmament_starship");
   private static final String TAG_INVENTORY = "Inventory";
   private static final String TAG_LOCKED_RECIPE = "LockedRecipe";
   private static final String TAG_PROGRESS = "Progress";
   private static final String TAG_INITIAL_LOOT_ROLLED = "InitialLootRolled";
   private final FirmamentConversionInventory inventory = new FirmamentConversionInventory(this::onInventoryChanged);
   private final FirmamentConversionAutomationInventory automationInventory = new FirmamentConversionAutomationInventory(this.inventory);
   private FirmamentConversionLockedRecipe lockedRecipe;
   private int progress;
   private boolean initialLootRolled;
   private Boolean insideStarship;

   public FirmamentConversionCoreBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.FIRMAMENT_CONVERSION_CORE.get(), pos, state);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, FirmamentConversionCoreBlockEntity be) {
      if (!level.m_5776_()) {
         be.tickServer();
      }
   }

   public FirmamentConversionInventory getInventory() {
      return this.inventory;
   }

   public IItemHandlerModifiable getAutomationInventory() {
      return this.automationInventory;
   }

   public int getProgress() {
      return this.progress;
   }

   public int getProcessTime() {
      return this.lockedRecipe != null ? this.lockedRecipe.processTime() : 0;
   }

   public boolean isInsideFirmamentStarship() {
      return this.canProcessHere();
   }

   public boolean hasInactiveSpiritCoreOutput() {
      for (int slot = 3; slot <= 6; slot++) {
         if (this.inventory.getStackInSlot(slot).m_150930_((Item)ModItems.INACTIVE_FIRMAMENT_SPIRIT_CORE.get())) {
            return true;
         }
      }

      return false;
   }

   public boolean insertHeldItem(Player player, InteractionHand hand) {
      ItemStack held = player.m_21120_(hand);
      if (held.m_41619_()) {
         return false;
      } else {
         ItemStack attempted = held.m_41777_();
         ItemStack remainder = this.automationInventory.insertItem(attempted, false);
         int inserted = held.m_41613_() - remainder.m_41613_();
         if (inserted <= 0) {
            return false;
         } else {
            if (!player.m_150110_().f_35937_) {
               held.m_41774_(inserted);
            }

            this.m_6596_();
            return true;
         }
      }
   }

   public boolean extractToPlayer(Player player) {
      ItemStack extracted = ItemStack.f_41583_;

      for (int slot = 3; slot <= 6; slot++) {
         extracted = this.inventory.extractItem(slot, 64, false);
         if (!extracted.m_41619_()) {
            break;
         }
      }

      if (extracted.m_41619_()) {
         for (int slotx = 0; slotx <= 2; slotx++) {
            extracted = this.inventory.extractItem(slotx, 64, false);
            if (!extracted.m_41619_()) {
               break;
            }
         }
      }

      if (extracted.m_41619_()) {
         return false;
      } else {
         ItemStack remainder = extracted.m_41777_();
         player.m_36356_(remainder);
         if (!remainder.m_41619_() && this.f_58857_ != null) {
            NativeStackDropHelper.popResource(this.f_58857_, this.f_58858_, remainder);
         }

         this.m_6596_();
         return true;
      }
   }

   public void addDrops(List<ItemStack> drops) {
      for (int slot = 0; slot < this.inventory.getSlots(); slot++) {
         ItemStack stack = this.inventory.getStackInSlot(slot);
         if (!stack.m_41619_()) {
            NativeStackDropHelper.addDrops(drops, stack);
         }
      }
   }

   public void dropContents(Level level, BlockPos pos) {
      for (int slot = 0; slot < this.inventory.getSlots(); slot++) {
         ItemStack stack = this.inventory.getStackInSlot(slot);
         if (!stack.m_41619_()) {
            NativeStackDropHelper.popResource(level, pos, stack);
         }
      }

      this.inventory.clear();
   }

   private void tickServer() {
      if (this.canProcessHere()) {
         if (this.lockedRecipe == null) {
            this.lockCurrentRecipe();
         } else {
            Optional<FirmamentConversionRecipeCandidate> candidate = FirmamentConversionRecipeService.findLockedRecipeMatch(
               this.f_58857_, this.inventory, this.lockedRecipe
            );
            if (candidate.isEmpty()) {
               this.abortProcessing();
            } else {
               this.progress++;
               if (this.progress >= this.lockedRecipe.processTime()) {
                  if (!this.completeLockedRecipe(this.lockedRecipe, candidate.get())) {
                     this.abortProcessing();
                  }
               } else {
                  this.m_6596_();
               }
            }
         }
      } else {
         if (this.lockedRecipe != null || this.progress != 0) {
            this.abortProcessing();
         }
      }
   }

   public void initializeNaturalLoot(RandomSource random) {
      if (!this.initialLootRolled) {
         this.initialLootRolled = true;
         if (random.m_188499_()) {
            this.inventory.insertRecipeOutput(new ItemStack((ItemLike)ModItems.INACTIVE_FIRMAMENT_SPIRIT_CORE.get()), false);
         }

         this.m_6596_();
      }
   }

   private Optional<FirmamentConversionLockedRecipe> lockCurrentRecipe() {
      Optional<FirmamentConversionRecipeCandidate> candidate = FirmamentConversionRecipeService.findFirstProcessable(this.f_58857_, this.inventory);
      if (candidate.isEmpty()) {
         return Optional.empty();
      } else {
         this.lockedRecipe = FirmamentConversionLockedRecipe.fromCandidate(candidate.get());
         this.progress = 0;
         this.m_6596_();
         return Optional.of(this.lockedRecipe);
      }
   }

   private boolean canProcessHere() {
      if (this.insideStarship != null) {
         return this.insideStarship;
      } else {
         this.insideStarship = this.computeInsideStarship();
         return this.insideStarship;
      }
   }

   private boolean computeInsideStarship() {
      if (this.f_58857_ instanceof ServerLevel serverLevel) {
         Structure structure = (Structure)serverLevel.m_9598_().m_175515_(Registries.f_256944_).m_7745_(FIRMAMENT_STARSHIP_ID);
         return structure == null ? false : serverLevel.m_215010_().m_220524_(this.f_58858_, structure).m_73603_();
      } else {
         return false;
      }
   }

   public boolean completeLockedRecipe(FirmamentConversionLockedRecipe lockedRecipe, FirmamentConversionRecipeCandidate candidate) {
      List<ItemStack> results = lockedRecipe.results();
      if (!this.inventory.canAcceptRecipeOutputs(results)) {
         return false;
      } else {
         ItemStack[] extractedInputs = new ItemStack[3];

         for (int slot = 0; slot <= 2; slot++) {
            int toConsume = candidate.match().getConsumptionForSlot(slot);
            if (toConsume > 0) {
               ItemStack extracted = this.inventory.extractItem(slot, toConsume, false);
               if (extracted.m_41613_() != toConsume) {
                  this.rollbackInputs(extractedInputs);
                  if (!extracted.m_41619_()) {
                     this.inventory.insertItem(slot, extracted, false);
                  }

                  return false;
               }

               extractedInputs[slot] = extracted;
            }
         }

         if (!this.inventory.insertRecipeOutputs(results)) {
            this.rollbackInputs(extractedInputs);
            return false;
         } else {
            this.clearLockedRecipe();
            this.progress = 0;
            this.m_6596_();
            return true;
         }
      }
   }

   private void rollbackInputs(ItemStack[] extractedInputs) {
      for (int slot = 0; slot <= 2; slot++) {
         ItemStack extracted = extractedInputs[slot];
         if (extracted != null && !extracted.m_41619_()) {
            this.inventory.insertItem(slot, extracted, false);
         }
      }
   }

   private void abortProcessing() {
      this.clearLockedRecipe();
      this.progress = 0;
      this.m_6596_();
   }

   private void clearLockedRecipe() {
      this.lockedRecipe = null;
   }

   private void onInventoryChanged() {
      this.m_6596_();
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      this.inventory.saveToTag(tag, "Inventory");
      tag.m_128405_("Progress", this.progress);
      tag.m_128379_("InitialLootRolled", this.initialLootRolled);
      if (this.lockedRecipe != null) {
         tag.m_128365_("LockedRecipe", this.lockedRecipe.toTag());
      } else {
         tag.m_128473_("LockedRecipe");
      }
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.inventory.loadFromTag(tag, "Inventory");
      this.progress = Math.max(0, tag.m_128451_("Progress"));
      this.initialLootRolled = tag.m_128471_("InitialLootRolled");
      if (tag.m_128425_("LockedRecipe", 10)) {
         this.lockedRecipe = FirmamentConversionLockedRecipe.fromTag(tag.m_128469_("LockedRecipe"));
      } else {
         this.lockedRecipe = null;
      }

      if (this.lockedRecipe == null) {
         this.progress = 0;
      } else {
         this.progress = Math.min(this.progress, this.lockedRecipe.processTime());
      }
   }

   public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
      return cap == ForgeCapabilities.ITEM_HANDLER ? LazyOptional.of(this::getAutomationInventory).cast() : super.getCapability(cap, side);
   }

   public void clearContent() {
      this.inventory.clear();
      this.clearLockedRecipe();
      this.progress = 0;
   }
}
