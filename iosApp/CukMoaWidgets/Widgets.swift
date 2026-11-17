import SwiftUI
import WidgetKit

// Four widgets; the variant follows the family the user picks:
//   학사일정  small: next event D-day · medium/large: upcoming list · lock screen: D-day / next event
//   시간표    small: next class · medium: today's (or the next school day's) classes · large: the week · lock screen: next class
//   공지      small: new-post count · medium/large: latest posts · lock screen: count / newest title (feed set per widget)
//   오늘      medium/large: next class, next event and newest posts

@main
struct CukMoaWidgets: WidgetBundle {
    var body: some Widget {
        CalendarWidget()
        TimetableWidget()
        AnnouncementsWidget()
        TodayWidget()
    }
}

struct CalendarWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "calendar", provider: SnapshotProvider()) { CalendarView(entry: $0) }
            .configurationDisplayName(L("widget.calendar_title"))
            .description(L("widget.calendar_desc"))
            .supportedFamilies([.systemSmall, .systemMedium, .systemLarge, .accessoryCircular, .accessoryRectangular, .accessoryInline])
    }
}

struct TimetableWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "timetable", provider: SnapshotProvider()) { TimetableView(entry: $0) }
            .configurationDisplayName(L("widget.timetable_title"))
            .description(L("widget.timetable_desc"))
            .supportedFamilies([.systemSmall, .systemMedium, .systemLarge, .accessoryRectangular, .accessoryInline])
    }
}

struct AnnouncementsWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "announcements", intent: FeedIntent.self, provider: FeedProvider()) { AnnouncementsView(entry: $0) }
            .configurationDisplayName(L("widget.announcements_title"))
            .description(L("widget.announcements_desc"))
            .supportedFamilies([.systemSmall, .systemMedium, .systemLarge, .accessoryCircular, .accessoryRectangular])
    }
}

struct TodayWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "today", provider: SnapshotProvider()) { TodayView(entry: $0) }
            .configurationDisplayName(L("widget.today_title"))
            .description(L("widget.today_desc"))
            .supportedFamilies([.systemMedium, .systemLarge])
    }
}

// MARK: - Views

struct CalendarView: View {
    let entry: SnapshotEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        let now = KstMoment(entry.date)
        let events = entry.snapshot?.upcomingEvents(now.date, family == .systemLarge ? 8 : 3) ?? []
        WidgetFrame(target: "home") {
            switch family {
            case .accessoryInline:
                Text(verbatim: events.first.map { "\(dday(now.date, $0)) \($0.title)" } ?? L("widget.no_events"))
            case .accessoryCircular:
                VStack(spacing: 0) {
                    Text(verbatim: events.first.map { dday(now.date, $0) } ?? "–").font(.system(size: 14, weight: .bold)).minimumScaleFactor(0.6)
                    Text(verbatim: events.first?.title ?? "").font(.system(size: 9)).lineLimit(1)
                }
            case .accessoryRectangular:
                VStack(alignment: .leading) {
                    Text(verbatim: L("widget.next_event")).font(.caption2).foregroundStyle(.secondary)
                    if let e = events.first {
                        Text(verbatim: e.title).font(.headline).lineLimit(1)
                        Text(verbatim: "\(dday(now.date, e)) · \(eventDates(e))").font(.caption)
                    } else { Text(verbatim: L("widget.no_events")).font(.caption) }
                }
            case .systemSmall:
                VStack(alignment: .leading, spacing: 2) {
                    Header(L("widget.calendar_title"))
                    if let e = events.first {
                        Spacer(minLength: 0)
                        Big(dday(now.date, e))
                        Text(verbatim: e.title).font(.subheadline.weight(.medium)).lineLimit(2)
                        Caption(eventDates(e))
                    } else { Empty(L("widget.no_events")) }
                }
            default:
                VStack(alignment: .leading, spacing: 6) {
                    Header(L("widget.variant_upcoming"))
                    if events.isEmpty { Empty(L("widget.no_events")) }
                    ForEach(events, id: \.self) { e in
                        HStack {
                            Text(verbatim: dday(now.date, e)).font(.footnote.bold()).foregroundStyle(Brand.accent).frame(width: 56, alignment: .leading)
                            Text(verbatim: e.title).font(.subheadline).lineLimit(1)
                            Spacer(minLength: 4)
                            Caption(eventDates(e))
                        }
                    }
                    Spacer(minLength: 0)
                }
            }
        }
    }
}

