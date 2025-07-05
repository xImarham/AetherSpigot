package net.minecraft.server;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufProcessor;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ReferenceCounted;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.util.UUID;
// PandaSpigot start
import io.netty.util.ByteProcessor;
import java.nio.channels.FileChannel;
// PandaSpigot end

import org.bukkit.craftbukkit.inventory.CraftItemStack; // CraftBukkit

public class PacketDataSerializer extends ByteBuf {

    private final ByteBuf a;

    // PandaSpigot start - large packet limit
    private static final int DEFAULT_LIMIT = Short.MAX_VALUE;
    private static final int LARGE_PACKET_LIMIT = Short.MAX_VALUE * 1024;
    private final boolean allowLargePackets;
    public PacketDataSerializer(ByteBuf bytebuf) {
        /*
         * By default, we limit the size of the received byte array to Short.MAX_VALUE, which is 31 KB.
         * However, we make an exception when ProtocolSupport is installed, to allow 1.7 clients to work,
         * and limit them to 31 MEGABYTES as they seem to need to send larger packets sometimes.
         * Although a 31 MB limit leaves the server slightly vulnerable,
         * it's still much better than the old system of having no limit,
         * which would leave the server vulnerable to packets up to 2 GIGABYTES in size.
         */
        this.allowLargePackets = com.hpfxd.pandaspigot.CompactHacks.hasProtocolSupport();
    // PandaSpigot end
        this.a = bytebuf;
    }

    public static int a(int i) {
        for (int j = 1; j < 5; ++j) {
            if ((i & -1 << j * 7) == 0) {
                return j;
            }
        }

        return 5;
    }

    public void a(byte[] abyte) {
        this.b(abyte.length);
        this.writeBytes(abyte);
    }

    // Paper start
    public byte[] a() {
        return readByteArray(this.allowLargePackets ? LARGE_PACKET_LIMIT : DEFAULT_LIMIT); // PandaSpigot - large packet limit
    }

    public byte[]readByteArray(int limit) {
        int len = this.e();
        if (len > limit) throw new DecoderException("The received a byte array longer than allowed " + len + " > " + limit);
        byte[] abyte = new byte[len];
    // Paper end

        this.readBytes(abyte);
        return abyte;
    }

    public BlockPosition c() {
        return BlockPosition.fromLong(this.readLong());
    }

    public void a(BlockPosition blockposition) {
        this.writeLong(blockposition.asLong());
    }

    public IChatBaseComponent d() throws IOException {
        return IChatBaseComponent.ChatSerializer.a(this.c(32767));
    }

    public void a(IChatBaseComponent ichatbasecomponent) throws IOException {
        this.a(IChatBaseComponent.ChatSerializer.a(ichatbasecomponent));
    }

    public <T extends Enum<T>> T a(Class<T> oclass) {
        return ((T[]) oclass.getEnumConstants())[this.e()]; // CraftBukkit - fix decompile error
    }

    public void a(Enum<?> oenum) {
        this.b(oenum.ordinal());
    }

    public int e() {
        return com.hpfxd.pandaspigot.network.VarIntUtil.readVarInt(this.a); // PandaSpigot - Optimize VarInt reading
    }

    public long f() {
        long i = 0L;
        int j = 0;

        byte b0;

        do {
            b0 = this.readByte();
            i |= (long) (b0 & 127) << j++ * 7;
            if (j > 10) {
                throw new RuntimeException("VarLong too big");
            }
        } while ((b0 & 128) == 128);

        return i;
    }

    public void a(UUID uuid) {
        this.writeLong(uuid.getMostSignificantBits());
        this.writeLong(uuid.getLeastSignificantBits());
    }

    public UUID g() {
        return new UUID(this.readLong(), this.readLong());
    }

    public void b(int i) {
        com.hpfxd.pandaspigot.network.VarIntUtil.writeVarInt(this.a, i); // PandaSpigot - Optimize VarInt writing
    }

    public void b(long i) {
        while ((i & -128L) != 0L) {
            this.writeByte((int) (i & 127L) | 128);
            i >>>= 7;
        }

        this.writeByte((int) i);
    }

