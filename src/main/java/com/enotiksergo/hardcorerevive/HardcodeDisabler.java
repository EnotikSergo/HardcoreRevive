package com.enotiksergo.hardcorerevive;

import com.enotiksergo.hardcorerevive.mixin.LevelPropertiesAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.LevelInfo;

public final class HardcodeDisabler {
    private HardcodeDisabler() {}

    public static boolean disableHardcore(MinecraftServer server) {
        SaveProperties sp = server.getSaveProperties();

        if (!(sp instanceof LevelPropertiesAccessor accessor)) {
            return false;
        }

        LevelInfo oldInfo = accessor.hardcorerevive$getLevelInfo_();
        if (!oldInfo.isHardcore()) {
            return false;
        }

        LevelInfo newInfo = new LevelInfo(
                oldInfo.getLevelName(),
                oldInfo.getGameMode(),
                false,
                oldInfo.getDifficulty(),
                oldInfo.areCommandsAllowed(),
                oldInfo.getGameRules(),
                oldInfo.getDataConfiguration()
        );

        accessor.hardcorerevive$setLevelInfo_(newInfo);
        return true;
    }

    public static void notifyPlayerConverted(MinecraftServer server, java.util.UUID playerId) {
        var p = server.getPlayerManager().getPlayer(playerId);
        if (p != null) {
            p.sendMessage(Text.translatable("hardcorerevive.chat.disabled"), false);
        }
    }
}
