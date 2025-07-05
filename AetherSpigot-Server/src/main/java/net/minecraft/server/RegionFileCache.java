package net.minecraft.server;

import org.github.paperspigot.exception.ServerInternalException;
import xyz.aether.spigot.config.AetherConfig;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class RegionFileCache {

    public static final Map<File, RegionFile> a = new LinkedHashMap(AetherConfig.regionFileCacheSize, 0.75f, true); // Spigot - private -> public, Paper - HashMap -> LinkedHashMap

    // PaperSpigot start
    public static synchronized RegionFile a(File file, int i, int j) {
        return a(file, i, j, true);
    }

    public static synchronized RegionFile a(File file, int i, int j, boolean create) {
        // PaperSpigot end
        File file1 = new File(file, "region");
        File file2 = new File(file1, "r." + (i >> 5) + "." + (j >> 5) + ".mca");
        RegionFile regionfile = (RegionFile) RegionFileCache.a.get(file2);

        if (regionfile != null) {
            return regionfile;
        } else {
            if (!create && !file2.exists()) {
                return null;
            } // PaperSpigot
            if (!file1.exists()) {
                file1.mkdirs();
            }

            if (RegionFileCache.a.size() >= AetherConfig.regionFileCacheSize) { // Paper
                trimCache(); // Paper
            }

            RegionFile regionfile1 = new RegionFile(file2);

            RegionFileCache.a.put(file2, regionfile1);
            return regionfile1;
        }
    }

    public static synchronized void a() {
        Iterator iterator = RegionFileCache.a.values().iterator();

        while (iterator.hasNext()) {
            RegionFile regionfile = (RegionFile) iterator.next();

            try {
                if (regionfile != null) {
                    regionfile.c();
                }
            } catch (IOException ioexception) {
                ioexception.printStackTrace();
                ServerInternalException.reportInternalException(ioexception); // Paper
            }
        }

        RegionFileCache.a.clear();
    }

    // Paper Start
    private static synchronized void trimCache() {
        Iterator<Map.Entry<File, RegionFile>> itr = RegionFileCache.a.entrySet().iterator();
        int count = RegionFileCache.a.size() - AetherConfig.regionFileCacheSize;
        while (count-- >= 0 && itr.hasNext()) {
            try {
                itr.next().getValue().c();
            } catch (IOException ioexception) {
                ioexception.printStackTrace();
                ServerInternalException.reportInternalException(ioexception);
            }
            itr.remove();
        }
    }
    // Paper End

    public static DataInputStream c(File file, int i, int j) {
        RegionFile regionfile = a(file, i, j);

        return regionfile.a(i & 31, j & 31);
    }

    public static DataOutputStream d(File file, int i, int j) throws IOException { // KigPaper - add throws
        RegionFile regionfile = a(file, i, j);

        return regionfile.b(i & 31, j & 31);
    }
}
