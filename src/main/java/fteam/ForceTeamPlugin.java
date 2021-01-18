package fteam;

import arc.*;
import arc.struct.ObjectMap;
import arc.util.*;
import mindustry.Vars;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.world.Tile;

// use java.util for now
import java.util.Arrays;

public class ForceTeamPlugin extends Plugin {
    //private boolean DEBUG = false;
    private long TEAM_CD = 5000L;
    private Team setTeam = null;

    private ObjectMap<Player, Team> rememberSpectate = new ObjectMap<>();

    //register event handlers and create variables in the constructor
    public ForceTeamPlugin(){
        Events.on(PlayerJoin.class, event -> {
            if(!event.player.admin() && setTeam != null){
                Call.setPlayerTeamEditor(event.player, setTeam);
                event.player.sendMessage("[orange] > ForceTeam is active - good luck");
            }
        });

        Events.on(PlayerLeave.class, event -> {
            //if team has 0 players --> problem STALL
        });

        Events.on(GameOverEvent.class, event -> {
            if(setTeam != null){
                setTeam = null;
                Call.sendMessage("[orange] forceTeam disabled");
            }
        });
    }

    //register commands that run on the server
    @Override
    public void registerServerCommands(CommandHandler handler){
    }

    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("forceteam", "[team] [transfer_players]","[scarlet]Admin only[] For info use the command without arguments.", (args, player) ->{
            if(!Vars.state.rules.pvp){
                player.sendMessage("[scarlet]Only possible in PVP mode.\n");
                return;
            }

            if(args.length == 0 || args.length > 2){
                StringBuilder sbinfo = new StringBuilder();
                sbinfo.append("[orange]/forceteam[] - INFO\n\nThis command will force new players to join a selected team.\n");
                sbinfo.append("[sky] arguments[]\n* [team] : selected team or [accent]off[] to disable\n");
                sbinfo.append("* [transfer_players] : (0 or 1) Transfer all current online players (except admins) to the selected team. Default: 0");
                sbinfo.append("\n\n[green]Auto disable after a game over![]");
                Call.infoMessage(player.con, sbinfo.toString());
                return;
            }
            // select team
            switch (args[0]){
                case "off":
                    setTeam = null;
                    Call.sendMessage(" [orange] forceTeam disabled");
                    //randomize again?
                    return;
                case "sharded":
                    setTeam = Team.sharded;
                    break;
                case "blue":
                    setTeam = Team.blue;
                    break;
                case "crux":
                    setTeam = Team.crux;
                    break;
                case "derelict":
                    setTeam = Team.derelict;
                    break;
                case "green":
                    setTeam = Team.green;
                    break;
                case "purple":
                    setTeam = Team.purple;
                    break;
                default:
                    setTeam = null;
                    player.sendMessage("[scarlet]ABORT: Team not found[] - available teams:");
                    for(int i=0; i<6; i++){
                        if(!Team.baseTeams[i].cores().isEmpty()){
                            player.sendMessage(setTeam.baseTeams[i].name);
                        }
                    }
                    return;
            }
            if(setTeam.cores().isEmpty()){
                player.sendMessage("[scarlet]ABORT: This team has 0 cores...");
                return;
            }


            boolean forceAll;
            if(args.length == 2){
                if(!Strings.canParseInt(args[1])){
                    player.sendMessage("[scarlet][transfer_players] should be 0 or 1");
                    return;
                }
                forceAll = Strings.parseInt(args[1]) == 1;
            }else{
                forceAll = false;
            }









            if(forceAll){
                Groups.player.forEach(p ->{
                    if(!p.admin()){
                        Call.setPlayerTeamEditor(p, setTeam);
                    }
                });
            }

            /*
            if(System.currentTimeMillis() < teamTimers.get(player,0L)){
                player.sendMessage(">[orange] command is on a 5 second cooldown...");
                return;
            }
            coreTeamReturn ret = getPosTeamLoc(player);
            if(ret != null) {
                Call.setPlayerTeamEditor(player, ret.team);
                player.team(ret.team);
                //maybe not needed
                Call.setPosition(player.con, ret.x, ret.y);
                player.unit().set(ret.x, ret.y);
                player.snapSync();
                teamTimers.put(player, System.currentTimeMillis()+TEAM_CD);
                Call.sendMessage(String.format("> %s []changed to team [sky]%s", player.name, ret.team));
            }else{
                player.sendMessage("[scarlet]You can't change teams ...");
            }

             */
        });
    }
    //search a possible team
    private Team getPosTeam(Player p){
        Team currentTeam = p.team();
        int c_index = Arrays.asList(Team.baseTeams).indexOf(currentTeam);
        int i = (c_index+1)%6;
        while (i != c_index){
            if (Team.baseTeams[i].cores().size > 0){
                return Team.baseTeams[i];
            }
            i = (i + 1) % Team.baseTeams.length;
        }
        return currentTeam;
    }

    private coreTeamReturn getPosTeamLoc(Player p){
        Team currentTeam = p.team();
        Team newTeam = getPosTeam(p);
        if (newTeam == currentTeam){
            return null;
        }else{
            Tile coreTile = newTeam.core().tileOn();
            return new coreTeamReturn(newTeam, coreTile.drawx(), coreTile.drawy());
        }
    }

    class coreTeamReturn{
        Team team;
        float x,y;
        public coreTeamReturn(Team _t, float _x, float _y){
            team = _t;
            x = _x;
            y = _y;
        }
    }
}
