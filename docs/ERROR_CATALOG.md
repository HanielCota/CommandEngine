# Error Catalog

CommandEngine failure modes, the `FailureReason` used internally, the user-facing message method and how to customize it.

| Failure | `FailureReason` | Message method | Default text | How to customize |
| --- | --- | --- | --- | --- |
| Sender type does not match `@Sender` parameter | `INVALID_SENDER` | `CommandMessages.invalidSender()` | `"This command cannot be executed by this sender."` | Pass a custom `CommandMessages` to `CommandEngine` or `PaperPlatform`. |
| Missing command permission | `NO_PERMISSION` | `CommandMessages.noPermission()` | `"You do not have permission to execute this command."` | Custom `CommandMessages`. |
| Invalid command syntax (Brigadier parse failure) | `INVALID_SYNTAX` | `CommandMessages.invalidSyntax()` | `"Invalid command syntax."` | Custom `CommandMessages`. |
| Handler threw an exception | `EXCEPTION` | `CommandMessages.internalError()` | `"An internal error occurred while executing this command."` | Custom `CommandMessages`; the exception is logged. |
| Rate limit exceeded | `RATE_LIMITED` | `CommandMessages.rateLimited()` | `"You are executing this command too quickly."` | Custom `CommandMessages` or adjust rate-limit config. |

## Configuring messages (Paper)

In `config.yml`:

```yaml
commandengine:
  messages:
    internal-error: "An internal error occurred while executing this command."
    invalid-sender: "This command cannot be executed by this sender."
    invalid-syntax: "Invalid command syntax."
    no-permission: "You do not have permission to execute this command."
    rate-limited: "You are executing this command too quickly."
```

## Rate-limit bypass

The default `CaffeineCommandRateLimiter` bypass permission is `commandengine.bypass.ratelimit`.

## Async handler failures

When an async handler throws, the framework logs the exception and sends `internalError`. The result reported to Brigadier is success (`1`) because async dispatch cannot revoke the already-returned code; failures are delivered via messaging.