struct TimetableView: View {
    let entry: SnapshotEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        let now = KstMoment(entry.date)
        let tt = entry.snapshot?.timetable
        let next = tt?.nextClass(now)
        WidgetFrame(target: "timetable") {
            switch family {
            case .accessoryInline:
                Text(verbatim: next.map { "\(whenText(now, $0)) \($0.slot.name)" } ?? L("widget.no_classes"))
            case .accessoryRectangular:
                VStack(alignment: .leading) {
                    Text(verbatim: L("widget.next_class")).font(.caption2).foregroundStyle(.secondary)
                    if let next {
                        Text(verbatim: next.slot.name).font(.headline).lineLimit(1)
                        Text(verbatim: [whenText(now, next), next.slot.room].compactMap { $0 }.joined(separator: " · ")).font(.caption)
                    } else { Text(verbatim: L(tt == nil ? "widget.no_timetable" : "widget.no_classes")).font(.caption) }
                }
            case .systemSmall:
                VStack(alignment: .leading, spacing: 2) {
                    Header(L("widget.next_class"))
                    if tt == nil { Empty(L("widget.no_timetable")) }
                    else if let next {
                        Spacer(minLength: 0)
                        Big(whenText(now, next), accent: next.ongoing, size: next.daysAhead > 0 ? 18 : 26)
                        Text(verbatim: next.slot.name).font(.subheadline.weight(.medium)).lineLimit(2)
                        Caption([clockLabel(next.slot.startMinute) + "–" + clockLabel(next.slot.endMinute), next.slot.room].compactMap { $0 }.joined(separator: " · "))
                    } else { Empty(L("widget.no_classes")) }
                }
            case .systemLarge:
                VStack(alignment: .leading, spacing: 4) {
                    Header(L("widget.variant_week"))
                    if let tt { WeekGrid(tt: tt, now: now) } else { Empty(L("widget.no_timetable")) }
                }
            default:
                let day = tt?.dayToShow(now)
                VStack(alignment: .leading, spacing: 6) {
                    Header(dayTitle(now, day?.0))
                    if tt == nil { Empty(L("widget.no_timetable")) }
                    else if let day {
                        ForEach(day.1.prefix(4), id: \.self) { s in
                            let today = day.0 == 0
                            let on = today && s.startMinute <= now.minute && now.minute < s.endMinute
                            let past = today && s.endMinute <= now.minute
                            HStack {
                                Text(verbatim: clockLabel(s.startMinute)).font(.footnote.bold()).foregroundStyle(on ? Brand.accent : .secondary).frame(width: 46, alignment: .leading)
                                Text(verbatim: s.name).font(.subheadline).foregroundStyle(past ? .secondary : .primary).lineLimit(1)
                                Spacer(minLength: 4)
                                if let room = s.room { Caption(room) }
                            }
                        }
                        Spacer(minLength: 0)
                    } else { Empty(L("widget.no_classes")) }
                }
            }
        }
    }

    private func dayTitle(_ now: KstMoment, _ ahead: Int?) -> String {
        switch ahead {
        case nil, 0: return L("widget.variant_today_classes")
        case 1: return L("widget.tomorrow")
        default: return dayName((now.weekday - 1 + ahead!) % 7 + 1)
        }
    }
}

/// Mon–Fri (Saturday when a class meets then) on a time scale; the current class is filled.
struct WeekGrid: View {
    let tt: WidgetTimetable
    let now: KstMoment

