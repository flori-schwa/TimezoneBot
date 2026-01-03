package me.florian.tzbot.commands;

import discord4j.core.event.domain.interaction.UserInteractionEvent;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class UserCommandListener {

    private static final Map<String, UserCommandHandler> HANDLERS = new HashMap<>();

    private static void register(UserCommandHandler handler) {
        HANDLERS.put(handler.getName(), handler);
    }

    static {
        register(new GetUserTimeHandler());
    }

    private UserCommandListener() {

    }

    public static Mono<Void> handle(UserInteractionEvent event) {
        final UserCommandHandler handler = HANDLERS.get(event.getCommandName());

        if (handler == null) {
            return Mono.empty();
        }

        return handler.handle(event);
    }
}
