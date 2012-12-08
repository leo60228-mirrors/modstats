package org.modstats.reporter.v1;

import java.io.File;

import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;

public class Config
{
    private static final String CONFIG_NAME = "modstats.cfg"; 
    
    public boolean allowUpdates;
    public boolean betaNotifications;
    
    public Config()
    {
        File configLocation = new File(Loader.instance().getConfigDir(), CONFIG_NAME);
        Configuration configuration = new Configuration(configLocation);
        configuration.load();
        
        Property prop = configuration.get("updates", "AllowUpdates", true);
        prop.comment = "Allow to send current mod versions to the server and check for updates";
        allowUpdates = prop.getBoolean(true);
        
        prop = configuration.get("updates", "BetaNotifications", false);
        prop.comment = "Set true to receive notifications about beta versions. Otherwise you will only receive information about stable versions";
        betaNotifications = prop.getBoolean(false);
        
        configuration.save();
        
        FMLLog.info("[Modstats] Config loaded. allowUpdates: %b,  betaNotification: %b", allowUpdates, betaNotifications);
    }

}
