-- Carries the target operation and per-group field hashes from command-emission (PENDING) time
-- forward to result-consumption time. ChannelResult carries neither: without this, ChannelResultHandler
-- could not tell a successful UPSERT from a successful END, nor promote the right hashes into
-- field_hashes on success (docs/06-publish-flow.md section 7).
ALTER TABLE publication.channel_listing ADD COLUMN pending_operation TEXT NULL;
ALTER TABLE publication.channel_listing ADD COLUMN pending_field_hashes JSONB NULL;
