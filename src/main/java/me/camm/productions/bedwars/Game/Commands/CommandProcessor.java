package me.camm.productions.bedwars.Game.Commands;

import me.camm.productions.bedwars.Game.Arena;
import me.camm.productions.bedwars.Game.GameInitializer;
import me.camm.productions.bedwars.Game.GameRunner;
import me.camm.productions.bedwars.Game.BattlePlayer;
import me.camm.productions.bedwars.Game.Teams.BattleTeam;
import me.camm.productions.bedwars.Game.Teams.TeamColor;
import me.camm.productions.bedwars.BedWars;
import me.camm.productions.bedwars.Files.TeamFileJsonParser;
import me.camm.productions.bedwars.Files.WorldDataJsonParser;
import me.camm.productions.bedwars.Util.Helpers.BlockTagManager;
import me.camm.productions.bedwars.Util.Helpers.ChatSender;
import me.camm.productions.bedwars.Util.Locations.Boundaries.GameBoundary;
import me.camm.productions.bedwars.Util.Exceptions.BedWarsException;
import me.camm.productions.bedwars.Util.Exceptions.CommandPermissionException;
import me.camm.productions.bedwars.Util.Exceptions.InitializationException;
import me.camm.productions.bedwars.Util.Exceptions.StateException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import static me.camm.productions.bedwars.Game.Commands.CommandKeyword.*;


/*
 * @author CAMM
 * This class handles command processing from the game initializer
 */
public class CommandProcessor {


    private GameRunner runner;
    private final ChatSender messager;


    public CommandProcessor(){
        runner = null;
        messager = ChatSender.getInstance();
    }



    /*
     *
     * @param sender commandsender
     * @param plugin plugin
     * @return a game runner
     * @throws BedWarsException if the sender has no perms, or if a problem occurred
     */
    public GameRunner initRunner(CommandSender sender, Plugin plugin, GameInitializer initializer) throws Exception {

        //make sure they have the permission
        if (noPermission(sender, SETUP))
            throw getPermException(SETUP);



        //try to read the config from the config files and make a new
        //arena object
        Arena arena;
        GameRunner runner;

        ArrayList<BattleTeam> teams;
        WorldDataJsonParser fileReader = new WorldDataJsonParser();

            arena = fileReader.getArena();
        if (arena==null)
            throw new InitializationException("Was not able to init the arena! (Check the config)");

        BlockTagManager.initialize(arena);
        GameBoundary.initializeTagManager();

        /////////////////////////////


        messager.sendMessage("Attempting to register the map. Expect some lag.");
        final long timeInitial = System.currentTimeMillis();

        arena.registerMap();

        try {
            teams = new TeamFileJsonParser(arena).getTeams();
        }
        catch (NullPointerException e) {
            throw new InitializationException(e.getMessage());
        }


            //ensure that there are teams for opposition in the game
            //if valid, then register their areas from the config and put them into
            //the arena


            if (teams==null||teams.size()<=1) {
                throw new InitializationException("The teams are invalid!" +
                        (teams == null ? (" teams are not defined") : (" There must be more than 1 team")));
            }

        arena.addTeams(teams);
        runner = new GameRunner(plugin, arena, initializer);

        //check if this.runner was not null.
        //if it was, then reset the packethandler.

        arena.registerTeamZones();
        this.runner = runner;

        messager.sendMessage("Registered the map! Took "+(System.currentTimeMillis() - timeInitial)+" ms.");
        return runner;
    }