    public void a(NBTTagCompound nbttagcompound) {
        if (nbttagcompound == null) {
            this.writeByte(0);
        } else {
            try {
                NBTCompressedStreamTools.a(nbttagcompound, (DataOutput) (new ByteBufOutputStream(this)));
            } catch (Exception ioexception) { // CraftBukkit - IOException -> Exception
                throw new EncoderException(ioexception);
            }
        }

    }

    public NBTTagCompound h() throws IOException {
        int i = this.readerIndex();
        byte b0 = this.readByte();

        if (b0 == 0) {
            return null;
        } else {
            this.readerIndex(i);
            return NBTCompressedStreamTools.a((DataInput) (new ByteBufInputStream(this)), new NBTReadLimiter(50000L)); // PandaSpigot - Reduce NBT Read Limiter
        }
    }

    public void a(ItemStack itemstack) {
        if (itemstack == null || itemstack.getItem() == null) { // CraftBukkit - NPE fix itemstack.getItem()
            this.writeShort(-1);
        } else {
            this.writeShort(Item.getId(itemstack.getItem()));
            this.writeByte(itemstack.count);
            this.writeShort(itemstack.getData());
            NBTTagCompound nbttagcompound = null;

            if (itemstack.getItem().usesDurability() || itemstack.getItem().p()) {
                // Spigot start - filter
                itemstack = itemstack.cloneItemStack();
                CraftItemStack.setItemMeta(itemstack, CraftItemStack.getItemMeta(itemstack));
                // Spigot end
                nbttagcompound = itemstack.getTag();
            }

            this.a(nbttagcompound);
        }

    }

    public ItemStack i() throws IOException {
        ItemStack itemstack = null;
        short short0 = this.readShort();

        if (short0 >= 0) {
            byte b0 = this.readByte();
            short short1 = this.readShort();

            itemstack = new ItemStack(Item.getById(short0), b0, short1);
            itemstack.setTag(this.h());
            // CraftBukkit start
            if (itemstack.getTag() != null) {
                CraftItemStack.setItemMeta(itemstack, CraftItemStack.getItemMeta(itemstack));
            }
            // CraftBukkit end
        }

        return itemstack;
    }

