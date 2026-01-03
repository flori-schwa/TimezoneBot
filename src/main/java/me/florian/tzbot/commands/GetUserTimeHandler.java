package me.florian.tzbot.commands;

import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import me.florian.tzbot.DatabaseAccess;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.TimeZone;

public class GetUserTimeHandler implements UserCommandHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withLocale(Locale.ENGLISH);

    @Override
    public String getName() {
        return "Get Time";
    }

    @Override
    public Mono<Void> handle(UserInteractionEvent event) {
        final User user = event.getTargetUser().blockOptional().orElseThrow();
        final UserData targetUser = event.getInteraction().getGuildId()
                .flatMap(guildId -> user.asMember(guildId).blockOptional())
                .map(UserData::fromMember)
                .orElseGet(() -> UserData.fromUser(user));

        try {
            final ZoneId userZoneId = DatabaseAccess.getUserZoneId(targetUser.id());

            if (userZoneId == null) {
                return event.reply(targetUser.name() + " does not have their timezone set").withEphemeral(true);
            }

            ZonedDateTime now = Instant.now().atZone(userZoneId);

            final EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .author(EmbedCreateFields.Author.of(String.format("%s's current local time", targetUser.name()), null, targetUser.avatarUrl()))
                    .description(now.format(FORMATTER))
                    .addField("%s's timezone".formatted(targetUser.name()), TimeZone.getTimeZone(userZoneId).getDisplayName(Locale.ENGLISH), true)
                    .build();

            return event.reply()
                    .withEmbeds(embed)
                    .withEphemeral(true);
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            return event.reply("Failed to query " + targetUser.name() + "'s Time zone").withEphemeral(true);
        }
    }

    private record UserData(long id, String name, String avatarUrl) {
        public static UserData fromUser(User user) {
            return new UserData(user.getId().asLong(), user.getGlobalName().orElseGet(user::getUsername), user.getAvatarUrl());
        }

        public static UserData fromMember(Member member) {
            return new UserData(member.getId().asLong(), member.getDisplayName(), member.getEffectiveAvatarUrl());
        }
    }
}
