# Subscription review fixes

> **For agentic workers:** Use superpowers:executing-plans for inline implementation.

**Goal:** Fix the three verified review findings and recheck subscription behavior.

**Architecture:** Keep the handwritten platform-thread pool, Treiber stack and per-subscription locks. Restrict the interactive command to the interruptible HTTP client; keep the service injectable for tests. Distinguish a queued request from a running one when expiring subscriptions.

**Tech Stack:** Java 25, Maven, JUnit 5, picocli, JDK HttpServer.

**Spec:** The review and the user's instruction in this chat: fix the findings, review again, keep the student project simple.

## Constraints

- Preserve fixed delay after completion and allow an already running request to finish after lifetime.
- Preserve sequential CLI calls and independent cancellation of tickers.
- Do not replace the custom pool or introduce production infrastructure.
- Work in the existing clean seminar10 checkout; leave changes available for review.

## Review focus

- Re-registering an expired queued ticker must not revive the old task.
- A duplicate watch must leave an active request intact.
- Removing and re-registering a ticker must suppress an old late response.
- Callback failure must not masquerade as a quote-loading failure.
- Invalid input, EOF, cancellation and interrupted shutdown must release owned threads.

## Tasks

- [x] Add failing CLI tests for rejecting non-HTTP_CLIENT implementations before reading input; add the minimal command guard and update command documentation.
- [x] Add failing expiry/state and re-registration tests; expire subscriptions with no running request and recheck expiration in watch.
- [x] Strengthen SubscriptionHttpTest: after remove, a second ticker must return with one worker before the first HTTP response is released. Verify a temporary no-interrupt mutation fails.
- [x] Recheck CLI, subscription lifecycle, callbacks, pool and tests; reproduce additional concrete defects before fixing them.
- [x] Run the targeted tests and the full Maven verification; request an independent read-only review and address material findings.

## Verification record

- Baseline: `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -o test` succeeded before edits.
- Regression tests first failed for unsupported CLI clients and expired queued subscriptions, then passed after the minimal fixes.
- An output callback failure caused three calls in the original code. The regression test failed, then passed after moving publication outside the load-error handler and using stderr for task failures.
- The strengthened HTTP removal test rejects a temporary mutation with `runner.interrupt()` disabled.
- Independent read-only review found no further implementation defects under the teaching contract. Added its suggested duplicate-watch preservation test. LIFO fairness, cooperative cancellation and sequential CLI calls remain explicit limitations.
- Final `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -o verify`: BUILD SUCCESS; 250 tests, 0 failures, 0 errors, 5 pre-existing intentional skips. `git diff --check` passed.
