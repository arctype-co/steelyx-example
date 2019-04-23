# Example Onyx Data Engine

## Configuration
The server loads the config from the default path at resources/config/app.yml.
Set the envrionment variable *APP_CONFIG* to override the default config file path.

## Development
Launch a repl with the following command:

    make repl

Run the server from the repl:

    ; Start the server
    (start)
    ; Stop server, reload code, and restart
    (reload)
