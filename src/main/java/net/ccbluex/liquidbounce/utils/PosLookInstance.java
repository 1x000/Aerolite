package net.ccbluex.liquidbounce.utils;

import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class PosLookInstance {

    private double x = 0;
    private double y = 0;
    private double z = 0;
    private float yaw = 0;
    private float pitch = 0;

    public PosLookInstance() {}


    public void reset() {
        set(0, 0, 0, 0, 0);
    }

    public void set(S08PacketPlayerPosLook packet) {
        set(packet.getX(), packet.getY(), packet.getZ(), packet.yaw, packet.pitch);
    }

    public void set(double a, double b, double c, float d, float e) {
        this.x = a;
        this.y = b;
        this.z = c;
        this.yaw = d;
        this.pitch = e;
    }

}