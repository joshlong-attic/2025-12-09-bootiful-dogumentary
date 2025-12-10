# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Bootiful Dogumentary** is a Spring Boot microservices-based dog adoption platform deployed on Tanzu Application Service (TAS). This is the **Auth Service** - an OAuth2 Authorization Server that provides authentication for the entire microservices ecosystem.

The Auth service is one component in a 7-service architecture:
- **auth** (this service): OAuth2/OIDC Authorization Server
- **adoptions**: Core dog adoption business logic
- **gateway**: API Gateway with OAuth2 Client
- **assistant**: AI-powered chatbot with Spring AI
- **scheduler**: MCP server providing scheduling tools
- **ui**: Frontend static site
- **db/task**: Database initialization service

## Building and Running

### Build Commands

```bash
# Clean and build
./mvnw clean package

# Build native image (GraalVM)
./mvnw -Pnative native:compile

# Run tests
./mvnw test

# Run single test
./mvnw test -Dtest=AuthApplicationTests
```

### Running Locally

**Prerequisites:**
1. PostgreSQL with pgvector must be running (use db/compose.yml from parent directory)
2. Initialize database by running `../db/init_db.sh` from parent directory

**Start the service:**
```bash
# Run with Maven
./mvnw spring-boot:run

# Or run the JAR
java -jar target/auth-0.0.1-SNAPSHOT.jar
```

The service runs on **port 9090** by default.

### Database Setup

The Auth service uses the shared PostgreSQL database (`adoptions-pg` in TAS) with Spring Security's default schema:

```sql
-- users table
CREATE TABLE users (
    username text PRIMARY KEY,
    password text NOT NULL,
    enabled boolean NOT NULL
);

-- authorities table
CREATE TABLE authorities (
    username text REFERENCES users(username),
    authority text NOT NULL,
    UNIQUE (username, authority)
);
```

Database initialization is handled by:
- `../db/init_db.sh` - Main initialization script
- `../db/auth.sql` - User and authorities data
- `../db/compose.yml` - Docker Compose for local PostgreSQL with pgvector

To reset and reinitialize the database:
```bash
cd ../db
./init_db.sh
```

## Architecture and Configuration

### OAuth2 Authorization Server Setup

This service implements Spring Security OAuth2 Authorization Server with:

1. **OIDC Support** - Full OpenID Connect protocol implementation
2. **One-Time Token (OTT) Login** - Passwordless authentication via console tokens
3. **WebAuthn** - Biometric/FIDO2 authentication support

**Key Configuration in AuthApplication.java:**

- OIDC client redirect URI points to Gateway service
- OTT tokens printed to console for development
- WebAuthn configured for `localhost` and TAS domain
- JDBC-based user details manager for Spring Security schema

### OAuth2 Client Configuration

**Default client registration:**
- Client ID: `spring`
- Client Secret: `spring`
- Redirect URI: `https://gateway.apps.tas-ndc.kuhn-labs.com/login/oauth2/code/spring`

For local development, the redirect URI changes to:
- `http://127.0.0.1:8085/login/oauth2/code/spring`

### Security Configuration

The `httpSecurityCustomizer` bean in AuthApplication.java:19 configures:
1. OAuth2 Authorization Server with OIDC endpoints
2. One-Time Token login flow with console output
3. WebAuthn with:
   - Allowed origins: `https://auth.apps.tas-ndc.kuhn-labs.com`
   - Relying Party name: `bootiful`
   - Relying Party ID: `localhost`

### Integration with Other Services

**Gateway Service (OAuth2 Client):**
- Gateway acts as OAuth2 client, redirecting users to Auth for login
- After authentication, Gateway receives authorization code
- Gateway exchanges code for access token from Auth service
- Tokens are relayed to downstream services (Adoptions)

**Adoptions Service (Resource Server):**
- Validates JWT tokens issued by this Auth service
- Configured with issuer URI pointing to Auth service
- Extracts user principal from JWT claims

**Authentication Flow:**
```
User → Gateway → Auth (login) → Gateway (token) → Adoptions (validate JWT)
```

## Cloud Foundry / TAS Deployment

### Manifest Configuration

The service deploys to TAS with `manifest.yml`:

**Key properties:**
- Buildpack: java_buildpack_offline
- Java version: 25 (via `JBP_CONFIG_OPEN_JDK_JRE`)
- Memory: 1G
- Instances: 1
- Domain: `apps.tas-ndc.kuhn-labs.com`
- Route: `auth.apps.tas-ndc.kuhn-labs.com`

**Service bindings:**
- `adoptions-pg`: Shared PostgreSQL service (with pgvector extension)

**Environment variables:**
- `SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_CLIENT_OIDC_CLIENT_REGISTRATION_REDIRECT_URIS`: Points to Gateway's OAuth2 callback

### Deployment Commands

```bash
# Push to TAS
cf push

# View logs
cf logs auth --recent
cf logs auth

# Check service status
cf app auth

# Scale instances
cf scale auth -i 2

# Restart
cf restart auth
```

## Technology Stack

- **Spring Boot**: 4.0.0
- **Java**: 25
- **Spring Security**: OAuth2 Authorization Server with OIDC
- **Spring Security WebAuthn**: Passwordless authentication
- **Database**: PostgreSQL (JDBC)
- **Build Tool**: Maven
- **Native Image**: GraalVM buildtools support

## Dependencies

Key dependencies from pom.xml:
- `spring-boot-starter-security` - Core security
- `spring-boot-starter-security-oauth2-authorization-server` - OAuth2/OIDC
- `spring-security-webauthn` - FIDO2 support
- `spring-boot-starter-webmvc` - Web layer
- `spring-boot-starter-jdbc` - Database access
- `postgresql` - PostgreSQL driver

## Development Notes

### Port Configuration

- **Local**: Port 9090
- **TAS**: Standard HTTP/HTTPS via Cloud Foundry routing

### WebAuthn Configuration

When developing locally vs TAS:
- `rpId` should be `localhost` for local development
- `allowedOrigins` should include the actual domain for TAS
- Current configuration has `rpId: "localhost"` but allowed origin for TAS domain (AuthApplication.java:39-41)

### OIDC Endpoints

Standard OAuth2/OIDC endpoints available:
- `/.well-known/openid-configuration` - Discovery endpoint
- `/oauth2/authorize` - Authorization endpoint
- `/oauth2/token` - Token endpoint
- `/oauth2/jwks` - JSON Web Key Set
- `/userinfo` - User info endpoint
- `/login/ott` - One-Time Token login

### User Management

Users are managed via JDBC with `JdbcUserDetailsManager`:
- Password updates enabled (AuthApplication.java:24)
- Users stored in standard Spring Security schema
- Authorities define user roles/permissions

## Monorepo Context

This Auth service is part of a larger monorepo at:
`/Users/dashaun/fun/joshlong-attic/2025-12-09-bootiful-dogumentary/`

**Related services:**
- `../gateway/` - API Gateway (OAuth2 Client)
- `../adoptions/` - Resource Server using JWT validation
- `../assistant/` - AI Assistant
- `../scheduler/` - MCP Tool Server
- `../ui/` - Frontend
- `../db/` - Database scripts and initialization

When working across services, be aware of:
1. Gateway relies on this service's issuer URI
2. Adoptions validates JWTs against this service
3. Database is shared across Auth, Adoptions, Assistant, and Task services
4. All services deploy to the same TAS space with cross-service networking
