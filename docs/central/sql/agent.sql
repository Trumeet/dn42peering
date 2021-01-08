-- Use this script to add a new node to the database.
-- Note: Current node support is minimal: Do not edit or delete the node after creation.
-- A GUI will be provided later with full support.

INSERT INTO `node`
(public_ip,
dn42_ip4,
dn42_ip6,
asn,
internal_ip,
internal_port,
name,
notice,
vpn_type_wg)
VALUES
('127.0.0.1', -- The public IP address to display
'172.23.105.1', -- The dn42 IPv4 address (No prefixes)
'fe80:2980::1',  -- The dn42 or link local IPv6 address (No prefixes)
'AS4242422980', -- The ASN of this node to display. It is possible to have multiple ASNs in different nodes.
'127.0.0.1', -- The internal address for management. See agent/Configuration.md for more details. Must be the same with agent configuration.
49200, -- Currently only support 49200
'North America GCP', -- Display name
'<b>North America</b> 的 GCP', -- Optional notice to display. Support HTML.
1)