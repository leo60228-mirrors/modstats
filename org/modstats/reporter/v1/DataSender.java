package org.modstats.reporter.v1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.src.CallableMinecraftVersion;
import net.minecraftforge.common.MinecraftForge;

import org.modstats.ModVersionData;
import org.modstats.ModsUpdateEvent;

import argo.jdom.JdomParser;
import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;
import argo.jdom.JsonStringNode;
import argo.saj.InvalidSyntaxException;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;

class DataSender extends Thread
{
    private static final String urlAutoTemplate = "http://modstats.org/api/v1/report?mc=%s&user=%s&data=%s&sign=%s";
    private static final String urlManualTemplate = "http://modstats.org/api/v1/check?mc=%s&user=%s&data=%s&sign=%s";
    
    private final Reporter reporter;
    public final boolean manual;
    
    public DataSender(Reporter reporter, boolean manual)
    {
        this.reporter = reporter;
        this.manual = manual;
    }
    
    private String toHexString(byte[] bytes) {
        char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j*2] = hexArray[v/16];
            hexChars[j*2 + 1] = hexArray[v%16];
        }
        return new String(hexChars);
    }
    
    private String getPlayerId() throws IOException
    {
        File statDir =  new File(Minecraft.getMinecraftDir(), "stats");
        if(!statDir.exists())
        {
            statDir.mkdirs();
        }
        String mac = "";
        try
        {
            InetAddress address = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByInetAddress(address);
            byte[] macArray = ni.getHardwareAddress();
            if(macArray != null)
            {
                mac = toHexString(macArray);
            }
        }
        catch(Exception ex)
        {
        }
        File uidFile = new File(statDir, "player.uid");
        if(uidFile.exists() && uidFile.canRead() && uidFile.length() == 32+mac.length())
        {
            String data = Files.toString(uidFile, Charsets.US_ASCII);
            String storedMac = data.substring(32);
            if(storedMac.equalsIgnoreCase(mac))
                return data.substring(0, 32);
        }
        uidFile.createNewFile();
        if(uidFile.canWrite())
        {
            String uid = UUID.randomUUID().toString().replace("-", "");
            FileOutputStream output = new FileOutputStream(uidFile);
            output.write((uid+mac).getBytes());
            output.close();
            return uid;
        }
        return "";
    }
    
    private String getSignature(String data)
    {
        return Hashing.md5().hashString(data).toString();
    }
    
    private String getData()
    {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, ModVersionData> item : reporter.registeredMods.entrySet())
        {
            b.append(item.getKey()).append("+").append(item.getValue().version).append("$");
        }
        return b.toString();
    }
    
    private boolean checkIsNewer(String current, String received)
    {
        return true;
    }
    
    
    private void parseResponse(String response)
    {
        try
        {
            JsonRootNode json = (new JdomParser()).parse(response);
            List<JsonNode> modList = json.getArrayNode("mods");
            ModsUpdateEvent event = new ModsUpdateEvent();
            for (JsonNode modObject : modList)
            {
                String prefix = modObject.getStringValue("code");
                if(!reporter.registeredMods.containsKey(prefix))
                {
                    FMLLog.warning("[Modstats] Extra mod '%s' in service response", prefix);
                    continue;
                }
                String version = modObject.getStringValue("ver");
                if(version.equals(reporter.registeredMods.get(prefix)))
                {
                    continue;
                }
                if(checkIsNewer(reporter.registeredMods.get(prefix).version, version))
                {
                    ModVersionData data = new ModVersionData(prefix, reporter.registeredMods.get(prefix).name, version);
                    Map<JsonStringNode, JsonNode> fields = modObject.getFields();
                    for (Map.Entry<JsonStringNode, JsonNode> entry : fields.entrySet())
                    {
                        String fieldName = entry.getKey().getText();
                        if(fieldName.equals("code") || fieldName.equals("ver"))
                            continue;
                        if(!(entry.getValue() instanceof JsonStringNode))
                        {
                            FMLLog.warning(String.format("[Modstats] Too complex data in response for field '%s'.", fieldName)); 
                            continue;
                        }
                        String value = ((JsonStringNode)entry.getValue()).getText();
                        if(fieldName.equals("chlog"))
                        {
                            data.changeLogUrl = value;
                        }
                        else if(fieldName.equals("link"))
                        {
                            data.downloadUrl = value;
                        }
                        else
                        {
                            data.extraFields.put(fieldName, value);
                        }
                    }
                    event.add(data);
                }
                
            }
            if(event.getUpdatedMods().size() > 0)
            {
                MinecraftForge.EVENT_BUS.post(event);
            }
            if(!event.isCanceled() && event.getUpdatedMods().size() > 0)
            {
                List<ModVersionData> updatedModsToOutput = event.getUpdatedMods();
                StringBuilder builder = new StringBuilder("Updates found: ");
                Iterator<ModVersionData> iterator = updatedModsToOutput.iterator();
                while(iterator.hasNext())
                {
                    ModVersionData modVersionData = iterator.next();
                    builder.append(modVersionData.name)
                        .append(" (")
                        .append(modVersionData.version)
                        .append(")")
                        .append(iterator.hasNext()?",":".");
                }
                FMLLog.info("[Modstats] %s", builder.toString());
                if(FMLCommonHandler.instance().getSide().isClient())
                {
                    FMLClientHandler.instance().getClient().thePlayer.addChatMessage(builder.toString());
                }
            }
                
        } catch (InvalidSyntaxException e)
        {
            FMLLog.warning("[Modstats] Can't parse response: '%s'.", e.getMessage());
        }
    }

    
    @Override
    public void run() 
    {
        try
        {
            String data = getData();
            String playerId = getPlayerId();
            String hash = getSignature(playerId+"!"+data);
            String template = manual?urlManualTemplate:urlAutoTemplate;
            String mcVersion = new CallableMinecraftVersion(null).minecraftVersion();
            URL url = new URL(String.format(template, mcVersion, playerId, data, hash));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            String out = "";
            while ((line = reader.readLine()) != null) {
                //in most cases it will contain just one line
                out += line;
            }
            reader.close();
            parseResponse(out);
        } catch (MalformedURLException e)
        {
            FMLLog.warning("[Modstats] Invalid stat report url");
        } catch (IOException e)
        {
            FMLLog.info("[Modstats] Stat wasn't reported '"+e.getMessage()+"'");
        } 
    }
}
