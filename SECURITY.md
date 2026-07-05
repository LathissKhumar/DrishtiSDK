# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in Drishti SDK, please report it responsibly.

**Do NOT open a public GitHub issue for security vulnerabilities.**

Instead, please email security concerns to: [See profile for contact info](https://github.com/LathissKhumar)

Include the following in your report:
- Description of the vulnerability
- Steps to reproduce
- Potential impact assessment
- Suggested fix (if available)

## Response Timeline

- **Acknowledgment**: Within 48 hours of your report
- **Initial assessment**: Within 1 week
- **Fix or mitigation**: Within 30 days for critical vulnerabilities

## Scope

This security policy applies to:
- Drishti SDK source code (this repository)
- Published artifacts via JitPack and Maven Central
- Official documentation and examples

## Out of Scope

- Third-party dependencies (report to upstream maintainers)
- Applications built with Drishti SDK (report to the application developer)
- Issues requiring physical device access beyond normal testing

## Supported Versions

| Version | Supported |
|---------|-----------|
| 1.0.x   | ✅ Yes    |

## Security Best Practices for Integrators

When integrating Drishti SDK into your application:

1. **Keep dependencies updated** — Use the latest stable release
2. **Review permissions** — Drishti SDK requires camera and vibration permissions
3. **Network access** — The core SDK is offline-only; PubChem client makes optional network calls
4. **Input validation** — All public APIs validate inputs, but validate upstream data sources too
