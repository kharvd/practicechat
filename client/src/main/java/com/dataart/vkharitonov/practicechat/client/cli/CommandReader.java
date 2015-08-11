package com.dataart.vkharitonov.practicechat.client.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public class CommandReader {

    private static final String USERNAME_PATTERN = "[\\S&&[^#]]+";
    private static final String HOST_PATTERN = "([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])" +
            "(\\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9]))*";

    private static final String CONNECT_SYNTAX_STRING = "Syntax: connect <username>@<host>:<port> <password>";
    private static final Pattern CONNECT_PATTERN =
            Pattern.compile("connect\\s+(?<username>" + USERNAME_PATTERN + ")@(?<host>" + HOST_PATTERN +
                                    "):(?<port>\\d+)\\s+(?<password>\\S+)");

    private static final String LIST_SYNTAX_STRING = "Syntax: list [#<room>]";
    private static final Pattern LIST_PATTERN = Pattern.compile("list(\\s+(?<roomName>#?" + USERNAME_PATTERN + "))?");

    private static final String SEND_SYNTAX_STRING = "Syntax: send <username> \"<message>\"";
    private static final Pattern SEND_PATTERN =
            Pattern.compile("send\\s+(?<name>#?" + USERNAME_PATTERN + ")\\s+\"(?<message>.*)\"");

    private static final String HISTORY_SYNTAX_STRING = "Syntax: history <username>";
    private static final Pattern HISTORY_PATTERN = Pattern.compile("history\\s+(?<name>#?" + USERNAME_PATTERN + ")");

    private static final String JOIN_SYNTAX_STRING = "Syntax: join #<room>";
    private static final Pattern JOIN_PATTERN = Pattern.compile("join\\s+(?<roomName>#?" + USERNAME_PATTERN + ")");

    private static final String LEAVE_SYNTAX_STRING = "Syntax: leave #<room>";
    private static final Pattern LEAVE_PATTERN = Pattern.compile("leave\\s+(?<roomName>#?" + USERNAME_PATTERN + ")");

    private static final String DROP_SYNTAX_STRING = "Syntax: drop #<room>";
    private static final Pattern DROP_PATTERN = Pattern.compile("drop\\s+(?<roomName>#?" + USERNAME_PATTERN + ")");

    private final BufferedReader in;
    private CommandHandler handler;

    /**
     * Creates a new CommandReader
     *
     * @param in      Reader to read commands from
     * @param handler Callback which handles incoming commands
     */
    public CommandReader(Reader in, CommandHandler handler) {
        checkNotNull(handler);

        this.in = new BufferedReader(in);
        this.handler = handler;

        try {
            loop();
        } catch (IOException e) {
            handler.onError(e);
        }
    }

    private void loop() throws IOException {
        String line;
        boolean exit = false;
        while (!exit && (line = in.readLine()) != null) {
            String trimmed = line.trim();
            String[] split = trimmed.split(" ", 2);
            try {
                switch (split[0]) {
                    case "connect":
                        parseConnectCommand(line);
                        break;
                    case "disconnect":
                        handler.onDisconnect();
                        break;
                    case "list":
                        parseListCommand(line);
                        break;
                    case "rooms":
                        handler.onRoomsList();
                        break;
                    case "send":
                        parseSendMsgCommand(line);
                        break;
                    case "history":
                        parseHistoryCommand(line);
                        break;
                    case "join":
                        parseJoinCommand(line);
                        break;
                    case "leave":
                        parseLeaveCommand(line);
                        break;
                    case "drop":
                        parseDropCommand(line);
                        break;
                    case "help":
                        handler.onHelp();
                        break;
                    case "exit":
                        exit = true;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown command: " + split[0]);
                }
            } catch (IllegalArgumentException e) {
                handler.onError(e);
            }
        }

        handler.onExit();
    }

    private String parseSimpleCommand(String line, Pattern pattern, String syntaxString, String argName) {
        Matcher matcher = pattern.matcher(line);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(syntaxString);
        }

        return matcher.group(argName);
    }

    private void parseLeaveCommand(String line) {
        String roomName = parseSimpleCommand(line, LEAVE_PATTERN, LEAVE_SYNTAX_STRING, "roomName");

        if (!roomName.startsWith("#")) {
            roomName = "#" + roomName;
        }

        handler.onLeave(roomName);
    }

    private void parseDropCommand(String line) {
        String roomName = parseSimpleCommand(line, DROP_PATTERN, DROP_SYNTAX_STRING, "roomName");

        if (!roomName.startsWith("#")) {
            roomName = "#" + roomName;
        }

        handler.onDrop(roomName);
    }

    private void parseJoinCommand(String line) {
        String roomName = parseSimpleCommand(line, JOIN_PATTERN, JOIN_SYNTAX_STRING, "roomName");

        if (!roomName.startsWith("#")) {
            roomName = "#" + roomName;
        }

        handler.onJoin(roomName);
    }

    private void parseHistoryCommand(String line) {
        handler.onHistory(parseSimpleCommand(line, HISTORY_PATTERN, HISTORY_SYNTAX_STRING, "name"));
    }

    private void parseSendMsgCommand(String line) {
        Matcher matcher = SEND_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(SEND_SYNTAX_STRING);
        }

        handler.onSendMessage(matcher.group("name"), matcher.group("message"));
    }

    private void parseListCommand(String line) {
        String roomName = parseSimpleCommand(line, LIST_PATTERN, LIST_SYNTAX_STRING, "roomName");

        if (roomName != null && !roomName.startsWith("#")) {
            roomName = "#" + roomName;
        }

        handler.onList(roomName);
    }

    private void parseConnectCommand(String line) {
        Matcher matcher = CONNECT_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(CONNECT_SYNTAX_STRING);
        }

        handler.onConnect(matcher.group("username"), matcher.group("password"), matcher.group("host"),
                          Integer.parseInt(matcher.group("port")));
    }
}
