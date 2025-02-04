# Bridge Practice Custom Plugins

This is the main repository for all the custom plugins that BridgePractice (under my 2021-2023 ownership) used.

## Selfhosting BridgePractice

### Server Setup

1. Install Java 8 and MySQL on your server.
2. Create a MySQL user with the username `mc` and the password `mcserver`.
3. Run the sql commands in this repo's `default_schema.sql` file to create the database and tables.
4. Download and extract the `bp-selfhost-server-config.zip` file. It contains 4 directories.

### Running the Server

Open 4 terminals (eg with tmux) and run `./start` in each directory. The server should be accessible on port `25565`.

Note that some failures will be seen (broken webhooks, etc) but they are not critical to the server's operation.
