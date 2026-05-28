package com.enotiksergo.hardcorerevive;

import com.enotiksergo.hardcorerevive.duck.LevelDataReviveExt;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.WorldData;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;

public final class HardcodeDisabler {
    private HardcodeDisabler() {}

    public static boolean disableHardcore(MinecraftServer server) {
        WorldData sp = server.getWorldData();

        if (!(sp instanceof LevelDataReviveExt accessor)) {
            return false;
        }

        LevelSettings oldSettings = accessor.hardcorerevive$getLevelInfo_();

        LevelSettings newSettings = (LevelSettings) deepCloneAndDisableHardcore(oldSettings);

        if (newSettings == null || newSettings == oldSettings) {
            return false;
        }

        accessor.hardcorerevive$setLevelInfo_(newSettings);
        return true;
    }

    private static Object deepCloneAndDisableHardcore(Object obj) {
        if (obj == null) return null;
        Class<?> cls = obj.getClass();

        if (!cls.isRecord()) {
            return obj;
        }

        try {
            RecordComponent[] components = cls.getRecordComponents();
            Class<?>[] ctorTypes = new Class<?>[components.length];
            Object[] ctorArgs = new Object[components.length];
            boolean changed = false;

            for (int i = 0; i < components.length; i++) {
                RecordComponent c = components[i];
                ctorTypes[i] = c.getType();
                Object value = c.getAccessor().invoke(obj);
                String name = c.getName().toLowerCase();

                if (c.getType() == boolean.class && name.contains("hardcore")) {
                    if ((Boolean) value) {
                        ctorArgs[i] = false; // Отключает хардкор
                        changed = true;
                    } else {
                        ctorArgs[i] = value;
                    }
                }
                else if (c.getType().isRecord()) {
                    Object newValue = deepCloneAndDisableHardcore(value);
                    ctorArgs[i] = newValue;
                    if (newValue != value) {
                        changed = true;
                    }
                }
                else {
                    ctorArgs[i] = value;
                }
            }

            if (!changed) {
                return obj;
            }

            Constructor<?> ctor = cls.getDeclaredConstructor(ctorTypes);
            ctor.setAccessible(true);
            return ctor.newInstance(ctorArgs);

        } catch (Throwable t) {
            t.printStackTrace();
            return obj;
        }
    }

    public static void notifyPlayerConverted(MinecraftServer server, java.util.UUID playerId) {
        var p = server.getPlayerList().getPlayer(playerId);
        if (p != null) {
            p.sendSystemMessage(Component.translatable("hardcorerevive.chat.disabled"));
        }
    }
}