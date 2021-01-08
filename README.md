# dn42peering

A dn42 auto-peering platform.

## Components

* **Central**: Installs on some machines. They will provide HTTP portal and store user data.
* **Agent**: Installs on target nodes. They will receive commands from the **central** and apply changes to the system.

Refer to project Wiki for more details.

## Overview

The central provides a HTTP portal, and connects to a MySQL database. It will perform most tasks, including user management and peering management.

The central establishes connections to agents when necessary. It will ask the agents to perform provisioning tasks. For example, setup BGP and VPN tunnels.

The whole project is written in Java, with the support of Vert.x framework. The communication between centrals and agents is done by gRPC.

## Get Started

See [Quick Start](docs/QuickStart.md) for more details.

## License

Proprietary Software with open source components.