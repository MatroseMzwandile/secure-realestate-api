# secure-realestate-api : Secure Coding & Pentest Project
Simple Real estate listings API (Java/Javalin/JDBC) demonstrating SQL Injection &amp; Broken Access Control — vulnerable-by-design, then fixed, as a secure coding exercise.

A small REST API where realtors can register/log in and post property
listings, and anyone can view listings without logging in.

## Known vulnerabilities (introduced on purpose - Phase 2)

1. SQL Injection (login) - RealtorDao.login() builds its query with
   string concatenation instead of a PreparedStatement.
2. SQL Injection (other queries) - findById, create, update, delete in
   ListingDao also concatenate raw input into SQL strings.
3. Broken Access Control - PUT/DELETE /listings/{id} only check that
   someone is logged in, not that they own the listing.
4. Plaintext password storage - passwords are stored and compared as
   plain text, with no hashing.

## Exploitation demo
(To be added in Phase 3.)

## Fixes applied
(To be added in Phase 4.)
Stay tuned for more.