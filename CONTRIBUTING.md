# Contributing to ChatGames

Thank you for your interest in contributing to ChatGames! This document will help you get started with the codebase.

## Project Structure

ChatGames is a multi-platform Minecraft plugin built with a modular architecture:

```
ChatGames/
├── common/          # Platform-agnostic core logic (42 classes)
├── paper/           # Paper-specific implementation
├── spigot/          # Spigot-specific implementation
├── folia/           # Folia-specific implementation
├── sponge/          # Sponge-specific implementation
└── pom.xml          # Parent Maven configuration
```

The `common` module contains all shared logic and is included as a dependency by each platform module. Platform modules only contain platform-specific implementations of the interfaces defined in `common`.

## Architecture Overview

### Core Components

| Package | Purpose |
|---------|---------|
| `platform` | Platform abstraction layer (logging, tasks, config, players) |
| `game` | Game logic, registry, and configuration |
| `game/types` | Individual game implementations |
| `command` | Command system with handler-based architecture |
| `command/handlers` | Individual subcommand handlers |
| `config` | Configuration management and language files |
| `afk` | AFK detection with pluggable provider system |
| `util` | Utilities for messages, templates, etc. |

### Key Classes

- **`ChatGamesCore`** - Main plugin entry point, initializes all managers
- **`Platform`** - Interface that each platform implements for platform-specific operations
- **`GameManager`** - Handles game lifecycle (start, stop, scheduling, win detection)
- **`GameRegistry`** - Registers game types and loads game configurations
- **`CommandRegistry`** - Manages command handlers

## Platform Abstraction

The `Platform` interface abstracts all platform-specific operations.

Each platform (Paper, Spigot, Folia, Sponge) provides its own implementation. This allows the `common` module to remain completely platform-agnostic. Please limit the contents of the platform modules only to implementing abstractions defined in `common`. If there is a piece of functionality you need that does not yet have an abstraction, you will need to add it or request that someone provide one; the plugin should act identically (as much as is feasible) regardless of platform, and there are no platform-specific features.

## Adding a New Game Type

1. **Create the game class** in `common/.../game/types/`:

```java
public class MyGame extends AbstractGame {

    public MyGame(ChatGamesCore plugin, GameConfig config) {
        super(plugin, config);
        // Initialize game state
    }

    @Override
    public void start() {
        this.plugin.broadcast(this.createStartMessage());
    }

    @Override
    public boolean checkAnswer(String answer) {
        // Return true if answer is correct
    }

    @Override
    public Component getQuestion() {
        // Return the question to display
    }

    @Override
    public Optional<String> getCorrectAnswer() {
        // Return the correct answer for display
    }
}
```

2. **Add the game type** to `GameType` enum:

```java
public enum GameType {
    // ...existing types
    MY_GAME("my_game");
}
```

3. **Register the factory** in `GameRegistry.registerDefaults()`:

```java
this.registerGameType(GameType.MY_GAME, MyGame::new);
```

4. **Create a default config** in `common/src/main/resources/games/my_game.yml`

5. **Update `GameRegistry.createDefaultGames()`** to include your new config file

## Adding a New Platform

1. Create a new module directory (e.g., `velocity/`)

2. Create `pom.xml` referencing the parent and `chatgames-common`

3. Implement the `Platform` interface:
   - `VelocityPlatform implements Platform`
   - `VelocityPlatformLogger implements PlatformLogger`
   - `VelocityPlatformTask implements PlatformTask`
   - etc.

4. Create the plugin entry point that initializes `ChatGamesCore`

5. Add the module to the parent `pom.xml`

## Command System

Commands use a handler-based architecture:

- **`SubCommand`** - Enum identifying each subcommand
- **`SubCommandHandler`** - Interface that handlers implement
- **`CommandRegistry`** - Maps subcommands to handlers
- **`CommandContext`** - Provides args, sender, and plugin access to handlers

Each handler is self-contained with its own permission, usage string, description, and tab completion:

```java
public class MyHandler implements SubCommandHandler {

    @Override
    public String getPermission() {
        return "chatgames.mycommand";
    }

    @Override
    public String getUsage() {
        return "/chatgames mycommand <arg>";
    }

    @Override
    public void execute(CommandContext context) {
        // Handle the command
    }

    @Override
    public List<String> tabComplete(CommandContext context) {
        // Return completions
    }
}
```

## Building

```bash
# Build all modules
mvn clean package

# Build specific module
mvn clean package -pl paper -am
```

Build artifacts are placed in each module's `target/` directory.

## Code Style

- Use `final` for parameters and local variables where possible
- Prefix instance fields with `this.`
- Use early returns to reduce nesting
- Keep methods focused and small
- Add Javadoc to public interfaces

## Testing Changes

1. Build the plugin: `mvn clean package`
2. Copy the JAR from `<platform>/target/` to your test server
3. Test on the appropriate platform (Paper, Spigot, Folia, or Sponge)
4. Check console for errors and verify functionality

## Submitting Changes

1. Fork the repository
2. Create a feature branch from `dev`
3. Make your changes with clear commit messages
4. Ensure the project builds without errors
5. Submit a pull request with a description of your changes