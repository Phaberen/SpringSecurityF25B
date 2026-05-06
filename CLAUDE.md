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

The app requires a local MySQL instance (see Database section). There is no linting or code coverage tooling configured. The `contextLoads` test will also require MySQL unless H2 is configured for the test profile (see Testing section).

## Architecture

This is a Spring Boot 4.0.4 / Java 25 REST API for learning Spring Security with JWT authentication and role-based access control. It is a tutorial project — controllers return placeholder strings and not all features are wired together yet.

### Package Structure

```
src/main/java/org/example/springsecurityf25b/
├── SpringSecurityF25BApplication.java   # Entry point
├── config/
│   ├── ProjectSecurityConfig.java       # SecurityFilterChain bean, PasswordEncoder bean
│   └── EazyBankUsernamePwdAuthenticationProvider.java  # Custom AuthenticationProvider
├── constants/
│   └── SecurityConstants.java           # JWT_SECRET, JWT_HEADER, JWT_EXPIRATION (interface)
├── controller/
│   ├── AccountController.java           # GET /myAccount
│   ├── BalanceController.java           # GET /myBalance
│   ├── ContactController.java           # GET /contact
│   ├── HelloWorld.java                  # GET /test
│   └── UserController.java              # POST /register
├── filter/
│   ├── JWTTokenGeneratorFilter.java     # Coded but NOT registered in filter chain
│   └── JWTTokenValidatorFilter.java     # Empty class — not yet started
├── model/
│   └── Customer.java                    # JPA entity (id, email, pwd, role)
├── repository/
│   └── CustomerRepository.java          # JpaRepository<Customer, Integer>
└── service/
    └── BankLoadUserService.java          # UserDetailsService implementation
```

### Request Flow

**Current actual behaviour (not the intended end state):**

1. HTTP request arrives at the servlet filter chain.
2. `ProjectSecurityConfig` applies the three authorization rules (see below). Routes not listed have **no explicit rule** — they are not automatically protected.
3. For form-login or HTTP Basic requests, `EazyBankUsernamePwdAuthenticationProvider` is invoked.
4. The provider calls `BankLoadUserService.loadUserByUsername()` to load the `Customer` by email from MySQL.
5. `BankLoadUserService` splits the `role` field by comma (with `.trim()`) into `SimpleGrantedAuthority` objects.
6. The provider validates the password with the delegating `PasswordEncoder`; throws `BadCredentialsException` on mismatch.
7. **`JWTTokenGeneratorFilter` is coded and complete but is NOT registered in the `SecurityFilterChain`**, so it never runs. Once registered it would generate a JWT on successful `/login` and write it to the `Authorization` response header.

### Security Configuration (`config/`)

`ProjectSecurityConfig` is the single `SecurityFilterChain` bean. **Only three authorization rules are configured — there is no `anyRequest()` catch-all:**

| Path | Rule |
|------|------|
| `/myAccount` | `hasRole("ADMIN")` |
| `/myBalance` | `hasAnyRole("ADMIN", "SALES")` |
| `/contact`, `/register` | `permitAll()` |
| All other paths | **No explicit rule** — denied by default (Spring Security 6+ deny-all semantics when no `anyRequest()` is present) |

CSRF is disabled only for `/contact` and `/register`. Form login and HTTP Basic are both enabled.

`EazyBankUsernamePwdAuthenticationProvider` is the custom `AuthenticationProvider`. It uses `@Autowired` field injection for `BankLoadUserService` and `PasswordEncoder`.

