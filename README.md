# secure-realestate-api : Secure Coding & Pentest Project

Simple Real estate listings API (Java/Javalin/JDBC) demonstrating SQL Injection & Broken Access Control — vulnerable-by-design, then fixed, as a secure coding exercise.

A small REST API where realtors can register/log in and post property listings, and anyone can view listings without logging in.

## Tech stack

- Java 17
- Javalin — lightweight web framework
- MySQL + raw JDBC (no ORM, on purpose — this is a learning project)
- BCrypt (jbcrypt) for password hashing
- Maven

## Setup

1. Install MySQL locally, then load the schema:
   ```
   mysql -u root -p < src/main/resources/schema.sql
   ```
2. Set your DB credentials as environment variables:
   ```
   export DB_URL="jdbc:mysql://localhost:3306/realestate"
   export DB_USER="root"
   export DB_PASSWORD="yourpassword"
   ```
3. Run the app:
   ```
   mvn compile exec:java
   ```
   Or build and run the packaged jar:
   ```
   mvn clean package
   java -jar target/realestate-listings-1.0-SNAPSHOT.jar
   ```
4. The API runs on `http://localhost:7000`

## Endpoints

| Method | Path            | Auth required? | Description                    |
|--------|-----------------|-----------------|---------------------------------|
| GET    | /listings       | No              | View all listings               |
| GET    | /listings/{id}  | No              | View one listing                |
| POST   | /login          | No              | Log in, returns a token         |
| POST   | /listings       | Yes             | Create a listing                |
| PUT    | /listings/{id}  | Yes (owner only)| Update a listing                |
| DELETE | /listings/{id}  | Yes (owner only)| Delete a listing                |

Sample login accounts (see `schema.sql`): `alice` / `password123`, `bob` / `hunter2`.

## Known vulnerabilities (introduced on purpose - Phase 2)

1. SQL Injection (login) - RealtorDao.login() builds its query with
   string concatenation instead of a PreparedStatement.
2. SQL Injection (other queries) - findById, create, update, delete in
   ListingDao also concatenate raw input into SQL strings.
3. Broken Access Control - PUT/DELETE /listings/{id} only check that
   someone is logged in, not that they own the listing.
4. Plaintext password storage - passwords are stored and compared as
   plain text, with no hashing.

These were intentional, to demonstrate real exploitation before fixing them. See below.

## Exploitation demo (Phase 3)

Both vulnerabilities above were exploited and documented in full, with terminal output and screenshots, in the `evidence/` folder:

- `evidence/sql-injection.md` — login bypass via two different injection payloads (username field and password field), including a walkthrough of why each payload works and the exact SQL the database ends up executing.
- `evidence/broken-access-control.md` — one realtor (alice) editing and another realtor (bob) deleting listings they do not own, confirmed by checking the listings before and after each exploit.

Summary of what was demonstrated:

- Logging in as any realtor with no valid password, using a crafted `username` field: `' OR '1'='1' -- `
- Logging in as any realtor with no valid password, using a crafted `password` field: `' OR '1'='1`
- Editing another realtor's listing (title, description, price) despite not owning it
- Deleting another realtor's listing entirely, despite not owning it

Full commands, responses, and explanations are in the linked evidence files above.

## Fixes applied (Phase 4)

All three vulnerabilities documented above were fixed. Here's what changed, and why.

### 1. SQL Injection -> fixed with PreparedStatement

**Before:** `RealtorDao` and `ListingDao` built queries by concatenating raw user input directly into the SQL string, e.g.:

```java
String query = "SELECT * FROM realtors WHERE username = '" + username + "'...";
```

**After:** every query now uses `PreparedStatement` with `?` placeholders, and user input is bound as a parameter instead of being pasted into the SQL text:

```java
String query = "SELECT * FROM realtors WHERE username = ?";
PreparedStatement stmt = conn.prepareStatement(query);
stmt.setString(1, username);
```

With this change, input like `' OR '1'='1' -- ` is treated as a literal string to search for — not as SQL syntax — so it can no longer alter the query's logic. Re-running the exact exploit from `evidence/sql-injection.md` now correctly returns `401 Unauthorized` instead of bypassing login.

### 2. Broken Access Control -> fixed with ownership checks

**Before:** `update()` and `delete()` in `ListingDao` only checked that a listing ID existed — never that the logged-in realtor actually owned it. Any authenticated realtor could edit or delete any listing.

**After:** `Main.java` now compares the listing's `realtorId` against the logged-in user's ID before allowing the edit or delete to proceed:

```java
if (listing.getRealtorId() != realtorId) {
    ctx.status(403).json(Map.of("error", "You do not own this listing"));
    return;
}
```

Re-running the exploit from `evidence/broken-access-control.md` (alice editing bob's listing) now correctly returns `403 Forbidden` instead of succeeding.

### 3. Plaintext passwords -> fixed with BCrypt hashing

**Before:** passwords were stored and compared as plain text in the `realtors` table, readable by anyone with database access.

**After:** passwords are hashed with BCrypt (jbcrypt) before being stored, and login now verifies the supplied password against the stored hash rather than comparing strings directly:

```java
if (BCrypt.checkpw(password, storedHash)) {
    // login successful
}
```

Even if the database were exposed, an attacker would only see irreversible hashes, not usable passwords.

A small gotcha worth noting: the jbcrypt library used here only recognises bcrypt hashes tagged with the `$2a$` prefix. Some hash generators (including the one used to seed this database) default to the newer `$2b$` tag. This caused an `Invalid salt revision` error on login until the seed data's hash prefixes were corrected to `$2a$`. Both prefixes produce compatible hashes for passwords under 72 bytes — the mismatch was purely a library compatibility issue, not a hashing algorithm problem.

### 4. Bonus fix: database connection handling

While making these changes, a related bug in `Database.java` was also fixed: the original version cached a single shared `Connection` and returned it on every call, but each DAO method closed that same connection via `try-with-resources` after its query — meaning every request after the first would silently fail. `Database.java` now opens a fresh connection per call, which resolved this.

Verification: every fix above was manually re-tested by re-running the exact exploit commands documented in `evidence/sql-injection.md` and `evidence/broken-access-control.md`, confirming each attack now fails while legitimate use (correct login, editing your own listings) continues to work as expected.

## Demo video

(Link to the YouTube walkthrough will go here once recorded.)

## Why these choices

This project deliberately avoids Spring and an ORM so that the security issues are visible and explainable at the JDBC/SQL level, rather than hidden behind framework defaults — the goal is to understand why each vulnerability exists and why each fix works, not just to apply a framework's built-in protections without knowing what they do.

# Verification Code:
WTC-BVNF7W75