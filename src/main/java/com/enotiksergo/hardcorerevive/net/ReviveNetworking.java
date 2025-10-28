package com.enotiksergo.hardcorerevive.net;

import com.enotiksergo.hardcorerevive.HardcodeDisabler;
import com.enotiksergo.hardcorerevive.HardcoreHeartsFx;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.server.world.ServerWorld;

public final class ReviveNetworking {

    public static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(ReviveRequestC2S.ID, ReviveRequestC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(ReadyAfterTerrainC2S.ID, ReadyAfterTerrainC2S.CODEC);
        PayloadTypeRegistry.playS2C().register(FxBeginS2C.ID, FxBeginS2C.CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(ReviveRequestC2S.ID, (payload, context) -> {
            var server = context.server();
            var player = context.player();
            server.execute(() -> {
                com.enotiksergo.hardcorerevive.util.ReviveCoordinator.markWaiting(player.getUuid());

                server.execute(() -> {
                    ServerWorld world = player.getEntityWorld();
                    ChunkPos cpos = new ChunkPos(player.getBlockPos());
                    com.enotiksergo.hardcorerevive.util.ReviveCoordinator.addPreload(world, cpos, player.getUuid());
                });
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ReadyAfterTerrainC2S.ID, (payload, context) -> {
            var server = context.server();
            var player = context.player();
            server.execute(() -> {
                finalizeRevive(server, player);
                com.enotiksergo.hardcorerevive.util.ReviveCoordinator.removePreload(server, player.getUuid());
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

    private static void finalizeRevive(MinecraftServer server, ServerPlayerEntity player) {
        if (!com.enotiksergo.hardcorerevive.util.ReviveCoordinator.consumeWaiting(player.getUuid())) return;

        boolean converted = HardcodeDisabler.disableHardcore(server);
        player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);

        if (converted) {
            player.sendMessage(net.minecraft.text.Text.translatable("hardcorerevive.chat.revive"), false);
            HardcodeDisabler.notifyPlayerConverted(server, player.getUuid());
        }

        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new FxBeginS2C());
    }

    public record ReviveRequestC2S() implements CustomPayload {
        public static final Id<ReviveRequestC2S> ID =
                new Id<>(Identifier.of("hardcorerevive", "revive_request"));
        public static final PacketCodec<RegistryByteBuf, ReviveRequestC2S> CODEC =
                PacketCodec.of((buf, payload) -> {}, buf -> new ReviveRequestC2S());
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ReadyAfterTerrainC2S() implements CustomPayload {
        public static final Id<ReadyAfterTerrainC2S> ID =
                new Id<>(Identifier.of("hardcorerevive", "ready_after_terrain"));
        public static final PacketCodec<RegistryByteBuf, ReadyAfterTerrainC2S> CODEC =
                PacketCodec.of((buf, payload) -> {}, buf -> new ReadyAfterTerrainC2S());
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record FxBeginS2C() implements CustomPayload {
        public static final Id<FxBeginS2C> ID =
                new Id<>(Identifier.of("hardcorerevive", "fx_begin"));
        public static final PacketCodec<RegistryByteBuf, FxBeginS2C> CODEC =
                PacketCodec.of((buf, payload) -> {}, buf -> new FxBeginS2C());
        @Override public Id<? extends CustomPayload> getId() { return ID; }
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