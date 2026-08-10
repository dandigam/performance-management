# Security Implementation Reference

Status: JWT authentication foundation implemented on 2026-08-10. The remaining
production-hardening work below should be completed before a production release.

## Implemented

- Spring Security protects application endpoints by default.
- `POST /api/auth/login` authenticates through `AuthenticationManager`.
- Access tokens are signed JWTs with issuer, audience, expiry, token type, and JTI.
- Access tokens expire after 15 minutes by default.
- Refresh tokens are opaque random values stored only as SHA-256 hashes.
- Refresh tokens rotate on every refresh and are revoked by logout.
- Refresh tokens are returned only in an `HttpOnly`, `SameSite=Strict` cookie.
- New passwords use BCrypt strength 12. Legacy plaintext passwords are accepted
  once and automatically upgraded after successful authentication.
- Inactive users cannot authenticate or refresh credentials.
- Vendor and bank-account APIs require `ADMIN` or `FINANCE`.
- All controller-level wildcard CORS declarations were removed; allowed browser
  origins now come from `ALLOWED_ORIGINS`.
- Anonymous and forbidden responses consistently use HTTP 401 and 403.
- Focused tests cover JWT tampering, password migration, anonymous access, and
  financial-role authorization.

## Authentication API

- `POST /api/auth/login`: returns an access token and sets the refresh cookie.
- `POST /api/auth/refresh`: rotates the refresh cookie and returns a new access token.
- `POST /api/auth/logout`: revokes the current refresh token and clears its cookie.
- `GET /api/auth/me`: returns the currently authenticated user.

Clients send the access token as `Authorization: Bearer <token>` and must include
browser credentials on login, refresh, and logout so the refresh cookie is accepted.

## Required production configuration

- Override the local default with a unique, random `JWT_SECRET` of at least 32 bytes.
- Override the local default with `SECURE_COOKIE=true` and terminate traffic only over HTTPS.
- Set `ALLOWED_ORIGINS` to the exact deployed UI origin(s).
- Run `database/migrations/create_refresh_tokens.sql` through the deployment migration process.
- Move database, mail, JWT, and encryption secrets to a secrets manager.
- Local Swagger access is public by default. Set `SWAGGER_PUBLIC=false` in production
  to require `ADMIN`, or disable Swagger entirely when it is not needed.

## Remaining high-priority hardening

1. Encrypt full account and routing numbers at rest with AES-GCM and a managed key.
2. Add an immutable audit record for every sensitive-bank-detail reveal.
3. Add object-level review authorization so employees and managers see only assigned records.
4. Add login throttling, temporary lockout, and refresh-token reuse detection by token family.
5. Add document ownership checks, content-signature verification, malware scanning, and private storage.
6. Add production CSP/HSTS settings and a deployment-level rate limiter.
7. Add refresh-token cleanup/retention and a user-level "log out all sessions" operation.

This implementation materially improves API security, but completion of the remaining
items is still required before describing the entire application as production-ready.