    var body: some View {
        let days = tt.slots.contains { $0.weekday == 6 } ? Array(1...6) : Array(1...5)
        let first = (tt.slots.map(\.startMinute).min() ?? 540) / 60 * 60
        let last = ((tt.slots.map(\.endMinute).max() ?? 1080) + 59) / 60 * 60
        HStack(alignment: .top, spacing: 3) {
            ForEach(days, id: \.self) { d in
                VStack(spacing: 2) {
                    Text(verbatim: dayName(d)).font(.caption2.bold()).foregroundStyle(d == now.weekday ? Brand.accent : .secondary)
                    GeometryReader { geo in
                        let perMinute = geo.size.height / CGFloat(max(last - first, 60))
                        ForEach(tt.classesOn(d), id: \.self) { s in
                            let on = d == now.weekday && s.startMinute <= now.minute && now.minute < s.endMinute
                            Text(verbatim: s.name)
                                .font(.system(size: 9, weight: .medium))
                                .foregroundStyle(on ? Color.white : Brand.onAccentSoft)
                                .padding(2)
                                .frame(width: geo.size.width, height: perMinute * CGFloat(s.endMinute - s.startMinute), alignment: .topLeading)
                                .background(on ? Brand.accent : Brand.accentSoft, in: RoundedRectangle(cornerRadius: 4))
                                .offset(y: perMinute * CGFloat(s.startMinute - first))
                        }
                    }
                }
            }
        }
    }
}

struct AnnouncementsView: View {
    let entry: SnapshotEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        let posts = entry.snapshot?.posts(entry.feed) ?? []
        let n = entry.snapshot?.newCount(entry.feed) ?? 0
        WidgetFrame(target: "announcements") {
            switch family {
            case .accessoryCircular:
                VStack(spacing: 0) {
                    Text(verbatim: "\(n)").font(.system(size: 20, weight: .bold))
                    Text(verbatim: L("widget.variant_new_posts")).font(.system(size: 9)).lineLimit(1)
                }
            case .accessoryRectangular:
                VStack(alignment: .leading) {
                    Text(verbatim: n > 0 ? String(format: L("widget.new_posts"), n) : feedName(entry.feed)).font(.caption2).foregroundStyle(.secondary)
                    Text(verbatim: posts.first?.title ?? L("widget.no_posts")).font(.headline).lineLimit(2)
                }
            case .systemSmall:
                VStack(alignment: .leading, spacing: 2) {
                    Header(feedName(entry.feed))
                    Spacer(minLength: 0)
                    Big(n > 0 ? String(format: L("widget.new_posts"), n) : L("widget.no_new_posts"), accent: n > 0, size: n > 0 ? 24 : 16)
                    if let p = posts.first { Text(verbatim: p.title).font(.subheadline.weight(.medium)).lineLimit(2) }
                    else { Caption(L("widget.no_posts")) }
                }
            default:
                VStack(alignment: .leading, spacing: 6) {
                    Header(L("widget.latest_posts") + " · " + feedName(entry.feed))
                    if posts.isEmpty { Empty(L("widget.no_posts")) }
                    ForEach(posts.prefix(family == .systemLarge ? 6 : 3), id: \.self) { PostRow(post: $0, isNew: entry.snapshot?.isNew($0) ?? false) }
                    Spacer(minLength: 0)
                }
            }
        }
    }
}

struct TodayView: View {
    let entry: SnapshotEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        let now = KstMoment(entry.date)
        let next = entry.snapshot?.timetable?.nextClass(now)
        let event = entry.snapshot?.upcomingEvents(now.date, 1).first
        WidgetFrame(target: "home") {
            VStack(alignment: .leading, spacing: 6) {
                Header(L("widget.today_title"))
                if let next { Line(label: L("widget.next_class"), text: "\(next.slot.name) · \(whenText(now, next))") }
                if let event { Line(label: L("widget.next_event"), text: "\(event.title) · \(dday(now.date, event))") }
                ForEach((entry.snapshot?.posts(.mine) ?? []).prefix(family == .systemLarge ? 5 : 2), id: \.self) {
                    PostRow(post: $0, isNew: entry.snapshot?.isNew($0) ?? false)
                }
                Spacer(minLength: 0)
            }
        }
    }

    private struct Line: View {
        let label: String
        let text: String
        var body: some View {
            HStack {
                Caption(label).frame(width: 64, alignment: .leading)
                Text(verbatim: text).font(.subheadline.weight(.medium)).lineLimit(1)
            }
        }
    }
}

// MARK: - Pieces