`passwordEncoderNone()` exists in the config class but is **not** annotated with `@Bean` and is inactive. The active encoder is `passwordEncoder()` which uses `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (bcrypt-aware).

### API Endpoints

| Endpoint | Method | Access | Response |
|----------|--------|--------|----------|
| `/register` | POST | Public | 201 Created / 500 on error |
| `/contact` | GET | Public | Placeholder string |
| `/myAccount` | GET | `ROLE_ADMIN` | Placeholder string |
| `/myBalance` | GET | `ROLE_ADMIN` or `ROLE_SALES` | Placeholder string |
| `/test` | GET | No explicit rule (denied by default) | `"Hello World!"` (unreachable until a security rule is added) |

### JWT (`filter/`, `constants/`)

- **`JWTTokenGeneratorFilter`** — extends `OncePerRequestFilter`. `shouldNotFilter()` returns `true` for all paths except `/login`, so it would run only on `/login`. On authentication it builds a JJWT (0.12.6) token: issuer `"Eazy Bank"`, subject `"JWT Token"`, claims `username` and `authorities` (comma-joined), 1-hour expiry, HMAC-SHA signed with `JWT_SECRET`. Writes the token to the `Authorization` response header. **NOT registered in `ProjectSecurityConfig`** — currently dead code.

- **`JWTTokenValidatorFilter`** — an empty Java class (`public class JWTTokenValidatorFilter {}`). It does not extend `OncePerRequestFilter` and contains no logic. This is the **primary outstanding work item** (see below).

- **`SecurityConstants`** — a Java interface (not a class) holding three constants: `JWT_SECRET` (hardcoded string), `JWT_HEADER` (`"Authorization"`), and `JWT_EXPIRATION` (1 hour in milliseconds).

### User / Role Model

`Customer` entity fields: `id` (int, auto-increment), `email`, `pwd`, `role`.

The `role` field is a comma-separated string, e.g. `"ROLE_ADMIN,ROLE_USER"`. `BankLoadUserService.loadUserByUsername()` splits it by `","` (calling `.trim()` on each token) and wraps each in a `SimpleGrantedAuthority`. The `ROLE_` prefix is required — Spring Security's `hasRole("ADMIN")` checks for the authority `ROLE_ADMIN`.

`UserController.POST /register` reads the `Customer` JSON body, encodes the `pwd` field with the delegating `PasswordEncoder`, and saves the record. A plain-text password in the request will be stored as `{bcrypt}...`.

## Database

MySQL is required locally:

```
URL:      jdbc:mysql://127.0.0.1:3306/springsecf25b?useSSL=false&serverTimezone=UTC
Username: root
Password: pass123
```

`spring.jpa.hibernate.ddl-auto=update` — Hibernate auto-updates the schema on startup.
`spring.jpa.show-sql=true` — SQL is printed to stdout.

## Testing

Only one test file exists: `SpringSecurityF25BApplicationTests` with a single `contextLoads()` test. This test spins up the full Spring context and therefore requires a MySQL connection by default.

To use H2 instead for tests, create `src/test/resources/application.properties` (this file does not currently exist):

```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
```

The `h2` and test starter dependencies are already present in `pom.xml`.

## Outstanding Work / Known Issues

These are the gaps between the current code and a working JWT-secured API:

1. **`JWTTokenValidatorFilter` is not implemented.** It must extend `OncePerRequestFilter`, read the `Authorization` request header, parse and verify the JWT using `SecurityConstants.JWT_SECRET`, extract the username and authorities claims, and populate `SecurityContextHolder` so that downstream authorization rules apply to token-authenticated requests.

2. **Neither JWT filter is registered in `ProjectSecurityConfig`.** After implementing the validator, both filters need to be added to the chain, e.g.:
   ```java
   http.addFilterAfter(new JWTTokenGeneratorFilter(), BasicAuthenticationFilter.class)
       .addFilterBefore(new JWTTokenValidatorFilter(), BasicAuthenticationFilter.class);
   ```

3. **No `anyRequest()` catch-all.** Paths not listed are denied by default in Spring Security 6+. If `/test` is intended to be accessible, add `.requestMatchers("/test").authenticated()` (or `permitAll()`). Consider adding a final `anyRequest().authenticated()` for all other paths.

4. **`JWT_SECRET` is hardcoded** in `SecurityConstants.java`. For anything beyond a tutorial, externalize it via an environment variable or `application.properties`.

5. **Controllers return placeholder strings.** Real implementations would query the database and return meaningful data.

## Code Conventions

- `@Autowired` field injection is used throughout (no constructor injection).
- No Lombok — `Customer` uses manually written getters and setters.
- Custom `AuthenticationProvider` pattern: `EazyBankUsernamePwdAuthenticationProvider` handles authentication; Spring does not auto-wire `BankLoadUserService` directly into the filter chain.
- Roles are stored and created with the `ROLE_` prefix (e.g. `ROLE_ADMIN`). Always use `hasRole("ADMIN")` (not `hasAuthority("ROLE_ADMIN")`) in `authorizeHttpRequests` to stay consistent.
- The `SecurityConstants` interface is used as a namespace for constants (no instances created).

## AI Assistant Notes

Key traps to avoid when working in this codebase:

- **Do not assume JWT is functional.** Neither filter runs at runtime. Form login and HTTP Basic are the only active authentication mechanisms currently.
- **`/test` returns 403**, not `"Hello World!"`, until a security rule explicitly permits it — Spring Security 6+ denies unmatched routes.
- **`BankLoadUserService` passes roles verbatim** from the database string with no transformation. The `ROLE_` prefix must be present in the stored value (e.g. `ROLE_ADMIN`, not `ADMIN`) for `hasRole("ADMIN")` to match.
- **The `@Nullable` annotation on `authenticate()`** is from `org.jspecify`, not Spring or JDK. It is a static analysis annotation only and does not change runtime behavior.
- **`passwordEncoderNone()` in `ProjectSecurityConfig` is NOT active** — it has no `@Bean` annotation. The active encoder is `passwordEncoder()`.
- **`spring-boot-h2console` is a compile-scope dependency** (active at runtime). If you enable H2 for tests you must also permit `/h2-console/**` in `ProjectSecurityConfig`, otherwise Spring Security will block it.
