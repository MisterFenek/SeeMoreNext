package com.seemorenext.seemorenext.geyser;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

public class GeyserRenderDistanceProvider {
    private final Logger logger;
    private boolean available = false;
    private boolean initialized = false;

    private Class<?> geyserApiClass;
    private Method apiMethod;
    private Method isBedrockMethod;
    private Method connectionByUuidMethod;
    private Method javaUuidMethod;
    private Field clientRenderDistanceField;

    public GeyserRenderDistanceProvider(Logger logger) {
        this.logger = logger;
    }

    public boolean isAvailable() {
        if (!initialized) {
            initialized = true;
            initialize();
        }
        return available;
    }

    private void initialize() {
        try {
            geyserApiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
            apiMethod = geyserApiClass.getMethod("api");
            isBedrockMethod = geyserApiClass.getMethod("isBedrockPlayer", UUID.class);
            connectionByUuidMethod = geyserApiClass.getMethod("connectionByUuid", UUID.class);

            Class<?> connectionClass = Class.forName("org.geysermc.api.connection.Connection");
            javaUuidMethod = connectionClass.getMethod("javaUuid");

            Class<?> geyserSessionClass = Class.forName("org.geysermc.geyser.session.GeyserSession");
            clientRenderDistanceField = geyserSessionClass.getDeclaredField("clientRenderDistance");
            clientRenderDistanceField.setAccessible(true);

            Object api = apiMethod.invoke(null);
            if (api != null) {
                available = true;
                logger.info("Geyser detected. Bedrock render distance tracking enabled.");
            }
        } catch (ClassNotFoundException ignored) {
            // Geyser not installed
        } catch (Throwable t) {
            logger.warning("Geyser found but reflection failed: " + t.getMessage());
        }
    }

    public boolean isBedrockPlayer(UUID uuid) {
        if (!isAvailable()) return false;
        try {
            Object api = apiMethod.invoke(null);
            return Boolean.TRUE.equals(isBedrockMethod.invoke(api, uuid));
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Returns the real Bedrock client render distance, or -1 if unavailable.
     */
    public int getBedrockRenderDistance(UUID uuid) {
        if (!isAvailable()) return -1;
        try {
            Object api = apiMethod.invoke(null);
            Object connection = connectionByUuidMethod.invoke(api, uuid);
            if (connection == null) return -1;
            return (int) clientRenderDistanceField.get(connection);
        } catch (Throwable t) {
            return -1;
        }
    }
}
