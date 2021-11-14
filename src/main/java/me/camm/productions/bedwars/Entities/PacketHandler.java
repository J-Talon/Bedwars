package me.camm.productions.bedwars.Entities;

import io.netty.channel.*;
import me.camm.productions.bedwars.Arena.GameRunning.Arena;
import me.camm.productions.bedwars.Arena.Players.BattlePlayer;
import me.camm.productions.bedwars.Util.Helpers.ItemHelper;
import net.minecraft.server.v1_8_R3.PacketPlayInUseEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_8_R3.PacketPlayOutRemoveEntityEffect;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class PacketHandler extends ItemHelper
{
    static HashMap<UUID, Channel> channels;
    static HashMap<Integer, UUID> invisiblePlayers;
    private final Arena arena;
    private final ArrayList<ShopKeeper> keepers;


    static {
        channels = new HashMap<>();
        invisiblePlayers = new HashMap<>();
    }



    public PacketHandler(ArrayList<ShopKeeper> shops, Arena arena)
    {
        this.keepers = shops;
        this.arena = arena;
    }

    public boolean contains(Player player)
    {
     return channels.containsKey(player.getUniqueId());
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

        ChannelDuplexHandler handler = new ChannelDuplexHandler()
        {
            //reading packets sent from player
            @Override
            public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception
            {

                if (o instanceof PacketPlayInUseEntity)
                {
                    if (!arena.getPlayers().containsKey(player.getUniqueId()))
                    {
                        super.channelRead(channelHandlerContext, o);
                        return;
                    }

                    if (getValue(o,"action").toString().equalsIgnoreCase("ATTACK")) {
                        super.channelRead(channelHandlerContext, o);
                        return;
                    }

                    if (getValue(o,"action").toString().equalsIgnoreCase("INTERACT_AT"))
                        return;

                        int id = (int)getValue(o, "a");
                        if (id==-1)
                        {
                           // super.channelRead(channelHandlerContext, o);
                            return;
                        }

                        ShopKeeper clicked = null;
                        for (ShopKeeper keeper: keepers)
                        {
                            if (id==keeper.getId())
                            {
                                clicked = keeper;
                                break;
                            }
                        }

                        if (clicked==null)
                        {
                            super.channelRead(channelHandlerContext, o);
                            return;
                        }

                        BattlePlayer openingPlayer = arena.getPlayers().get(player.getUniqueId());

                        if (clicked.getIsTeamKeeper())
                        {
                            player.sendMessage("[DEBUG]Open team upgrades");
                        }
                        else
                        {
                            new BukkitRunnable()
                            {
                               public void run()
                               {
                                   player.sendMessage("[DEBUG]open quick buy");
                                   player.openInventory(openingPlayer.getShopManager().getQuickBuy().getInventory());
                                   cancel();
                               }
                            }.runTaskAsynchronously(arena.getPlugin());

                        }
                }
                super.channelRead(channelHandlerContext, o);
            }

            //writing packets to a player.
            //So this is where we discriminate and see which packets we intercept, and which ones we write.
            @Override
            public void write(ChannelHandlerContext channelHandlerContext, Object o, ChannelPromise channelPromise) throws Exception {


                /*
                If we send a packet to the invis player that voids all of their items, then
                add the invis player to the handler, then
                resend a packet that gives them back, technically that should work right?
                 */

                if (o instanceof PacketPlayOutEntityEquipment)
                {
                    try
                    {
                        Field entityId = PacketPlayOutEntityEquipment.class.getDeclaredField("a");
                        entityId.setAccessible(true);
                        int value = (int)entityId.get(o);
                        entityId.setAccessible(false);

                        Field item = PacketPlayOutEntityEquipment.class.getDeclaredField("c");
                        item.setAccessible(true);
                        net.minecraft.server.v1_8_R3.ItemStack stack = (net.minecraft.server.v1_8_R3.ItemStack)item.get(o);

                        ItemStack bukkitItem = toBukkitItem(stack);


                        if (invisiblePlayers.containsKey(value)&&isArmor(bukkitItem.getType()))
                        {
                                BattlePlayer invisible = arena.getPlayers().get(invisiblePlayers.get(value));
                                BattlePlayer receiving = arena.getPlayers().get(player.getUniqueId());

                                //If they are not on the same team.
                                if (!invisible.getTeam().equals(receiving.getTeam()))
                                      return;
                        }
                    }
                    catch (Exception ignored)
                    {

                    }
                }


                if (o instanceof PacketPlayOutRemoveEntityEffect) {
                    TEST_INVIS:
                    {
                        try {
                            PacketPlayOutRemoveEntityEffect packet = (PacketPlayOutRemoveEntityEffect) o;
                            Field potionEffect = PacketPlayOutRemoveEntityEffect.class.getDeclaredField("b");
                            potionEffect.setAccessible(true);
                            int effectId = (int) potionEffect.get(packet);
                            potionEffect.setAccessible(false);


                            if (effectId != 14)  //the effect id for invisibility is 14
                                break TEST_INVIS;


                                Field entityId = PacketPlayOutRemoveEntityEffect.class.getDeclaredField("a");
                                entityId.setAccessible(true);
                                int entity = (int) entityId.get(packet);
                                entityId.setAccessible(false);

                            if (!invisiblePlayers.containsKey(entity))
                                 break TEST_INVIS;

                             UUID id = invisiblePlayers.get(entity);

                             if (!arena.getPlayers().containsKey(id))
                                 break TEST_INVIS;

                             BattlePlayer currentPlayer = arena.getPlayers().get(id);
                            removeInvisiblePlayer(currentPlayer.getRawPlayer());
                             currentPlayer.removeUnprocessedInvisibility();


                        } catch (Exception ignored) {

                        }
                    }
                }





                super.write(channelHandlerContext, o, channelPromise);
            }
        };



        line.addBefore("packet_handler",player.getName(),handler);
        channels.put(player.getUniqueId(),channel);
    }

    public Object getValue(Object object, String value)
    {
        Object result;
        try
        {
          Field field = object.getClass().getDeclaredField(value);
                field.setAccessible(true);
                result = field.get(object);
                field.setAccessible(false);
                return result;
        }
        catch (Exception e)
        {
            return null;
        }
    }



    public void removePlayer(Player player)
    {
        if (!player.isOnline())
            return;

        Channel channel = ((CraftPlayer)player).getHandle().playerConnection.networkManager.channel;
        channel.eventLoop().submit(() -> {
            channel.pipeline().remove(player.getName());
        });
        channels.remove(player.getUniqueId());
    }
}