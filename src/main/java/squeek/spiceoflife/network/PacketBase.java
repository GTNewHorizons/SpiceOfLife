package squeek.spiceoflife.network;

import net.minecraft.network.Packet;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import squeek.spiceoflife.compat.ByteIO;
import squeek.spiceoflife.interfaces.IPackable;
import squeek.spiceoflife.interfaces.IPacketProcessor;

public abstract class PacketBase implements IMessage, IPackable, IPacketProcessor {

    public PacketBase() {}

    public Packet getPacket() {
        return PacketHandler.channel.getPacketFrom(this);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        unpack(ByteIO.get(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        pack(ByteIO.get(buf));
    }
}
