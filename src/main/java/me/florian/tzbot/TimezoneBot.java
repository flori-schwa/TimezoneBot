package me.florian.tzbot;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.MessageReferenceData;
import org.natty.DateGroup;
import org.natty.Parser;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class TimezoneBot {

    static void main(String[] args) throws SQLException {
        String token = args[0];

        UserTimezoneStore.init();

        DiscordClient client = DiscordClient.create(token);
        GatewayDiscordClient gateway = client.login().block();

        gateway.on(MessageCreateEvent.class).subscribe(event -> {
            Message message = event.getMessage();
            String content = message.getContent();

            try {
                UserTimezoneStore.getUserZoneId(message.getUserData().id().asLong()).ifPresent(zone -> {
                    Parser parser = new Parser(TimeZone.getTimeZone(zone));
                    final List<DateGroup> groups = parser.parse(content);

                    if (!groups.isEmpty()) {
                        final MessageChannel channel = message.getChannel().block();

                        StringBuilder messageBuilder = new StringBuilder();

                        for (DateGroup group : groups) {
                            String text = group.getText();
                            Date date = group.getDates().getFirst();

                            messageBuilder.append(String.format("%s -> <t:%d:t>%n", text, date.getTime() / 1000));
                        }

                        channel.createMessage(messageBuilder.toString().trim()).withMessageReference(MessageReferenceData.builder().messageId(message.getId().asLong()).build()).block();
                    }
                });
            } catch (SQLException e) {
                e.printStackTrace(System.err);
            }

        });

        gateway.onDisconnect().block();
    }

}
