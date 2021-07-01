package fteam;

import arc.*;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.*;
import mindustry.Vars;
import mindustry.content.UnitTypes;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.mod.Mods;
import mindustry.mod.Plugin;
import mindustry.world.Tile;

public class ForceTeamPlugin extends Plugin {
    private double TEAMPLUGINVERSION = 6.126;

    private Team setTeam = null;
    private String cmdRunner = null; //uuid


    private ObjectMap<Player, Team> rememberSpectate = new ObjectMap<>();

    //register event handlers and create variables in the constructor
    public ForceTeamPlugin(){
        Events.on(PlayerJoin.class, event -> {
            if(!event.player.admin() && setTeam != null){
                Call.setPlayerTeamEditor(event.player, setTeam);
                event.player.team(setTeam);
                event.player.sendMessage("[orange] > ForceTeam is active - good luck");
            }
        });

        Events.on(PlayerLeave.class, event -> {
            //if team has 0 players --> problem STALL
            if(event.player.uuid().equals(cmdRunner) && this.setTeam != null){
                //disable forceteam
                this.setTeam = null;
                Call.sendMessage("[orange] > ForceTeam disabled");
            }
            /*
            if(setTeam != null) {
                Team pt = event.player.team();
                if (!pt.cores().isEmpty() && Groups.player.count(player -> (player.team() == pt && player != event.player)) == 0) {
                    //reshuffle to block problems
                    Call.sendMessage("<<< reshuffle players to prevent deadlock >>>");
                    int current = 0;
                    int maxsw = Math.round((Groups.player.size() / 2f) - 0.1f);
                    for (Player p : Groups.player) {
                        if (current >= maxsw) break;
                        Call.setPlayerTeamEditor(p, pt);
                        p.team(pt);
                        current++;
                    }
                }
                Call.sendMessage("[orange] forceTeam disabled");
                setTeam = null;
                Vars.state.rules.tags.put("forceteam", "false");
            }

             */
        });

        Events.on(GameOverEvent.class, event -> {
            /*
            if(setTeam != null){
                setTeam = null;
                Call.sendMessage("[orange] forceteam disabled");
                Vars.state.rules.tags.put("forceteam", "false");
            }
             */
        });

        Timer.schedule(this::checkCompatibility, 3f);
    }

    private void checkCompatibility(){
        Mods.LoadedMod temp = Vars.mods.list().find(m -> m.name.equalsIgnoreCase("teamplugin"));
        if(temp != null){
            if(Strings.parseDouble(temp.meta.version, 0) < TEAMPLUGINVERSION){
                Log.err("<forceteam> Teamplugin is outdated! Mininmum required version @ ", TEAMPLUGINVERSION);
            }
        }else{
            Log.info("<forceteam> Compatible with TeamPlugin @", TEAMPLUGINVERSION);
        }
    }

    //register commands that run on the server
    @Override
    public void registerServerCommands(CommandHandler handler){
    }

    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("test", "kill", (args, player)-> {
            if (player.unit().type != UnitTypes.alpha && player.unit().type != UnitTypes.beta && player.unit().type != UnitTypes.gamma) {
                player.clearUnit();
                player.deathTimer = Player.deathDelay + 1f;
            }
        });

