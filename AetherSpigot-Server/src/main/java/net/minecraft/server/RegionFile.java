package net.minecraft.server;

import com.github.luben.zstd.ZstdInputStreamNoFinalizer;
import com.github.luben.zstd.ZstdOutputStreamNoFinalizer;
import com.google.common.collect.Lists;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Factory;
import org.github.paperspigot.exception.ServerInternalException;
import xyz.aether.spigot.config.AetherConfig;

import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public class RegionFile {

    private static final byte[] a = new byte[4096]; // Spigot - note: if this ever changes to not be 4096 bytes, update constructor! // PAIL: empty 4k block
    private static final LZ4Factory lz4Factory = LZ4Factory.fastestJavaInstance(); // KigPaper
    private final File b;
    private RandomAccessFile c;
    private final int[] d = new int[1024];
    private final int[] e = new int[1024];
    private List<Boolean> f;
    private int g;
    private long h;

    public RegionFile(File file) {
        this.b = file;
        this.g = 0;

        try {
            if (file.exists()) {
                this.h = file.lastModified();
            }

            this.c = new RandomAccessFile(file, "rw");
            int i;

            if (this.c.length() < 4096L) {
                // Spigot - more effecient chunk zero'ing
                this.c.write(RegionFile.a); // Spigot
                this.c.write(RegionFile.a); // Spigot

                this.g += 8192;
            }

            if ((this.c.length() & 4095L) != 0L) {
                for (i = 0; (long) i < (this.c.length() & 4095L); ++i) {
                    this.c.write(0);
                }
            }

            i = (int) this.c.length() / 4096;
            this.f = Lists.newArrayListWithCapacity(i);

            int j;

            for (j = 0; j < i; ++j) {
                this.f.add(Boolean.valueOf(true));
            }

            this.f.set(0, Boolean.valueOf(false));
            this.f.set(1, Boolean.valueOf(false));
            this.c.seek(0L);

            int k;
            // Paper Start
            ByteBuffer header = ByteBuffer.allocate(8192);
            while (header.hasRemaining()) {
                if (this.c.getChannel().read(header) == -1) {
                    throw new EOFException();
                }
            }
            ((Buffer) header).clear();
            IntBuffer headerAsInts = header.asIntBuffer();
            // Paper end

            for (j = 0; j < 1024; ++j) {
                k = headerAsInts.get(); // Paper
                this.d[j] = k;
                if (k != 0 && (k >> 8) + (k & 255) <= this.f.size()) {
                    for (int l = 0; l < (k & 255); ++l) {
                        this.f.set((k >> 8) + l, Boolean.valueOf(false));
                    }
                }
            }

            for (j = 0; j < 1024; ++j) {
                k = headerAsInts.get(); // Paper
                this.e[j] = k;
            }
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
            ServerInternalException.reportInternalException(ioexception); // Paper
        }

    }

    // AetherSpigot start
    private boolean[] existingChunkCache = new boolean[1024];

    private boolean checkExistingChunkCache(int i, int j) {
        return this.existingChunkCache[i + j * 32];
    }

    private void addCoordinatesToCache(int i, int j) {
        this.existingChunkCache[i + j * 32] = true;
    }
    // AetherSpigot end

    // CraftBukkit start - This is a copy (sort of) of the method below it, make sure they stay in sync
    public synchronized boolean chunkExists(int i, int j) {
        if (this.d(i, j)) {
            return false;
        } else {
            // AetherSpigot start
            if(checkExistingChunkCache(i, j)) {
                return true;
            }
            // AetherSpigot end
            try {
                int k = this.e(i, j);

                if (k == 0) {
                    return false;
                } else {
                    int l = k >> 8;
                    int i1 = k & 255;

                    if (l + i1 > this.f.size()) {
                        return false;
                    }

                    this.c.seek(l * 4096L);
                    int j1 = this.c.readInt();

                    if (j1 > 4096 * i1 || j1 <= 0) {
                        return false;
                    }

                    byte b0 = this.c.readByte();
                    if (b0 == 1 || b0 == 2) {
                        this.addCoordinatesToCache(i, j); // AetherSpigot
                        return true;
                    }
                }
            } catch (IOException ioexception) {
                return false;
            }
        }

        return false;
    }
    // CraftBukkit end

    public synchronized DataInputStream a(int i, int j) {
        if (this.d(i, j)) {
            return null;
        } else {
            try {
                int k = this.e(i, j);

                if (k == 0) {
                    return null;
                } else {
                    int l = k >> 8;
                    int i1 = k & 255;

                    if (l + i1 > this.f.size()) {
                        return null;
                    } else {
                        this.c.seek((long) (l * 4096));
                        int j1 = this.c.readInt();

                        if (j1 > 4096 * i1) {
                            return null;
                        } else if (j1 <= 0) {
                            return null;
                        } else {
                            byte b0 = this.c.readByte();
                            byte[] abyte;

                            if (b0 == CompressionAlgorithm.NONE.ordinal()) {
                                // KigPaper start - add no decompression
                                abyte = new byte[j1 - 1];
                                this.c.read(abyte);
                                return new DataInputStream(new ByteArrayInputStream(abyte));
                                // KigPaper end
                            } else if (b0 == CompressionAlgorithm.GZIP.ordinal()) {
                                abyte = new byte[j1 - 1];
                                this.c.read(abyte);
                                return new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(abyte)));
                            } else if (b0 == CompressionAlgorithm.ZLIB.ordinal()) {
                                abyte = new byte[j1 - 1];
                                this.c.read(abyte);
                                return new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(abyte)));
                            } else if (b0 == CompressionAlgorithm.LZ4.ordinal()) {
                                // KigPaper start - add LZ4 decompression
                                abyte = new byte[j1 - 1];
                                this.c.read(abyte);
                                return new DataInputStream(new LZ4BlockInputStream(new ByteArrayInputStream(abyte), lz4Factory.fastDecompressor()));
                                // KigPaper end
                            } else if (b0 == CompressionAlgorithm.ZSTD.ordinal()) {
                                // KigPaper start - add Zstandard decompression
                                abyte = new byte[j1 - 1];
                                this.c.read(abyte);
                                return new DataInputStream(new ZstdInputStreamNoFinalizer(new ByteArrayInputStream(abyte)));
                                // KigPaper end
                            } else {
                                return null;
                            }
                        }
                    }
                }
            } catch (IOException ioexception) {
                return null;
            }
        }
    }

    public DataOutputStream b(int i, int j) throws IOException { // PAIL: getChunkOutputStream // KigPaper - add throws
        // PAIL: isInvalidRegion
        if (this.d(i, j)) {
            return null;
        }
        // KigPaper start - add alternative compression algorithms
        ChunkBuffer buffer = new RegionFile.ChunkBuffer(i, j);
        OutputStream stream;
        switch (AetherConfig.regionCompressionAlgorithm) {
            case NONE:
                stream = buffer;
                break;
            case GZIP:
                stream = new GZIPOutputStream(buffer); // Normally not supported by the Notchian server
                break;
            case ZLIB:
                stream = new DeflaterOutputStream(buffer);
                break;
            case LZ4:
                stream = new LZ4BlockOutputStream(buffer, 65536, lz4Factory.fastCompressor());
                break;
            case ZSTD:
                stream = new ZstdOutputStreamNoFinalizer(buffer);
                break;
            default:
                throw new UnsupportedEncodingException("invalid compression algorithm");
        }
        return new DataOutputStream(new BufferedOutputStream(stream));
        // KigPaper end
    }

    protected synchronized void a(int i, int j, byte[] abyte, int k) {
        try {
            int l = this.e(i, j);
            int i1 = l >> 8;
            int j1 = l & 255;
            int k1 = (k + 5) / 4096 + 1;

            if (k1 >= 256) {
                return;
            }

            if (i1 != 0 && j1 == k1) {
                this.a(i1, abyte, k);
            } else {
                int l1;

                for (l1 = 0; l1 < j1; ++l1) {
                    this.f.set(i1 + l1, Boolean.valueOf(true));
                }

                l1 = this.f.indexOf(Boolean.valueOf(true));
                int i2 = 0;
                int j2;

                if (l1 != -1) {
                    for (j2 = l1; j2 < this.f.size(); ++j2) {
                        if (i2 != 0) {
                            if (((Boolean) this.f.get(j2)).booleanValue()) {
                                ++i2;
                            } else {
                                i2 = 0;
                            }
                        } else if (((Boolean) this.f.get(j2)).booleanValue()) {
                            l1 = j2;
                            i2 = 1;
                        }

                        if (i2 >= k1) {
                            break;
                        }
                    }
                }

                if (i2 >= k1) {
                    i1 = l1;
                    this.a(i, j, l1 << 8 | k1);

                    for (j2 = 0; j2 < k1; ++j2) {
                        this.f.set(i1 + j2, Boolean.valueOf(false));
                    }

                    this.a(i1, abyte, k);
                } else {
                    this.c.seek(this.c.length());
                    i1 = this.f.size();

                    for (j2 = 0; j2 < k1; ++j2) {
                        this.c.write(RegionFile.a);
                        this.f.add(Boolean.valueOf(false));
                    }

                    this.g += 4096 * k1;
                    this.a(i1, abyte, k);
                    this.a(i, j, i1 << 8 | k1);
                }
            }

            this.b(i, j, (int) (MinecraftServer.az() / 1000L));
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
            ServerInternalException.reportInternalException(ioexception); // Paper
        }

    }

    private void a(int i, byte[] abyte, int j) throws IOException {
        this.c.seek(i * 4096L);
        this.c.writeInt(j + 1);
        this.c.writeByte(AetherConfig.regionCompressionAlgorithm.ordinal());
        this.c.write(abyte, 0, j);
    }

    private boolean d(int i, int j) {
        return i < 0 || i >= 32 || j < 0 || j >= 32;
    }

    private int e(int i, int j) {
        return this.d[i + j * 32];
    }

    public boolean c(int i, int j) {
        return this.e(i, j) != 0;
    }

    private void a(int i, int j, int k) throws IOException {
        this.d[i + j * 32] = k;
        this.c.seek((long) ((i + j * 32) * 4));
        this.c.writeInt(k);
    }

    private void b(int i, int j, int k) throws IOException {
        this.e[i + j * 32] = k;
        this.c.seek((long) (4096 + (i + j * 32) * 4));
        this.c.writeInt(k);
    }

    public void c() throws IOException {
        if (this.c != null) {
            this.c.close();
        }

    }

    class ChunkBuffer extends ByteArrayOutputStream {

        private int b;
        private int c;

        public ChunkBuffer(int i, int j) {
            super(8096);
            this.b = i;
            this.c = j;
        }

        @Override
        public void close() {
            RegionFile.this.a(this.b, this.c, this.buf, this.count);
        }
    }

    // KigPaper start
    public enum CompressionAlgorithm {
        NONE,
        GZIP,
        ZLIB,
        LZ4, // KigPaper
        ZSTD // KigPaper
    }
    // KigPaper end
}
