package me.florian.tzbot.commands;

import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class SlashCommandListener {

    private static final List<SlashCommandHandler> HANDLERS = List.of(new SetTimezoneCommandHandler());

    private SlashCommandListener() {

    }

    public static Mono<Void> handle(ChatInputInteractionEvent e) {
        return Flux.fromIterable(HANDLERS)
                .filter(command -> command.getName().equals(e.getCommandName())) //
                .next()
                .flatMap(command -> command.handle(e));
    }

    public static Mono<Void> handleAutoComplete(ChatInputAutoCompleteEvent e) {
        return Flux.fromIterable(HANDLERS)
                .filter(command -> command.getName().equals(e.getCommandName())) //
                .next() //
                .flatMap(command -> command.handleAutoComplete(e));
    }


}