        handler.<Player>register("forceteam", "[team] [transfer_players]","[scarlet]Admin only[] For info use the command without arguments.", (args, player) -> {
                if(!player.admin()){
                    player.sendMessage("[scarlet]Admin only");
                    return;
                }

                //check if sandbox or pvp
                if (!Vars.state.rules.pvp && !Vars.state.rules.infiniteResources) {
                    player.sendMessage("[scarlet]Only possible in PVP mode.\n");
                    return;
                }

                if (args.length == 0 || args.length > 2) {
                    StringBuilder sbinfo = new StringBuilder();
                    sbinfo.append("[orange]/forceteam[] - INFO\n\nThis command will force new players to join a selected team.\n");
                    sbinfo.append("[sky] arguments[]\n* [team] : selected team or [accent]off[] to disable\n");
                    sbinfo.append("* [transfer_players] : (0 or 1) Transfer all current online players (except admins) to the selected team. Default: 0");
                    sbinfo.append("\n\n[green]Auto disable after a game over![]");
                    Call.infoMessage(player.con, sbinfo.toString());
                    return;
                }
                //disable first
                this.cmdRunner = null;
                disableFT();
                // select team
                switch (args[0]) {
                    case "off":
                        this.setTeam = null;
                        player.sendMessage("You disabled forceTeam");
                        Call.sendMessage(" [orange] forceTeam disabled");
                        //randomize again?
                        return;
                    case "sharded":
                        this.setTeam = Team.sharded;
                        break;
                    case "blue":
                        this.setTeam = Team.blue;
                        break;
                    case "crux":
                        this.setTeam = Team.crux;
                        break;
                    case "derelict":
                        this.setTeam = Team.derelict;
                        break;
                    case "green":
                        this.setTeam = Team.green;
                        break;
                    case "purple":
                        this.setTeam = Team.purple;
                        break;
                    default:
                        this.setTeam = null;
                        player.sendMessage("[scarlet]ABORT: Team not found[] - available teams:");
                        for (int i = 0; i < 6; i++) {
                            if (!Team.baseTeams[i].cores().isEmpty()) {
                                player.sendMessage(setTeam.baseTeams[i].name);
                            }
                        }
                        return;
                }
                //check if the selected team has cores
                if (this.setTeam.cores().isEmpty()) {
                    player.sendMessage(Strings.format("[scarlet]ABORT: The selected team (@) has 0 cores...", this.setTeam.name));
                    return;
                }
                //enable forceTeam
                Call.sendMessage(Strings.format("[orange]Forceteam is enabled - selected: @", this.setTeam.name));
                this.cmdRunner = player.uuid();
                enableFT();


                if(args.length == 2){
                    if (!Strings.canParseInt(args[1])) {
                        player.sendMessage("[scarlet][transfer_players] should be 0 or 1");
                        return;
                    }
                    if(Strings.parseInt(args[1]) == 1){
                        switchPlayers();
                    }
                }
        });

        handler.<Player>register("ftt", "[scarlet]Admin only[] FORCE SWITCH", (args, player)->{
            if(!player.admin()){
                player.sendMessage("[scarlet] Admin only!");
                return;
            }
            if(setTeam == null){
                player.sendMessage("[orange] > no team was set!");
            }else {
                player.sendMessage(String.format("[green] > try to move all players (except admins) to %s", setTeam.name));

                switchPlayers();
            }
        });
    }

    private void switchPlayers(){
        Tile coreTile = this.setTeam.core().tileOn();
        float x = coreTile.drawx();
        float y = coreTile.drawy();

        Seq<Player> currentPlayers = Groups.player.copy(new Seq<Player>());
        for(int i=0; i<currentPlayers.size; i++){
            Player p = currentPlayers.get(i);
            if(!p.admin() && p!=null){
                Call.setPlayerTeamEditor(p, setTeam);
                p.team(setTeam);
                p.snapSync();
                if(p.unit().type != UnitTypes.alpha && p.unit().type != UnitTypes.beta && p.unit().type != UnitTypes.gamma){
                    p.clearUnit();
                    p.deathTimer = Player.deathDelay + 1f;
                }else {
                    Call.setPosition(p.con, x, y);
                    p.unit().set(x, y);
                    p.snapSync();
                }
            }
        }
    }

    private void disableFT(){
        this.setTeam = null;
        Vars.state.rules.tags.put("forceteam", "false");
        Log.info(Strings.format("<forceteam> disabled (@)",Vars.state.rules.tags.getBool("forceteam")));
    }

    private void enableFT(){
        //this.setTeam = team;
        Vars.state.rules.tags.put("forceteam", "true");
        Log.info(Strings.format("<forceteam> enabled (@)",Vars.state.rules.tags.getBool("forceteam")));
    }
}
