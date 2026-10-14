package academy.backend.market_pulse.demo;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Quote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Gatherers;

public class StreamExercisesImpl implements StreamExercises {

    @Override
    public List<String> tickersInCurrency(List<Quote> quotes, Currency currency) {
        return quotes.stream()
                .filter(q -> q.getInstrument().getCurrency() == currency)
                .map(q -> q.getInstrument().getTicker())
                .toList();
    }

    @Override
    public List<Quote> topByPrice(List<Quote> quotes, int n) {
        return quotes.stream()
                .sorted(Comparator.comparing(Quote::getPrice).reversed())
                .limit(n)
                .toList();
    }

    @Override
    public Set<Currency> distinctCurrencies(List<Quote> quotes) {
        return quotes.stream()
                .map(q -> q.getInstrument().getCurrency())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean hasAnyLoser(List<Quote> quotes) {
        return quotes.stream()
                .anyMatch(q -> q.getChangePercent().signum() < 0);
    }

    @Override
    public long countByType(List<Quote> quotes, InstrumentType instrumentType) {
        return quotes.stream()
                .filter(q -> q.getInstrument().getType() == instrumentType)
                .count();
    }

    @Override
    public BigDecimal totalPrice(List<Quote> quotes) {
        return quotes.stream()
                .map(Quote::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public double averageChangePercent(List<Quote> quotes) {
        return quotes.stream()
                .map(Quote::getChangePercent)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);
    }

    @Override
    public Map<InstrumentType, Long> countByTypeGrouped(List<Quote> quotes) {
        return quotes.stream()
                .collect(Collectors.groupingBy(
                        q -> q.getInstrument().getType(),
                        Collectors.counting()
                ));
    }

    @Override
    public Map<InstrumentType, Double> averageChangeByType(List<Quote> quotes) {
        return quotes.stream()
                .collect(Collectors.groupingBy(
                        q -> q.getInstrument().getType(),
                        Collectors.averagingDouble(q -> q.getChangePercent().doubleValue())
                ));
    }

    @Override
    public Map<Boolean, List<Quote>> partitionByGain(List<Quote> quotes) {
        return quotes.stream()
                .collect(Collectors.partitioningBy(
                        q -> q.getChangePercent().signum() > 0
                ));
    }

    @Override
    public Map<String, Quote> latestByTicker(List<Quote> quotes) {
        return quotes.stream()
                .collect(Collectors.toMap(
                        q -> q.getInstrument().getTicker(),
                        q -> q,
                        (older, newer) -> newer
                ));
    }

    @Override
    public String joinTickers(List<Quote> quotes) {
        return quotes.stream()
                .map(q -> q.getInstrument().getTicker())
                .collect(Collectors.joining(", "));
    }

    @Override
    public Map<Currency, String> tickersJoinedByCurrency(List<Quote> quotes) {
        return quotes.stream()
                .collect(Collectors.groupingBy(
                        q -> q.getInstrument().getCurrency(),
                        Collectors.mapping(
                                q -> q.getInstrument().getTicker(),
                                Collectors.joining(", ")
                        )
                ));
    }

    @Override
    public List<Quote> findByTickers(List<Quote> quotes, List<String> tickers) {
        Set<String> wanted = new HashSet<>(tickers);
        return quotes.stream()
                .filter(q -> wanted.contains(q.getInstrument().getTicker()))
                .toList();
    }

    @Override
    public List<BigDecimal> movingAveragePrice(List<Quote> quotes, int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be > 0");
        }
        return quotes.stream()
                .map(Quote::getPrice)
                .gather(Gatherers.windowSliding(windowSize))
                .map(this::average)
                .toList();
    }

    private BigDecimal average(List<BigDecimal> window) {
        BigDecimal sum = window.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(window.size()), 10, RoundingMode.HALF_UP);
    }

    @Override
    public int longestLossStreak(List<Quote> quotes) {
        return quotes.stream()
                .map(q -> q.getChangePercent().signum() < 0)
                .gather(Gatherers.scan(
                        () -> 0,
                        (streak, isLoser) -> isLoser ? streak + 1 : 0
                ))
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }
}
