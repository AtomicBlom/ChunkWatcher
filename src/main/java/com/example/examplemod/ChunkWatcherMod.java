package com.example.examplemod;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;
import java.util.Map;

@Mod(modid = ChunkWatcherMod.MODID, version = ChunkWatcherMod.VERSION)
public class ChunkWatcherMod
{
    public static final String MODID = "chunkwatcher";
    public static final String VERSION = "1.0";

    @NetworkCheckHandler
    public boolean checkRemoteVersions(Map<String, String> versions, Side side) {
        return true;
    }

    @Mod.EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new GetCausesCommand());
    }
}
