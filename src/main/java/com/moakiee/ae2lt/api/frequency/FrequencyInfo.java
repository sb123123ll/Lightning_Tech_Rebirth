package com.moakiee.ae2lt.api.frequency;

import java.util.UUID;

public record FrequencyInfo(int id, String name, int color, UUID owner, FrequencySecurity security) {
}
