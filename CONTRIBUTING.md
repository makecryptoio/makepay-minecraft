# Contributing

Before opening a pull request:

1. Run `node scripts/validate.mjs`.
2. Run `gradle build` with Java 21.
3. Keep MakePay credentials out of plugin source and config.
4. Update docs when backend contracts, command placeholders, or config keys change.
5. Include a security note for changes that affect checkout creation, polling, or command execution.
