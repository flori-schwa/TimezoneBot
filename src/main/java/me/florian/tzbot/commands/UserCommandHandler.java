package me.florian.tzbot.commands;

import discord4j.core.event.domain.interaction.UserInteractionEvent;
import reactor.core.publisher.Mono;

public interface UserCommandHandler {

    String getName();

    Mono<Void> handle(UserInteractionEvent event);

}
