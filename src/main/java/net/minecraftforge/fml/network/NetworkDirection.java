/*
 * Minecraft Forge
 * Copyright (c) 2016-2022.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.fml.network;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.login.client.CCustomPayloadLoginPacket;
import net.minecraft.network.login.server.SCustomPayloadLoginPacket;
import net.minecraft.network.play.client.CCustomPayloadPacket;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.unsafe.UnsafeHacks;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum NetworkDirection
{
    PLAY_TO_SERVER(NetworkEvent.ClientCustomPayloadEvent::new, LogicalSide.CLIENT, CCustomPayloadPacket.class, 1),
    PLAY_TO_CLIENT(NetworkEvent.ServerCustomPayloadEvent::new, LogicalSide.SERVER, SCustomPayloadPlayPacket.class, 0),
    LOGIN_TO_SERVER(NetworkEvent.ClientCustomPayloadLoginEvent::new, LogicalSide.CLIENT, CCustomPayloadLoginPacket.class, 3),
    LOGIN_TO_CLIENT(NetworkEvent.ServerCustomPayloadLoginEvent::new, LogicalSide.SERVER, SCustomPayloadLoginPacket.class, 2);

    private final BiFunction<ICustomPacket<?>, Supplier<NetworkEvent.Context>, NetworkEvent> eventSupplier;
    private final LogicalSide logicalSide;
    private final Class<? extends IPacket> packetClass;
    private final int otherWay;

    private static final Reference2ReferenceArrayMap<Class<? extends IPacket>, NetworkDirection> packetLookup;

    static {
        packetLookup = Stream.of(values()).
                collect(Collectors.toMap(NetworkDirection::getPacketClass, Function.identity(), (m1,m2)->m1, Reference2ReferenceArrayMap::new));
    }

    NetworkDirection(BiFunction<ICustomPacket<?>, Supplier<NetworkEvent.Context>, NetworkEvent> eventSupplier, LogicalSide logicalSide, Class<? extends IPacket> clazz, int i)
    {
        this.eventSupplier = eventSupplier;
        this.logicalSide = logicalSide;
        this.packetClass = clazz;
        this.otherWay = i;
    }

    private Class<? extends IPacket> getPacketClass() {
        return packetClass;
    }
    public static <T extends ICustomPacket<?>> NetworkDirection directionFor(Class<T> customPacket)
    {
        return packetLookup.get(customPacket);
    }

    public NetworkDirection reply() {
        return NetworkDirection.values()[this.otherWay];
    }
    public NetworkEvent getEvent(final ICustomPacket<?> buffer, final Supplier<NetworkEvent.Context> manager) {
        return this.eventSupplier.apply(buffer, manager);
    }

    public LogicalSide getOriginationSide()
    {
        return logicalSide;
    }

    public LogicalSide getReceptionSide() { return reply().logicalSide; };

    @SuppressWarnings("unchecked")
    public <T extends IPacket<?>> ICustomPacket<T> buildPacket(Pair<PacketBuffer,Integer> packetData, ResourceLocation channelName)
    {
        ICustomPacket<T> packet = (ICustomPacket<T>)UnsafeHacks.newInstance(getPacketClass());
        packet.setName(channelName);
        packet.setData(packetData.getLeft());
        packet.setIndex(packetData.getRight());
        return packet;
    }
}
