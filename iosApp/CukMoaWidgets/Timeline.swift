import AppIntents
import WidgetKit

struct SnapshotEntry: TimelineEntry {
    let date: Date
    let snapshot: WidgetSnapshot?
    let feed: Feed
}

/// Entries at each class start and end in the next 24 hours and at midnight (Korea time), so "next class" and
/// D-days move on without the app. Asks again in an hour, when the calendar and posts are fetched again.
func makeTimeline(feed: Feed) async -> Timeline<SnapshotEntry> {
    let snapshot = await Server.refreshed(SnapshotStore.load())
    let now = Date()
    var dates: Set<Date> = [now]
    let today = kstStartOfDay(now)
    for dayOffset in 0...1 {
        let dayStart = today.addingTimeInterval(Double(dayOffset) * 86_400)
        let weekday = KstMoment(dayStart).weekday
        for slot in snapshot?.timetable?.slots ?? [] where slot.weekday == weekday {
            for minute in [slot.startMinute, slot.endMinute] {
                let d = dayStart.addingTimeInterval(Double(minute) * 60 + 1)
                if d > now && d < now.addingTimeInterval(86_400) { dates.insert(d) }
            }
        }
    }
    dates.insert(today.addingTimeInterval(86_400 + 1))
    let entries = dates.sorted().prefix(30).map { SnapshotEntry(date: $0, snapshot: snapshot, feed: feed) }
    return Timeline(entries: Array(entries), policy: .after(now.addingTimeInterval(3_600)))
}

/// Calendar, timetable and today widgets
struct SnapshotProvider: TimelineProvider {
    func placeholder(in context: Context) -> SnapshotEntry { SnapshotEntry(date: Date(), snapshot: Preview.snapshot, feed: .mine) }

    func getSnapshot(in context: Context, completion: @escaping (SnapshotEntry) -> Void) {
        completion(SnapshotEntry(date: Date(), snapshot: context.isPreview ? Preview.snapshot : SnapshotStore.load(), feed: .mine))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<SnapshotEntry>) -> Void) {
        Task { completion(await makeTimeline(feed: .mine)) }
    }
}

// MARK: - Announcement widget configuration (내 학과 / 학교 / 전체)

enum FeedOption: String, AppEnum {
    case mine, school, all

    static var typeDisplayRepresentation: TypeDisplayRepresentation = TypeDisplayRepresentation(name: LocalizedStringResource("widget.feed"))
    static var caseDisplayRepresentations: [FeedOption: DisplayRepresentation] = [
        .mine: DisplayRepresentation(title: LocalizedStringResource("widget.feed_mine")),
        .school: DisplayRepresentation(title: LocalizedStringResource("widget.feed_school")),
        .all: DisplayRepresentation(title: LocalizedStringResource("widget.feed_all")),
    ]
}

struct FeedIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = LocalizedStringResource("widget.pick_feed")

    @Parameter(title: LocalizedStringResource("widget.feed"), default: .mine)
    var feed: FeedOption
}

struct FeedProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> SnapshotEntry { SnapshotEntry(date: Date(), snapshot: Preview.snapshot, feed: .mine) }

    func snapshot(for configuration: FeedIntent, in context: Context) async -> SnapshotEntry {
        SnapshotEntry(date: Date(), snapshot: context.isPreview ? Preview.snapshot : SnapshotStore.load(), feed: Feed(rawValue: configuration.feed.rawValue) ?? .mine)
    }

    func timeline(for configuration: FeedIntent, in context: Context) async -> Timeline<SnapshotEntry> {
        await makeTimeline(feed: Feed(rawValue: configuration.feed.rawValue) ?? .mine)
    }
}

/// Made-up data for the widget gallery
enum Preview {
    static var snapshot: WidgetSnapshot {
        let now = KstMoment(Date())
        let today = min(now.weekday, 5)
        func slot(_ name: String, _ weekday: Int, _ from: Int, _ to: Int, _ room: String) -> WidgetSlot {
            WidgetSlot(name: name, room: room, professor: nil, weekday: weekday,
                       startMinute: (8 + from) * 60, endMinute: (8 + to) * 60 + 50, startPeriod: from, endPeriod: to)
        }
        return WidgetSnapshot(
            version: supportedSnapshotVersion,
            generatedAt: "",
            events: [
                WidgetEvent(title: "수강정정", start: now.date, end: nil, kind: "registration"),
                WidgetEvent(title: "기말고사", start: "2026-12-15", end: "2026-12-19", kind: "exam"),
            ],
            timetable: WidgetTimetable(year: 2026, semester: "2", title: "", slots: [
                slot("자료구조", today, 1, 2, "M306"), slot("운영체제", today, 5, 6, "K201"),
                slot("알고리즘", today % 5 + 1, 3, 4, "N401"),
            ]),
            feeds: ["mine": WidgetPosts(items: [
                WidgetPost(id: 1, title: "복수전공 신청 안내", board: "", date: now.date, pinned: false, link: "https://www.catholic.ac.kr", firstSeen: "2"),
                WidgetPost(id: 2, title: "캡스톤디자인 발표회 일정", board: "", date: now.date, pinned: false, link: "https://www.catholic.ac.kr", firstSeen: "0"),
            ], asOf: "")],
            majors: [],
            seenAt: "1"
        )
    }
}
