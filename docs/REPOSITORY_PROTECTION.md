# Repository Protection

This repository should keep the same baseline protection as the other MakePay integrations:

- public repository under `makecryptoio`;
- branch protection on `main`;
- required `validate` status check;
- one approving pull request review before merge;
- stale review dismissal enabled;
- admin enforcement enabled;
- linear history required;
- force pushes and branch deletion disabled;
- vulnerability alerts and automated security fixes enabled;
- GitHub Actions workflow token set to read-only;
- squash merge enabled;
- merge commits, rebase merges, and wiki disabled;
- branch deletion after merge enabled.

The validation workflow checks plugin metadata, Gradle build health, expected docs, and credential safety.
