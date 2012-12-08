package org.modstats;

import java.util.HashMap;
import java.util.Map;

public class ModVersionData
{
    public String prefix;
    public String name;
    public String version;
    public String downloadUrl;
    public String changeLogUrl;
    
    public Map<String, String> extraFields;
    
    
    public ModVersionData()
    {
        extraFields  = new  HashMap<String, String>();
    }
    
    public ModVersionData(String prefix, String name, String version)
    {
        this.prefix = prefix;
        this.name = name;
        this.version = version;
        extraFields = new HashMap<String, String>();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((changeLogUrl == null) ? 0 : changeLogUrl.hashCode());
        result = prime * result + ((downloadUrl == null) ? 0 : downloadUrl.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ModVersionData other = (ModVersionData) obj;
        if (changeLogUrl == null)
        {
            if (other.changeLogUrl != null)
                return false;
        } else if (!changeLogUrl.equals(other.changeLogUrl))
            return false;
        if (downloadUrl == null)
        {
            if (other.downloadUrl != null)
                return false;
        } else if (!downloadUrl.equals(other.downloadUrl))
            return false;
        if (name == null)
        {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (prefix == null)
        {
            if (other.prefix != null)
                return false;
        } else if (!prefix.equals(other.prefix))
            return false;
        if (version == null)
        {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }
    
    
}
