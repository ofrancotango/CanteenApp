import { useState } from "react";

type Screen = "home" | "result-ok" | "result-denied" | "today" | "stats";

const DOMAIN = "7fb809e0-f6f1-4555-a8d5-60fbd0ed2ff6-00-3hk6j9fpj7h6u.janeway.replit.dev";

const users = [
  { name: "Marco Rossi", company: "Streicher", time: "12:04", status: "ok" },
  { name: "Laura Bianchi", company: "Streicher", time: "12:07", status: "ok" },
  { name: "Ahmed Karim", company: "Ext. Contractor", time: "12:09", status: "denied", reason: "Not whitelisted" },
  { name: "Sofia Greco", company: "Streicher", time: "12:14", status: "ok" },
  { name: "Luca Ferrari", company: "Streicher", time: "12:18", status: "bonus", reason: "Bonus entry" },
  { name: "Elena Marin", company: "Streicher", time: "12:21", status: "ok" },
  { name: "Radu Popescu", company: "Ext. Contractor", time: "12:25", status: "denied", reason: "Limit reached" },
  { name: "Giulia Moretti", company: "Streicher", time: "12:28", status: "ok" },
];

const C = {
  bg: "#09090f",
  surface: "#13131e",
  border: "#1e1e30",
  accent: "#6366f1",
  accentGlow: "rgba(99,102,241,0.18)",
  text: "#f0f0ff",
  muted: "#6b6b8a",
  green: "#22c55e",
  amber: "#f59e0b",
  red: "#ef4444",
  white: "#ffffff",
};

function StatusDot({ status }: { status: string }) {
  const color = status === "ok" ? C.green : status === "bonus" ? C.amber : C.red;
  return (
    <span style={{
      width: 8, height: 8, borderRadius: 9999,
      background: color, display: "inline-block",
      boxShadow: `0 0 6px ${color}`,
      flexShrink: 0,
    }} />
  );
}

function HomeScreen({ nav }: { nav: (s: Screen) => void }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100%", padding: "0 0 28px 0" }}>
      {/* Status bar */}
      <div style={{ display: "flex", justifyContent: "space-between", padding: "12px 24px 0", fontSize: 12, color: C.muted }}>
        <span>9:41</span>
        <span style={{ display: "flex", gap: 6 }}>
          <span>●●●</span>
          <span>WiFi</span>
          <span>100%</span>
        </span>
      </div>
      {/* Header */}
      <div style={{ padding: "32px 28px 24px" }}>
        <div style={{ fontSize: 11, letterSpacing: "0.15em", color: C.muted, textTransform: "uppercase", marginBottom: 6 }}>Max Streicher</div>
        <div style={{ fontSize: 34, fontWeight: 700, color: C.text, letterSpacing: "-0.02em", lineHeight: 1.1 }}>
          Canteen
          <span style={{ color: C.accent }}>.</span>
        </div>
        <div style={{ fontSize: 14, color: C.muted, marginTop: 6 }}>Access Control System</div>
      </div>
      {/* Counter card */}
      <div style={{ margin: "0 24px", background: C.surface, borderRadius: 20, padding: "20px 24px", border: `1px solid ${C.border}`, marginBottom: 20 }}>
        <div style={{ fontSize: 12, color: C.muted, letterSpacing: "0.1em", textTransform: "uppercase", marginBottom: 8 }}>Today's Entries</div>
        <div style={{ display: "flex", alignItems: "baseline", gap: 8 }}>
          <span style={{ fontSize: 52, fontWeight: 800, color: C.text, letterSpacing: "-0.03em", lineHeight: 1 }}>42</span>
          <span style={{ fontSize: 14, color: C.muted }}>/ 150 expected</span>
        </div>
        <div style={{ marginTop: 14, background: C.border, borderRadius: 999, height: 4, overflow: "hidden" }}>
          <div style={{ width: "28%", height: "100%", background: `linear-gradient(90deg, ${C.accent}, #818cf8)`, borderRadius: 999 }} />
        </div>
        <div style={{ display: "flex", justifyContent: "space-between", marginTop: 8 }}>
          <span style={{ fontSize: 11, color: C.green }}>38 admitted</span>
          <span style={{ fontSize: 11, color: C.amber }}>2 bonus</span>
          <span style={{ fontSize: 11, color: C.red }}>2 denied</span>
        </div>
      </div>
      {/* Main scan button */}
      <div style={{ padding: "0 24px", flex: 1, display: "flex", flexDirection: "column", justifyContent: "center" }}>
        <button
          onClick={() => nav("result-ok")}
          style={{
            width: "100%", height: 80, borderRadius: 20,
            background: `linear-gradient(135deg, ${C.accent} 0%, #818cf8 100%)`,
            boxShadow: `0 8px 32px ${C.accentGlow}, 0 2px 8px rgba(0,0,0,0.4)`,
            border: "none", cursor: "pointer",
            display: "flex", alignItems: "center", justifyContent: "center", gap: 14,
            color: C.white, fontSize: 18, fontWeight: 700, letterSpacing: "0.04em",
          }}
        >
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
            <rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" />
            <rect x="3" y="14" width="7" height="7" rx="1" />
            <path d="M14 14h3m4 0v3m0 4h-7m3-3v3" />
          </svg>
          SCAN QR CODE
        </button>
      </div>
      {/* Nav items */}
      <div style={{ padding: "0 24px" }}>
        {[
          { label: "Today's Users", sub: "42 scans", screen: "today" as Screen, icon: "👥" },
          { label: "Statistics", sub: "Last 30 days", screen: "stats" as Screen, icon: "📊" },
        ].map((item) => (
          <button
            key={item.screen}
            onClick={() => nav(item.screen)}
            style={{
              width: "100%", background: C.surface, border: `1px solid ${C.border}`,
              borderRadius: 16, padding: "14px 18px", marginBottom: 10,
              display: "flex", alignItems: "center", gap: 14, cursor: "pointer",
            }}
          >
            <span style={{ fontSize: 20 }}>{item.icon}</span>
            <div style={{ textAlign: "left", flex: 1 }}>
              <div style={{ fontSize: 15, fontWeight: 600, color: C.text }}>{item.label}</div>
              <div style={{ fontSize: 12, color: C.muted, marginTop: 1 }}>{item.sub}</div>
            </div>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={C.muted} strokeWidth="2" strokeLinecap="round">
              <path d="M9 18l6-6-6-6" />
            </svg>
          </button>
        ))}
      </div>
    </div>
  );
}

