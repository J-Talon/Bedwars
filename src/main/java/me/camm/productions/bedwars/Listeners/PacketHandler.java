package me.camm.productions.bedwars.Listeners;

import io.netty.channel.*;
import me.camm.productions.bedwars.Game.Arena;
import me.camm.productions.bedwars.Game.Entities.ShopKeeper;
import me.camm.productions.bedwars.Util.Helpers.ChatSender;
import me.camm.productions.bedwars.Util.Helpers.ItemHelper;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;


import java.util.*;

public class PacketHandler extends ItemHelper
{
    private static HashMap<UUID, Channel> channels;
    private static HashMap<Integer, UUID> invisiblePlayers;
    private final Arena arena;
    private final ArrayList<ShopKeeper> keepers;
    private final ChatSender sender;


    static {
        channels = new HashMap<>();
        invisiblePlayers = new HashMap<>();
    }




    public PacketHandler(ArrayList<ShopKeeper> shops, Arena arena)
    {
        this.keepers = shops;
        this.arena = arena;
        sender = ChatSender.getInstance();
    }

    public void clearEntries(){
        channels.clear();
        invisiblePlayers.clear();
    }

    public boolean channelsContains(Player player)
    {
     return channels.containsKey(player.getUniqueId());
    }

    public UUID getInvisiblePlayer(int entityId){
        return invisiblePlayers.get(entityId);
    }

    public boolean invisiblesContains(int entityId){
        return invisiblePlayers.containsKey(entityId);
    }

    public List<ShopKeeper> getKeepers(){
        return keepers;
    }



    public synchronized void addInvisiblePlayer(Player player)
    {
        invisiblePlayers.put(player.getEntityId(),player.getUniqueId());
    }

    public synchronized void removeInvisiblePlayer(Player player)
    {
        invisiblePlayers.remove(player.getEntityId());
    }


    public void addPlayer(Player player)
    {
        if (!player.isOnline())
            return;

        Channel channel = ((CraftPlayer)player).getHandle().playerConnection.networkManager.channel;
        ChannelPipeline line = channel.pipeline();

        BedwarsDuplexHandler handler = new BedwarsDuplexHandler(player,arena,this);

        line.addBefore("packet_handler",player.getName(),handler);
        channels.put(player.getUniqueId(),channel);
    }





    public void removePlayer(Player player)
    {
        if (!player.isOnline())
            return;



        Channel channel = ((CraftPlayer)player).getHandle().playerConnection.networkManager.channel;


        ChannelPipeline pipe = channel.pipeline();
        if (pipe.toMap().containsKey(player.getName())) {
           pipe.remove(player.getName());
         }
        channels.remove(player.getUniqueId());
    }


    //debug
    public Arena getArena(){
        return arena;
    }
}
