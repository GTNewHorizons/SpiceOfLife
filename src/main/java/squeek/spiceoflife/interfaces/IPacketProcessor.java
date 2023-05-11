package squeek.spiceoflife.interfaces;

import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.relauncher.Side;
import squeek.spiceoflife.network.PacketBase;

public interface IPacketProcessor {

    PacketBase processAndReply(Side side, EntityPlayer player);
}
