# Passly sync protocol `sync/1`

Status: **normative draft** (M0). Server side lands in M2, delta client in M3.
This document is platform independent. Android is the first implementation;
the browser extension, iOS and desktop clients implement the same wire format
and the same apply rules.

Compatibility with vanilla Passbolt is explicitly **not** a goal. Passly owns
both the client and the server (`Svaroh/passly_api`), so the protocol is
designed around a server side change journal instead of timestamp guessing.

---

## 1. Design invariants

These follow from requirement R0 ("a local replica stays fully usable without
the server, for an unlimited time"). They are binding on every implementation.

1. The local database is a **self-sufficient store**, not a projection of the
   server. Sync is an optional enhancement; its absence degrades freshness,
   never availability.
2. There is **no TTL** on locally stored data, and **no server-driven wipe**.
   Erasing local data is always an explicit user action on the device.
3. An expired session, JWT or MFA challenge never blocks local reads or local
   writes. It only pauses synchronisation.
4. No protocol error is fatal to the local replica. `410 Gone` means
   "re-bootstrap on top of what you already have", never "discard".
5. The cursor is a **monotonic integer**, never a timestamp. Clock skew,
   equal timestamps and time zones must not be able to lose an event.

---

## 2. Vocabulary

| Term | Meaning |
|------|---------|
| `seq` | Monotonic 64-bit unsigned change-journal sequence number assigned by the server |
| cursor | The highest `seq` a client has fully applied and committed |
| epoch | Opaque server identity token; a change of `epoch` forces a bootstrap |
| watermark | `MAX(seq)` among journal rows older than `WATERMARK_LAG`; events above it are withheld |
| upsert | Create-or-replace of one entity, delivered as a full DTO |
| deletion | Entity is gone **or no longer visible** to this user; identical for the client |

`WATERMARK_LAG` is 2 seconds in `sync/1`.

---

## 3. Server change journal

```sql
CREATE TABLE sync_log (
  seq         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  entity_type ENUM('resource','folder','secret','permission','user','group',
                   'group_user','tag','resource_type','metadata_key') NOT NULL,
  entity_id   CHAR(36) NOT NULL,
  op          ENUM('upsert','delete') NOT NULL,
  created     DATETIME(6) NOT NULL,
  INDEX (entity_type, entity_id),
  INDEX (created)
);
```

Rules:

- One row per event, no payload. The payload is resolved at read time so that
  ACL is always evaluated against **current** visibility.
- `permission` and `group_user` events are translated into events on the
  affected resource or folder, because that is what changes visibility.
- Retention: rows older than 90 days are pruned. A client whose cursor is below
  the oldest retained `seq` receives `410 Gone` and re-bootstraps.
- Because a transaction with a lower `seq` can commit after a higher one, the
  server never serves events above the watermark. Reconciliation (§7) is the
  backstop for anything the watermark still misses.

---

## 4. `GET /sync/meta.json`

Served without a cursor. Used once during account setup and whenever the client
wants to check protocol compatibility.

```json
{ "body": {
    "protocol": "sync/1",
    "epoch": "0f2b...",
    "min_client": "3.1.0",
    "max_page_size": 500,
    "retention_days": 90
}}
```

A client that does not understand `protocol` reports a configuration error. It
does **not** degrade into a read-only or wipe-on-mismatch mode.

## 5. `GET /sync/bootstrap.json?limit=500&page=N`

Full snapshot of everything visible to the caller, including secrets, plus the
`cursor` from which delta continues.

```json
{ "header": { "pagination": { "count": 1240, "page": 1, "limit": 500 } },
  "body": {
    "cursor": 148100,
    "epoch": "0f2b...",
    "upserts": {
      "resources": [], "secrets": [], "folders": [], "users": [],
      "groups": [], "resource_types": [], "metadata_keys": []
    }
}}
```

- Secrets travel in the same stream, so autonomy (R0) is reached **before**
  first login finishes, not at some later point.
- `cursor` is pinned at page 1 and repeated unchanged on every page.
- Interrupted bootstrap resumes at the same page; pages are applied
  incrementally, so a partial bootstrap is still useful data.
- Progress is surfaced to the user.

## 6. `GET /sync/changes.json?since=<seq>&limit=500`

```json
{ "body": {
    "cursor": 148213,
    "has_more": true,
    "upserts": {
      "resources": [ { "...": "full DTO incl. permissions, tags, favorite" } ],
      "secrets":   [ { "resource_id": "…", "data": "-----BEGIN PGP MESSAGE-----…" } ],
      "folders": [], "users": [], "groups": [], "resource_types": [], "metadata_keys": []
    },
    "deletions": {
      "resources": [ "uuid" ], "folders": [], "users": [], "groups": []
    }
}}
```

Key property: **`deletions` includes access revocation.** For every journal
event the server re-evaluates whether the entity is still visible to the caller;
if not, it is reported as a deletion. This is what makes the delta a complete
mechanism rather than an optimisation — revoking a permission does not touch
`resources.modified`, so a timestamp-based delta could never see it.

### Apply rules

1. A page is applied in **one local transaction together with the new cursor**,
   so an interrupted sync resumes exactly where it stopped and never leaves the
   cursor ahead of the data.
2. Upserts are idempotent: replaying a page must produce the same state.
3. Deletion of a resource cascades to its secret, metadata, URIs, tags and
   permission rows.
4. An entity that fails metadata decryption is stored and flagged
   `undecryptable`; it must not abort the page.
5. Unknown entity types and unknown DTO fields are ignored, not rejected.

## 7. `GET /sync/manifest.json?type=resources`

Compact reconciliation list, roughly 90 bytes per record
(≈150 KB gzipped for 5 000 records):

```json
{ "body": [ { "id": "…", "modified": "…", "secret_modified": "…" } ] }
```

The client diffs the manifest against local state and re-fetches the difference
with `filter[has-id][]` in batches of 100.

Reconciliation runs:

- always on a **user-triggered sync** (the force-sync button, R4);
- once a day in the background;
- after every 20 delta cycles.

It exists because no journal is immune to bugs, retention and watermark gaps.

---

## 8. Client sync cycle

```
sync(trigger):
    if no network            -> Skipped(NoNetwork)      # never a blocking error
    pendingOps.flush()                                   # push local changes first
    cursor = syncState.cursor
    if cursor == null        -> bootstrap()
    repeat
        page = GET /sync/changes.json?since=cursor       # 410 -> bootstrap()
        transaction:
            applyUpserts(page.upserts)
            applyDeletions(page.deletions)
            syncState.cursor = page.cursor
        cursor = page.cursor
    while page.has_more
    if trigger == MANUAL or reconcileDue() -> reconcileAll()
```

### Error handling

| Situation | Behaviour |
|-----------|-----------|
| No network / DNS failure / server switched off | Status "not synchronised", backoff, app works normally |
| 401 / expired session | Silent re-auth; on failure sync pauses, local data untouched |
| MFA required | Prompt shown **only** if the user triggered the sync |
| 410 Gone (cursor outside retention) | Transparent bootstrap on top of existing data |
| Epoch changed | Same as 410 |
| 5xx / timeout | Backoff 30 s → 1 min → 5 min → 15 min → 1 h → 6 h, no attempt cap |
| Metadata decryption failure | Row flagged `undecryptable`, sync continues |
| Corrupted local database | The only data-loss case: warn, offer restore from export or server |

Nothing in this table deletes local data.

---

## 9. Pending operations (offline writes)

Local writes are queued in the database, so the queue survives restarts and has
**no expiry**.

| Operation | Offline |
|-----------|---------|
| View, search, copy, TOTP, autofill, passkey | yes |
| Create / edit / delete (personal and shared) | yes, queued |
| Favourite, move between folders, tags | yes, queued |
| Share / change permissions | queued, applied on reconnect (the server validates the full secret set) |
| Account passphrase change, MFA, user management | no — the other side of the operation is the server |

Re-encryption for shared records is computable offline because every user's
public key and the v5 metadata keys are held locally.

### Conflicts

- Each queued operation carries `baseModified` (and `baseSecretModified` where
  relevant) captured at queue time.
- If the server state diverged, **the server wins**, and the local version is
  preserved in a `sync_conflicts` table and surfaced to the user for re-apply.
  Data never disappears silently.
- Idempotency: `CREATE` carries a client-generated UUID; after an indeterminate
  timeout the client first checks whether that id already exists on the server.

---

## 10. Per-platform storage bindings

The wire format and the apply rules are shared; storage is not.

| Platform | Store | Key material | Scheduler |
|----------|-------|--------------|-----------|
| Android | Room + SQLCipher | Android Keystore, **not** bound to biometrics | WorkManager |
| Browser extension | IndexedDB, values under AES-GCM (WebCrypto) | key derived from the passphrase, held in the service worker | `chrome.alarms` (MV3 sleeps; sync is cursor-resumable) |
| iOS | SQLite + SQLCipher | Keychain / Secure Enclave, **not** bound to biometrics | `BGAppRefreshTask` |
| Desktop | SQLCipher | OS keyring | own timer |

The database key must never be bound to biometric enrolment: re-enrolling a
fingerprint would destroy the store permanently, which directly violates R0.

---

## 11. Test fixtures

Shared JSON fixtures live next to this document and are consumed by the test
suites of every client:

- a bootstrap page,
- a delta page containing an upsert, a secret change and a revocation,
- a manifest that diverges from local state in three ways (missing locally,
  stale locally, extra locally),
- a `410 Gone` response.

## 12. Compatibility notes

- The legacy routes (`/resources.json` and friends) stay available for the
  browser extension. The delta protocol does not use them.
- The minimum server version is pinned in the client and checked once, online,
  during account setup. An older server is a configuration error, not a
  degraded mode.
