package com.example.examplemod;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Mod.EventBusSubscriber
public class Events
{
    private static LoadingCache<ChunkPos, AtomicInteger> seenBuild = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build(new CacheLoader<ChunkPos, AtomicInteger>() {
                @Override
                public AtomicInteger load(ChunkPos key)
                {
                    return new AtomicInteger(0);
                }
            });

    private static HashMap<Integer, Integer> causeOfChunkLoad = Maps.newHashMap();
    private static HashMap<Integer, String> causes = Maps.newHashMap();
    private static Integer currentCause;
    private static int nextCauseIndex;
    private static Throwable elements;
    public static boolean watching = false;

    public static void setCauseOfChunkLoad(Throwable elements) {
        Events.elements = elements;
    }

    private static boolean shouldLog(StackTraceElement element)
    {
        final String className = element.getClassName();
        final String methodName = element.getMethodName();
        //Banned classes
        if (className.endsWith(".AltChunkProvider")) return false;
        if (className.endsWith(".Thread")) return false;
        if (className.endsWith(".FutureTask")) return false;
        if (className.endsWith(".Executors$RunnableAdapter")) return false;
        if (className.endsWith(".PacketThreadUtil$1")) return false;


        if (className.endsWith(".ChunkProviderServer") && methodName == "loadChunk") return false;
        if (className.endsWith(".ChunkProviderServer") && methodName == "func_186028_c") return false;
        if (className.endsWith(".Packet") && methodName == "processPacket") return false;
        if (className.endsWith(".Packet") && methodName == "func_148833_a") return false;

        if (className.endsWith(".IntegratedServer") && methodName == "tick") return false;
        if (className.endsWith(".IntegratedServer") && methodName == "func_71217_p ") return false;
        if (className.endsWith(".Util") && methodName == "runTask") return false;
        if (className.endsWith(".Util") && methodName == "func_181617_a ") return false;

        //Not Obfuscated
        if (className.endsWith(".MinecraftServer") && methodName == "run") return false;

        return true;
    }

    private static Field tileEntitiesToBeRemoved = null;
    private static HashMap<Integer, Set<BlockPos>> lastTickTileEntities = Maps.newHashMap();

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) throws NoSuchFieldException, IllegalAccessException
    {
        if (event.side == Side.SERVER && event.phase == TickEvent.Phase.START) {
            Class aClass = WorldServer.class;
            tileEntitiesToBeRemoved = getField(aClass, "tileEntitiesToBeRemoved");
            if (tileEntitiesToBeRemoved == null) {
                tileEntitiesToBeRemoved = getField(aClass, "field_147483_b");
            }
            if (tileEntitiesToBeRemoved == null) {
                throw new NoSuchFieldException("tileEntitiesToBeRemoved (deObf)/ field_147483_b (obf)");
            }
            final List<TileEntity> tileEntitiesToRemove = (List<TileEntity>)tileEntitiesToBeRemoved.get(event.world);

            Set<BlockPos> tileEntitiesToExamine = lastTickTileEntities.get(event.world.provider.getDimension());
            if (tileEntitiesToExamine == null) {
                tileEntitiesToExamine = Sets.newHashSet();
            }

            Set<BlockPos> nextSet = Sets.newHashSet();
            boolean found = false;
            int searched = 0;
            for (final TileEntity tileEntity : tileEntitiesToRemove)
            {
                final BlockPos pos = tileEntity.getPos();
                if (searched < 1024 && tileEntitiesToExamine.contains(pos)) {
                    found = true;
                }
                searched++;
                nextSet.add(pos);
            }

            if (found) {
                Logger.severe("Warning there are tile entities that are not being removed from the world.");
            }

            lastTickTileEntities.put(event.world.provider.getDimension(), nextSet);
        }
    }


    @SubscribeEvent
    public static void onChunkLoadedEvent(ChunkEvent.Load event) throws ExecutionException
    {
        currentCause = -1;
        if (elements != null)
        {

            StringBuilder stackTrace = new StringBuilder();
            for (final StackTraceElement element : elements.getStackTrace())
            {
                if (!shouldLog(element)) continue;

                stackTrace.append(element);
                stackTrace.append("\r\n");
            }
            String e = stackTrace.toString();
            final int hashCode = e.hashCode();
            Integer index = causeOfChunkLoad.get(hashCode);
            if (index == null)
            {
                index = nextCauseIndex++;
                causeOfChunkLoad.put(hashCode, index);
                final String indexIndicator = "[" + index + "] ";
                e = indexIndicator + e.replace("\r\n", "\r\n" + indexIndicator);

                causes.put(index, e);
            }
            currentCause = index;
        }
        final Chunk chunk = event.getChunk();
        final ChunkPos chunkCoordIntPair = chunk.getChunkCoordIntPair();
        final AtomicInteger atomicInteger = seenBuild.get(chunkCoordIntPair);

        final World world = event.getWorld();
        Logger.info("[tick %d] [%d]: Chunk Loaded @ %s in %s, loaded %d times in 30 minutes (%d tile entities)",
                world.getTotalWorldTime(),
                currentCause,
                chunkCoordIntPair,
                world.provider.getDimension(),
                atomicInteger.incrementAndGet(),
                chunk.getTileEntityMap().size()
        );
    }

    @SubscribeEvent
    public static void onChunkLoadedEvent(ChunkEvent.Unload event) {
        final World world = event.getWorld();
        final Chunk chunk = event.getChunk();
        Logger.info("[tick %d]: Chunk Unloading @ %s in %s (%d tile entities)",
                world.getTotalWorldTime(),
                chunk.getChunkCoordIntPair(),
                world.provider.getDimension(),
                chunk.getTileEntityMap().size()
        );
    }

    @SubscribeEvent
    public static void onWorldLoaded(WorldEvent.Load event) throws IllegalAccessException, NoSuchFieldException
    {
        final World world = event.getWorld();

        final Class<? extends World> aClass = world.getClass();
        Field chunkProvider = getField(aClass, "chunkProvider");
        if (chunkProvider == null) {
            chunkProvider = getField(aClass, "field_73020_y");
        }
        if (chunkProvider == null) {
            Logger.info(world.getClass().getName());
            throw new NoSuchFieldException("chunkProvider (deObf)/ field_73020_y (obf)");
        }
        chunkProvider.setAccessible(true);
        final IChunkProvider chunkProvider1 = world.getChunkProvider();
        if (chunkProvider1 instanceof ChunkProviderServer)
        {
            chunkProvider.set(world, new AltChunkProvider((ChunkProviderServer)chunkProvider1));
        }
    }

    private static Field getField(Class clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class superClass = clazz.getSuperclass();
            if (superClass == null) {
                return null;
            } else {
                return getField(superClass, fieldName);
            }
        }
    }

    public static void logCauses()
    {
        for (final Map.Entry<Integer, String> integerStringEntry : causes.entrySet())
        {
            Logger.info(integerStringEntry.getValue());
        }
    }

    public static void clearCauses()
    {
        causeOfChunkLoad.clear();
        causes.clear();

        nextCauseIndex = 0;
    }

    public static void startWatching()
    {
        watching = true;
        clearCauses();
    }

    public static void stopWatching()
    {
        watching = false;
    }
}
