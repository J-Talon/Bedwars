package me.camm.productions.bedwars.Util.Locations;


import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;


public class GameBoundary
{
    private int x1, x2, y1, y2, z1, z2;
    private int[] bounds;

    public GameBoundary(int[] bounds) {
        this.bounds = bounds;
        analyze();
        reArrange();
        dissectArray();
    }


    private void analyze()
    {
            if (bounds==null||bounds.length != 6)
                bounds = new int[]{0, 0, 0, 0, 0, 0};
    }


    private void dissectArray() {
        if (bounds != null && bounds.length == 6) {
            x1 = bounds[0];
            x2 = bounds[1];
            y1 = bounds[2];
            y2 = bounds[3];
            z1 = bounds[4];
            z2 = bounds[5];
        } else {
            this.bounds = new int[]{0, 0, 0, 0, 0, 0};
        }
    }

    private void reArrange(int one, int two, int [] order, int repetition) //method loop for rearranging
    {
        int placeHold;

        final int LENGTH = 6;
        final int TOTAL = 2;  //total amount of repetitions for the method

        if (order.length == LENGTH)  //if the length of the array is 6
        {
            if (order[one] > order[two]) {
                placeHold = order[two];
                order[two] = order[one];
                order[one] = placeHold;
            }
            one += 2;
            two += 2;

            if (repetition < TOTAL) {
                repetition++;
                reArrange(one, two, order, repetition);
            } else
                bounds = order;
        }
        else
            bounds = new int[]{0,0,0,0,0,0};
    }

    private void reArrange()  //invoking the method loop
    {
        final int ONE = 0;
        final int TWO = 1;
        final int REPETITION = 0;
        reArrange(ONE, TWO, bounds, REPETITION);
    }

    @SuppressWarnings("deprecation")
    public void replace(Material replacement, Material toReplace, byte[] toReplaceData, World world)
    {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    Block block = world.getBlockAt(x, y, z);

                    CURRENT_BLOCK:
                    {
                        if (block.getType() != toReplace)
                        {
                            continue;
                        }

                            for (byte data : toReplaceData)
                            {
                                if (data != block.getData())
                                    continue;

                                block.setType(replacement);

                                //Breaking out of the for loop: for (byte data: ...)
                                break CURRENT_BLOCK;
                                //Break from the current targeted block in the registration process.

                            }

                    }
                }
            }
        }
    }

    public boolean doesBoxContainBlock(Material mat, World world)
    {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    Block block = world.getBlockAt(x, y, z);
                  if (block.getType()==mat)
                      return true;
                }
            }
        }
        return false;
    }

    public void unregister(String metadata, World world, Plugin plugin) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    block.removeMetadata(metadata, plugin);
                }
            }
        }
    }

    public void register(World world, String type, int blocks, Plugin plugin)//include stuff to register different blocks [e.g. air, wood, wool{include colors}]
    {

        if (blocks > 0) //if 0, then register blocks that are not air
            registerAll(world, type, plugin);  //if 0, then register blocks that are not air
        else if (blocks == 0)
            registerSolids(world, type, plugin);
        else
            registerAir(world, type, plugin); //only register the air

    }
    //1 = all, 0 = !air, -1 = air only



    //registers all blocks in the bounds with a metadata of string type
    private void registerAll(World world, String type, Plugin plugin) {

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {

                    Block block = world.getBlockAt(x, y, z);
                    block.setMetadata(type, new FixedMetadataValue(plugin, 1));

                }
            }
        }
        sendRegistry(plugin,type);
    }

    //Registering everything that is not air
    private void registerSolids(World world, String type, Plugin plugin) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.AIR)
                        block.setMetadata(type, new FixedMetadataValue(plugin, 1));
                }
            }
        }
        sendRegistry(plugin,type);
    }

   //registering all materials that are air
    private void registerAir(World world, String type, Plugin plugin) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.AIR)
                        block.setMetadata(type, new FixedMetadataValue(plugin, 1));
                }
            }
        }
        sendRegistry(plugin,type);
    }

    //registering all materials except for that provided, and air
    public void registerButNotBlockOrAir(World world, String type, Plugin plugin, Material notRegister) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != notRegister && block.getType() != Material.AIR)  //if not air and not the material, then register
                        block.setMetadata(type, new FixedMetadataValue(plugin, 1));
                }
            }
        }
       sendRegistry(plugin,type);
    }

    //Checking if the boundary contains a coordinate
    public boolean containsCoordinate(int x, int y, int z)
    {
        return (x1<=x && x<=x2) && (x1<=y && y<=y2) && (z1<=z && z<=z2);
    }

    private void sendRegistry(Plugin plugin, String type)
    {
        plugin.getServer().broadcastMessage(ChatColor.YELLOW + "[BEDWARS] [MAP REGISTER] Registered Zone from (x1=" + x1 + ",y1=" + y1 + ",z1=" + z1 + ") to (x2=" + x2 + ",y2=" + y2 + ",z2=" + z2 + ") with " + type);
    }

    public int[] getValues() {
        reArrange();
        return bounds;
    }
}
