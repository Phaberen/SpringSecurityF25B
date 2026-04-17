# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Run the application
./mvnw spring-boot:run

# Build (skip tests)
./mvnw clean package -DskipTests

# Run all tests
./mvnw test

# Run a specific test class
./mvnw -Dtest=SpringSecurityF25BApplicationTests test
```

The app requires a local MySQL instance (see Database section). There is no linting or code coverage tooling configured.

## Architecture

This is a Spring Boot 4.0.4 / Java 25 REST API demonstrating Spring Security with JWT authentication and role-based access control.

### Request Flow

1. HTTP request arrives at the servlet filter chain.
2. `JWTTokenValidatorFilter` (currently a stub — not yet implemented) would validate a bearer token on protected routes.
3. `ProjectSecurityConfig` defines authorization rules per endpoint.
4. On form-login/Basic-auth requests, `EazyBankUsernamePwdAuthenticationProvider` is invoked.
5. The provider delegates to `BankLoadUserService` (implements `UserDetailsService`) to load the `Customer` entity by email from MySQL.
6. After successful authentication, `JWTTokenGeneratorFilter` writes a signed JWT to the `Authorization` response header.

### Security Configuration (`config/`)

`ProjectSecurityConfig` is the single `SecurityFilterChain` bean. Key rules:
- `/myAccount` → `ADMIN` only
- `/myBalance` → `ADMIN` or `SALES`
- `/contact`, `/register` → public (`permitAll`)
- All other routes → authenticated

CSRF is disabled for `/contact` and `/register`. Form login and HTTP Basic are both enabled.

`EazyBankUsernamePwdAuthenticationProvider` is the custom `AuthenticationProvider`; it validates passwords via the delegating `PasswordEncoder` (bcrypt-aware) and throws `BadCredentialsException` on mismatch.

### JWT (`filter/`, `constants/`)

- `JWTTokenGeneratorFilter` runs `OncePerRequestFilter`, skips `/register`. On any other successful authentication it builds a JJWT (0.12.6) token signed with the HMAC-SHA secret in `SecurityConstants.JWT_SECRET`, sets 1-hour expiry, and writes it to the `Authorization` header.
- `JWTTokenValidatorFilter` exists but is **not implemented** — it is the primary outstanding work item.
- Constants (secret, header name, expiration) live in `SecurityConstants`.

### User / Role Model

`Customer` entity: `id`, `email`, `pwd`, `role` (stored as a comma-separated string, e.g. `"ROLE_ADMIN,ROLE_USER"`).  
`BankLoadUserService.loadUserByUsername()` splits that string into `SimpleGrantedAuthority` objects. The Spring Security convention of `ROLE_` prefix is followed.

## Database

MySQL is required locally:

```
URL:      jdbc:mysql://127.0.0.1:3306/springsecf25b
Username: root
Password: pass123
```

`spring.jpa.hibernate.ddl-auto=update` — Hibernate auto-updates the schema on startup.  
`spring.jpa.show-sql=true` — SQL is printed to stdout.

An H2 dependency is present in `pom.xml` and can be used for tests by overriding `spring.datasource` in `src/test/resources/application.properties`.
