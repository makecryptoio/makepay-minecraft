# Security Policy

## Supported Versions

Security fixes are prepared for the latest tagged release and the `main` branch.

## Reporting A Vulnerability

Report suspected vulnerabilities to `info@makepay.io` with enough detail to reproduce the issue. Please avoid public disclosure until the MakePay team has confirmed impact and prepared a fix.

## Server Integration Rules

- Never put MakePay API credentials on Minecraft servers.
- Use a merchant backend relay to create checkout links and verify webhooks.
- Keep command templates reviewed because they execute as console.
- Store and acknowledge entitlement IDs idempotently.
- Treat player names as display values; use UUIDs for durable entitlement records.
