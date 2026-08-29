package team.cklob.mudda.domain.feed.domain.type

// The API spec references FEED_TYPE without listing its values. Only one kind of event is publicly
// observable today -- someone opening a PUBLIC capsule -- so the enum starts there rather than
// speculating about types nothing produces.
enum class FeedType {
	CAPSULE_OPENED,
}
