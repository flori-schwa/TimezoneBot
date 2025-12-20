package me.florian.tzbot;

import com.novamaday.d4j.gradle.simplebot.GlobalCommandRegistrar;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.MessageReferenceData;
import discord4j.discordjson.json.gateway.MessageCreate;
import me.florian.tzbot.commands.SlashCommandHandler;
import me.florian.tzbot.commands.SlashCommandListener;
import org.natty.DateGroup;
import org.natty.Parser;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class TimezoneBot {

    static void main(String[] args) throws Exception {
        String token = System.getenv("BOT_TOKEN");

        UserTimezoneStore.init();

        DiscordClient.create(token)
                .withGateway(client -> {
                    try {
                        new GlobalCommandRegistrar(client.getRestClient()).registerCommands(List.of("settimezone.json"));
                    } catch (IOException e) {
                        e.printStackTrace(System.err);
                    }

                    Mono<?> respondToMessage = client.on(MessageCreateEvent.class, TimezoneBot::handleMessage).then();
                    Mono<Void> respondToCommands = client.on(ChatInputInteractionEvent.class, SlashCommandListener::handle).then();
                    Mono<Void> respondToAutocomplete = client.on(ChatInputAutoCompleteEvent.class, SlashCommandListener::handleAutoComplete).then();

                    return respondToMessage.and(respondToCommands).and(respondToAutocomplete);
                }).block();
    }

    private static Mono<?> handleMessage(MessageCreateEvent event) {
        Message message = event.getMessage();
        String content = message.getContent();

        final ZoneId userTimezone;

        try {
            userTimezone = UserTimezoneStore.getUserZoneId(message.getUserData().id().asLong());
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            return Mono.empty();
        }

        if (userTimezone == null) {
            return Mono.empty();
        }

        Parser parser = new Parser(TimeZone.getTimeZone(userTimezone));
        final List<DateGroup> groups = parser.parse(content);

        if (!groups.isEmpty()) {
            final MessageChannel channel = message.getChannel().block();

            StringBuilder messageBuilder = new StringBuilder();

            for (DateGroup group : groups) {
                String text = group.getText();
                Date date = group.getDates().getFirst();

                messageBuilder.append(String.format("%s -> <t:%d:t>%n", text, date.getTime() / 1000));
            }

            return channel.createMessage(messageBuilder.toString().trim()).withMessageReference(MessageReferenceData.builder().messageId(message.getId().asLong()).build());
        } else {
            return Mono.<Void>empty();
        }

    }

}
