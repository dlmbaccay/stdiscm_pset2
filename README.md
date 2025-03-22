# Dungeon LFG System

This project simulates a Looking-For-Group (LFG) system for a dungeon-based multiplayer game. It models tanks, healers, and DPS players forming parties to enter dungeon instances.

## Requirements

- OpenJDK 17 or later
  - Current development uses:
    ```
    openjdk 17.0.12 2024-07-16 LTS
    OpenJDK Runtime Environment Zulu17.52+17-CA (build 17.0.12+7-LTS)
    OpenJDK 64-Bit Server VM Zulu17.52+17-CA (build 17.0.12+7-LTS, mixed mode, sharing)
    ```
- A terminal that supports ANSI color codes for visualization

## Project Structure

- `Main.java` - Entry point for the application
- `Config.java` - Handles configuration loading from `config.txt`
- `DungeonManager.java` - Manages the dungeon LFG system
- `DungeonInstance.java` - Represents a dungeon instance
- `config.txt` - Configuration file for simulation parameters

## Building and Running

### Compile the Project

```bash
javac *.java
```

### Run the Application

```bash
java Main
```

## Configuration

The `config.txt` file contains the following parameters:

- `n` - Maximum number of concurrent dungeon instances
- `t` - Number of tank players in the queue
- `h` - Number of healer players in the queue
- `d` - Number of DPS players in the queue
- `t1` - Minimum time before an instance is finished (seconds)
- `t2` - Maximum time before an instance is finished (seconds)

Example configuration:
```
# Maximum number of concurrent instances
n=3

# Number of tank players in the queue
t=5

# Number of healer players in the queue
h=5

# Number of DPS players in the queue
d=15

# Minimum time before an instance is finished (seconds)
t1=5

# Maximum time before an instance is finished (seconds)
t2=15
```
- When a config fails to parse, the program will use the default values.

## Output

The program will display colored text showing:
- System initialization details
- Party formation and assignment to dungeon instances
- Dungeon completion status
- Final summary of all instances and remaining players 