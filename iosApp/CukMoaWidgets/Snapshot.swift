import Foundation

// Swift mirror of the widget snapshot the app writes (shared/.../model/Widget.kt) and of the few helpers in
// shared/.../logic/Widgets.kt that widgets need. Keep the field names and the helper rules in step with the Kotlin side.

let supportedSnapshotVersion = 1

struct WidgetSnapshot: Codable {
    var version: Int
    var generatedAt: String
    var events: [WidgetEvent]?
    var eventsAsOf: String?
    var timetable: WidgetTimetable?
    /// Keyed by feed: "mine", "school", "all"
    var feeds: [String: WidgetPosts]?
    var majors: [String]?
    var seenAt: String?
}

struct WidgetEvent: Codable, Hashable {
    var title: String
    var start: String
    var end: String?
    var kind: String?
}

struct WidgetTimetable: Codable {
    var year: Int
    var semester: String
    var title: String
    var slots: [WidgetSlot]
}

struct WidgetSlot: Codable, Hashable {
    var name: String
    var room: String?
    var professor: String?
    /// 1 = Monday … 6 = Saturday
    var weekday: Int
    var startMinute: Int
    var endMinute: Int
    var startPeriod: Int
    var endPeriod: Int
}

struct WidgetPosts: Codable {
    var items: [WidgetPost]
    var asOf: String
}

struct WidgetPost: Codable, Hashable {
    var id: Int64
    var title: String
    var board: String
    var date: String
    var pinned: Bool?
    var link: String
    var firstSeen: String
}

// MARK: - Korea time

let kst = TimeZone(identifier: "Asia/Seoul")!

private var kstCalendar: Calendar = {
    var c = Calendar(identifier: .gregorian)
    c.timeZone = kst
    return c
}()

/// A moment in Korea time: date (YYYY-MM-DD), weekday (1 = Monday … 7 = Sunday), minutes since midnight
struct KstMoment {
    let date: String
    let weekday: Int
    let minute: Int

    init(_ d: Date) {
        let c = kstCalendar.dateComponents([.year, .month, .day, .weekday, .hour, .minute], from: d)
        date = String(format: "%04d-%02d-%02d", c.year!, c.month!, c.day!)
        // Calendar: Sunday = 1 … Saturday = 7
        weekday = (c.weekday! + 5) % 7 + 1
        minute = c.hour! * 60 + c.minute!
    }
}

/// Start of the Korea-time day containing [d]
func kstStartOfDay(_ d: Date) -> Date { kstCalendar.startOfDay(for: d) }

private func epochDay(_ iso: String) -> Int? {
    let p = iso.prefix(10).split(separator: "-").compactMap { Int($0) }
    guard p.count == 3, let d = kstCalendar.date(from: DateComponents(year: p[0], month: p[1], day: p[2])) else { return nil }
    return Int((d.timeIntervalSince1970 + Double(kst.secondsFromGMT(for: d))) / 86_400)
}

func daysUntil(_ today: String, _ date: String) -> Int {
    guard let a = epochDay(today), let b = epochDay(date) else { return 0 }
    return b - a
}

/// "2026-10-16" → "10.16"
func shortDate(_ iso: String) -> String {
    let p = iso.prefix(10).split(separator: "-")
    guard p.count == 3, let m = Int(p[1]) else { return iso }
    return "\(m).\(p[2])"
}

/// "09:00"
func clockLabel(_ minute: Int) -> String { String(format: "%02d:%02d", minute / 60, minute % 60) }

// MARK: - Questions widgets ask (same rules as logic/Widgets.kt)

struct NextClass {
    let slot: WidgetSlot
    let daysAhead: Int
    let ongoing: Bool
}

extension WidgetTimetable {
    func nextClass(_ now: KstMoment) -> NextClass? {
        if slots.isEmpty { return nil }
        for ahead in 0...7 {
            let weekday = (now.weekday - 1 + ahead) % 7 + 1
            let slot = slots
                .filter { $0.weekday == weekday && (ahead > 0 || $0.endMinute > now.minute) }
                .min { $0.startMinute < $1.startMinute }
            if let slot {
                return NextClass(slot: slot, daysAhead: ahead, ongoing: ahead == 0 && slot.startMinute <= now.minute)
            }
        }
        return nil
    }

    func classesOn(_ weekday: Int) -> [WidgetSlot] {
        slots.filter { $0.weekday == weekday }.sorted { $0.startMinute < $1.startMinute }
    }

    /// Today while classes remain, otherwise the next day with classes: (days from today, that day's classes)
    func dayToShow(_ now: KstMoment) -> (Int, [WidgetSlot])? {
        guard let next = nextClass(now) else { return nil }
        return (next.daysAhead, classesOn((now.weekday - 1 + next.daysAhead) % 7 + 1))
    }
}

