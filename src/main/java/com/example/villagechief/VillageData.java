package com.example.villagechief;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

public class VillageData extends WorldSavedData {
    private static final String DATA_NAME = VillageChiefMod.MOD_ID + "_village";
    private boolean founded;
    private BlockPos center = BlockPos.ZERO;
    private BlockPos storage = BlockPos.ZERO;
    private int residents;
    private int expansionStage;
    private int tickCounter;

    public VillageData() { super(DATA_NAME); }
    public VillageData(String name) { super(name); }

    public static VillageData get(World world) {
        return (VillageData) world.getMapStorage().getOrLoadData(VillageData::new, DATA_NAME);
    }

    public boolean isFounded() { return founded; }
    public BlockPos getCenter() { return center; }
    public BlockPos getStorage() { return storage; }
    public int getResidents() { return residents; }
    public int getExpansionStage() { return expansionStage; }
    public int tick() { return ++tickCounter; }
    public void setFounded(BlockPos center, BlockPos storage) { this.founded = true; this.center = center; this.storage = storage; this.residents = 3; markDirty(); }
    public void addResident() { residents++; markDirty(); }
    public void setExpansionStage(int stage) { expansionStage = stage; markDirty(); }

    @Override
    public void read(CompoundNBT tag) {
        founded = tag.getBoolean("Founded");
        center = readPos(tag, "Center");
        storage = readPos(tag, "Storage");
        residents = tag.getInt("Residents");
        expansionStage = tag.getInt("ExpansionStage");
        tickCounter = tag.getInt("TickCounter");
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        tag.putBoolean("Founded", founded);
        writePos(tag, "Center", center);
        writePos(tag, "Storage", storage);
        tag.putInt("Residents", residents);
        tag.putInt("ExpansionStage", expansionStage);
        tag.putInt("TickCounter", tickCounter);
        return tag;
    }

    private static BlockPos readPos(CompoundNBT tag, String key) {
        CompoundNBT pos = tag.getCompound(key);
        return new BlockPos(pos.getInt("X"), pos.getInt("Y"), pos.getInt("Z"));
    }
    private static void writePos(CompoundNBT tag, String key, BlockPos value) {
        CompoundNBT pos = new CompoundNBT();
        pos.putInt("X", value.getX()); pos.putInt("Y", value.getY()); pos.putInt("Z", value.getZ());
        tag.put(key, pos);
    }
}
