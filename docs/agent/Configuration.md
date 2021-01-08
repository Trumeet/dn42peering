# Configuration references

The configuration format of agent is JSON.

## Reference

```json
{
  "internal_ip": "<Your internal controlling IPv4 address without port, see below # Controlling>"
}
```

For both IP addresses, prefixes must not be included.

## Controlling

The central communicates with agents using gRPC. An internal IP address is required for that.

The only requirement for internal IP address is that the agent can communicate with central. Agents are not required to communicate with each other.

Though is it OK to use the dn42 IP address, it is strongly recommended to create a separete VPN tunnel between each agent and central combination, as some provision failure could cause the agent to disconnected.