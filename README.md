# AutoServer

## Overview

AutoServer is a Minecraft plugin designed for the [Velocity Proxy](https://papermc.io/software/velocity). This plugin automatically starts offline backend servers when a player attempts to connect to them. It's perfect for server networks looking to save resources by keeping idle servers offline until needed.

## Features

- Automatically starts backend servers when a player attempts to connect.
- Automatically stops local servers when no players are online.
- Manually start and stop backend servers using commands.
- Supports configuration hot reloading.
- Customizable messages.

## Getting Started

This section will cover how to install and configure a minimal setup of AutoServer.

1. **Downloading AutoServer**
   Head over to [installation](#installation) section to get the latest version of AutoServer. After downloading AutoServer, place the JAR file into the `plugins` directory of your Velocity proxy.
2. **Launching for the first time**
   Once you have placed AutoServer into the `plugins` directory, we need to restart Velocity to load the plugin. On the first launch AutoServer will generate a configuration file `autoserver/config.toml` located in the `plugins` directory.
3. **Configuring your servers**
   Start by taking note of your `velocity.toml` file looking for the `[servers]` section. How the servers are named needs to match exactly when configuring AutoServer.

   Open the `autoserver/config.toml` in a text editor and search for the `[servers]` section. This section specifies the servers that AutoServer will manage. Create a table for each server you want AutoServer to manage. For example if you have a server named `lobby` you will create a table called `[servers.lobby]`.

   Two routes you can head with configuring a server either a remote server or a local server. If you have a remote server you need to configure check out the [Remote Backend](#remote-backend) section for more details. For the sake of this section we will handle a local server that we have a script for. This example local server is called `lobby` here is a minimal settings to start the server using a script:

   ```toml
   [servers]
   [servers.lobby]
   # Path to the directory where the server runs.
   workingDirectory = "/home/user/minecraft/servers/lobby"
   # Command to start the "lobby" server.
   start = "bash start.sh"
   # Or if you have a Windows Batch Script
   # start = "start.bat"
   ```

4. **Repeat for each server**
   Now repeat step 3 for each server you want to be managed by AutoServer. There is a lot of example commands in the [Commands](#command-examples) section of this document. To help you build a command that works for your situation.

## Installation

1. Download the latest version of `autoserver-velocity-1.x.x.jar` from [Modrinth](https://modrinth.com/plugin/autoserver).
2. Place the `.jar` file into your Velocity `plugins` folder.
3. Restart your Velocity Proxy to load the plugin.

## Configuration

After the first launch, the plugin will generate a `config.toml` file in the `plugins/autoserver` directory. Modify this file to suit your setup.

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

| **Key**             | **Type**  | **Description**                                                                                                                                                     |
|---------------------|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `startupDelay`      | `int`     | Time in seconds to wait before attempting to connect a player after starting the server.                                                                            |
| `shutdownDelay`     | `int`     | Time in seconds to wait before verifying whether the server has successfully shut down.                                                                             |
| `start`             | `string`  | Command used to start the server locally.                                                                                                                           |
| `stop`              | `string`  | Command used to stop the server locally.                                                                                                                            |
| `workingDirectory`  | `string`  | Path to the directory where the server runs.                                                                                                                        |
| `remote`            | `boolean` | Specifies whether the server is remote (`true`) or local (`false`).                                                                                                 |
| `port`              | `int`     | Port number on which the remote server listens for the start command.                                                                                               |
| `preserveQuotes`    | `boolean` | (Optional) Controls whether leading and trailing quotes are preserved, with quotes being removed by default on non-Windows systems unless explicitly set to `true`. |
| `security`          | `boolean` | Specifies whether the remote server should use security on message (default: `true`)                                                                                |
| `autoShutdownDelay` | `int`     | Duration (in seconds) to wait before automatically shutting down the server; set to 0 to disable this feature.                                                      |

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
start = "bash start.sh"
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
The `workingDirectory` setting won't be effective for this command. To set the working directory go into the Task Scheduler GUI and edit the task. There is a text box for the working directory called `Start in`.

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

## Remote Backend

**NOTICE: the functionality of the remote server support is likely to change with continued development be aware of this.**

Remote backend support is available for **PaperMC** and **FabricMC** servers. Remote backend is required only for **remote** server startup functionality.

1. Download from Modrinth
   - [AutoServer PaperMC](https://modrinth.com/plugin/autoserver/versions?l=paper)
   - [AutoServer FabricMC](https://modrinth.com/plugin/autoserver/versions?l=fabric)
2. Place into `mods` or `plugins` folder.
3. Restart to generate config file

After the first launch, the plugin will generate a `AutoServer/config.yml` file in the same directory as the JAR file either `mods` or `plugins`.

There are a few settings available on the remote backend to make this work. First you'll want to set the `server.workingDirectory` and `server.startCommand` for starting your server on the remote machine. The `startCommand` setting follows the same rules as `start` for the Velocity plugin so check out [Commands](#command-examples) for examples.

Don't need to change the `bootListener` section unless you have a different port available for the backend listener.

Backend servers support HMAC security for messages between the backend and the Velocity plugin. To enable this feature you must copy the key from the file `forwarding.secret` on your Velocity machine. Then set `security.enabled` to true and set `security` to true for that server in AutoServer's Velocity config.

Here is an example config table for a remote server that is named `survival`. This goes in Velocity `autoserver/config.toml`:

```toml
[servers.survival]
# Specify that is a remote server
remote = true
# Port the remote server listens for the start command (default: 8080).
port = 8080
# Time in seconds to wait before attempting to connect a player.  
# For a remote server probably want a longer delay.
startupDelay = 120  
# Enable security for the connection with this backend server.  
# If not provided the default is true.  
security = true
```

Then the matching example config for the `survival` server. This goes in `AutoServer/config.yml`:

```yaml
bootListener:
  # This command is used to launch boot listener when server is shutdown.
  # Only change this if you need to.
  runJarCommand: "java -jar %jarName"
  enabled: true
  port: 8080

server:
  workingDirectory: "/home/user/servers/minecraft-server/survival"
  startCommand: "x-terminal-emulator -e java -jar server.jar nogui"

## Security Settings
security:
  # Enables HMAC security for message transmission between the backend and the Velocity plugin.
  # If this setting is omitted, it defaults to "true".
  enabled: true
  
  # The shared secret key used for HMAC authentication.
  # This must be the same as the "forwarding.secret" in your Velocity proxy's configuration file.
  # To ensure proper authentication, copy the value directly from Velocity's config file.
  # Replace this with the exact value from Velocity's forwarding.secret
  secret: "xxxxxxxxxxxx"
```


### PaperMC Backend

PaperMC AutoServer plugin supports hot reloading of the config file though the `/autoserver reload` command available on the backend server.

### Boot Listener

The Boot Listener is the piece of software that will wait and listen for a command from the Velocity plugin to boot the server - hence the name "Boot Listener". It also includes a command-line interface(CLI) so you can run commands like `reload` to hot reload the config file without needing to restart the Boot Listener.

Because of the CLI support you might want to run the Boot Listener in a terminal that allows you to interact with it, rather than running it in the background. To do that reference the [Commands](#command-examples) section to help build a command that will do this, then you will update the `bootListener.runJarCommand` to what you want.

For example if you want to use `screen` do something like this:
```yaml
bootListener:
  runJarCommand: "screen -DmS boot-listener java -jar %jarName%"
```

## Troubleshooting

- **Server not starting?** Ensure the `start` command in `config.toml` is correct and executable. It's helpful to run the command in a new terminal to test the commands output.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.