function ResultScreen({ ok, nav }: { ok: boolean; nav: (s: Screen) => void }) {
  const bg = ok ? "#071a10" : "#1a0707";
  const accent = ok ? C.green : C.red;
  const icon = ok ? "✓" : "✕";
  const title = ok ? "ENJOY YOUR MEAL" : "ACCESS DENIED";
  const sub = ok ? "Marco Rossi · Streicher" : "Limit reached for today";

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100%", background: bg }}>
      <div style={{ display: "flex", justifyContent: "space-between", padding: "12px 24px 0", fontSize: 12, color: `${accent}60` }}>
        <span>9:41</span><span>100%</span>
      </div>

      <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: 32 }}>
        {/* Big icon */}
        <div style={{
          width: 110, height: 110, borderRadius: 9999,
          background: `${accent}18`, border: `2px solid ${accent}40`,
          display: "flex", alignItems: "center", justifyContent: "center",
          fontSize: 54, color: accent, marginBottom: 36,
          boxShadow: `0 0 60px ${accent}30`,
        }}>
          {icon}
        </div>

        <div style={{ fontSize: 13, letterSpacing: "0.2em", color: `${accent}99`, textTransform: "uppercase", marginBottom: 10 }}>
          {ok ? "Accesso consentito" : "Accesso negato"}
        </div>
        <div style={{ fontSize: 28, fontWeight: 800, color: C.text, textAlign: "center", letterSpacing: "-0.01em", marginBottom: 12 }}>
          {title}
        </div>
        <div style={{ fontSize: 15, color: C.muted, textAlign: "center" }}>{sub}</div>

        {ok && (
          <div style={{ marginTop: 32, background: `${accent}12`, borderRadius: 14, padding: "12px 24px", border: `1px solid ${accent}25` }}>
            <div style={{ fontSize: 12, color: `${accent}90`, textAlign: "center" }}>Entry #43 · 12:31</div>
          </div>
        )}
      </div>

      <div style={{ padding: "0 24px 40px", display: "flex", flexDirection: "column", gap: 10 }}>
        <button
          onClick={() => nav("home")}
          style={{
            width: "100%", height: 56, borderRadius: 16,
            background: accent, border: "none", cursor: "pointer",
            color: C.white, fontSize: 16, fontWeight: 700, letterSpacing: "0.05em",
          }}
        >
          NEXT SCAN
        </button>
        <button
          onClick={() => nav("home")}
          style={{
            width: "100%", height: 48, borderRadius: 16,
            background: "transparent", border: `1px solid ${C.border}`,
            cursor: "pointer", color: C.muted, fontSize: 14, fontWeight: 500,
          }}
        >
          Home
        </button>
      </div>
    </div>
  );
}

