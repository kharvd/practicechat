package com.dataart.vkharitonov.practicechat.client.cli;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.Objects;

/**
 * Reads user commands from a {@link Reader}
 */
public class CommandReader {

    private final StreamTokenizer tok;
    private CommandHandler handler;

    /**
     * Creates a new CommandReader
     *
     * @param in      Reader to read commands from
     * @param handler Callback which handles incoming commands
     */
    public CommandReader(Reader in, CommandHandler handler) {
        if (handler == null) {
            throw new NullPointerException();
        }

        this.handler = handler;

        tok = new StreamTokenizer(in);
        tok.resetSyntax();
        tok.wordChars(0x23, 0xFF);
        tok.whitespaceChars(0x00, 0x20);
        tok.quoteChar('"');
        tok.eolIsSignificant(true);
        tok.lowerCaseMode(true);

        try {
            loop();
        } catch (IOException e) {
            handler.onError(e);
        }
    }

    private void loop() throws IOException {
        int ttype = tok.nextToken();

        while (!Objects.equals(tok.sval, "exit") && ttype != StreamTokenizer.TT_EOF) {
            if (ttype == StreamTokenizer.TT_WORD) {
                try {
                    switch (tok.sval) {
                        case "connect":
                            parseConnectCommand();
                            break;
                        case "disconnect":
                            handler.onDisconnect();
                            break;
                        case "list":
                            parseListCommand();
                            break;
                        case "send":
                            parseSendMsgCommand();
                            break;
                        case "history":
                            parseHistoryCommand();
                            break;
                        case "join":
                            parseJoinCommand();
                            break;
                        case "help":
                            handler.onHelp();
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown command: " + tok.sval);
                    }
                } catch (IllegalArgumentException e) {
                    handler.onError(e);
                }
            }

            do {
                ttype = tok.nextToken();
            } while (ttype != StreamTokenizer.TT_EOF && ttype != StreamTokenizer.TT_EOL);

            ttype = tok.nextToken();
        }

        handler.onExit();
    }

    private void parseListCommand() throws IOException {
        String roomName = null;
        if (tok.nextToken() == StreamTokenizer.TT_WORD) {
            roomName = tok.sval;
        }

        if (roomName != null && !roomName.startsWith("#")) {
            roomName = "#" + tok.sval;
        }

        handler.onList(roomName);
    }

    private void parseHistoryCommand() throws IOException {
        final String historySyntaxString = "Syntax: history <username>";

        if (tok.nextToken() != StreamTokenizer.TT_WORD) {
            throw new IllegalArgumentException(historySyntaxString);
        }

        handler.onHistory(tok.sval);
    }

    private void parseJoinCommand() throws IOException {
        final String joinSyntaxString = "Syntax: join #<room>";

        if (tok.nextToken() != StreamTokenizer.TT_WORD) {
            throw new IllegalArgumentException(joinSyntaxString);
        }

        String roomName = tok.sval;
        if (!roomName.startsWith("#")) {
            roomName = "#" + tok.sval;
        }

        handler.onJoin(roomName);
    }

    private void parseSendMsgCommand() throws IOException, IllegalArgumentException {
        int ttype = tok.nextToken();
        final String sendMsgSyntaxString = "Syntax: send <username> \"<message>\"";

        if (ttype != StreamTokenizer.TT_WORD) {
            throw new IllegalArgumentException(sendMsgSyntaxString);
        }

        String username = tok.sval;
        ttype = tok.nextToken();

        if (ttype != '"') {
            throw new IllegalArgumentException(sendMsgSyntaxString);
        }

        String message = tok.sval;
        handler.onSendMessage(username, message);
    }

    private void parseConnectCommand() throws IOException, IllegalArgumentException {
        final String connectSyntaxString = "Syntax: connect <username>@<host>:<port> <password>";

        if (tok.nextToken() != StreamTokenizer.TT_WORD) {
            throw new IllegalArgumentException(connectSyntaxString);
        }

        String connectString = tok.sval;
        String[] userAndHostPort = connectString.split("@");

        if (userAndHostPort.length < 2) {
            throw new IllegalArgumentException(connectSyntaxString);
        }

        String username = userAndHostPort[0];
        String[] hostAndPort = userAndHostPort[1].split(":");
        if (hostAndPort.length < 2) {
            throw new IllegalArgumentException(connectSyntaxString);
        }

        String host = hostAndPort[0];
        int port;
        try {
            port = Integer.parseInt(hostAndPort[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(connectSyntaxString);
        }

        if (tok.nextToken() != StreamTokenizer.TT_WORD) {
            throw new IllegalArgumentException(connectSyntaxString);
        }

        String password = tok.sval;

        handler.onConnect(username, password, host, port);
    }
}
