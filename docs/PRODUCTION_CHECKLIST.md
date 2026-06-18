# CommandEngine — Production Checklist

Use this checklist before shipping CommandEngine in a real plugin or publishing a release.

## Required Gates

- Run `./gradlew spotlessCheck build --stacktrace`.
- Run CI on a pull request with JDK 25.
- Verify generated adapters contain no `else`, `break` or `continue` regressions.
- Test the plugin on a real Paper server matching the supported Paper API version.
- Validate plugin disable/reload unregisters commands and aliases.
- Validate permission failures do not execute handlers.
- Validate invalid sender, invalid syntax, internal error and rate limited messages.
- Validate tab-complete for static and dynamic suggestions.
- Validate async handlers do not call thread-confined Paper APIs directly.

## Configuration

- Provide a `config.yml` with the `commandengine` section documented in `QUICKSTART.md`.
- Keep `async-timeout-millis` positive and small enough to surface stuck handlers.
- Keep rate limit cache `maximum-size` bounded.
- Tune `rate-limit.window-millis` and `rate-limit.max-executions` for the plugin audience.

## Observability

- Enable `LoggingCommandTelemetry` or a custom `CommandTelemetry` implementation.
- Record command failures with reasons.
- Review logs for internal errors that expose repeated handler failures.

## Release Notes

- Document supported Java, Gradle and Paper versions.
- Document public API changes in `commandengine-api`.
- Keep `*.internal.*` packages out of downstream usage examples.
