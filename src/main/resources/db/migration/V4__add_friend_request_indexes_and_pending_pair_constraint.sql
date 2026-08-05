-- Add the composite indexes the new Friend APIs actually query by (received/sent PENDING lists,
-- ACCEPTED friend list lookups).
CREATE INDEX idx_friend_requester_status ON tbl_friend (requester_id, status);
CREATE INDEX idx_friend_receiver_status ON tbl_friend (receiver_id, status);

-- idx_friend_receiver (receiver_id) from V2 is now a strict prefix of idx_friend_receiver_status
-- (receiver_id, status) above, so it is fully redundant -- it only costs an extra index to maintain on
-- every write. Safe to drop since this migration hasn't been deployed anywhere yet.
DROP INDEX idx_friend_receiver;

-- uq_friend_requester_receiver (requester_id, receiver_id) from V2 is an unconditional unique
-- constraint: at most one row can ever exist for a given (requester_id, receiver_id) pair, regardless
-- of status. Since RespondFriendRequestService rejects a request by flipping its status to REJECTED
-- in place (the row is never deleted), that REJECTED row permanently occupies the pair's only slot --
-- the same requester can never send that receiver another request again, because inserting a fresh
-- PENDING row for the same (requester_id, receiver_id) pair always violates this constraint.
-- Replacing it with a partial unique index that excludes REJECTED rows fixes this: at most one
-- PENDING/ACCEPTED row can still exist per direction (the original intent), but any number of REJECTED
-- rows can accumulate as history, and a fresh request after a rejection is a plain new row.
ALTER TABLE tbl_friend DROP CONSTRAINT uq_friend_requester_receiver;
CREATE UNIQUE INDEX uq_friend_requester_receiver
    ON tbl_friend (requester_id, receiver_id)
    WHERE status <> 'REJECTED';

-- Two members can still race a PENDING request in opposite directions at nearly the same time
-- (A -> B and B -> A) and end up with two live PENDING rows for the same pair, since each row targets
-- a different unique-constraint key above. This partial unique index normalizes the pair with
-- LEAST/GREATEST so at most one PENDING row can exist between any two members regardless of
-- direction. The application layer still pre-checks for a reverse PENDING request before insert; this
-- index is the safety net for the race the application check alone cannot close.
CREATE UNIQUE INDEX uq_friend_pending_pair
    ON tbl_friend (LEAST(requester_id, receiver_id), GREATEST(requester_id, receiver_id))
    WHERE status = 'PENDING';

-- RespondFriendRequestService always sets accepted_at when it transitions a row to ACCEPTED, and every
-- read path (e.g. FriendResponse.of) trusts that with requireNotNull(). That invariant was previously
-- only a code convention -- a batch job or manual data fix touching this table could silently break it
-- and turn the friend list endpoint into a 500 for the affected member. Enforcing it as a CHECK
-- constraint makes it impossible to violate regardless of which code path writes to this table.
ALTER TABLE tbl_friend
    ADD CONSTRAINT ck_friend_accepted_at CHECK (status <> 'ACCEPTED' OR accepted_at IS NOT NULL);
