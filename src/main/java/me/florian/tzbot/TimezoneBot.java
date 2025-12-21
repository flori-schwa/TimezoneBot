package me.florian.tzbot;

import com.novamaday.d4j.gradle.simplebot.GlobalCommandRegistrar;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.MessageReferenceData;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.Permission;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimezoneBot {

    private static final String DELETE_MESSAGE_BUTTON_ID_BASE = "delete-message";

    private static final Pattern DELETE_MESSAGE_BUTTON_ID_PATTERN = Pattern.compile(DELETE_MESSAGE_BUTTON_ID_BASE + "_(?<authorid>\\d+)");


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
                    Mono<Void> respondToButtons = client.on(ButtonInteractionEvent.class, TimezoneBot::handleButton).then();
                    Mono<Void> respondToCommands = client.on(ChatInputInteractionEvent.class, SlashCommandListener::handle).then();
                    Mono<Void> respondToAutocomplete = client.on(ChatInputAutoCompleteEvent.class, SlashCommandListener::handleAutoComplete).then();

                    return respondToMessage.and(respondToButtons).and(respondToCommands).and(respondToAutocomplete);
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
            final Member messageAuthor = message.getAuthorAsMember().block();

            if (channel == null || messageAuthor == null) {
                return Mono.empty();
            }

            final ZonedDateTime messageDate = message.getTimestamp().atZone(userTimezone);
            final StringBuilder descriptionBuilder = new StringBuilder();

            for (ParsedDate parsedDate : parsedDates) {
                descriptionBuilder.append(String.format("%s -> %s%n", parsedDate.matchedText(), formatTimeStamp(parsedDate.instant(), messageDate)));
            }

            final EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .author(EmbedCreateFields.Author.of(String.format("Converting from %s's timezone", messageAuthor.getDisplayName()), null, messageAuthor.getEffectiveAvatarUrl()))
                    .description(descriptionBuilder.toString().trim())
                    .addField("%s's timezone".formatted(messageAuthor.getDisplayName()), timeZone.getDisplayName(Locale.ENGLISH), true)
                    .build();

            Button deleteButton = Button.danger(DELETE_MESSAGE_BUTTON_ID_BASE + "_" + messageAuthor.getId().asLong(), "Delete");

            return channel.createMessage(embed)
                    .withMessageReference(MessageReferenceData.builder().messageId(message.getId().asLong()).build())
                    .withComponents(ActionRow.of(deleteButton))
                    .withAllowedMentions(AllowedMentions.suppressAll());
        } else {
            return Mono.<Void>empty();
        }
    }

    private static Mono<?> handleButton(ButtonInteractionEvent event) {
        final Message message = event.getMessage().orElseThrow();
        final Matcher matcher = DELETE_MESSAGE_BUTTON_ID_PATTERN.matcher(event.getCustomId());

        if (!matcher.matches()) {
            return Mono.empty();
        }

        final long authorId = Long.parseLong(matcher.group("authorid"));
        String reason = "Requested by original message author";

        if (authorId != event.getUser().getId().asLong()) {
            final Member user = event.getUser().asMember(message.getGuildId().orElseThrow()).blockOptional().orElseThrow();

            if (!user.getBasePermissions().blockOptional().orElseThrow().contains(Permission.MANAGE_MESSAGES)) {
                return event.reply()
                        .withContent("You do not have permission to delete this message!")
                        .withEphemeral(true);
            }

            reason = "Deleted by Administrator";
        }

        return message.delete(reason);
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
