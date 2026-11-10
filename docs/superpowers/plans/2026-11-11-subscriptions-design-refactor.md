# Subscription design refactor

> **For agentic workers:** Use superpowers:executing-plans for inline implementation.

**Goal:** Address the four architecture review comments without changing the teaching example's behavior.

**Architecture:** Keep the Treiber pool and per-subscription locking. Introduce an interruptible client contract and typed success/failure results. Keep formatting at the CLI boundary. Make request phases explicit and leave the maximum dispatch wait in the dispatcher alone.

**Tech Stack:** Java 25, Maven, JUnit 5, picocli.

**Spec:** The user's instruction in this chat to refactor all four comments using SOLID, KISS and DRY, with the existing student-project constraint.

## Constraints

- Commands remain sequential; no concurrent watch/remove/close API is introduced.
- Running requests may finish after lifetime; queued expired requests must not start.
- Returning from remove still guarantees no later result callbacks for that subscription.
- Keep existing console text and interrupt restoration.
- Do not add executors, frameworks or a hierarchy of state objects.

## Review focus

- Alternative clients satisfying the cancellation contract must be accepted.
- Typed errors must retain their cause; output failures must not re-enter the result callback.
- New state transitions must preserve no overlap and cancellation of the correct worker.
- Queued expiry must wake the dispatcher, while running work needs no timer polling from Subscription.
- Callback execution must remain under the publication lock.

## Tasks

- [x] Add failing scheduling tests; introduce explicit request phases, rename the expiry predicate, and centralize the dispatch wait cap.
- [x] Introduce the interruptible client interface; prove another implementation is accepted and unsupported clients remain rejected.
- [x] Introduce typed subscription results and a CLI formatter; inject task failure handling and migrate the behavioral tests.
- [x] Update teaching documentation and run full Maven verification.
- [x] Obtain an independent read-only review and resolve substantive findings.

## Verification record

- Starting revision: 8a01513 on a clean seminar10 checkout; its prior full verification was successful (250 tests, 5 intentional skips).
- Three scheduling assertions, acceptance of an alternative interruptible client, and two typed-result assertions failed against the old behavior and passed after the refactor.
- Added checks for console formatting, waiting for an active publication during remove, and worker survival after the task-error handler itself throws.
- `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -o verify`: BUILD SUCCESS, 260 tests, 0 failures, 0 errors, 5 intentional skips. The initial sandbox-only CLI run could not attach Mockito; the permitted run passed.
- Independent read-only review found no substantive issues. Sequential CLI commands, cooperative cancellation and LIFO scheduling remain deliberate teaching constraints.
- Ruling: Failure retains the original exception reference for diagnostics. Its narrow SpotBugs EI_EXPOSE_REP/EI_EXPOSE_REP2 exclusions document this contract; callers must not mutate the exception. The existing empty synchronization block in cancel remains the publication barrier.
