package org.modstats;

import cpw.mods.fml.common.FMLLog;

public class Modstats
{
    private static final Modstats INSTANCE = new Modstats();
    private static final String CLASS_TEMPLATE = "org.modstats.reporter.v%d.Reporter";
    private IModstatsReporter reporter;

    private Modstats()
    {
        reporter = locateReporter();
    }
    
    public IModstatsReporter getReporter()
    {
        return reporter;
    }
    
    private IModstatsReporter locateReporter()
    {
        int i=1;
        Class<?> latest = null;
        while(i<100)
        {
            try
            {
                Class<?> candidate = Class.forName(String.format(CLASS_TEMPLATE, i));
                if(IModstatsReporter.class.isAssignableFrom(candidate))
                {
                    latest = candidate;
                }
            }
            catch (Exception e) {
                break;
            }
            i++;
        }
        if(latest == null)
        {
            FMLLog.warning("Modstats reporter class not found.");
        }
        else
        {
            try
            {
                return (IModstatsReporter)latest.newInstance();
            } catch (Exception e)
            {
                FMLLog.warning("Modstats reporter class can't be instantiated.");
            } 
        }
        return null;
    }
    
    public static Modstats instance()
    {
        return INSTANCE;
    }
    
}
