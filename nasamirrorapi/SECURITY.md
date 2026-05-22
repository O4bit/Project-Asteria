# Security Policy

## Supported Versions

We provide security updates for the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |

## Reporting a Vulnerability

We take security seriously. If you discover a security vulnerability in the NASA Mirror API, please report it responsibly:

### How to Report

**DO NOT** open a public GitHub issue for security vulnerabilities.

Instead, please email: security@example.com (replace with your actual security contact)

Or use GitHub's private vulnerability reporting:
1. Go to the repository's Security tab
2. Click "Report a vulnerability"
3. Fill out the form with details

### What to Include

Please include the following information:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)
- Your name/handle for credit (optional)

### Response Timeline

- **Initial Response**: Within 48 hours
- **Triage**: Within 7 days
- **Fix Timeline**: Depends on severity
  - Critical: 1-7 days
  - High: 7-14 days
  - Medium: 14-30 days
  - Low: Best effort

## Security Measures

### Implemented Protections

1. **Rate Limiting**
   - Per-IP request limits
   - Configurable thresholds
   - KV-based tracking

2. **Input Validation**
   - Strict date format validation
   - Request size limits
   - Type-safe Rust with compile-time checks

3. **SSRF Prevention**
   - Whitelist-based URL validation
   - Only approved domains allowed
   - No user-controlled URLs

4. **XSS Prevention**
   - HTML sanitization
   - CSP headers
   - No user-generated content

5. **Security Headers**
   - Content-Security-Policy
   - Strict-Transport-Security
   - X-Content-Type-Options
   - X-Frame-Options
   - Referrer-Policy

6. **Privacy**
   - IP address anonymization in logs
   - No PII stored
   - Minimal data retention

7. **Dependency Security**
   - Regular `cargo audit` scans
   - Automated dependency updates
   - Minimal dependency footprint

### Known Limitations

1. **No Authentication**: This is a public API with no user authentication
2. **Source Trust**: We trust the NCKU mirror source (mitigated by sanitization)
3. **Cache Poisoning**: Low risk due to content hashing, but possible

## Security Best Practices for Users

### Self-Hosting
If you deploy your own instance:
- Use Cloudflare secrets for sensitive data
- Configure strict rate limits
- Monitor usage and logs
- Keep dependencies updated
- Enable WAF rules if available

### API Consumption
When consuming this API:
- Validate and sanitize responses
- Implement client-side rate limiting
- Handle errors gracefully
- Don't trust external content blindly
- Use HTTPS only

## Vulnerability Disclosure Policy

We follow coordinated disclosure:

1. **Report Received**: We acknowledge receipt within 48 hours
2. **Triage**: We assess severity and impact within 7 days
3. **Development**: We develop a fix
4. **Testing**: We test the fix thoroughly
5. **Release**: We release a patched version
6. **Disclosure**: After 90 days or once users have time to update, we:
   - Publish a security advisory
   - Credit the reporter (if desired)
   - Document the vulnerability and fix

## Security Hall of Fame

We recognize security researchers who responsibly disclose vulnerabilities:

<!-- Add names here as vulnerabilities are reported and fixed -->
- *Your name could be here!*

## Scope

### In Scope
- Security vulnerabilities in the Worker code
- Authentication/authorization bypass (if implemented)
- Data exposure vulnerabilities
- XSS, CSRF, injection vulnerabilities
- SSRF vulnerabilities
- DoS vulnerabilities

### Out of Scope
- Issues with third-party dependencies (report to upstream)
- Social engineering attacks
- Physical attacks
- Issues in the NCKU mirror source (not under our control)
- Rate limit bypass via distributed attacks
- Known limitations documented above

## Incident Response

In case of a security incident:

1. **Immediate**: Disable affected functionality if critical
2. **Investigate**: Analyze logs and impact
3. **Contain**: Deploy hotfix if possible
4. **Communicate**: Notify users via GitHub
5. **Remediate**: Deploy permanent fix
6. **Review**: Post-mortem and process improvements

## Security Contacts

- **Email**: security@example.com
- **PGP Key**: [Link to PGP key]
- **GitHub Security**: Use private vulnerability reporting

## Acknowledgments

We appreciate the security research community and thank:
- All responsible security researchers
- The Rust security team
- Cloudflare security team
- OWASP project contributors

---

**Last Updated**: October 2024
**Version**: 1.0
