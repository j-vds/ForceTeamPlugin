# ForceTeamPlugin
*Compatible with the [TeamPlugin](https://github.com/J-VdS/TeamPlugin)*

### Commands
There are no commands for normal players.

### Admin Only commands
* `/forceteam [team/off] [transfer_players]` New players will be assigned to the selected team (resets after a game over). 
<br/>--> Usage:
  * `/forceteam crux` All new players will join team crux.
  * `/forceteam off`Back to normal mode
  * `/forceteam blue 1` All new players and current players except admins will be transferred to the blue team.

### Important
All commands are chatcommands.
This plugin isn't compatible with hexed!

### Feedback
Open an issue if you have a suggestion.

### Releases
Prebuild relases can be found [here](https://github.com/J-VdS/ForceTeamPlugin/releases)

### Building a Jar 

`gradlew jar` / `./gradlew jar`

Output jar should be in `build/libs`.


### Installing

Simply place the output jar from the step above in your server's `config/mods` directory and restart the server.
List your currently installed plugins by running the `plugins` command.
