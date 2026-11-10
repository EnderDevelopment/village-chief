package com.example.villagechief;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(VillageChiefMod.MOD_ID)
public class VillageChiefMod {
    public static final String MOD_ID = "villagechief";

    public VillageChiefMod() {
        MinecraftForge.EVENT_BUS.register(new VillageEvents());
    }
}
