package me.florian.tzbot;

import com.novamaday.d4j.gradle.simplebot.GlobalCommandRegistrar;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.MessageReferenceData;
import discord4j.rest.util.AllowedMentions;
import me.florian.tzbot.commands.SlashCommandListener;
import me.florian.tzbot.datemodel.ParsedDate;
import org.natty.DateGroup;
import org.natty.ParseLocation;
import org.natty.Parser;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

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

        final TimeZone timeZone = TimeZone.getTimeZone(userTimezone);
        final List<ParsedDate> parsedDates = parseDates(content, userTimezone, message.getTimestamp());

        if (!parsedDates.isEmpty()) {
            final MessageChannel channel = message.getChannel().block();

            ZonedDateTime messageDate = message.getTimestamp().atZone(userTimezone);
            StringBuilder messageBuilder = new StringBuilder(String.format("Converting from %s:%n", timeZone.getDisplayName(Locale.ENGLISH)));

            for (ParsedDate parsedDate : parsedDates) {
                messageBuilder.append(String.format("%s -> %s", parsedDate.matchedText(), formatTimeStamp(parsedDate.instant(), messageDate)));
            }

            return channel.createMessage(messageBuilder.toString().trim())
                    .withMessageReference(MessageReferenceData.builder().messageId(message.getId().asLong()).build())
                    .withAllowedMentions(AllowedMentions.suppressAll());
        } else {
            return Mono.<Void>empty();
        }
    }

    private static String formatTimeStamp(Instant timeStamp, ZonedDateTime reference) {
        String format;

        if (reference.truncatedTo(ChronoUnit.DAYS).isEqual(timeStamp.atZone(reference.getZone()).truncatedTo(ChronoUnit.DAYS))) {
            format = "t"; // Just the time
        } else {
            format = "f"; // Date and time
        }

        return String.format("<t:%d:%s>", timeStamp.getEpochSecond(), format);
    }

    private static List<ParsedDate> parseDates(String text, ZoneId timeZone, Instant referenceDate) {
        final Parser parser = new Parser(TimeZone.getTimeZone(timeZone));
        final List<DateGroup> groups = parser.parse(text, Date.from(referenceDate));
        final List<ParsedDate> result = new ArrayList<>();

        for (DateGroup group : groups) {
            String matchedText = group.getText();

            final List<ParseLocation> matchedDateTimes = group.getParseLocations().getOrDefault("date_time", Collections.emptyList());

            if (!matchedDateTimes.isEmpty() && matchedDateTimes.stream().allMatch(loc -> loc.getText().matches("\\d+"))) {
                continue; // Edge case for natty detecting integers as dates...
            }

            // TODO Handle Conjunctions and ranges (i.e. multiple Dates)
            List<Date> dates = group.getDates();
            result.add(new ParsedDate(matchedText, dates.getFirst().toInstant()));
        }

        return result;
    }


}
