# Database

A MySQL database is required for dn42peering central to work. Before installing the central program, make sure you have a proper database with necessary privileges setup.

Create a database and user, then grant it with all data and structure privileges. The server will automatically create tables and upgrade schema in the future.

## Host

IPv4 Address, IPv6 Address or domain name is supported. For IPv6 address, quote your address in brackets like `[fd3f:a1f1:54ed:2::41]`.

## Auto Migration

The server will migrate database schema before starts. All migration SQL files can be found in the resources.

In case migration fails, the server will return an exception with instructions and exit.

Follow the instructions and combine it with the SQL files to solve the problem yourself.

For example, a duplicated column name may be caused by duplicated SQL statements. In this case, delete the column may fix the problem.

After manually inspection, change the migration action to `repair`. In this mode, the server automatically rolls back the history and becomes ready to retry.

The server will never fix database errors itself. Repair mode only rolls back history and version state.

### Migrate modes

```json
{
  "database": {
    "migrate": "auto"
  }
}
```

Case-insensitive.

* **auto**: Default mode. The server will automatically upgrades the schema.
* **auto_no_baseline**: Similar to `auto`, but will not set the version to 3 (which is the last schema version before implementing migration) if the database is not empty but does not have a history record. In this case, it will fail.
* **disabled**: Not recommended. Disable all migration and start silently. This may led to schema errors.
* **repair**: Repair the history record after incomplete migrations. The server will only roll back history state and exit. Use this as an oneshot action after manually fixing database structure. Good luck!