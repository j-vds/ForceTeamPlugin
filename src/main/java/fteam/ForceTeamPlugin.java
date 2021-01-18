package fteam;

import arc.*;
import arc.struct.ObjectMap;
import arc.util.*;
import mindustry.Vars;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.mod.Plugin;


public class ForceTeamPlugin extends Plugin {
    //private boolean DEBUG = false;
    private Team setTeam = null;

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
                Vars.state.rules.tags.put("forceTeam", "false");
            }
        });

        Events.on(GameOverEvent.class, event -> {
            if(setTeam != null){
                setTeam = null;
                Call.sendMessage("[orange] forceTeam disabled");
                Vars.state.rules.tags.put("forceTeam", "false");
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
        handler.<Player>register("forceteam", "[team] [transfer_players]","[scarlet]Admin only[] For info use the command without arguments.", (args, player) -> {
                    if (!Vars.state.rules.pvp) {
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
                    Vars.state.rules.tags.put("forceTeam", "false");
                    // select team
                    switch (args[0]) {
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
                            for (int i = 0; i < 6; i++) {
                                if (!Team.baseTeams[i].cores().isEmpty()) {
                                    player.sendMessage(setTeam.baseTeams[i].name);
                                }
                            }
                            return;
                    }
                    if (setTeam.cores().isEmpty()) {
                        player.sendMessage("[scarlet]ABORT: The selected team has 0 cores...");
                        return;
                    }
                    Vars.state.rules.tags.put("forceTeam", "true");

                    if (args.length == 2) {
                        if (!Strings.canParseInt(args[1])) {
                            player.sendMessage("[scarlet][transfer_players] should be 0 or 1");
                            return;
                        }
                        if (Strings.parseInt(args[1]) == 1) {
                            Groups.player.forEach(p -> {
                                if(!p.admin()){
                                    Call.setPlayerTeamEditor(p, setTeam);
                                    player.team(setTeam);
                                    player.unit().kill();
                                }
                            });
                        }
                    }
        });
    }
}
