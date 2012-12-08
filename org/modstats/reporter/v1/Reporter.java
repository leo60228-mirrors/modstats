package org.modstats.reporter.v1;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.world.WorldEvent;

import org.modstats.IModstatsReporter;
import org.modstats.ModVersionData;
import org.modstats.ModstatInfo;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;


public class Reporter implements IModstatsReporter
{

    public Map<String, ModVersionData> registeredMods;
    private DataSender sender;
    public Config config; 
    
    /**
     * At least one auto check was completed successfully
     */
    private boolean checkedAuto;

    public Reporter()
    {
        checkedAuto = false;
        registeredMods = new  ConcurrentHashMap<String, ModVersionData>(2, 0.9f, 1);
        MinecraftForge.EVENT_BUS.register(this);
        config = new Config();
    }
    
    
    private void startCheck(boolean manual)
    {
        if(!config.allowUpdates)
            return;
        //only manual check is allowed on servers 
        if(!FMLCommonHandler.instance().getSide().isClient() && !manual)
            return;
        if(registeredMods.isEmpty())
            return;
        DataSender currentSender = sender;
        if(!manual && checkedAuto)
            return;
        if(currentSender!=null && (currentSender.manual == false || manual))
            return;
        currentSender = new DataSender(this, manual);
        currentSender.run();
        sender = currentSender;
        
    }
    
    @ForgeSubscribe
    public void worldLoad(WorldEvent.Load event)
    {
        startCheck(false);
    }

    
    @Override
    public void registerMod(Object mod)
    {
        if(!config.allowUpdates)
            return;
        if(mod == null)
        {
            FMLLog.warning("[Modstats] Can't register null mod.");
            return;
        }
        ModstatInfo info = mod.getClass().getAnnotation(ModstatInfo.class);
        if(info == null)
        {
            FMLLog.warning("[Modstats] ModstatsInfo annotation not found for given mod.");
            return;
        }
        Mod modData = mod.getClass().getAnnotation(Mod.class);
        if(modData == null)
        {
            FMLLog.warning("[Modstats] Mod annotation not found. Only FML mods are supported.");
            return;
        }
        if(info.prefix() == null || info.prefix().equals(""))
        {
            FMLLog.warning("[Modstats] Mod prefix can't be empty.");
            return;
        }
        ModVersionData data = new ModVersionData(info.prefix(), modData.name(), modData.version());
        registeredMods.put(info.prefix(), data);
    }

    @Override
    public void doManualCheck()
    {
        startCheck(true);
    }

}
