package org.modstats;

import java.util.LinkedList;
import java.util.List;

import cpw.mods.fml.common.FMLLog;

import net.minecraftforge.event.Cancelable;
import net.minecraftforge.event.Event;

@Cancelable
public class ModsUpdateEvent extends Event
{
    private List<ModVersionData> updatedMods;
    
    public ModsUpdateEvent()
    {
        updatedMods = new LinkedList<ModVersionData>();
    }
    
    public void add(ModVersionData data)
    {
        if(!updatedMods.contains(data))
        {
            updatedMods.add(data);
        }
        else
        {
            FMLLog.info("ModsUpdateEvent shouldn't have same mods data", data);
        }
    }
    
    public List<ModVersionData> getUpdatedMods()
    {
        return updatedMods;
    }
    
}
