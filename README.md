# AutoServer

## Overview

AutoServer is a Minecraft plugin designed for the [Velocity Proxy](https://papermc.io/software/velocity). This plugin automatically starts offline backend servers when a player attempts to connect to them. It's perfect for server networks looking to save resources by keeping idle servers offline until needed.

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

| Command                         | Description                                                                 | Permission                   |
|---------------------------------|-----------------------------------------------------------------------------|------------------------------|
| (No command, base permission)   | Base permission required to access any command                              | `autoserver.base`            |
| `/autoserver reload`            | Reloads the plugin configuration.                                           | `autoserver.command.reload`  |
| `/autoserver help`              | Displays the help menu with available commands                              | `autoserver.command.help`    |
| `/autoserver status [<server>]` | Checks the status of a specified server or all servers if none is specified | `autoserver.command.status`  |
| `/autoserver start <server>`    | Run the start sequence for a server                                         | `autoserver.command.start`   |
| `/autoserver stop <server>`     | Run to stop sequence for a server                                           | `autoserver.command.stop`    |
| `/autoserver info <server>`     | Displays detailed information about a specified server                      | `autoserver.command.info`    |
| `/autoserver version`           | Version of the plugin                                                       | `autoserver.command.version` |

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

| **Key**            | **Type**  | **Description**                                                                                                                                                     |
|--------------------|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `startupDelay`     | `int`     | Time in seconds to wait before attempting to connect a player after starting the server.                                                                            |
| `shutdownDelay`    | `int`     | Time in seconds to wait before verifying whether the server has successfully shut down.                                                                             |
| `start`            | `string`  | Command used to start the server locally.                                                                                                                           |
| `stop`             | `string`  | Command used to stop the server locally.                                                                                                                            |
| `workingDirectory` | `string`  | Path to the directory where the server runs.                                                                                                                        |
| `remote`           | `boolean` | Specifies whether the server is remote (`true`) or local (`false`).                                                                                                 |
| `port`             | `int`     | Port number on which the remote server listens for the start command.                                                                                               |
| `preserveQuotes`   | `boolean` | (Optional) Controls whether leading and trailing quotes are preserved, with quotes being removed by default on non-Windows systems unless explicitly set to `true`. |

###  Command examples

Here are some command examples to help construct your `start` and `stop` commands.

#### Running Java command

To run the server using java:
```toml
start = "java -Xmx4G -Xms4G -jar server.jar nogui"
```

By default, this will run the process in the background. If you need to attach to the process, consider launching it in a new terminal session. On Linux, you can use `x-terminal-emulator`:
```toml
start = "x-terminal-emulator -e java -Xmx4G -Xms4G -jar server.jar nogui"
```

#### Using a Bash or sh Script

To run a script using Bash or `sh`:
```toml
start = "bash run.sh"
stop = "bash stop.sh"
```

#### Using `screen`

To run the server in a detached `screen` session:
```toml
start = "screen -DmS mc-example java -Xmx4G -Xms4G -jar server.jar nogui"
stop = "screen -p 0 -S mc-example -X stuff \"stop\r\""
```

#### Using `tmux`

To run the server in a new `tmux` session:
```toml
start = "tmux new -d -s mc-example 'java -Xmx4G -Xms4G -jar server.jar nogui'"
stop = "tmux send-keys -t mc-example 'stop' C-m"
```

#### Using `systemd`

If you have `systemd` service for your server, you can control it like this:
```toml
start = "systemctl start myserver"
stop = "systemctl stop myserver"
```
**Note**: You might have privilege issues with this.

#### Using Docker

To start and stop a server using Docker:
```toml
start = "docker start mc-example"
stop = "docker stop mc-example"
```

#### Windows

#### Using `batch` script

Be aware this runs in the background.
```toml
start = "start.bat"
```

#### Using `start` (Windows Command Prompt)

To launch the server in a new Command Prompt window:
```toml
start = "cmd.exe /c start \"ExampleTitle\" cmd /c java -Xmx4G -Xms4G -jar server.jar nogui"
```

#### Using `PowerShell`

To start the server in a new `PowerShell` window:
```toml
start = "powershell -Command Start-Process -FilePath 'java' -ArgumentList '-Xmx4G -Xms4G -jar server.jar nogui'"
```

To start the server in a hidden `PowerShell` window:
```toml
start = "powershell -WindowStyle Hidden -Command Start-Process -NoNewWindow -FilePath 'java' -ArgumentList '-Xmx4G -Xms4G -jar server.jar nogui'"
```

#### Using `wt`

To start the server in the same `PowerShell` window as a new tab do a command like this:
```toml
start = "wt -w 0 new-tab -d \"C:/path/to/directory\" \"C:/path/to/script/start.bat\""
```
The `workingDirectory` setting won't be effective for this command which is why working directory is passed in the command.

#### Using Task Scheduler

If you have a scheduled task to start your server:
```toml
start = "schtasks /run /tn \"MinecraftExampleTask\""
stop = "schtasks /end /tn \"MinecraftExampleTask\""
```
Do not use a `batch` script for this task if you want to use the `stop` command. It won't work because it will end the `batch` script but not the `java` process.
The `workingDirectory` setting won't be effective for this command. To set the working directory go into the Task Scheduler GUI and edit the task there is a text box for the working directory called `Start in`.

## Remote Backend

**NOTICE: the functionality of the remote server support is likely to change with continued development be aware of this**

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