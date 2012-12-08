package org.modstats;

public interface IModstatsReporter
{
    public void registerMod(Object mod);
    public void doManualCheck();
}
