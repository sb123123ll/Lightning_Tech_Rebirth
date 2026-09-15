package com.moakiee.ae2lt.logic.energy;

import appeng.api.networking.IGridNode;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.IStorageProvider;
import appeng.api.storage.MEStorage;

public class BufferedStorageService implements IStorageService {
   private final IStorageService delegate;
   private final BufferedMEStorage bufferedStorage;

   public BufferedStorageService(IStorageService delegate, BufferedMEStorage bufferedStorage) {
      this.delegate = delegate;
      this.bufferedStorage = bufferedStorage;
   }

   public MEStorage getInventory() {
      return this.bufferedStorage;
   }

   public KeyCounter getCachedInventory() {
      return this.delegate.getCachedInventory();
   }

   public void addGlobalStorageProvider(IStorageProvider provider) {
      this.delegate.addGlobalStorageProvider(provider);
   }

   public void removeGlobalStorageProvider(IStorageProvider provider) {
      this.delegate.removeGlobalStorageProvider(provider);
   }

   public void refreshNodeStorageProvider(IGridNode node) {
      this.delegate.refreshNodeStorageProvider(node);
   }

   public void refreshGlobalStorageProvider(IStorageProvider provider) {
      this.delegate.refreshGlobalStorageProvider(provider);
   }

   public void invalidateCache() {
      this.delegate.invalidateCache();
   }

   public BufferedMEStorage getBufferedStorage() {
      return this.bufferedStorage;
   }
}
