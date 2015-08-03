Simplistic chat written in Java

## DB configuration

This project uses PostgreSQL and [[http://flywaydb.org/getstarted/firststeps/maven.html | Flyway Maven Plugin]]
for database migration.

First you have to create `flyway.properties` file under `server` directory. There, you should specify your 
PostgreSQL connection information. For example:

    flyway.url=jdbc:postgresql://localhost:5432/practicechat
    flyway.user=postgres
    flyway.password=1234
    
Then, execute `mvn compile flyway:migrate` in `server` subdirectory to configure the database.

## Building project

Use `package` Maven task for root project. Two standalone jars will be placed in `out/` directory

## Chat protocol description

Chat uses JSON-based protocol for client-server communication. 

Every message is a JSON object that contains two fields:

* message_type -- a string that determines the type of the message
* payload -- a JSON object which is specific to the message type

### Client-to-Server messages

#### connect
Tries to connect the client to the server. Must be called within 1 second after socket connection.

Payload example:

        {
            "username": "john_doe1952"
        }

The server must answer with `connection_result` message

#### disconnect
Disconnects currently connected user from the server. 

Payload must be `null`.

#### send_message
Sends a message to the specified user.

Payload example:

        {
            "username": "NAGibaTOR_40k",
            "message": "sup m8"
        }

#### list_users
Ask the server to return the list of all currently connected users.

Payload must be `null`.

### Server-to-Client messages

#### connection_result
Sent by the server upon client's connect request. If the user with 
the specified name is already connected, returns `"success": false`, otherwise returns `true`.

Payload example:

        {
            "success": true
        }

#### user_list
Sent by the server as a response to `list_users` command.

Payload example:

        {
            "users": ["john_doe1952", "NAGibaTOR_40k"]
        }

#### message_sent
Sent by a server as an acknowledgement of the message delivery by a user.

Payload example:

        {
            "username": "NAGibaTOR_40k"
        }

#### new_message
Sent by the server when some user sends a message to the current user. Timestamp is in milliseconds.

Payload example:

        {
            "username": "NAGibaTOR_40k",
            "online": true,
            "message": "sup m8",
            "timestamp": 1438182184000
        }


