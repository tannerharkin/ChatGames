# ChatGames

A Minecraft plugin that adds interactive chat-based minigames to engage your server's players with trivia, math challenges, word scrambles, and reaction games.

## Why use ChatGames?

- Instant, lightweight chat minigames that keep your chat active
- Multi-platform: works across Spigot, Paper, Folia, and Sponge via a platform abstraction layer
- Fully configurable games, schedules, messages and reward commands
- AFK detection integration with popular plugins (EssentialsX, CMI, AntiAFKPlus)
- Small, well-documented, and easy to extend

## Features

- **Multiple Game Types:**
  - **Trivia** - Answer trivia questions correctly
  - **Math** - Solve math problems
  - **Unscramble** - Unscramble words
  - **Multiple Choice** - Pick the correct answer from several options
  - **Reaction** - Respond quickly (type or click)

- **Automatic Game Scheduling** - Games start automatically at configurable intervals
- **Customizable Rewards** - Configure commands to run when players win (economy rewards, items, etc.)
- **AFK Detection** - Optionally exclude AFK players from minimum player count

## Quick Install

1. Drop the plugin `.jar` into your server's `plugins/` folder
2. Start the server once to generate default configurations
3. Edit `config.yml`, language files, and game configurations as needed

## Configuration

Configuration files are generated in your plugin's data folder on first run.

### Main Configuration

The main [`config.yml`](common/src/main/resources/config.yml) controls plugin behavior:

| Setting | Description |
|---------|-------------|
| `language` | Language file to use (default: `en-us`) |
| `game-interval` | Seconds between automatic games |
| `minimum-players` | Minimum players online to start games |
| `automatic-games` | Enable/disable automatic game scheduling |
| `answer-cooldown-ticks` | Cooldown after wrong answer in multiple choice |
| `debug` | Enable debug logging |
| `afk-detection` | AFK player exclusion settings |

### Language Files

Messages are fully customizable via language files. See [`languages/en-us.yml`](common/src/main/resources/languages/en-us.yml) for all available message keys.

All messages support [MiniMessage](https://docs.papermc.io/adventure/minimessage/format/) formatting for colors, gradients, click events, and more.

### Game Configuration

Each game type has its own configuration file in the `games/` folder:

| Game | Config File | Description |
|------|-------------|-------------|
| Math | [`math.yml`](common/src/main/resources/games/math.yml) | Solve math equations |
| Trivia | [`trivia.yml`](common/src/main/resources/games/trivia.yml) | Answer trivia questions |
| Unscramble | [`unscramble.yml`](common/src/main/resources/games/unscramble.yml) | Unscramble words |
| Multiple Choice | [`multiple-choice.yml`](common/src/main/resources/games/multiple-choice.yml) | Pick the correct answer |
| Reaction | [`reaction.yml`](common/src/main/resources/games/reaction.yml) | React quickly (type or click) |

#### Common Game Config Options

```yaml
name: game-name              # Internal identifier
display-name: "<gold>Name</gold>"  # Display name with formatting
timeout: 60                  # Seconds before game times out

reward-commands:
  - "give {player} diamond {rand:1-3}"  # Commands to run on win

messages:
  start: "..."    # Shown when game starts
  win: "..."      # Shown when someone wins
  timeout: "..."  # Shown when time runs out
```

#### Placeholders

| Placeholder | Description |
|-------------|-------------|
| `{player}` | Winner's username |
| `{answer}` | The correct answer |
| `{name}` | Game display name |
| `{timeout}` | Timeout in seconds |
| `{rand:min-max}` | Random number in range (rewards only) |

## Supported Platforms & Versions

| Platform | Supported Versions |
|----------|--------------------|
| Spigot   | 1.13.x – 1.21.x    |
| Paper    | 1.20.6 – 1.21.x    |
| Folia    | 1.20.6 – 1.21.x    |
| Sponge   | 1.21.x             |

The Java 8 runtime is currently supported, but EOL and will be discontinued with little prior notice. Please move to at least Java 21.

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/chatgames help` | none | Show command help |
| `/chatgames reload` | `chatgames.reload` | Reload all configurations |
| `/chatgames start <game>` | `chatgames.start` | Manually start a game |
| `/chatgames stop` | `chatgames.stop` | Stop the current game |
| `/chatgames list` | `chatgames.list` | List all available games |
| `/chatgames info` | `chatgames.info` | Show plugin information |
| `/chatgames toggle` | `chatgames.toggle` | Toggle automatic games |

## Permissions

| Permission | Description |
|------------|-------------|
| `chatgames.reload` | Reload the plugin |
| `chatgames.start` | Manually start games |
| `chatgames.stop` | Stop the current game |
| `chatgames.list` | List available games |
| `chatgames.info` | View plugin information |
| `chatgames.toggle` | Toggle automatic games |

## AFK Detection

ChatGames can exclude AFK players from the minimum player count. Enable this in `config.yml`:

```yaml
afk-detection:
  enabled: true
  providers: []  # Empty = use all available
```

We recommend leaving providers blank unless your development team has implemented a custom AFK provider.

Supported providers by platform:

| Platform | Providers |
|----------|-----------|
| Spigot | `essentialsx`, `cmi`, `antiafkplus` |
| Paper/Folia | `essentialsx`, `cmi`, `antiafkplus`, `paper-idle` |
| Sponge | `nucleus` |

## Troubleshooting

**Games not starting:**
- Check `minimum-players` setting in `config.yml`
- Verify enough non-AFK players are online (if AFK detection is enabled)
- Check console for errors

**Rewards not working:**
- Verify placeholder format is `{player}`
- Check console for command errors

**Questions not loading:**
- Verify YAML syntax in game config files
- Use `/chatgames reload` after making changes
- Check console for parsing errors

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for information on the codebase architecture and how to contribute.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
