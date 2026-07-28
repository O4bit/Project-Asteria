# Security Policy

## Supported Versions

Only the latest release of Project Asteria receives security fixes.

| Version | Supported |
|---------|-----------|
| Latest  | ✅        |
| Older   | ❌        |

## Reporting a Vulnerability

**Do not open a public GitHub issue for security vulnerabilities.**

Please use [GitHub's private vulnerability reporting feature](https://github.com/O4bit/Project-Asteria/security/advisories/new) to report security issues confidentially. This feature is available under the **Security** tab in the repository.

Your report should include:
- A description of the vulnerability
- Steps to reproduce the issue
- The potential impact
- Any suggested fixes (optional)

You can expect an acknowledgement within 5 business days and a status update within 14 days.

## Scope

The following are in scope for security reports:

- **Android app** (`space.o4bit.projectasteria`) — permission misuse, exported component abuse, insecure data storage, intent injection, PendingIntent vulnerabilities
- **Backend proxy** (https://asteria.o4bit.dev) — injection vulnerabilities, authentication issues, information disclosure
- **Dependencies** — known CVEs in transitive dependencies used at runtime

The following are **out of scope**:

- Reports on third-party APIs (NASA APOD, The Space Devs, Where The ISS At?) — report those to their respective owners
- Missing rate-limiting on the public proxy that cannot be exploited for user harm
- Theoretical vulnerabilities without a practical attack path

## Privacy

Project Asteria does not collect personal data, telemetry, or crash reports externally. The app uses a local, in-memory diagnostic log that is only shared when the user explicitly exports it. No user identifiers are collected or stored.

## Disclosure Timeline

1. You report the vulnerability privately
2. We acknowledge within 5 business days
3. We investigate and develop a fix
4. We release a patched version
5. We credit you in the release notes (unless you prefer to remain anonymous)

We aim to release security fixes within 30 days of a confirmed vulnerability report.
