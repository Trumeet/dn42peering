# Quick Start

This is a quick guide of setting dn42peering up.

## Prepare a local copy of dn42 registry and regularly update it

You need to have a local copy of the dn42 registry, and regularly update it.

Important: You need to delete all contents from `data/inetnum` and `data/inet6num`.

Finally, enable and start `whois42d` service.

## Prepare the SQL database

Create a database in a MySQL instance, then create required tables. 

Refer to [Database](central/Database.md) for more details.

## Install the central

Go to the download section and download the prebuilt central package, or build it yourself.

## Create a VPN tunnel between central and your first node

You may want to use WireGuard or similar VPN technologies to establish a dedicated tunnel between them. 

Please notice that the communication between central and agent is not encrypted, so be sure to use a VPN.

Also, do not conflict the VPN address with dn42 addresses.

## Install the agent

Go to the download section and download the prebuilt agent package, or build it yourself.

## Register the agent in the database and configure the agent

Create a record in the database / node table as [agent.sql](sql/agent.sql)

Create the agent configuration as shown in [Agent Configuration](agent/Configuration.md)

Then, start the agent.

## Register a mail service account and get the SMTP credentials

For example, MailGun.

## Configure the central

See [Central Configuration](central/Configuration.md)

## Start the central

Finally, start the central.