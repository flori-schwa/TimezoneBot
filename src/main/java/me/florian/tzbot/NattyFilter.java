package me.florian.tzbot;

import org.natty.DateGroup;
import org.natty.ParseLocation;

import java.util.Collections;
import java.util.List;

public class NattyFilter {

    public boolean ignoreParseResult(DateGroup group) {
        final List<ParseLocation> matchedDateTimes = group.getParseLocations().getOrDefault("date_time", Collections.emptyList());

        if (!matchedDateTimes.isEmpty() && matchedDateTimes.stream().allMatch(loc -> loc.getText().matches("\\d+"))) {
            return true; // Edge case for natty detecting integers as dates...
        }

        return false;
    }

}
