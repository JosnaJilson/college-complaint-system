# College Complaint Management System

A working website (login, dashboards, admin panel) built in plain Java —
no frameworks, no external libraries, no internet connection needed to run.
Uses only the JDK's built-in `com.sun.net.httpserver`.

## How to run

You need a JDK (17+) installed. Then, from the project root:

```bash
# 1. Compile
mkdir out
find src -name "*.java" > sources.txt
javac -d out @sources.txt

# 2. Run (default port 8080; pass a different port as an argument if needed)
cd out
java Main 8080
```

Open **http://localhost:8080** in your browser.

## Logging in

**Students** — sign up at `/signup`. Email must end in `@college.edu`
(change `ALLOWED_STUDENT_DOMAIN` in `AuthService.java` to your real college
domain, e.g. `@sngce.ac.in`).

**Admins/handlers** — pre-seeded, one per leaf category, password
`admin123`. A few examples:
- `hostel.warden@college.edu` — all HOSTEL complaints
- `cse.ai@college.edu` — CSE > AI complaints
- `canteen.admin@college.edu` — CANTEEN complaints

(Full list of seeded admin emails is in `CategoryService.java`, one per
leaf category — every leaf you listed has a handler.)

## What's implemented

- **Category tree** exactly as specified: COLLEGE (Canteen, Store,
  Maintenance, Transport, Administrative, Faculty→CSE/EEE/ECE/MECH/CE with
  CSE sub-branches AI/Cyber/Core/Design/Business, Others→Individual/
  Library/Exam/Other) and HOSTEL (Room-Maintenance, Mess-Food,
  Warden-Discipline). Cascading dropdown on the "File Complaint" page walks
  the tree until it hits a leaf.
- **Login restricted to college email** for students (`AuthService`).
- **Priority-based ordering** — every complaint list (student dashboard,
  admin panel) is sorted most-urgent-first via `Complaint implements
  Comparable`.
- **Status tracking** — `OPEN → IN_PROGRESS → ESCALATED → RESOLVED →
  CLOSED`, enforced by `ComplaintStatus.canTransitionTo()` so illegal jumps
  throw `InvalidStatusTransitionException`. Visible as a colored badge on
  both student and admin views.
- **Auto-escalation** — each `Priority` has an SLA in minutes
  (`Priority.java`). Click "Run escalation sweep" on the admin page (or
  wire `ComplaintService.runEscalationSweep()` to a scheduled thread) to
  bump overdue complaints to `ESCALATED` and raise their priority.
- **Admin resolution workflow** — each handler only sees complaints routed
  to their managed categories, with Start / Resolve / Close actions.

## Where to go from here

This runs entirely in memory (data resets when you restart it) — good for
a demo/viva, not for production. Natural next steps:
1. Swap the in-memory `List`s in `ComplaintService`/`AuthService` for a
   real database (MySQL + JDBC, or move to Spring Boot + JPA).
2. Hash passwords (currently plain text — fine for a college mini-project
   demo, not for real deployment).
3. Auto-run the escalation sweep on a background timer instead of a manual
   button.
4. Add file/photo attachments to complaints.
