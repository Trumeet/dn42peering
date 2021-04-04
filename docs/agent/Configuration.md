# Configuration references

The configuration format of agent is JSON.

## Reference

```json
{
  "internal_ip": "<Your internal controlling IPv4 address without port, see below # Controlling>",
  "persistent": false,
  "persistent_path": "/var/lib/dn42peering/agent/config"
}
```

For both IP addresses, prefixes must not be included.

## Controlling

The central communicates with agents using gRPC. An internal IP address is required for that.

The only requirement for internal IP address is that the agent can communicate with central. Agents are not required to communicate with each other.

Though is it OK to use the dn42 IP address, it is strongly recommended to create a separete VPN tunnel between each agent and central combination, as some provision failure could cause the agent to disconnected.

# Persistent

Even though the agent is supposed to be stateless, it is inevitable to store the configuration for recovery. WireGuard interfaces will not be saved by the operating system across reboots, so
it is necessary to redeploy after rebooting.

If `persistent` is set to true, the agent stores incoming change after deployments are finished and successful to the path given by `persistent_path`.

Whether the saving process is successful or not, it will not affect the deployment result. In other words, persistent failures are not fetal.

The persistent file will be created if not existing. Because it contains sensitive data (e.g. WireGuard keys), it must be saved with care. The agent will chmod 600 each time after closing the file so make sure the agent has ownership with it.

Upon starting, the agent will reads the persistent file. If it does not exist or if `persistent` is set to false, the agent will start as normal. However, if the persistent file is corrupt or cannot be read, the agent will refuse to start. In this case, you may manually delete it and redeploy from the administrative panel.