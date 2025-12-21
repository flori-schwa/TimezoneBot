import me.florian.tzbot.util.Trie;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.zone.ZoneRulesProvider;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class TrieTest {

    @Test
    void testInsert() {
        Trie<String> trie = new Trie<>();

        assertThat(trie.insert("a", "a")).isTrue();
        assertThat(trie.insert("aa", "aa")).isTrue();
        assertThat(trie.insert("aa", "a")).isTrue();

        assertThat(trie.insert("a", "a")).isFalse();
    }

    @Test
    void testSearch() {
        Trie<String> trie = new Trie<>();

        trie.insert("ABC/123", "ABC/123");
        trie.insert("ABC/456", "ABC/456");
        trie.insert("ABC", "ABC");

        trie.insert("DEF/123", "DEF/123");
        trie.insert("DEF/456", "DEF/456");
        trie.insert("DEF", "DEF");

        assertThat(trie.search("ABC")).containsExactly("ABC", "ABC/123", "ABC/456");
        assertThat(trie.search("DEF")).containsExactly("DEF", "DEF/123", "DEF/456");
    }

}
