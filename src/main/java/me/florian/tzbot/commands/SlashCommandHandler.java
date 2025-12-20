package me.florian.tzbot.commands;

import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

public interface SlashCommandHandler {

    String getName();

    Mono<Void> handle(ChatInputInteractionEvent event);

    default Mono<Void> handleAutoComplete(ChatInputAutoCompleteEvent event) {
        return Mono.empty();
    }

}
