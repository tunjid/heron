# data/core AGENTS.md

Read this before changing anything under `data/*`. It covers the data layer's rules that aren't
visible from the code alone, each with the implementation to copy. The root
[AGENTS.md](../../AGENTS.md) still applies.

The theme: the architecture almost always has an answer already. Find it and copy it instead of
inventing a new mechanism. If nothing fits, propose the new mechanism in your summary rather than
building it.

Adding a new record collection? Follow the step-by-step checklist in
[.claude/skills/add-record-type/SKILL.md](../../.claude/skills/add-record-type/SKILL.md).

Paths below are relative to this module unless they start with `data/`. The saver lives in
`src/commonMain/kotlin/com/tunjid/heron/data/utilities/multipleEntitysaver/`, the DAOs in
`data/database/src/commonMain/kotlin/com/tunjid/heron/data/database/daos/`.

## MultipleEntitySaver

The saver is a pure, synchronous mapping from network views to entities. It runs inside a write
transaction, so it does no suspending lookups. `heron:entity-saver-shape` enforces its shape.

- **Two verbs: `add` and `remove`.** Every entity gets one `LazyList` and an `add(entity)`. Methods
  take fully formed entities. Anything more is built from those two verbs.
- **`add` is an upsert, triaged at flush.** In `flushPendingOperations`, partition each list into
  full, partial and stub rows so a sparse view never clobbers a valid row. Full rows upsert,
  partials go through `insertOrPartiallyUpdate…`, and stubs go through `insertOrIgnore…`.
  Reference: the post and profile blocks in `MultipleEntitySaver.flushPendingOperations`.
- **`remove` takes a `Deletion` partial keyed by the primary key.** Use it when a view says
  something is gone. Reference: `SaveEventView.kt`, where `viewerRsvp == null` becomes
  `remove(RsvpEntity.Deletion(…))`. Standard-site subscriptions and recommendations work the same
  way.
- **Replacing a set is delete-for-subject, then upsert, at flush.** When a view says "this
  subject's children are exactly these", always queue a stubbed sentinel for the subject so an
  empty set still triggers the delete, and filter the sentinel out before the upsert. Reference:
  `stubProfileLinkEntity` in `SaveProfileLinks.kt`, with `Collections.stubbedId` / `isStubbedId`.
- **Express intent through entities.** No side lists, flags, timing windows or one-off state.
  If the saver needs to know something, it's encoded in an entity (e.g. a stubbed sentinel).
- **Keep stubs explicit.** No defaults that quietly create them.
- **A foreign-key stub goes in the same `add` as the row that needs it**, not in the caller.
  Reference: `MultipleEntitySaver.add(rsvpView)` in `SaveEventView.kt` adds the author and a
  `stubEventEntity` alongside the `RsvpEntity`.

**Known violations; don't copy them:**

- **Labels** are only ever added from views, so a label the labeler removes lives on locally. They
  need replace-per-subject with a sentinel. Their primary key `(uri, value)` also lacks
  `creatorId`, so two labelers applying the same value collide.
- **Verifications** use delete-for-subject, but only when a verification is queued. A profile whose
  last verification is revoked queues nothing, so the stale row stays. They need the sentinel.

## DAOs

- **Reads are reactive.** A `SELECT` returns a `Flow`; no one-shot `suspend` reads. If a write
  needs a value from another row, look it up with a SQL subquery inside the `UPDATE`.
  `heron:dao-reactive-reads` enforces this.
- **Count methods follow the key each path naturally has: two per table, named for what they do.**
  The write path is keyed by the subject; the delete path is keyed by the record URI and finds the
  subject in SQL, because `RecordDeletion` only carries that URI. Don't split by variant
  (`updateGoingCount`, `updateInterestedCount`, …), and don't merge into one `COALESCE`-keyed
  method. Reference: `StandardSiteDao.incrementRecommendationCount(documentUri)` and
  `decrementRecommendationCount(recommendationUri)`; `EventDao.adjustRsvpCounts(eventUri, …)` and
  `decrementRsvpCounts(rsvpUri, …)`.

## Writes

All writes go through the `WriteQueue` as a `Writable`.

- **Optimistic counts live in the data layer, in the write's own transaction.** Never adjust counts
  in the UI. Reference: `PostDao.updateLikeCount`, and the count pairs above.
- **Carry what the UI already knows in the write.** Reference: `Rsvp.Put.previousStatus` is filled
  from the event's current RSVP when the writable is built, instead of a DAO read.
- **Trust the infrastructure for idempotency.** The write queue dedupes, and the count and row
  commit in one transaction. Don't add retry guards; likes don't have them.
- **Keep generic writes generic.** Don't add a record-specific `Writable` just to carry a key SQL can
  find. Reference: unrecommending is a `RecordDeletion` plus a subquery decrement, not a new
  `Writable.StandardSite.Unrecommend`.

## SavedState and persisted models

`Writable` and everything else reachable from `SavedState` (`repository/SavedStateDataSource.kt`)
is persisted as protobuf with positional field numbers.

- Keep these types sealed, with defaults.
- Never remove or reorder fields. Add new ones at the end. A reorder silently corrupts queued
  writes on users' devices.

## RecordResolver and read paths

- **Don't reshape `RecordResolver` to fix one path.** Add a DAO query and do the extra work at the
  few call sites that need it. Reference: `TimelineRepository.searchTimelineItems`.
- **`embeddableRecords` is the hottest read path.** It only gets more work if it clearly pays for
  it. Quoted posts stay one level deep: no second pass, no recursion.
- **Never store invalid or malformed data as a workaround.** Resolve it on the read path, where
  async work is allowed.
- **Batch invalidations and refreshes:** one per list, not one per item.

## Viewer state and accounts

- **Viewer state follows its sibling records.** Use whichever shape related records already use:
  a nullable column on a per-viewer row (likes on `PostViewerStatisticsEntity`), or row presence
  plus a `Deletion` list (standard-site subscriptions).
- **Several accounts can share a device.** Key per-viewer data by `viewingProfileId`. A cache
  clear never deletes the viewer's own row. Reference: `EventDao.deleteRsvpsForEvent`.

## Upstream and the appview

- **Trust the appview.** Don't duplicate server-side filtering on the client.
- **If the server can provide it, ask the appview.** When Heron owns the appview, write a prompt for
  heron-services instead of adding a client workaround.
- **Lexicons are generated.** Copy the server's lexicon JSON verbatim into
  `data/lexicons/schemas/` and rebuild; never hand-edit generated bindings. Add a Room migration
  (`data/database/.../migrations/`) when a view's shape changes the stored entities.
- **Keep workarounds for upstream gaps isolated**, so they're trivial to delete once upstream
  catches up.

## Changing existing behavior

- **Find out why existing logic exists before replacing it.** Read its commit message (e.g.
  `DatabaseCleanupCallback`).
- **When an invariant changes, remove the guards built for the old one in the same change.**
