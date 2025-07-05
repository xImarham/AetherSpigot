package xyz.aether.spigot.protocol;

public interface PacketHandler {
  boolean handleReceivedPacket(Object connection, Object packet);

  default boolean handleSentPacket(Object connection, Object packet) {
    return true;
  }
}