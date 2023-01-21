package io.github.czm23333.onemonitor.minecraft.oneprobe;

public class ResourceLocation {
    public String a;
    public String b;

    public ResourceLocation(String a, String b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString() {
        return "[%s, %s]".formatted(a, b);
    }
}