function TodayScreen({ nav }: { nav: (s: Screen) => void }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100%" }}>
      <div style={{ display: "flex", justifyContent: "space-between", padding: "12px 24px 0", fontSize: 12, color: C.muted }}>
        <span>9:41</span><span>100%</span>
      </div>

      {/* Header */}
      <div style={{ padding: "20px 24px 0", display: "flex", alignItems: "center", gap: 12 }}>
        <button onClick={() => nav("home")} style={{ background: C.surface, border: `1px solid ${C.border}`, borderRadius: 12, width: 36, height: 36, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={C.muted} strokeWidth="2" strokeLinecap="round">
            <path d="M15 18l-6-6 6-6" />
          </svg>
        </button>
        <div>
          <div style={{ fontSize: 20, fontWeight: 700, color: C.text }}>Today's Users</div>
          <div style={{ fontSize: 12, color: C.muted }}>42 total scans</div>
        </div>
        <div style={{ marginLeft: "auto", width: 8, height: 8, borderRadius: 9999, background: C.green, boxShadow: `0 0 8px ${C.green}` }} title="Synced" />
      </div>

      {/* Pills */}
      <div style={{ display: "flex", gap: 8, padding: "16px 24px" }}>
        {[
          { label: "38 admitted", color: C.green },
          { label: "2 bonus", color: C.amber },
          { label: "2 denied", color: C.red },
        ].map(p => (
          <div key={p.label} style={{
            background: `${p.color}15`, border: `1px solid ${p.color}30`,
            borderRadius: 999, padding: "4px 12px", fontSize: 11, fontWeight: 600, color: p.color,
          }}>
            {p.label}
          </div>
        ))}
      </div>

      {/* List */}
      <div style={{ flex: 1, overflowY: "auto", padding: "0 24px 24px" }}>
        {users.map((u, i) => (
          <div key={i} style={{
            display: "flex", alignItems: "center", gap: 12,
            padding: "12px 0", borderBottom: `1px solid ${C.border}`,
          }}>
            <StatusDot status={u.status} />
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 600, color: C.text }}>{u.name}</div>
              <div style={{ fontSize: 11, color: C.muted, marginTop: 1 }}>
                {u.company}
                {u.reason && <span style={{ color: u.status === "denied" ? C.red : C.amber }}> · {u.reason}</span>}
              </div>
            </div>
            <div style={{ fontSize: 12, color: C.muted }}>{u.time}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

function StatsScreen({ nav }: { nav: (s: Screen) => void }) {
  const days = [
    { date: "23 May", total: 47, unique: 41 },
    { date: "22 May", total: 52, unique: 48 },
    { date: "21 May", total: 39, unique: 35 },
    { date: "20 May", total: 55, unique: 50 },
  ];

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100%" }}>
      <div style={{ display: "flex", justifyContent: "space-between", padding: "12px 24px 0", fontSize: 12, color: C.muted }}>
        <span>9:41</span><span>100%</span>
      </div>

      <div style={{ padding: "20px 24px 0", display: "flex", alignItems: "center", gap: 12 }}>
        <button onClick={() => nav("home")} style={{ background: C.surface, border: `1px solid ${C.border}`, borderRadius: 12, width: 36, height: 36, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={C.muted} strokeWidth="2" strokeLinecap="round">
            <path d="M15 18l-6-6 6-6" />
          </svg>
        </button>
        <div>
          <div style={{ fontSize: 20, fontWeight: 700, color: C.text }}>Statistics</div>
          <div style={{ fontSize: 12, color: C.muted }}>Last 30 days</div>
        </div>
      </div>

      <div style={{ flex: 1, overflowY: "auto", padding: "20px 24px 24px" }}>
        {/* Today cards */}
        <div style={{ fontSize: 11, letterSpacing: "0.12em", textTransform: "uppercase", color: C.muted, marginBottom: 12 }}>Today</div>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, marginBottom: 24 }}>
          {[
            { label: "Total Scans", value: "42", accent: C.accent },
            { label: "Unique Users", value: "38", accent: C.green },
            { label: "Expected", value: "150", accent: C.amber },
            { label: "Denied", value: "2", accent: C.red },
          ].map(s => (
            <div key={s.label} style={{ background: C.surface, border: `1px solid ${C.border}`, borderRadius: 16, padding: "16px 16px 14px" }}>
              <div style={{ fontSize: 11, color: C.muted, marginBottom: 6 }}>{s.label}</div>
              <div style={{ fontSize: 32, fontWeight: 800, color: s.accent, letterSpacing: "-0.02em", lineHeight: 1 }}>{s.value}</div>
            </div>
          ))}
        </div>

        {/* Export */}
        <button style={{
          width: "100%", height: 46, borderRadius: 14,
          background: "transparent", border: `1px solid ${C.border}`,
          cursor: "pointer", color: C.muted, fontSize: 13, fontWeight: 500,
          display: "flex", alignItems: "center", justifyContent: "center", gap: 8, marginBottom: 28,
        }}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3" />
          </svg>
          Export CSV
        </button>

        {/* History */}
        <div style={{ fontSize: 11, letterSpacing: "0.12em", textTransform: "uppercase", color: C.muted, marginBottom: 12 }}>History</div>
        {days.map((d, i) => (
          <div key={i} style={{
            display: "flex", alignItems: "center",
            padding: "14px 0", borderBottom: `1px solid ${C.border}`,
          }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 600, color: C.text }}>{d.date}</div>
              <div style={{ fontSize: 12, color: C.muted, marginTop: 2 }}>{d.unique} unique users</div>
            </div>
            <div style={{ fontSize: 24, fontWeight: 800, color: C.text }}>{d.total}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

export function App() {
  const [screen, setScreen] = useState<Screen>("home");

  const screens: Partial<Record<Screen, JSX.Element>> = {
    home: <HomeScreen nav={setScreen} />,
    "result-ok": <ResultScreen ok={true} nav={setScreen} />,
    "result-denied": <ResultScreen ok={false} nav={setScreen} />,
    today: <TodayScreen nav={setScreen} />,
    stats: <StatsScreen nav={setScreen} />,
  };

  return (
    <div style={{
      minHeight: "100vh", background: "#05050c",
      display: "flex", alignItems: "center", justifyContent: "center",
      fontFamily: "'Inter', system-ui, sans-serif",
    }}>
      {/* Phone frame */}
      <div style={{
        width: 390, height: 844,
        background: C.bg, borderRadius: 48,
        border: "1px solid #1e1e30",
        overflow: "hidden", position: "relative",
        boxShadow: "0 0 0 8px #0d0d1a, 0 40px 80px rgba(0,0,0,0.8), 0 0 80px rgba(99,102,241,0.08)",
        display: "flex", flexDirection: "column",
      }}>
        {/* Dynamic Island notch */}
        <div style={{
          position: "absolute", top: 12, left: "50%", transform: "translateX(-50%)",
          width: 120, height: 34, background: "#000", borderRadius: 20, zIndex: 10,
        }} />

        {/* Screen content */}
        <div style={{ flex: 1, overflowY: "auto", paddingTop: 10, display: "flex", flexDirection: "column" }}>
          {screens[screen]}
        </div>
      </div>

      {/* Bottom nav pills for demo */}
      <div style={{
        position: "fixed", bottom: 20, left: "50%", transform: "translateX(-50%)",
        display: "flex", gap: 6, background: "rgba(13,13,26,0.95)",
        backdropFilter: "blur(12px)", border: "1px solid #1e1e30",
        borderRadius: 999, padding: "6px 10px",
      }}>
        {([
          ["home", "Home"],
          ["result-ok", "✓ OK"],
          ["result-denied", "✕ Denied"],
          ["today", "Today"],
          ["stats", "Stats"],
        ] as [Screen, string][]).map(([s, label]) => (
          <button
            key={s}
            onClick={() => setScreen(s)}
            style={{
              padding: "5px 12px", borderRadius: 999, border: "none", cursor: "pointer",
              fontSize: 11, fontWeight: 600,
              background: screen === s ? C.accent : "transparent",
              color: screen === s ? C.white : C.muted,
              transition: "all 0.15s",
            }}
          >
            {label}
          </button>
        ))}
      </div>
    </div>
  );
}
