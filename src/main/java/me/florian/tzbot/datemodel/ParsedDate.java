package me.florian.tzbot.datemodel;

import java.time.Instant;

public record ParsedDate(String matchedText, Instant instant) {
}
