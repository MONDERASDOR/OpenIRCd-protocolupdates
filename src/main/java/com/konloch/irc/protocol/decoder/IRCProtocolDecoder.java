package com.konloch.irc.protocol.decoder;

import static com.konloch.irc.protocol.encoder.messages.IRCOpcodes.ERR_UNKNOWNCOMMAND;

import com.konloch.irc.extension.events.listeners.IRCdUserListener;
import com.konloch.irc.protocol.decoder.messages.DecodeMessage;
import com.konloch.irc.server.client.User;
import com.konloch.irc.server.util.EscapeUtil;
import com.konloch.util.FastStringUtils;

import java.util.List;

/**
 * @author Konloch
 * @since 3/2/2023 */
public class IRCProtocolDecoder {
    public void decodeMessage(User user, String messages) {
        if (messages == null || messages.isEmpty())
            return;

        if (user.getIRC().isVerbose())
            System.out.println("I: " + messages);

        String[] msg = messages.split("\\r?\\n");

        for (String message : msg) {
            String command;
            String parameters = null;
            String prefix = null;

            if (message.startsWith(":")) {
                int spaceIndex = message.indexOf(' ');
                if (spaceIndex == -1) {
                    prefix = message.substring(1);
                    command = "";
                } else {
                    prefix = message.substring(1, spaceIndex);
                    String remaining = message.substring(spaceIndex + 1);
                    if (remaining.contains(" :")) {
                        int colonIndex = remaining.indexOf(" :");
                        command = remaining.substring(0, colonIndex);
                        parameters = remaining.substring(colonIndex + 2);
                    } else {
                        String[] parts = remaining.split(" ", 2);
                        command = parts[0];
                        if (parts.length > 1)
                            parameters = parts[1];
                    }
                }
            } else {
                if (message.contains(" :")) {
                    int colonIndex = message.indexOf(" :");
                    command = message.substring(0, colonIndex);
                    parameters = message.substring(colonIndex + 2);
                } else {
                    String[] parts = message.split(" ", 2);
                    command = parts[0];
                    if (parts.length > 1)
                        parameters = parts[1];
                }
            }

            String messageIdentifier = command.toLowerCase();
            String messageValue = parameters;

            DecodeMessage decodeMessage = DecodeMessage.getLookup().get(messageIdentifier);

            if (decodeMessage != null) {
                boolean cancelled = false;
                List<IRCdUserListener> listeners = user.getIRC().getEvents().getUserEvents();
                if (listeners != null) {
                    for (IRCdUserListener listener : listeners) {
                        if (!listener.onProtocolMessageSent(user, messageValue))
                            cancelled = true;
                    }
                }
                if (!cancelled)
                    decodeMessage.getDecodeRunnable().run(user, messageValue);
            } else {
                user.getEncoder().newServerUserMessage()
                        .opcode(ERR_UNKNOWNCOMMAND)
                        .message(user.getIRC().fromConfig("unknown.command"))
                        .send();
            }
        }
    }
}
