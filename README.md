# Example Onyx/Steelyx Data Engine

[Steelyx](https://github.com/arctype-co/steelyx/) provides a high level API service for managing [Onyx](https://github.com/onyxplatform/onyx/) clusters.

![API overview](https://i.imgur.com/Ryl3ClP.png)

## Configuration
The server loads a config file from the default path at resources/config/app.yml.
Set the envrionment variable *APP_CONFIG* to override the default config file path.

## Development
Launch a repl with the following command:

    make repl

Run the server from the repl:

    ; Start the server
    (start)
    ; Stop server, reload code, and restart
    (reload)

Steelyx includes clojure.tools.namespace for hot-reloading your code for fast development. The (reload) macro will handle stopping, reloading, and restarting for you to work at a fast pace. 
