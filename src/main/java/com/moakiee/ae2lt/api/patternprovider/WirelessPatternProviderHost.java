package com.moakiee.ae2lt.api.patternprovider;

import com.moakiee.ae2lt.logic.wireless.support.WirelessConnectionRef;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface WirelessPatternProviderHost {
   BlockPos getProviderPos();

   boolean isWirelessProvider();

   List<? extends WirelessConnectionRef> getConnections();

   boolean addOrUpdateConnection(ResourceKey<Level> var1, BlockPos var2, Direction var3);

   boolean removeConnection(ResourceKey<Level> var1, BlockPos var2);

   int getMaxWirelessConnections();
}
