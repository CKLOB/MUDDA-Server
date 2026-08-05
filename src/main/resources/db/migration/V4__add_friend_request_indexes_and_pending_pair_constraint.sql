-- tbl_friend already has idx_friend_receiver (receiver_id) from V2. Add the composite indexes the
-- new Friend APIs actually query by (received/sent PENDING lists, ACCEPTED friend list lookups).
CREATE INDEX idx_friend_requester_status ON tbl_friend (requester_id, status);
CREATE INDEX idx_friend_receiver_status ON tbl_friend (receiver_id, status);

-- uq_friend_requester_receiver (requester_id, receiver_id) only blocks a duplicate row in the exact
-- same direction. Two members can still race a PENDING request in opposite directions at nearly the
-- same time (A -> B and B -> A) and end up with two live PENDING rows for the same pair, since each
-- row targets a different unique-constraint key. This partial unique index normalizes the pair with
-- LEAST/GREATEST so at most one PENDING row can exist between any two members regardless of
-- direction, without altering existing columns, the existing constraint, or already-deployed
-- migrations. The application layer still pre-checks for a reverse PENDING request before insert;
-- this index is the safety net for the race the application check alone cannot close.
CREATE UNIQUE INDEX uq_friend_pending_pair
    ON tbl_friend (LEAST(requester_id, receiver_id), GREATEST(requester_id, receiver_id))
    WHERE status = 'PENDING';
