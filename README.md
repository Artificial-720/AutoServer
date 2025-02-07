# AutoServer

## Overview

AutoServer is a Minecraft plugin designed for the [Velocity Proxy](https://papermc.io/software/velocity). This plugin automatically starts backend servers when a player attempts to connect to them and they are offline. It's perfect for server networks looking to save resources by keeping idle servers offline until needed.

## Installation

1. Download the latest version of `AutoServer.jar` from [Modrinth](https://modrinth.com/plugin/autoserver).
2. Place the `.jar` file into your Velocity `plugins` folder.
3. Restart your Velocity Proxy to load the plugin.

## Features

- Automatically starts backend servers on player connection attempts.
- Configuration hot reloading.
- Manually start and stop backend servers though commands.
- Configurable messages.

## Commands and Permissions

| Command                         | Description                                                                  | Permission                   |  
|---------------------------------|------------------------------------------------------------------------------|------------------------------|  
| (No command, base permission)   | Base permission required to access any command                               | `autoserver.base`            |  
| `/autoserver reload`            | Reloads the plugin configuration.                                            | `autoserver.command.reload`  |  
| `/autoserver help`              | Displays the help menu with available commands                               | `autoserver.command.help`    |  
| `/autoserver status [<server>]` | Checks the status of a specified server or all servers if none is specified  | `autoserver.command.status`  |  
| `/autoserver start <server>`    | Run the start sequence for a server                                          | `autoserver.command.start`   |  
| `/autoserver stop <server>`     | Run to stop sequence for a server                                            | `autoserver.command.stop`    |  
| `/autoserver info <server>`     | Displays detailed information about a specified server                       | `autoserver.command.info`    |  
| `/autoserver version`           | Version of the plugin                                                        | `autoserver.command.version` |

## Configuration

After the first launch, the plugin will generate a `config.toml` file in the `plugins/AutoServer` directory. Modify this file to suit your setup.

### Global

| **Key**           | **Type**  | **Description**                              |  
|-------------------|-----------|----------------------------------------------|  
| `checkForUpdates` | `boolean` | Should AutoServer check for updates on boot? |  
| `messages`        | `table`   | Messages that will get sent to players.      |  
| `servers`         | `table`   | The configuration for each server.           |

### Messages

| **Key**    | **Type** | **Description**                                                                                           |  
|------------|----------|-----------------------------------------------------------------------------------------------------------|  
| `prefix`   | `string` | Prefix added to all messages displayed to the player.                                                     |  
| `starting` | `string` | Message displayed to the player when they attempt to connect to a server that is currently offline.       |  
| `failed`   | `string` | Message displayed to the player if the server fails to start or cannot be connected to.                   |
| `notify`   | `string` | Message displayed to the player when the server is ready, indicating that they will be connected shortly. |

### Servers

| **Key**            | **Type**  | **Description**                                                                          |  
|--------------------|-----------|------------------------------------------------------------------------------------------|  
| `startupDelay`     | `int`     | Time in seconds to wait before attempting to connect a player after starting the server. |  
| `shutdownDelay`    | `int`     | Time in seconds to wait before verifying whether the server has successfully shut down.  |
| `start`            | `string`  | Command used to start the server locally.                                                |
| `stop`             | `string`  | Command used to stop the server locally.                                                 |  
| `workingDirectory` | `string`  | Path to the directory where the server runs.                                             |
| `remote`           | `boolean` | Specifies whether the server is remote (`true`) or local (`false`).                      |
| `port`             | `int`     | Port number on which the remote server listens for the start command.                    |


## Remote Backend

Remote backend support is available for **PaperMC** and **FabricMC** servers. Remote backend is required only for remote server startup functionality.

1. Download from Modrinth
    - [AutoServer PaperMC](https://modrinth.com/plugin/autoserver/versions?l=paper)
    - [AutoServer FabricMC](https://modrinth.com/plugin/autoserver/versions?l=fabric)
2. Place into `mods` or `plugins` folder
3. Restart to generate config file

After the first launch, the plugin will generate a `AutoServer.properties` file in the `config` directory. Modify this file to suit your setup.

## Troubleshooting

- **Server not starting?** Ensure the start command in `config.toml` is correct and executable. It's helpful to run the command in a new terminal to test the commands output.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.