    /*
     * method for private chatting between team members in a team
     *
     * @param sender commandsender
     * @param args arguments
     * @throws BedWarsException if an error occurs
     */
    public void shout(CommandSender sender, String[] args) throws BedWarsException{

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED+"You must be a player to use this command.");
            return;
        }

        //check if they have perms
        if (noPermission(sender, SHOUT))
            throw getPermException(SHOUT);

        if (runner==null || !runner.isRunning())
            throw new StateException(ChatColor.RED+"The game is not running!");
        //You can't shout if the game isn't running


        //check if the player is registered.
        Map<UUID, BattlePlayer> players = runner.getArena().getPlayers();
        BattlePlayer current = players.getOrDefault(((Player) sender).getUniqueId(),null);
        if (current == null)
            return;


        //use the arguments of the command to create the message.
        StringBuilder message = new StringBuilder();
        for (String string: args) {
            message.append(string).append(" ");
        }

        //send the message
        TeamColor color = current.getTeam().getTeamColor();
       this.messager.broadcastMessage(ChatColor.YELLOW+"[SHOUT]"+
                color.getChatColor()+"<"+current.getRawPlayer().getName()+">"+ChatColor.GRAY+message,null);

    }


    /*
     * Attempts to start the game.
     *
     *
     * @param sender sender
     * @throws BedWarsException if an error occurs
     */
    public void startGame(CommandSender sender) throws BedWarsException {
        if (noPermission(sender, START))
            throw getPermException(START);

        if (runner==null)
            throw new StateException(ChatColor.RED+"The arena is not set up!");

        if (runner.isRunning())
            throw new StateException(ChatColor.RED+"The game is already running!");


        Collection<BattleTeam> values = runner.getArena().getTeams().values();


           int notOpposed = values.size();
            for (BattleTeam team: values)
            {
                if (team.getRemainingPlayers()==0)
                    notOpposed--;
            }

            //TODO turn on after done refactoring

            // if (!(notOpposed<2)) //game can start b/c there are at least 2 teams
                   runner.prepareAndStart();
          //      else
          //          throw new StateException(ChatColor.RED+"There must be opposition for a game to start!");

    }


    /*
     * ends the game manually
     * @param sender command sender
     */
    public void manualEndGame(CommandSender sender) throws BedWarsException {

        if (noPermission(sender, END)) {
            throw getPermException(END);
        }

        if (runner == null || !runner.isRunning())
            throw new StateException(ChatColor.RED+"The game is not running!");

        if (sender instanceof Player) {
            sender.sendMessage(ChatColor.GOLD+sender.getName()+" has ended the game with a manual override.");
        }
        else sender.sendMessage("The game has ended the game with a manual override.");

        runner.endGame(null);


    }


    /*
     * Attempts to register a player
     *
     *
     * @param sender Sender of the command
     * @throws BedWarsException if conditions are not met for safe registration
     */
    public void registerPlayer(CommandSender sender) throws BedWarsException{

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED+"You must be a player to use this command.");
            return;
        }
        if (noPermission(sender, REGISTER))
            throw getPermException(REGISTER);

        if (runner==null)
            throw new StateException(ChatColor.RED+"The arena is not set up! Please do /setup first!");

        if (runner.getArena().isRegistering())
            throw new StateException(ChatColor.RED+"The arena is still in the process of registering zones!");

        if (runner.isRunning())
            throw new StateException(ChatColor.RED+"The game is running! Wait for it to finish first!");

        ((Player)sender).openInventory((Inventory)runner.getJoinInventory());
    }




    /*
     *
     * @param player sender of the command
     * @throws BedWarsException if several conditions are not met
     */
    public void unregister(CommandSender player) throws BedWarsException {

        if (!(player instanceof Player)) {
            player.sendMessage(ChatColor.RED+"You must be a player to use this command.");
            return;
        }

        //at this point, the commandsender should be a player
        // (this method is only called from the game initializer, which already checks that the sender is a player)
        Player p = (Player)player;


        if (noPermission(player, UNREGISTER))
            throw getPermException(UNREGISTER);

        if (runner == null) {
            throw new StateException(ChatColor.RED+"The arena is not set up! Please do /setup first!");
        }

        if (runner.isRunning()){
            throw new StateException(ChatColor.RED+"You can't unregister while the game is running!");
        }

         runner.unregisterPlayer(p);
        this.messager.sendMessage(ChatColor.YELLOW+p.getName()+" has unregistered!");

    }




    /*
     * Checks if the player has permission to run a command
     *
     * @param sender sender of the command
     * @param word command label to check for permissions
     * @return if the sender does not have permission
     */
    private boolean noPermission(CommandSender sender, CommandKeyword word) {
        return !sender.hasPermission(word.getPerm());
    }

    private BedWarsException getPermException(CommandKeyword word){
        Plugin p = BedWars.getInstance();
        PluginCommand command = p.getServer().getPluginCommand(word.getWord());
        if (command == null)
            return new CommandPermissionException(ChatColor.RED+"You have no permission!");


        String permMessage = command.getPermissionMessage();
        return new CommandPermissionException(permMessage == null ? ChatColor.RED+"You have no permission!": permMessage);

    }



}