extension WidgetSnapshot {
    func upcomingEvents(_ today: String, _ limit: Int) -> [WidgetEvent] {
        Array((events ?? []).filter { daysUntil(today, $0.end ?? $0.start) >= 0 }.prefix(limit))
    }

    func posts(_ feed: Feed) -> [WidgetPost] { feeds?[feed.rawValue]?.items ?? [] }

    func isNew(_ p: WidgetPost) -> Bool {
        guard let seenAt else { return false }
        return p.firstSeen > seenAt
    }

    func newCount(_ feed: Feed) -> Int { posts(feed).filter(isNew).count }
}

/// Announcement widget feeds (WidgetFeed in Kotlin)
enum Feed: String, CaseIterable {
    case mine, school, all
}

// MARK: - Storage (App Group) and refresh from the server

enum SnapshotStore {
    private static var defaults: UserDefaults? {
        guard let group = Bundle.main.object(forInfoDictionaryKey: "AppGroup") as? String else { return nil }
        return UserDefaults(suiteName: group)
    }

    static func load() -> WidgetSnapshot? {
        guard let text = defaults?.string(forKey: "snapshot"), let data = text.data(using: .utf8),
              let s = try? JSONDecoder().decode(WidgetSnapshot.self, from: data),
              s.version <= supportedSnapshotVersion else { return nil }
        return s
    }

    static func save(_ s: WidgetSnapshot) {
        guard let data = try? JSONEncoder().encode(s), let text = String(data: data, encoding: .utf8) else { return }
        defaults?.set(text, forKey: "snapshot")
    }

    /// When the widget last fetched from the server itself
    static var lastFetch: Date? {
        get { defaults?.object(forKey: "widgetFetchedAt") as? Date }
        set { defaults?.set(newValue, forKey: "widgetFetchedAt") }
    }
}

/// Fetches the calendar and the three feeds (same queries as WidgetFeed.query in Kotlin) and merges them in.
enum Server {
    private struct CalendarResponse: Decodable { let events: [WidgetEvent] }
    private struct Page: Decodable { let items: [WidgetPost] }

    private static var base: String {
        (Bundle.main.object(forInfoDictionaryKey: "ApiBaseUrl") as? String) ?? "http://localhost:8080"
    }

    private static func get<T: Decodable>(_ path: String, _ query: [URLQueryItem]) async -> T? {
        guard var c = URLComponents(string: base + path) else { return nil }
        c.queryItems = query.isEmpty ? nil : query
        guard let url = c.url else { return nil }
        var request = URLRequest(url: url)
        request.timeoutInterval = 10
        guard let (data, response) = try? await URLSession.shared.data(for: request),
              (response as? HTTPURLResponse)?.statusCode == 200 else { return nil }
        return try? JSONDecoder().decode(T.self, from: data)
    }

    private static func query(_ feed: Feed, majors: [String]) -> [URLQueryItem] {
        var q = [URLQueryItem(name: "limit", value: "6"), URLQueryItem(name: "pinnedFirst", value: "false")]
        switch feed {
        case .mine where !majors.isEmpty:
            q += [URLQueryItem(name: "source", value: "dept"), URLQueryItem(name: "major", value: majors.joined(separator: ","))]
        case .all:
            break
        default:
            q.append(URLQueryItem(name: "source", value: "main,lms"))
        }
        return q
    }

    /// The stored snapshot with fresh calendar and posts. At most every 30 minutes; offline it returns what is stored.
    static func refreshed(_ stored: WidgetSnapshot?) async -> WidgetSnapshot? {
        if let last = SnapshotStore.lastFetch, Date().timeIntervalSince(last) < 30 * 60 { return stored }
        let now = Date()
        let iso = ISO8601DateFormatter().string(from: now)
        var s = stored ?? WidgetSnapshot(version: supportedSnapshotVersion, generatedAt: iso)
        let today = KstMoment(now).date

        if let cal: CalendarResponse = await get("/api/v1/calendar", [URLQueryItem(name: "campus", value: "성심")]), !cal.events.isEmpty {
            s.events = Array(cal.events.filter { daysUntil(today, $0.end ?? $0.start) >= 0 }.sorted { $0.start < $1.start }.prefix(12))
            s.eventsAsOf = iso
        }
        var feeds = s.feeds ?? [:]
        for feed in Feed.allCases {
            if let page: Page = await get("/api/v1/announcements", query(feed, majors: s.majors ?? [])) {
                feeds[feed.rawValue] = WidgetPosts(items: page.items, asOf: iso)
            }
        }
        s.feeds = feeds
        SnapshotStore.lastFetch = now
        SnapshotStore.save(s)
        return s
    }
}
