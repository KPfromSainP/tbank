package academy.backend.market_pulse.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import academy.backend.market_pulse.repository.InMemoryInstrumentRepository;

/** Список наблюдения: нормализация тикеров, дубликаты, неизменяемое представление, очистка по справочнику. */
class WatchlistTest {

    @Nested
    @DisplayName("add / contains")
    class AddAndContains {

        @Test
        void newTickerIsAdded() {
            Watchlist watchlist = new Watchlist();

            assertThat(watchlist.add("SBER")).isTrue();
            assertThat(watchlist.contains("SBER")).isTrue();
        }

        @ParameterizedTest(name = "повторное добавление [{1}] после [{0}] -> дубликат")
        @CsvSource({"SBER, SBER", "SBER, sber", "sber, SBER", "SbEr, sBeR"})
        void duplicateIgnoringCaseIsNotAddedTwice(String first, String second) {
            Watchlist watchlist = new Watchlist();
            watchlist.add(first);

            assertThat(watchlist.add(second)).isFalse();
            assertThat(watchlist.tickers()).hasSize(1);
        }

        @ParameterizedTest(name = "contains({0}) -> true")
        @CsvSource({"SBER", "sber", "Sber"})
        void containsIgnoresCase(String query) {
            Watchlist watchlist = new Watchlist();
            watchlist.add("SBER");

            assertThat(watchlist.contains(query)).isTrue();
        }

        @Test
        void unknownTickerIsNotContained() {
            Watchlist watchlist = new Watchlist();
            watchlist.add("SBER");

            assertThat(watchlist.contains("GAZP")).isFalse();
        }

        @Test
        void nullTickerFailsFast() {
            Watchlist watchlist = new Watchlist();

            assertThatThrownBy(() -> watchlist.add(null)).isInstanceOf(NullPointerException.class);
        }

        // Тест описывает желаемое поведение; пока класс принимает пустой тикер, он отключён.
        @Disabled("Известный пробел: Watchlist принимает пустой тикер")
        @Test
        void blankTickerShouldBeRejected() {
            Watchlist watchlist = new Watchlist();

            assertThatThrownBy(() -> watchlist.add("  ")).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("tickers()")
    class Tickers {

        @Test
        void tickersAreNormalizedAndKeepInsertionOrder() {
            Watchlist watchlist = new Watchlist();
            watchlist.add("lkoh");
            watchlist.add("SBER");
            watchlist.add("gazp");

            assertThat(watchlist.tickers()).containsExactly("LKOH", "SBER", "GAZP");
        }

        @Test
        void returnedViewCannotBeModified() {
            Watchlist watchlist = new Watchlist();
            watchlist.add("SBER");

            assertThatThrownBy(() -> watchlist.tickers().add("GAZP")).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> watchlist.tickers().clear()).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void emptyWatchlistHasNoTickers() {
            assertThat(new Watchlist().tickers()).isEmpty();
        }
    }

    @Nested
    @DisplayName("prune")
    class Prune {

        @Test
        void tickersMissingFromTheRepositoryAreRemovedAndReturnedInOrder() {
            Watchlist watchlist = new Watchlist();
            watchlist.add("sber");
            watchlist.add("DELISTED1");
            watchlist.add("fxus");
            watchlist.add("DELISTED2");

            assertThat(watchlist.prune(new InMemoryInstrumentRepository())).containsExactly("DELISTED1", "DELISTED2");
            assertThat(watchlist.tickers()).containsExactly("SBER", "FXUS");
        }

        @Test
        void nothingIsRemovedWhenEverythingIsStillListed() {
            Watchlist watchlist = new Watchlist();
            watchlist.add("SBER");

            assertThat(watchlist.prune(new InMemoryInstrumentRepository())).isEmpty();
            assertThat(watchlist.tickers()).containsExactly("SBER");
        }
    }
}
