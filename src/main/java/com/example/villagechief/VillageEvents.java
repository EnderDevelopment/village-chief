package com.example.villagechief;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static net.minecraft.command.Commands.literal;

public class VillageEvents {
    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
        dispatcher.register(literal("chief").then(literal("found").executes(context -> found(context.getSource())))
                .then(literal("status").executes(context -> status(context.getSource())))
                .then(literal("expand").executes(context -> expand(context.getSource()))));
    }

    private int found(CommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.asPlayer();
        World world = player.world;
        if (world.isRemote) return 0;
        VillageData data = VillageData.get(world);
        if (data.isFounded()) { source.sendFeedback(new StringTextComponent("This world already has a village."), false); return 0; }
        BlockPos chest = findChest(world, player.getPosition());
        if (chest == null) { source.sendFeedback(new StringTextComponent("Place a chest nearby, then run /chief found again."), false); return 0; }
        data.setFounded(chest, chest);
        spawnVillager((ServerWorld) world, chest.up(), "Builder");
        spawnVillager((ServerWorld) world, chest.up(), "Farmer");
        spawnVillager((ServerWorld) world, chest.up(), "Guard");
        source.sendFeedback(new StringTextComponent("You are now chief. Your homeless settlement begins at this chest. Store food and building materials here."), false);
        return 1;
    }

    private int status(CommandSource source) {
        VillageData data = VillageData.get(source.getWorld());
        if (!data.isFounded()) { source.sendFeedback(new StringTextComponent("No village yet. Place a chest and run /chief found."), false); return 0; }
        source.sendFeedback(new StringTextComponent("Village residents: " + data.getResidents() + " | buildings: " + data.getExpansionStage() + " | shared storage: " + pos(data.getStorage())), false);
        return 1;
    }

    private int expand(CommandSource source) {
        VillageData data = VillageData.get(source.getWorld());
        if (!data.isFounded()) { source.sendFeedback(new StringTextComponent("Found a village first with /chief found."), false); return 0; }
        if (!(source.getWorld() instanceof ServerWorld)) return 0;
        if (buildNext((ServerWorld) source.getWorld(), data)) { source.sendFeedback(new StringTextComponent("The builders completed a new village hut."), false); return 1; }
        source.sendFeedback(new StringTextComponent("Not enough materials in the shared chest. Each hut costs 16 planks and 8 cobblestone."), false);
        return 0;
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || !(event.world instanceof ServerWorld)) return;
        ServerWorld world = (ServerWorld) event.world;
        VillageData data = VillageData.get(world);
        int currentTick = data.tick();
        if (!data.isFounded()) return;
        runVillagerJobs(world, data, currentTick);
        if (currentTick % 1200 != 0) return;
        IInventory inventory = getInventory(world, data.getStorage());
        if (inventory == null) return;
        if (!takeAny(inventory, Items.BREAD, 1) && !takeAny(inventory, Items.WHEAT, 3) && !takeAny(inventory, Items.CARROT, 3) && !takeAny(inventory, Items.POTATO, 3)) {
            world.getPlayers().forEach(player -> player.sendMessage(new StringTextComponent("Village stores are low on food. Add bread, wheat, carrots, or potatoes to the shared chest.")));
        }
        if (data.getExpansionStage() < 8 && currentTick % 6000 == 0) buildNext(world, data);
    }

    private static void runVillagerJobs(ServerWorld world, VillageData data, int tick) {
        if (tick % 200 != 0) return;
        IInventory storage = getInventory(world, data.getStorage());
        if (storage == null) return;
        BlockPos center = data.getCenter();
        if (tick % 1200 == 0 && data.getExpansionStage() < 8) buildNext(world, data);
        java.util.List<VillagerEntity> villagers = world.getEntities(EntityType.VILLAGER, entity -> entity.isAlive());
        VillagerEntity farmer = findRole(villagers, "Farmer");
        VillagerEntity builder = findRole(villagers, "Builder");
        if (farmer != null) runFarmerJob(world, farmer, center, storage);
        if (builder != null && data.getExpansionStage() < 8) runBuilderJob(world, builder, data, storage);
    }

    private static VillagerEntity findRole(java.util.List<VillagerEntity> villagers, String role) {
        for (VillagerEntity villager : villagers) {
            if (villager.hasCustomName() && role.equals(villager.getCustomName().getString())) return villager;
        }
        return null;
    }

    private static void runFarmerJob(ServerWorld world, VillagerEntity farmer, BlockPos center, IInventory storage) {
        BlockPos cropPos = null;
        for (BlockPos pos : BlockPos.getAllInBoxMutable(center.add(-8, -1, -8), center.add(8, 1, 8))) {
            net.minecraft.block.BlockState state = world.getBlockState(pos);
            net.minecraft.block.Block block = state.getBlock();
            if ((block == Blocks.WHEAT || block == Blocks.CARROTS || block == Blocks.POTATOES)
                    && state.get(net.minecraft.block.CropsBlock.AGE) == 7) { cropPos = pos.toImmutable(); break; }
        }
        if (cropPos == null) return;
        if (farmer.getDistanceSq(cropPos.getX() + 0.5, cropPos.getY(), cropPos.getZ() + 0.5) > 9.0) {
            farmer.getNavigator().tryMoveToXYZ(cropPos.getX() + 0.5, cropPos.getY(), cropPos.getZ() + 0.5, 0.8);
            return;
        }
        net.minecraft.block.BlockState crop = world.getBlockState(cropPos);
        Item produce = crop.getBlock() == Blocks.WHEAT ? Items.WHEAT : crop.getBlock() == Blocks.CARROTS ? Items.CARROT : Items.POTATO;
        world.setBlockState(cropPos, crop.getBlock().getDefaultState(), 3);
        addToStorage(storage, produce, 2);
        if (produce == Items.WHEAT) addToStorage(storage, Items.WHEAT_SEEDS, 1);
    }

    private static void runBuilderJob(ServerWorld world, VillagerEntity builder, VillageData data, IInventory storage) {
        if (!has(storage, Items.OAK_PLANKS, 16) || !has(storage, Items.COBBLESTONE, 8)) return;
        int nextStage = data.getExpansionStage() + 1;
        BlockPos site = data.getCenter().add((nextStage % 3) * 7 + 2, 0, (nextStage / 3 + 1) * 7 + 2);
        if (builder.getDistanceSq(site.getX() + 0.5, site.getY(), site.getZ() + 0.5) > 9.0) {
            builder.getNavigator().tryMoveToXYZ(site.getX() + 0.5, site.getY(), site.getZ() + 0.5, 0.8);
            return;
        }
        buildNext(world, data);
    }

    private static void addToStorage(IInventory inventory, Item item, int count) {
        ItemStack remaining = new ItemStack(item, count);
        for (int slot = 0; slot < inventory.getSizeInventory() && !remaining.isEmpty(); slot++) {
            ItemStack existing = inventory.getStackInSlot(slot);
            if (existing.isEmpty()) inventory.setInventorySlotContents(slot, remaining.copy());
            else if (existing.getItem() == item && existing.getCount() < existing.getMaxStackSize()) {
                int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(moved);
                remaining.shrink(moved);
            }
        }
        inventory.markDirty();
    }

    private static boolean buildNext(ServerWorld world, VillageData data) {
        IInventory inventory = getInventory(world, data.getStorage());
        if (inventory == null || !has(inventory, Items.OAK_PLANKS, 16) || !has(inventory, Items.COBBLESTONE, 8)) return false;
        takeAny(inventory, Items.OAK_PLANKS, 16);
        takeAny(inventory, Items.COBBLESTONE, 8);
        int stage = data.getExpansionStage() + 1;
        BlockPos base = data.getCenter().add((stage % 3) * 7, 0, (stage / 3 + 1) * 7);
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
            world.setBlockState(base.add(x, 0, z), Blocks.COBBLESTONE.getDefaultState(), 3);
            if (x == 0 || x == 4 || z == 0 || z == 4) {
                for (int y = 1; y <= 3; y++) world.setBlockState(base.add(x, y, z), Blocks.OAK_PLANKS.getDefaultState(), 3);
            }
        }
        for (int x = 1; x < 4; x++) for (int z = 1; z < 4; z++) world.setBlockState(base.add(x, 4, z), Blocks.OAK_PLANKS.getDefaultState(), 3);
        world.setBlockState(base.add(2, 1, 0), Blocks.AIR.getDefaultState(), 3);
        data.setExpansionStage(stage);
        return true;
    }

    private static BlockPos findChest(World world, BlockPos origin) {
        for (BlockPos pos : BlockPos.getAllInBoxMutable(origin.add(-6, -2, -6), origin.add(6, 2, 6))) {
            if (world.getBlockState(pos).getBlock() == Blocks.CHEST && getInventory(world, pos) != null) return pos.toImmutable();
        }
        return null;
    }

    private static IInventory getInventory(World world, BlockPos pos) {
        net.minecraft.tileentity.TileEntity tile = world.getTileEntity(pos);
        return tile instanceof IInventory ? (IInventory) tile : null;
    }

    private static boolean has(IInventory inventory, Item item, int amount) {
        int count = 0;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) { ItemStack stack = inventory.getStackInSlot(slot); if (stack.getItem() == item) count += stack.getCount(); }
        return count >= amount;
    }

    private static boolean takeAny(IInventory inventory, Item item, int amount) {
        if (!has(inventory, item, amount)) return false;
        int left = amount;
        for (int slot = 0; slot < inventory.getSizeInventory() && left > 0; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.getItem() == item) { int take = Math.min(left, stack.getCount()); inventory.decrStackSize(slot, take); left -= take; }
        }
        inventory.markDirty();
        return true;
    }

    private static void spawnVillager(ServerWorld world, BlockPos pos, String role) {
        VillagerEntity villager = EntityType.VILLAGER.create(world);
        if (villager == null) return;
        villager.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        villager.setCustomName(new StringTextComponent(role));
        villager.setCustomNameVisible(true);
        world.addEntity(villager);
    }

    private static String pos(BlockPos pos) { return pos.getX() + ", " + pos.getY() + ", " + pos.getZ(); }
}
