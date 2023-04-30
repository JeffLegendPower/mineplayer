package io.github.jefflegendpower.mineplayerclient.utils;

import com.google.common.base.Charsets;
import net.minecraft.network.PacketByteBuf;

public class StringByteUtils {

    public static String dataToString(PacketByteBuf data) {
        return new String(data.readByteArray(), Charsets.UTF_8);
    }

    public static void writeString(PacketByteBuf byteBuf, String data) {
        byteBuf.writeByteArray(data.getBytes(Charsets.UTF_8));
    }
}
