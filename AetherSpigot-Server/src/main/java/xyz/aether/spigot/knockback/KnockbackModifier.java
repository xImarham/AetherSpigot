package xyz.aether.spigot.knockback;

import xyz.aether.spigot.util.YamlConfig;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;


public class KnockbackModifier<T> {

    private final Class<?> type;
    private final String label;
    private T value;

    public KnockbackModifier(Class<?> type, String label, T def) {
        this.type = type;
        this.label = label;
        value = def;
    }

    public void writeToConfig(KnockbackProfile profile, YamlConfig config, boolean save) {
        // Update the value in the config
        config.getConfig().set(String.format("profiles.%s.modifiers.%s", profile.getName(), label), value);

        if (save) {
            // Save the config file asynchronously since I/O actions should be async
            CompletableFuture.runAsync(() -> {
                // Wait for other configs to save first since they can overlap and cause issues
                synchronized (config) {
                    try {
                        config.getConfig().save(config.getFile());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public final void setValue(T value) {
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public void setValueUnsafe(Object value) {
        this.value = (T) value;
    }

    public T getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public Class<?> getType() {
        return type;
    }
}
