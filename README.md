Simplistic chat written in Java

## Building project

Use `package` Maven task for root project. Two standalone jars will be placed in `out/` directory

## DB configuration

This project uses PostgreSQL and [[http://flywaydb.org/ | Flyway]] for database migration.

To run the server you have to supply properties file, containing information about DB connection and TCP port.
Example:

    server.port = 1234
    db.name = practicechat
    db.serverName = localhost
    db.username = postgres
    db.password = 1234

## Chat protocol description

Chat uses JSON-based protocol for client-server communication. 

Every message is a JSON object that contains two fields:

* message_type -- a string that determines the type of the message
* payload -- a JSON object which is specific to the message type

### Client-to-Server messages

#### connect
Tries to login the user with the specified username and password. If the user doesn't exist, registers them.
Username cannot contain spaces. This message must be sent by the client within 

Payload example:

        {
            "username": "john_doe1952",
            "password": "qwerty"
        }

The server must answer with `login_result` message

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

#### get_history
Requests message history with the specified user

Payload example:

        {
            "username": "NAGibaTOR_40k"
        }

#### list_users
Ask the server to return the list of all currently connected users.

Payload must be `null`.

### Server-to-Client messages

#### connect_result
Sent by the server upon client's `connect` request. `user_exists` is true, if the user was already registered.
`success` is false if the specified user exists and the password is wrong, or if the username contains illegal characters.

Payload example (success):

        {
            "success": true,
            "user_exists": false
        }

        
Payload example (failure):

        {
            "success": false,
            "user_exists": true
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

#### message_history
Sent by the server as a response to `get_history` command.

Payload example:

        {
            "messages": [
                {
                    "sender": "NAGibaTOR_40k",
                    "destination": "john_doe1952",
                    "message": "sup m8",
                    "timestamp": 1438182184000
                }
            ]
        }

