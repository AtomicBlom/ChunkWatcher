package com.example.examplemod;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import java.util.Random;

@Mod.EventBusSubscriber
public class DebugEvents
{
    static BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
    static Random random = new Random();

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.side == Side.SERVER && event.phase == TickEvent.Phase.START) {
            final World world = event.world;
            final Chunk chunk = world.getChunkFromChunkCoords(random.nextInt(1024) - 512, random.nextInt(1024) - 512);
            for (int i = random.nextInt(10); i > 0; --i) {

                blockPos.setPos(
                        chunk.xPosition + random.nextInt(16),
                        random.nextInt(256),
                        chunk.zPosition + random.nextInt(16)
                );

                world.setBlockState(blockPos, Blocks.CHEST.getDefaultState());
                final TileEntity tileEntity = world.getTileEntity(blockPos);
                if (tileEntity instanceof TileEntityChest) {
                    ((IInventory) tileEntity).setInventorySlotContents(0, new ItemStack(Items.APPLE, 64));
                }
            }
        }
    }
}