    public String c(int i) {
        int j = this.e();

        if (j > i * 4) {
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + j + " > " + i * 4 + ")");
        } else if (j < 0) {
            throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
        } else {
            // PandaSpigot start - Switch from readBytes().array() to readBytes(byte[]) as we could be dealing with a DirectByteBuf
            byte[] b = new byte[j];
            this.readBytes(b);
            String s = new String(b, Charsets.UTF_8);
            // PandaSpigot end

            if (s.length() > i) {
                throw new DecoderException("The received string length is longer than maximum allowed (" + j + " > " + i + ")");
            } else {
                return s;
            }
        }
    }

    public PacketDataSerializer a(String s) {
        // PandaSpigot start - Optimize string writing
        int utf8Bytes = io.netty.buffer.ByteBufUtil.utf8Bytes(s);
        if (utf8Bytes > 32767) {
            throw new EncoderException("String too big (was " + s.length() + " bytes encoded, max " + 32767 + ")");
        } else {
            this.b(utf8Bytes);
            this.writeCharSequence(s, Charsets.UTF_8);
            return this;
        }
        // PandaSpigot end
    }

    // AetherSpigot start
    @Override
    public int capacity() {
        return this.a.capacity();
    }

    @Override
    public ByteBuf capacity(final int i) {
        return this.a.capacity(i);
    }

    @Override
    public int maxCapacity() {
        return this.a.maxCapacity();
    }

    @Override
    public ByteBufAllocator alloc() {
        return this.a.alloc();
    }

    @Override
    public ByteOrder order() {
        return this.a.order();
    }

    @Override
    public ByteBuf order(final ByteOrder byteorder) {
        return this.a.order(byteorder);
    }

    @Override
    public ByteBuf unwrap() {
        return this.a.unwrap();
    }

    @Override
    public boolean isDirect() {
        return this.a.isDirect();
    }

    @Override
    public boolean isReadOnly() {
        return this.a.isReadOnly();
    }

    @Override
    public ByteBuf asReadOnly() {
        return this.a.asReadOnly();
    }

    @Override
    public int readerIndex() {
        return this.a.readerIndex();
    }

    @Override
    public ByteBuf readerIndex(final int i) {
        return this.a.readerIndex(i);
    }

    @Override
    public int writerIndex() {
        return this.a.writerIndex();
    }

    @Override
    public ByteBuf writerIndex(final int i) {
        return this.a.writerIndex(i);
    }

    @Override
    public ByteBuf setIndex(final int i, final int j) {
        return this.a.setIndex(i, j);
    }

    @Override
    public int readableBytes() {
        return this.a.readableBytes();
    }

    @Override
    public int writableBytes() {
        return this.a.writableBytes();
    }

    @Override
    public int maxWritableBytes() {
        return this.a.maxWritableBytes();
    }

    @Override
    public boolean isReadable() {
        return this.a.isReadable();
    }

    @Override
    public boolean isReadable(final int i) {
        return this.a.isReadable(i);
    }

    @Override
    public boolean isWritable() {
        return this.a.isWritable();
    }

    @Override
    public boolean isWritable(final int i) {
        return this.a.isWritable(i);
    }

    @Override
    public ByteBuf clear() {
        return this.a.clear();
    }

    @Override
    public ByteBuf markReaderIndex() {
        return this.a.markReaderIndex();
    }

    @Override
    public ByteBuf resetReaderIndex() {
        return this.a.resetReaderIndex();
    }

    @Override
    public ByteBuf markWriterIndex() {
        return this.a.markWriterIndex();
    }

    @Override
    public ByteBuf resetWriterIndex() {
        return this.a.resetWriterIndex();
    }

    @Override
    public ByteBuf discardReadBytes() {
        return this.a.discardReadBytes();
    }

    @Override
    public ByteBuf discardSomeReadBytes() {
        return this.a.discardSomeReadBytes();
    }

    @Override
    public ByteBuf ensureWritable(final int i) {
        return this.a.ensureWritable(i);
    }

    @Override
    public int ensureWritable(final int i, final boolean flag) {
        return this.a.ensureWritable(i, flag);
    }

    @Override
    public boolean getBoolean(final int i) {
        return this.a.getBoolean(i);
    }

    @Override
    public byte getByte(final int i) {
        return this.a.getByte(i);
    }

    @Override
    public short getUnsignedByte(final int i) {
        return this.a.getUnsignedByte(i);
    }

    @Override
    public short getShort(final int i) {
        return this.a.getShort(i);
    }

    @Override
    public short getShortLE(final int i) {
        return this.a.getShortLE(i);
    }

    @Override
    public int getUnsignedShort(final int i) {
        return this.a.getUnsignedShort(i);
    }

    @Override
    public int getUnsignedShortLE(final int i) {
        return this.a.getUnsignedShortLE(i);
    }

    @Override
    public int getMedium(final int i) {
        return this.a.getMedium(i);
    }

    @Override
    public int getMediumLE(final int i) {
        return this.a.getMediumLE(i);
    }

    @Override
    public int getUnsignedMedium(final int i) {
        return this.a.getUnsignedMedium(i);
    }

    @Override
    public int getUnsignedMediumLE(final int i) {
        return this.a.getUnsignedMediumLE(i);
    }

    @Override
    public int getInt(final int i) {
        return this.a.getInt(i);
    }

    @Override
    public int getIntLE(final int i) {
        return this.a.getIntLE(i);
    }

    @Override
    public long getUnsignedInt(final int i) {
        return this.a.getUnsignedInt(i);
    }

    @Override
    public long getUnsignedIntLE(final int i) {
        return this.a.getUnsignedIntLE(i);
    }

    @Override
    public long getLong(final int i) {
        return this.a.getLong(i);
    }

    @Override
    public long getLongLE(final int i) {
        return this.a.getLongLE(i);
    }

    @Override
    public char getChar(final int i) {
        return this.a.getChar(i);
    }

    @Override
    public float getFloat(final int i) {
        return this.a.getFloat(i);
    }

    @Override
    public double getDouble(final int i) {
        return this.a.getDouble(i);
    }

    @Override
    public ByteBuf getBytes(final int i, final ByteBuf bytebuf) {
        return this.a.getBytes(i, bytebuf);
    }

    @Override
    public ByteBuf getBytes(final int i, final ByteBuf bytebuf, final int j) {
        return this.a.getBytes(i, bytebuf, j);
    }

    @Override
    public ByteBuf getBytes(final int i, final ByteBuf bytebuf, final int j, final int k) {
        return this.a.getBytes(i, bytebuf, j, k);
    }

    @Override
    public ByteBuf getBytes(final int i, final byte[] abyte) {
        return this.a.getBytes(i, abyte);
    }

    @Override
    public ByteBuf getBytes(final int i, final byte[] abyte, final int j, final int k) {
        return this.a.getBytes(i, abyte, j, k);
    }

    @Override
    public ByteBuf getBytes(final int i, final ByteBuffer bytebuffer) {
        return this.a.getBytes(i, bytebuffer);
    }

    @Override
    public ByteBuf getBytes(final int i, final OutputStream outputstream, final int j) throws IOException {
        return this.a.getBytes(i, outputstream, j);
    }

    @Override
    public int getBytes(final int i, final GatheringByteChannel gatheringbytechannel, final int j) throws IOException {
        return this.a.getBytes(i, gatheringbytechannel, j);
    }

    @Override
    public int getBytes(final int i, final FileChannel fileChannel, final long l, final int i1) throws IOException {
        return this.a.getBytes(i, fileChannel, l, i1);
    }

    @Override
    public CharSequence getCharSequence(final int i, final int i1, final Charset charset) {
        return this.a.getCharSequence(i, i1, charset);
    }

    @Override
    public ByteBuf setBoolean(final int i, final boolean flag) {
        return this.a.setBoolean(i, flag);
    }

    @Override
    public ByteBuf setByte(final int i, final int j) {
        return this.a.setByte(i, j);
    }

    @Override
    public ByteBuf setShort(final int i, final int j) {
        return this.a.setShort(i, j);
    }

    @Override
    public ByteBuf setShortLE(final int i, final int i1) {
        return this.a.setShortLE(i, i1);
    }

    @Override
    public ByteBuf setMedium(final int i, final int j) {
        return this.a.setMedium(i, j);
    }

    @Override
    public ByteBuf setMediumLE(final int i, final int i1) {
        return this.a.setMediumLE(i, i1);
    }

    @Override
    public ByteBuf setInt(final int i, final int j) {
        return this.a.setInt(i, j);
    }

    @Override
    public ByteBuf setIntLE(final int i, final int i1) {
        return this.a.setIntLE(i, i1);
    }

    @Override
    public ByteBuf setLong(final int i, final long j) {
        return this.a.setLong(i, j);
    }

    @Override
    public ByteBuf setLongLE(final int i, final long l) {
        return this.a.setLongLE(i, l);
    }

    @Override
    public ByteBuf setChar(final int i, final int j) {
        return this.a.setChar(i, j);
    }

    @Override
    public ByteBuf setFloat(final int i, final float f) {
        return this.a.setFloat(i, f);
    }

    @Override
    public ByteBuf setDouble(final int i, final double d0) {
        return this.a.setDouble(i, d0);
    }

    @Override
    public ByteBuf setBytes(final int i, final ByteBuf bytebuf) {
        return this.a.setBytes(i, bytebuf);
    }

    @Override
    public ByteBuf setBytes(final int i, final ByteBuf bytebuf, final int j) {
        return this.a.setBytes(i, bytebuf, j);
    }

    @Override
    public ByteBuf setBytes(final int i, final ByteBuf bytebuf, final int j, final int k) {
        return this.a.setBytes(i, bytebuf, j, k);
    }

    @Override
    public ByteBuf setBytes(final int i, final byte[] abyte) {
        return this.a.setBytes(i, abyte);
    }

    @Override
    public ByteBuf setBytes(final int i, final byte[] abyte, final int j, final int k) {
        return this.a.setBytes(i, abyte, j, k);
    }

    @Override
    public ByteBuf setBytes(final int i, final ByteBuffer bytebuffer) {
        return this.a.setBytes(i, bytebuffer);
    }

    @Override
    public int setBytes(final int i, final InputStream inputstream, final int j) throws IOException {
        return this.a.setBytes(i, inputstream, j);
    }

    @Override
    public int setBytes(final int i, final ScatteringByteChannel scatteringbytechannel, final int j) throws IOException {
        return this.a.setBytes(i, scatteringbytechannel, j);
    }

    @Override
    public int setBytes(final int i, final FileChannel fileChannel, final long l, final int i1) throws IOException {
        return this.a.setBytes(i, fileChannel, l, i1);
    }

    @Override
    public ByteBuf setZero(final int i, final int j) {
        return this.a.setZero(i, j);
    }

    @Override
    public int setCharSequence(final int i, final CharSequence charSequence, final Charset charset) {
        return this.a.setCharSequence(i, charSequence, charset);
    }

    @Override
    public boolean readBoolean() {
        return this.a.readBoolean();
    }

    @Override
    public byte readByte() {
        return this.a.readByte();
    }

    @Override
    public short readUnsignedByte() {
        return this.a.readUnsignedByte();
    }

    @Override
    public short readShort() {
        return this.a.readShort();
    }

    @Override
    public short readShortLE() {
        return this.a.readShortLE();
    }

    @Override
    public int readUnsignedShort() {
        return this.a.readUnsignedShort();
    }

    @Override
    public int readUnsignedShortLE() {
        return this.a.readUnsignedShortLE();
    }

    @Override
    public int readMedium() {
        return this.a.readMedium();
    }

    @Override
    public int readMediumLE() {
        return this.a.readMediumLE();
    }

    @Override
    public int readUnsignedMedium() {
        return this.a.readUnsignedMedium();
    }

    @Override
    public int readUnsignedMediumLE() {
        return this.a.readUnsignedMediumLE();
    }

    @Override
    public int readInt() {
        return this.a.readInt();
    }

    @Override
    public int readIntLE() {
        return this.a.readIntLE();
    }

    @Override
    public long readUnsignedInt() {
        return this.a.readUnsignedInt();
    }

    @Override
    public long readUnsignedIntLE() {
        return this.a.readUnsignedIntLE();
    }

    @Override
    public long readLong() {
        return this.a.readLong();
    }

    @Override
    public long readLongLE() {
        return this.a.readLongLE();
    }

    @Override
    public char readChar() {
        return this.a.readChar();
    }

    @Override
    public float readFloat() {
        return this.a.readFloat();
    }

    @Override
    public double readDouble() {
        return this.a.readDouble();
    }

    @Override
    public ByteBuf readBytes(final int i) {
        return this.a.readBytes(i);
    }

    @Override
    public ByteBuf readSlice(final int i) {
        return this.a.readSlice(i);
    }

    @Override
    public ByteBuf readRetainedSlice(final int i) {
        return this.a.readRetainedSlice(i);
    }

    @Override
    public ByteBuf readBytes(final ByteBuf bytebuf) {
        return this.a.readBytes(bytebuf);
    }

    @Override
    public ByteBuf readBytes(final ByteBuf bytebuf, final int i) {
        return this.a.readBytes(bytebuf, i);
    }

    @Override
    public ByteBuf readBytes(final ByteBuf bytebuf, final int i, final int j) {
        return this.a.readBytes(bytebuf, i, j);
    }

    @Override
    public ByteBuf readBytes(final byte[] abyte) {
        return this.a.readBytes(abyte);
    }

    @Override
    public ByteBuf readBytes(final byte[] abyte, final int i, final int j) {
        return this.a.readBytes(abyte, i, j);
    }

    @Override
    public ByteBuf readBytes(final ByteBuffer bytebuffer) {
        return this.a.readBytes(bytebuffer);
    }

    @Override
    public ByteBuf readBytes(final OutputStream outputstream, final int i) throws IOException {
        return this.a.readBytes(outputstream, i);
    }

    @Override
    public int readBytes(final GatheringByteChannel gatheringbytechannel, final int i) throws IOException {
        return this.a.readBytes(gatheringbytechannel, i);
    }

    @Override
    public CharSequence readCharSequence(final int i, final Charset charset) {
        return this.a.readCharSequence(i, charset);
    }

    @Override
    public int readBytes(final FileChannel fileChannel, final long l, final int i) throws IOException {
        return this.a.readBytes(fileChannel, l, i);
    }

    @Override
    public ByteBuf skipBytes(final int i) {
        return this.a.skipBytes(i);
    }

    @Override
    public ByteBuf writeBoolean(final boolean flag) {
        return this.a.writeBoolean(flag);
    }

    @Override
    public ByteBuf writeByte(final int i) {
        return this.a.writeByte(i);
    }

    @Override
    public ByteBuf writeShort(final int i) {
        return this.a.writeShort(i);
    }

    @Override
    public ByteBuf writeShortLE(final int i) {
        return this.a.writeShortLE(i);
    }

    @Override
    public ByteBuf writeMedium(final int i) {
        return this.a.writeMedium(i);
    }

    @Override
    public ByteBuf writeMediumLE(final int i) {
        return this.a.writeMediumLE(i);
    }

    @Override
    public ByteBuf writeInt(final int i) {
        return this.a.writeInt(i);
    }

    @Override
    public ByteBuf writeIntLE(final int i) {
        return this.a.writeIntLE(i);
    }

    @Override
    public ByteBuf writeLong(final long i) {
        return this.a.writeLong(i);
    }

    @Override
    public ByteBuf writeLongLE(final long l) {
        return this.a.writeLongLE(l);
    }

    @Override
    public ByteBuf writeChar(final int i) {
        return this.a.writeChar(i);
    }

    @Override
    public ByteBuf writeFloat(final float f) {
        return this.a.writeFloat(f);
    }

    @Override
    public ByteBuf writeDouble(final double d0) {
        return this.a.writeDouble(d0);
    }

    @Override
    public ByteBuf writeBytes(final ByteBuf bytebuf) {
        return this.a.writeBytes(bytebuf);
    }

    @Override
    public ByteBuf writeBytes(final ByteBuf bytebuf, final int i) {
        return this.a.writeBytes(bytebuf, i);
    }

    @Override
    public ByteBuf writeBytes(final ByteBuf bytebuf, final int i, final int j) {
        return this.a.writeBytes(bytebuf, i, j);
    }

    @Override
    public ByteBuf writeBytes(final byte[] abyte) {
        return this.a.writeBytes(abyte);
    }

    @Override
    public ByteBuf writeBytes(final byte[] abyte, final int i, final int j) {
        return this.a.writeBytes(abyte, i, j);
    }

    @Override
    public ByteBuf writeBytes(final ByteBuffer bytebuffer) {
        return this.a.writeBytes(bytebuffer);
    }

    @Override
    public int writeBytes(final InputStream inputstream, final int i) throws IOException {
        return this.a.writeBytes(inputstream, i);
    }

    @Override
    public int writeBytes(final ScatteringByteChannel scatteringbytechannel, final int i) throws IOException {
        return this.a.writeBytes(scatteringbytechannel, i);
    }

    @Override
    public int writeBytes(final FileChannel fileChannel, final long l, final int i) throws IOException {
        return this.a.writeBytes(fileChannel, l, i);
    }

    @Override
    public ByteBuf writeZero(final int i) {
        return this.a.writeZero(i);
    }

    @Override
    public int writeCharSequence(final CharSequence charSequence, final Charset charset) {
        return this.a.writeCharSequence(charSequence, charset);
    }

    @Override
    public int indexOf(final int i, final int j, final byte b0) {
        return this.a.indexOf(i, j, b0);
    }

    @Override
    public int bytesBefore(final byte b0) {
        return this.a.bytesBefore(b0);
    }

    @Override
    public int bytesBefore(final int i, final byte b0) {
        return this.a.bytesBefore(i, b0);
    }

    @Override
    public int bytesBefore(final int i, final int j, final byte b0) {
        return this.a.bytesBefore(i, j, b0);
    }

    @Override
    public int forEachByte(final ByteProcessor byteProcessor) {
        return this.a.forEachByte(byteProcessor);
    }

    @Override
    public int forEachByte(final int i, final int i1, final ByteProcessor byteProcessor) {
        return this.a.forEachByte(i, i1, byteProcessor);
    }

    @Override
    public int forEachByteDesc(final ByteProcessor byteProcessor) {
        return this.a.forEachByteDesc(byteProcessor);
    }

    @Override
    public int forEachByteDesc(final int i, final int i1, final ByteProcessor byteProcessor) {
        return this.a.forEachByteDesc(i, i1, byteProcessor);
    }

    @Override
    public ByteBuf copy() {
        return this.a.copy();
    }

    @Override
    public ByteBuf copy(final int i, final int j) {
        return this.a.copy(i, j);
    }

    @Override
    public ByteBuf slice() {
        return this.a.slice();
    }

    @Override
    public ByteBuf retainedSlice() {
        return this.a.retainedSlice();
    }

    @Override
    public ByteBuf slice(final int i, final int j) {
        return this.a.slice(i, j);
    }

    @Override
    public ByteBuf retainedSlice(final int i, final int i1) {
        return this.a.retainedSlice(i, i1);
    }

    @Override
    public ByteBuf duplicate() {
        return this.a.duplicate();
    }

    @Override
    public ByteBuf retainedDuplicate() {
        return this.a.retainedDuplicate();
    }

    @Override
    public int nioBufferCount() {
        return this.a.nioBufferCount();
    }

    @Override
    public ByteBuffer nioBuffer() {
        return this.a.nioBuffer();
    }

    @Override
    public ByteBuffer nioBuffer(final int i, final int j) {
        return this.a.nioBuffer(i, j);
    }

    @Override
    public ByteBuffer internalNioBuffer(final int i, final int j) {
        return this.a.internalNioBuffer(i, j);
    }

    @Override
    public ByteBuffer[] nioBuffers() {
        return this.a.nioBuffers();
    }

    @Override
    public ByteBuffer[] nioBuffers(final int i, final int j) {
        return this.a.nioBuffers(i, j);
    }

    @Override
    public boolean hasArray() {
        return this.a.hasArray();
    }

    @Override
    public byte[] array() {
        return this.a.array();
    }

    @Override
    public int arrayOffset() {
        return this.a.arrayOffset();
    }

    @Override
    public boolean hasMemoryAddress() {
        return this.a.hasMemoryAddress();
    }

    @Override
    public long memoryAddress() {
        return this.a.memoryAddress();
    }

    @Override
    public String toString(final Charset charset) {
        return this.a.toString(charset);
    }

    @Override
    public String toString(final int i, final int j, final Charset charset) {
        return this.a.toString(i, j, charset);
    }

    @Override
    public int hashCode() {
        return this.a.hashCode();
    }

    @Override
    public boolean equals(final Object object) {
        return this.a.equals(object);
    }

    @Override
    public int compareTo(final ByteBuf bytebuf) {
        return this.a.compareTo(bytebuf);
    }

    @Override
    public String toString() {
        return this.a.toString();
    }

    @Override
    public ByteBuf retain(final int i) {
        return this.a.retain(i);
    }

    @Override
    public ByteBuf retain() {
        return this.a.retain();
    }

    @Override
    public ByteBuf touch() {
        return this.a.touch();
    }

    @Override
    public ByteBuf touch(final Object o) {
        return this.a.touch(o);
    }

    @Override
    public int refCnt() {
        return this.a.refCnt();
    }

    @Override
    public boolean release() {
        return this.a.release();
    }

    @Override
    public boolean release(final int i) {
        return this.a.release(i);
    }
    // AetherSpigot end
}
