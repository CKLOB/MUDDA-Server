package team.cklob.mudda.domain.report.domain.type

// Persisted as VARCHAR(30) via @Enumerated(STRING); the columns already exist, so widening the model from
// raw String to enums needs no migration.
enum class ReportTargetType {
	MEMBER,
	CAPSULE,
	GUESTBOOK,
}

enum class ReportReason {
	SPAM,
	ABUSE,
	SEXUAL,
	FALSE_INFO,
	ETC,
}