enum Brand {
    static let accent = Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(red: 0x4F / 255, green: 0x6F / 255, blue: 0xE3 / 255, alpha: 1) : UIColor(red: 0x0C / 255, green: 0x2E / 255, blue: 0x86 / 255, alpha: 1) })
    static let accentSoft = Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(red: 0x24 / 255, green: 0x40 / 255, blue: 0x97 / 255, alpha: 1) : UIColor(red: 0xDC / 255, green: 0xE1 / 255, blue: 0xFF / 255, alpha: 1) })
    static let onAccentSoft = Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(red: 0xDC / 255, green: 0xE1 / 255, blue: 0xFF / 255, alpha: 1) : UIColor(red: 0x00 / 255, green: 0x15 / 255, blue: 0x50 / 255, alpha: 1) })
}

/// Background and tap target: cukmoa://open?target=<tab>
struct WidgetFrame<Content: View>: View {
    let target: String
    @ViewBuilder let content: () -> Content

    var body: some View {
        content()
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .widgetURL(openURL(target))
            .containerBackground(for: .widget) { Color(UIColor.systemBackground) }
    }
}

func openURL(_ target: String) -> URL? {
    var c = URLComponents()
    c.scheme = "cukmoa"
    c.host = "open"
    c.queryItems = [URLQueryItem(name: "target", value: target)]
    return c.url
}

struct Header: View {
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View { Text(verbatim: text).font(.caption.bold()).foregroundStyle(Brand.accent).lineLimit(1) }
}

struct Big: View {
    let text: String
    var accent = true
    var size: CGFloat = 26
    init(_ text: String, accent: Bool = true, size: CGFloat = 26) { self.text = text; self.accent = accent; self.size = size }
    var body: some View {
        Text(verbatim: text).font(.system(size: size, weight: .bold)).foregroundStyle(accent ? Brand.accent : .primary).lineLimit(1).minimumScaleFactor(0.6)
    }
}

struct Caption: View {
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View { Text(verbatim: text).font(.caption).foregroundStyle(.secondary).lineLimit(1) }
}

struct Empty: View {
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View {
        Text(verbatim: text).font(.footnote).foregroundStyle(.secondary).frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct PostRow: View {
    let post: WidgetPost
    let isNew: Bool
    var body: some View {
        Link(destination: openURL(post.link) ?? URL(string: "cukmoa://open")!) {
            HStack(spacing: 6) {
                Circle().fill(isNew ? Brand.accent : Color.clear).frame(width: 6, height: 6)
                Text(verbatim: post.title).font(.subheadline).lineLimit(1)
                Spacer(minLength: 4)
                Caption(shortDate(post.date))
            }
        }
    }
}

// MARK: - Labels

/// Text from Localizable.xcstrings (ko, en, zh-Hans, ja)
func L(_ key: String) -> String { NSLocalizedString(key, comment: "") }

func dday(_ today: String, _ e: WidgetEvent) -> String {
    let days = daysUntil(today, e.start)
    if days > 0 { return "D-\(days)" }
    if days == 0 { return "D-day" }
    return L("widget.ongoing")
}

func eventDates(_ e: WidgetEvent) -> String {
    guard let end = e.end, end != e.start else { return shortDate(e.start) }
    return shortDate(e.start) + "–" + shortDate(end)
}

func whenText(_ now: KstMoment, _ next: NextClass) -> String {
    if next.ongoing { return L("widget.now") }
    if next.daysAhead == 0 {
        let minutes = next.slot.startMinute - now.minute
        return minutes < 60 ? String(format: L("widget.in_minutes"), minutes) : String(format: L("widget.in_hours"), minutes / 60)
    }
    if next.daysAhead == 1 { return L("widget.tomorrow") + " " + clockLabel(next.slot.startMinute) }
    return dayName(next.slot.weekday) + " " + clockLabel(next.slot.startMinute)
}

func dayName(_ weekday: Int) -> String {
    L(["widget.mon", "widget.tue", "widget.wed", "widget.thu", "widget.fri", "widget.sat", "widget.sun"][max(0, min(6, weekday - 1))])
}

func feedName(_ feed: Feed) -> String { L("widget.feed_\(feed.rawValue)") }
