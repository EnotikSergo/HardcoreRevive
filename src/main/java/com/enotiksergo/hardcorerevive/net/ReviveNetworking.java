package com.enotiksergo.hardcorerevive.net;

import com.enotiksergo.hardcorerevive.HardcodeDisabler;
import com.enotiksergo.hardcorerevive.HardcoreHeartsFx;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;

public final class ReviveNetworking {

    public static void registerPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(ReviveRequestC2S.ID, ReviveRequestC2S.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ReadyAfterTerrainC2S.ID, ReadyAfterTerrainC2S.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FxBeginS2C.ID, FxBeginS2C.CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(ReviveRequestC2S.ID, (payload, context) -> {
            var server = context.server();
            var player = context.player();
            server.execute(() -> {
                com.enotiksergo.hardcorerevive.util.ReviveCoordinator.markWaiting(player.getUUID());

                server.execute(() -> {
                    ServerLevel world = player.level();
                    ChunkPos cpos = ChunkPos.containing(player.blockPosition());
                    com.enotiksergo.hardcorerevive.util.ReviveCoordinator.addPreload(world, cpos, player.getUUID());
                });
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ReadyAfterTerrainC2S.ID, (payload, context) -> {
            var server = context.server();
            var player = context.player();
            server.execute(() -> {
                finalizeRevive(server, player);
                com.enotiksergo.hardcorerevive.util.ReviveCoordinator.removePreload(server, player.getUUID());
            });
        });
    }

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(FxBeginS2C.ID, (payload, context) ->
                context.client().execute(() -> {
                    HardcoreHeartsFx.ensureAttached();
                    HardcoreHeartsFx.begin();
                })
        );
    }

    public static void sendReviveRequest() {
        ClientPlayNetworking.send(new ReviveRequestC2S());
    }
    public static void sendReady() {
        ClientPlayNetworking.send(new ReadyAfterTerrainC2S());
    }

    private static void finalizeRevive(MinecraftServer server, ServerPlayer player) {
        if (!com.enotiksergo.hardcorerevive.util.ReviveCoordinator.consumeWaiting(player.getUUID())) return;

        boolean converted = HardcodeDisabler.disableHardcore(server);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);

        if (converted) {
            player.sendSystemMessage(Component.translatable("hardcorerevive.chat.revive"));
            HardcodeDisabler.notifyPlayerConverted(server, player.getUUID());
        }

        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new FxBeginS2C());
    }

    public record ReviveRequestC2S() implements CustomPacketPayload {
        public static final Type<ReviveRequestC2S> ID =
                new Type<>(Identifier.fromNamespaceAndPath("hardcorerevive", "revive_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ReviveRequestC2S> CODEC =
                StreamCodec.ofMember((buf, payload) -> {}, buf -> new ReviveRequestC2S());
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record ReadyAfterTerrainC2S() implements CustomPacketPayload {
        public static final Type<ReadyAfterTerrainC2S> ID =
                new Type<>(Identifier.fromNamespaceAndPath("hardcorerevive", "ready_after_terrain"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ReadyAfterTerrainC2S> CODEC =
                StreamCodec.ofMember((buf, payload) -> {}, buf -> new ReadyAfterTerrainC2S());
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record FxBeginS2C() implements CustomPacketPayload {
        public static final Type<FxBeginS2C> ID =
                new Type<>(Identifier.fromNamespaceAndPath("hardcorerevive", "fx_begin"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FxBeginS2C> CODEC =
                StreamCodec.ofMember((buf, payload) -> {}, buf -> new FxBeginS2C());
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    private static final class ServerToClient {
        private static void fxBeginRegister() {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                    FxBeginS2C.ID,
                    (payload, context) -> context.client().execute(
                            HardcoreHeartsFx::begin
                    )
            );
        }
    }

    private ReviveNetworking() {}
}