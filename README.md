# AutoServer

## Overview

AutoServer is a Minecraft plugin designed for the [Velocity Proxy](https://papermc.io/software/velocity). This plugin automatically starts backend servers when a player attempts to connect to them and they are offline. It's perfect for server networks looking to save resources by keeping idle servers offline until needed.

## Features

- Automatically starts backend servers on player connection attempts.

## Requirements

- **Velocity Proxy**: Ensure your network is running Velocity Proxy.

## Installation

1. Download the latest version of `AutoServer.jar` from the [Releases](https://github.com/Artificial-720/AutoServer/releases).
2. Place the `AutoServer.jar` file into your Velocity `plugins` folder.
3. Restart your Velocity Proxy to load the plugin.

## Configuration

After the first launch, the plugin will generate a `config.toml` file in the `plugins/AutoServer` directory. Modify this file to suit your setup.

## Usage

1. Attempt to connect to a backend server using the Velocity Proxy.
2. If the server is offline, AutoServer will trigger its startup process using the command defined in `config.toml`.
3. Players will see a configurable message while the server is starting.
4. Once the server is online, players will be seamlessly connected.

## Troubleshooting

- **Server not starting?** Ensure the start command in `config.toml` is correct and executable.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.