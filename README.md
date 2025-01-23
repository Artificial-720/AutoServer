# AutoServer

## Overview

AutoServer is a Minecraft plugin designed for the [Velocity Proxy](https://papermc.io/software/velocity). This plugin automatically starts backend servers when a player attempts to connect to them and they are offline. It's perfect for server networks looking to save resources by keeping idle servers offline until needed.

## Features

- Automatically starts backend servers on player connection attempts.

## Requirements

- **Velocity Proxy**: Ensure your network is running Velocity Proxy.
- **PaperMC**: PaperMC is required only for remote server startup functionality; it must be set up on the backend servers.
- **FabricMC**: Fabric is required only for remote server startup functionality; it must be set up on the backend servers.

## Installation

1. Download the latest version of `AutoServer.jar` from the [Releases](https://github.com/Artificial-720/AutoServer/releases).
2. Place the `AutoServer.jar` file into your Velocity `plugins` folder.
3. Restart your Velocity Proxy to load the plugin.

### Remote Backend (PaperMC)

1. Download the latest version of `AutoServer.jar` from the [Releases](https://github.com/Artificial-720/AutoServer/releases).
2. Place the `AutoServer.jar` file into your PaperMC `plugins` folder.
3. Restart your PaperMC server to load the plugin.

### Remote Backend (FabricMC)

1. Download the latest version of `AutoServer.jar` from the [Releases](https://github.com/Artificial-720/AutoServer/releases).
2. Place the `AutoServer.jar` file into your Fabric `mods` folder.
3. Restart your Fabric server to load the mod.

## Configuration

After the first launch, the plugin will generate a `config.toml` file in the `plugins/AutoServer` directory. Modify this file to suit your setup.

### Remote Backend Configuration (PaperMC)

After the first launch, the plugin will generate a `AutoServer.properties` file in the `config` directory. Modify this file to suit your setup.

### Remote Backend Configuration (FabricMC)

After the first launch, the mod will generate a `AutoServer.properties` file in the `config` directory. Modify this file to suit your setup.


## Usage

1. Attempt to connect to a backend server using the Velocity Proxy.
2. If the server is offline, AutoServer will trigger its startup process using the command defined in `config.toml`.
3. Players will see a configurable message while the server is starting.
4. Once the server is online, players will be seamlessly connected.

## Permissions

- **autoserver.base** - Base permission required to access any command
- **autoserver.command.reload** - Allow to reload the plugin
- **autoserver.command.help** - Allows a player to access the help menu

## Troubleshooting

- **Server not starting?** Ensure the start command in `config.toml` is correct and executable. It's helpful to run the command in a new terminal to test the commands output.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.