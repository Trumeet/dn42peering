# Configuration references

The configuration format of central is JSON.

## Reference

```json
{
  "database": {
    "port": 3306,
    "host": "host.name.or.ip.for.MySQL.database",
    "database": "mysql database",
    "user": "test",
    "password": "123456"
  },
  "http": {
    "name": "<Site name> It will appear like <name> dn42 peering."
  },
  "mail": {
    "hostname": "SMTP hostname",
    "port": 587,
    "starttls": "DISABLED / OPTIONAL / REQUIRED",
    "username": "SMTP username",
    "password": "SMTP password",
    "from": "Postmaster <postmaster@example.tld>"
  },
  "whois": "localhost (Whois hostname. See below # Whois)"
}
```

## Database

A MySQL database with predefined schema is required for central to operate. See [Database](Database.md) section for more details.

## SMTP

A smtp server is required for email verification. A recommendation is mail services like MailGun.

## Whois

A whois server that is capable of looking up dn42 ASN's and routes is required for ASN and IP verification.

An example is [whois42d](https://github.com/Mic92/whois42d), but make sure you delete all contents under `data/inetnum` and `data/inet6num` or whois42d will not lookup routes.

## Configuration Location and Reloading

