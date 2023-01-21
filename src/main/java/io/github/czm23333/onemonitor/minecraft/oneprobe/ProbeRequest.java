package io.github.czm23333.onemonitor.minecraft.oneprobe;

import java.util.function.Consumer;

public record ProbeRequest(int dim, int x, int y, int z, Consumer<ProbeResponse> callback) {
}