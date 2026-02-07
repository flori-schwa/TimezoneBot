import me.florian.tzbot.NattyFilter;
import org.junit.jupiter.api.Test;
import org.natty.DateGroup;
import org.natty.Parser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class NattyFilterTest {

    Parser _parser = new Parser();

    NattyFilter _underTest = new NattyFilter();

    @Test
    void testFilterIntegersAsTimes() {
        for (int i = 1; i <= 12; i++) {
            List<DateGroup> dates = _parser.parse(String.valueOf(i));

            assertThat(dates).hasSize(1);
            assertThat(_underTest.ignoreParseResult(dates.getFirst())).withFailMessage("Single integers should be ignored").isTrue();

            dates = _parser.parse("%dam".formatted(i));

            assertThat(dates).hasSize(1);
            assertThat(_underTest.ignoreParseResult(dates.getFirst())).withFailMessage("Relative time (am) should not be ignored").isFalse();

            dates = _parser.parse("%dpm".formatted(i));

            assertThat(dates).hasSize(1);
            assertThat(_underTest.ignoreParseResult(dates.getFirst())).withFailMessage("Relative time (pm) should not be ignored").isFalse();
        }
    }

}
