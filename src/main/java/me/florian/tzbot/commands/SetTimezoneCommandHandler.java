package me.florian.tzbot.commands;

import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import me.florian.tzbot.UserTimezoneStore;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.zone.ZoneRulesProvider;
import java.util.List;

public class SetTimezoneCommandHandler implements SlashCommandHandler {
    @Override
    public String getName() {
        return "settimezone";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        String timezone = event.getOption("timezone").flatMap(ApplicationCommandInteractionOption::getValue).map(ApplicationCommandInteractionOptionValue::asString).orElseThrow();

        try {
            final ZoneId zoneId = ZoneId.of(timezone);
            boolean changed = UserTimezoneStore.saveUserTimezone(event.getUser().getId().asLong(), zoneId);

            if (!changed) {
                return event.reply("Your timezone was already set to " + zoneId.getId()).withEphemeral(true);
            } else {
                return event.reply("Your timezone was updated to " + zoneId.getId()).withEphemeral(true);
            }
        } catch (DateTimeException e) {
            return event.reply("Malformed timezone").withEphemeral(true);
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            return event.reply("Failed to update your timezone!").withEphemeral(true);
        }
    }

    @Override
    public Mono<Void> handleAutoComplete(ChatInputAutoCompleteEvent event) {
        final ApplicationCommandInteractionOption option = event.getFocusedOption();

        if (!"timezone".equals(option.getName())) {
            return Mono.empty();
        }

        String typing = option.getValue().map(ApplicationCommandInteractionOptionValue::asString).orElseThrow();

        List<ApplicationCommandOptionChoiceData> zones = ZoneRulesProvider.getAvailableZoneIds().stream() //
                .filter(zoneId -> zoneId.startsWith(typing))
                .sorted()
                .limit(25)
                .map(zoneId -> (ApplicationCommandOptionChoiceData) ApplicationCommandOptionChoiceData.builder().name(zoneId).value(zoneId).build())
                .toList();

        return event.respondWithSuggestions(zones);
    }
}
