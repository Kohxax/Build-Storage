package dev.buildassist.mod.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StoragePayload(byte[] data) implements CustomPayload {

    public static final Id<StoragePayload> ID = new Id<>(Identifier.of("buildassist", "main"));

    public static final PacketCodec<PacketByteBuf, StoragePayload> CODEC = PacketCodec.of(
        (value, buf) -> buf.writeBytes(value.data()),
        buf -> {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return new StoragePayload(bytes);
        }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
