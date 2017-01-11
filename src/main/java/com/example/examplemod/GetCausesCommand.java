package com.example.examplemod;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

/**
 * Created by codew on 10/01/2017.
 */
public class GetCausesCommand extends CommandBase
{
    @Override
    public String getCommandName()
    {
        return "chunkCauses";
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "chunkCauses";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length == 1) {
            if ("clear".equals(args[0])) {
                Events.clearCauses();
                return;
            }
            if ("log".equals(args[0])) {
                Events.logCauses();
                return;
            }
            if ("start".equals(args[0])) {
                Events.startWatching();
                return;
            }
            if ("stop".equals(args[0])) {
                Events.stopWatching();
            }
        }


    }
}
