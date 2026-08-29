# External product reference: Digital-WellBeing-Application

One-time review of [Singh233/Digital-WellBeing-Application](https://github.com/Singh233/Digital-WellBeing-Application) (MIT). Concepts only: no source, assets, Java/XML patterns, or architecture were copied. DigitalBalance remains offline-first, Compose/Material 3, Room-backed, and permission-minimal.

## Milestone 5 — Explainable score and immediate insights

- **ADOPT NOW:** Keep the dashboard hierarchy of one primary daily total, a compact top-app summary, goals, then secondary insights.
- **ADOPT NOW:** Express insights as factual comparisons and progress, with a visible path to the underlying apps/categories.
- **ADOPT LATER:** Compare today with a completed prior day only after history coverage is trustworthy; never infer a trend from partial days.
- **DO NOT ADOPT:** Random/estimated unlock counts, notification totals, generic praise, or any metric not reconstructed from real data.

## Milestone 6 — Daily and weekly history

- **ADOPT NOW:** Add a simple Today / Yesterday / 7 days period selector, daily totals, and ranked apps with icon, duration, and opens.
- **ADOPT NOW:** Use compact horizontal bars or native Compose visuals before considering a chart dependency.
- **ADOPT LATER:** Weekly average, day-to-day comparison, category breakdown, and sorting by duration, opens, or name.
- **DO NOT ADOPT:** Call foreground usage “screen time,” mix network traffic into wellbeing totals, or claim weekly support before durable daily coverage exists. (The reference README mentions weekly data, while the inspected selector implements Today/Yesterday.)

## Milestone 7 — Focus sessions and intent modes

- **ADOPT LATER:** Treat Work, Study, and Mindfulness as optional presets inside the existing Focus destination—not separate navigation tabs.
- **ADOPT LATER:** Offer a clear duration picker, start/stop state, calm countdown, completion summary, and optional guided breathing presets.
- **ADOPT LATER:** Let a preset carry a user-selected app set or intention only when it can be implemented without invasive control permissions.
- **DO NOT ADOPT:** Duplicate to-do managers, separate databases per mode, continuous foreground timers, automatic app blocking, or notification suppression/listener access.

## Milestone 8 — Respectful reminders

- **ADOPT LATER:** Opt-in, local reminders for a configured goal/limit, with clear disable and snooze actions and neutral wording.
- **ADOPT LATER:** Notify only on meaningful threshold transitions; avoid repeated alerts and do no periodic work when no reminder is enabled.
- **DO NOT ADOPT:** Exact/repeating alarms by default, boot-time services, permanent monitoring notifications, SMS/phone/calendar/storage permissions, or reading notification content.

## Milestone 9 — Onboarding and education

- **ADOPT LATER:** Progressive onboarding: benefit first, on-device privacy statement, Special App Access explanation, then a direct settings action.
- **ADOPT LATER:** Brief feature previews for history, goals, and Focus after the required data becomes available.
- **DO NOT ADOPT:** Sign-in, profiles, social login, ads, backend news/articles, or a decorative splash flow that delays access.

## Ongoing navigation and interaction rules

- **ADOPT NOW:** Keep DigitalBalance’s five stable destinations; open modes, history detail, and goal editors as lightweight subflows.
- **ADOPT LATER:** Add restrained expand/collapse for optional detail and contextual app actions only when they reduce navigation.
- **DO NOT ADOPT:** Activity-per-tab navigation, dense animated dashboards, blur/glass effects, large Lottie decoration, or “ignore app” controls that conflict with the existing category/visibility rules.

## Product boundary

Health/water tracking from the reference is outside DigitalBalance’s scope. Any future Work, Study, Focus, insight, or reminder feature must reuse current usage sessions, categories, goals, and Room repositories; request no permission unless the feature cannot work without it and the user explicitly enables that feature.
