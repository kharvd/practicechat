Simplistic chat written in Java

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
Sends a message to a specified user.

Payload example:

        {
            "user": "NAGibaTOR_40k",
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
Sent by the server upon the client's attempt to send a message to some user. Contains information 
whether the user is currently online. If the user is offline, message will be delivered
as soon as the user goes online.

Payload example:

        {
            "user": "NAGibaTOR_40k",
            "online": true
        }

#### new_message
Sent by the server when some user sends a message to the current user. Timestamp is in milliseconds.

Payload example:

        {
            "user": "NAGibaTOR_40k",
            "online": true,
            "message": "sup m8",
            "timestamp": 1438182184000
        }


