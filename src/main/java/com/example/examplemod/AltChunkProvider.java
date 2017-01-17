package com.example.examplemod;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import javax.annotation.Nullable;

public class AltChunkProvider extends ChunkProviderServer
{
    private final IChunkProvider parent;

    public AltChunkProvider(ChunkProviderServer parent)
    {
        super(parent.worldObj, parent.chunkLoader, parent.chunkGenerator);
        this.parent = parent;
    }

    @Nullable
    @Override
    public Chunk loadChunk(int x, int z, Runnable runnable)
    {
        if (Events.watching)
        {
            Events.setCauseOfChunkLoad(new Throwable());
        }

        return super.loadChunk(x, z, runnable);
    }
}
