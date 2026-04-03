<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Finance Dashboard Backend — README</title>
<link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;600;700&family=Sora:wght@300;400;600;700;800&display=swap" rel="stylesheet">
<style>
  :root {
    --bg: #0a0d14;
    --surface: #111827;
    --surface2: #1a2335;
    --border: #1e2d45;
    --accent: #00d4ff;
    --accent2: #7c3aed;
    --accent3: #10b981;
    --accent4: #f59e0b;
    --text: #e2e8f0;
    --muted: #64748b;
    --danger: #ef4444;
    --admin: #f59e0b;
    --analyst: #7c3aed;
    --viewer: #10b981;
  }

  * { box-sizing: border-box; margin: 0; padding: 0; }

  body {
    background: var(--bg);
    color: var(--text);
    font-family: 'Sora', sans-serif;
    line-height: 1.7;
    font-size: 15px;
  }

  /* ── HERO ── */
  .hero {
    position: relative;
    overflow: hidden;
    padding: 80px 40px 60px;
    text-align: center;
    background: linear-gradient(160deg, #050810 0%, #0d1b2e 50%, #0a0d14 100%);
    border-bottom: 1px solid var(--border);
  }
  .hero::before {
    content: '';
    position: absolute; inset: 0;
    background: radial-gradient(ellipse 60% 40% at 50% 0%, rgba(0,212,255,.12), transparent),
                radial-gradient(ellipse 40% 60% at 80% 80%, rgba(124,58,237,.08), transparent);
    pointer-events: none;
  }
  .hero-grid {
    position: absolute; inset: 0;
    background-image: linear-gradient(rgba(0,212,255,.04) 1px, transparent 1px),
                      linear-gradient(90deg, rgba(0,212,255,.04) 1px, transparent 1px);
    background-size: 40px 40px;
  }
  .badge-row { display: flex; gap: 8px; justify-content: center; flex-wrap: wrap; margin-bottom: 28px; }
  .badge {
    font-family: 'JetBrains Mono', monospace;
    font-size: 11px;
    font-weight: 600;
    padding: 4px 12px;
    border-radius: 4px;
    letter-spacing: .5px;
    text-transform: uppercase;
  }
  .badge-java { background: rgba(234,179,8,.15); color: #facc15; border: 1px solid rgba(234,179,8,.3); }
  .badge-spring { background: rgba(16,185,129,.15); color: #34d399; border: 1px solid rgba(16,185,129,.3); }
  .badge-mongo { background: rgba(0,212,255,.12); color: var(--accent); border: 1px solid rgba(0,212,255,.3); }
  .badge-jwt { background: rgba(124,58,237,.15); color: #a78bfa; border: 1px solid rgba(124,58,237,.3); }
  .badge-test { background: rgba(239,68,68,.12); color: #f87171; border: 1px solid rgba(239,68,68,.25); }

  .hero h1 {
    font-size: 48px;
    font-weight: 800;
    letter-spacing: -1.5px;
    background: linear-gradient(135deg, #fff 0%, var(--accent) 60%, #a78bfa 100%);
    -webkit-background-clip: text; -webkit-text-fill-color: transparent;
    margin-bottom: 16px;
    position: relative;
  }
  .hero p {
    font-size: 16px;
    color: var(--muted);
    max-width: 620px;
    margin: 0 auto 32px;
    font-weight: 300;
    letter-spacing: .2px;
  }
  .stat-row {
    display: flex; gap: 32px; justify-content: center; flex-wrap: wrap;
    position: relative;
  }
  .stat { text-align: center; }
  .stat-num { font-size: 28px; font-weight: 800; color: var(--accent); font-family: 'JetBrains Mono', monospace; }
  .stat-label { font-size: 11px; text-transform: uppercase; letter-spacing: 1px; color: var(--muted); }

  /* ── LAYOUT ── */
  .container { max-width: 1100px; margin: 0 auto; padding: 0 32px; }

  .toc {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 12px;
    padding: 28px 32px;
    margin: 40px auto;
    max-width: 1100px;
    margin-left: 32px;
    margin-right: 32px;
  }
  .toc h3 { font-size: 12px; letter-spacing: 2px; text-transform: uppercase; color: var(--muted); margin-bottom: 16px; }
  .toc-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 8px; }
  .toc a {
    font-family: 'JetBrains Mono', monospace;
    font-size: 12px;
    color: var(--accent);
    text-decoration: none;
    padding: 6px 10px;
    border-radius: 6px;
    border: 1px solid transparent;
    display: block;
    transition: all .2s;
  }
  .toc a:hover { border-color: var(--border); background: var(--surface2); color: #fff; }

  /* ── SECTIONS ── */
  section { padding: 60px 32px; max-width: 1100px; margin: 0 auto; }
  section + section { border-top: 1px solid var(--border); padding-top: 60px; }

  .section-label {
    font-size: 11px;
    letter-spacing: 2.5px;
    text-transform: uppercase;
    color: var(--accent);
    font-family: 'JetBrains Mono', monospace;
    margin-bottom: 8px;
    font-weight: 600;
  }
  h2 {
    font-size: 32px;
    font-weight: 700;
    letter-spacing: -0.5px;
    color: #fff;
    margin-bottom: 24px;
  }
  h3 {
    font-size: 18px;
    font-weight: 600;
    color: #fff;
    margin: 32px 0 14px;
  }
  p { color: #94a3b8; margin-bottom: 16px; }
  code {
    font-family: 'JetBrains Mono', monospace;
    font-size: 13px;
    background: rgba(0,212,255,.08);
    border: 1px solid rgba(0,212,255,.15);
    color: var(--accent);
    padding: 1px 6px;
    border-radius: 4px;
  }
  pre {
    font-family: 'JetBrains Mono', monospace;
    font-size: 13px;
    background: #070c15;
    border: 1px solid var(--border);
    border-radius: 10px;
    padding: 20px 24px;
    overflow-x: auto;
    color: #a8d5e2;
    line-height: 1.6;
    margin: 16px 0;
  }
  pre .kw { color: #7c9fff; }
  pre .str { color: #98d5a8; }
  pre .com { color: #4b5e78; font-style: italic; }
  pre .num { color: var(--accent4); }
  pre .cls { color: #e2b96e; }

  /* ── DIAGRAM WRAPPER ── */
  .diagram-wrap {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 16px;
    padding: 32px;
    margin: 24px 0;
    overflow-x: auto;
  }
  .diagram-wrap svg { display: block; margin: 0 auto; }

  /* ── TECH TABLE ── */
  .tech-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 14px;
    margin: 24px 0;
  }
  .tech-card {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 10px;
    padding: 16px 20px;
    display: flex;
    align-items: flex-start;
    gap: 14px;
    transition: border-color .2s, transform .2s;
  }
  .tech-card:hover { border-color: var(--accent); transform: translateY(-2px); }
  .tech-icon {
    width: 36px; height: 36px;
    border-radius: 8px;
    display: flex; align-items: center; justify-content: center;
    font-size: 18px;
    flex-shrink: 0;
  }
  .tech-card h4 { font-size: 14px; font-weight: 700; color: #fff; margin-bottom: 4px; }
  .tech-card p { font-size: 12px; color: var(--muted); margin: 0; }

  /* ── ROLE MATRIX ── */
  .matrix-table {
    width: 100%;
    border-collapse: collapse;
    font-family: 'JetBrains Mono', monospace;
    font-size: 13px;
    margin: 24px 0;
  }
  .matrix-table th {
    padding: 12px 16px;
    text-align: left;
    font-size: 11px;
    letter-spacing: 1.5px;
    text-transform: uppercase;
    color: var(--muted);
    border-bottom: 1px solid var(--border);
    background: var(--surface);
  }
  .matrix-table td {
    padding: 11px 16px;
    border-bottom: 1px solid rgba(30,45,69,.6);
    color: #94a3b8;
  }
  .matrix-table tr:hover td { background: rgba(0,212,255,.03); }
  .matrix-table .action { color: #e2e8f0; font-weight: 600; }
  .check { color: #10b981; font-size: 16px; }
  .cross { color: #374151; font-size: 16px; }
  .col-admin { color: var(--admin); }
  .col-analyst { color: #a78bfa; }
  .col-viewer { color: var(--viewer); }

  /* ── ENDPOINT TABLE ── */
  .endpoint-table { width: 100%; border-collapse: collapse; margin: 20px 0; font-size: 13px; }
  .endpoint-table th {
    padding: 10px 14px;
    background: var(--surface2);
    font-size: 11px;
    letter-spacing: 1px;
    text-transform: uppercase;
    color: var(--muted);
    text-align: left;
    border-bottom: 1px solid var(--border);
  }
  .endpoint-table td {
    padding: 11px 14px;
    border-bottom: 1px solid rgba(30,45,69,.5);
    color: #94a3b8;
  }
  .method {
    font-family: 'JetBrains Mono', monospace;
    font-size: 11px;
    font-weight: 700;
    padding: 3px 8px;
    border-radius: 4px;
    display: inline-block;
  }
  .GET { background: rgba(16,185,129,.15); color: #34d399; }
  .POST { background: rgba(0,212,255,.12); color: var(--accent); }
  .PUT { background: rgba(245,158,11,.12); color: #fbbf24; }
  .PATCH { background: rgba(124,58,237,.15); color: #a78bfa; }
  .DELETE { background: rgba(239,68,68,.12); color: #f87171; }
  .role-pill {
    font-size: 10px;
    font-family: 'JetBrains Mono', monospace;
    font-weight: 700;
    padding: 2px 8px;
    border-radius: 20px;
    display: inline-block;
    letter-spacing: .5px;
    margin: 2px;
  }
  .rp-admin { background: rgba(245,158,11,.15); color: #fbbf24; border: 1px solid rgba(245,158,11,.3); }
  .rp-analyst { background: rgba(124,58,237,.15); color: #a78bfa; border: 1px solid rgba(124,58,237,.3); }
  .rp-viewer { background: rgba(16,185,129,.12); color: #34d399; border: 1px solid rgba(16,185,129,.3); }
  .rp-open { background: rgba(100,116,139,.12); color: #94a3b8; border: 1px solid rgba(100,116,139,.3); }

  /* ── CALLOUT CARDS ── */
  .callout-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; margin: 24px 0; }
  .callout {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 12px;
    padding: 20px 22px;
    border-left: 3px solid;
  }
  .callout.blue { border-left-color: var(--accent); }
  .callout.purple { border-left-color: #7c3aed; }
  .callout.green { border-left-color: var(--accent3); }
  .callout.amber { border-left-color: var(--accent4); }
  .callout.red { border-left-color: var(--danger); }
  .callout h4 { font-size: 14px; font-weight: 700; color: #fff; margin-bottom: 8px; }
  .callout p { font-size: 13px; color: var(--muted); margin: 0; }

  /* ── ENV TABLE ── */
  .env-table { width: 100%; border-collapse: collapse; margin: 16px 0; font-size: 13px; }
  .env-table td {
    padding: 10px 14px;
    border-bottom: 1px solid var(--border);
    font-family: 'JetBrains Mono', monospace;
  }
  .env-table td:first-child { color: var(--accent); width: 240px; }
  .env-table td:nth-child(2) { color: #94a3b8; font-family: 'Sora', sans-serif; }
  .env-table td:last-child { color: #64748b; font-size: 12px; }

  /* ── STRUCTURE DIAGRAM ── */
  .dir-tree {
    background: #070c15;
    border: 1px solid var(--border);
    border-radius: 10px;
    padding: 24px;
    font-family: 'JetBrains Mono', monospace;
    font-size: 13px;
    line-height: 1.8;
    color: #94a3b8;
  }
  .dir-tree .folder { color: var(--accent); font-weight: 600; }
  .dir-tree .file { color: #64748b; }
  .dir-tree .comment { color: #4b5e78; font-size: 11px; margin-left: 8px; }

  /* ── FOOTER ── */
  footer {
    text-align: center;
    padding: 48px 32px;
    border-top: 1px solid var(--border);
    color: var(--muted);
    font-size: 13px;
    font-family: 'JetBrains Mono', monospace;
  }
  footer a { color: var(--accent); text-decoration: none; }

  /* ── TABS (for API sections) ── */
  .tabs { display: flex; gap: 4px; margin-bottom: 0; flex-wrap: wrap; }
  .tab-btn {
    font-family: 'JetBrains Mono', monospace;
    font-size: 12px;
    padding: 8px 16px;
    border: 1px solid var(--border);
    border-bottom: none;
    background: var(--surface);
    color: var(--muted);
    cursor: pointer;
    border-radius: 8px 8px 0 0;
    transition: all .2s;
    font-weight: 600;
  }
  .tab-btn.active { background: var(--surface2); color: var(--accent); border-color: var(--accent); }
  .tab-content {
    border: 1px solid var(--border);
    border-radius: 0 8px 8px 8px;
    background: var(--surface2);
    padding: 24px;
    display: none;
  }
  .tab-content.active { display: block; }

  /* scrollbar */
  ::-webkit-scrollbar { width: 6px; height: 6px; }
  ::-webkit-scrollbar-track { background: var(--bg); }
  ::-webkit-scrollbar-thumb { background: var(--border); border-radius: 3px; }
</style>
</head>
<body>

<!-- ══════════════════════════ HERO ══════════════════════════ -->
<div class="hero">
  <div class="hero-grid"></div>
  <div class="badge-row">
    <span class="badge badge-java">Java 17</span>
    <span class="badge badge-spring">Spring Boot 3</span>
    <span class="badge badge-mongo">MongoDB</span>
    <span class="badge badge-jwt">JWT Auth</span>
    <span class="badge badge-test">18 Tests</span>
  </div>
  <h1>Finance Dashboard Backend</h1>
  <p>Production-structured REST API with role-based access control, JWT authentication, MongoDB aggregation pipelines, and strict dual-layer security enforcement.</p>
  <div class="stat-row">
    <div class="stat"><div class="stat-num">3</div><div class="stat-label">User Roles</div></div>
    <div class="stat"><div class="stat-num">18</div><div class="stat-label">Unit Tests</div></div>
    <div class="stat"><div class="stat-num">8</div><div class="stat-label">Filter Paths</div></div>
    <div class="stat"><div class="stat-num">6</div><div class="stat-label">Dashboard KPIs</div></div>
    <div class="stat"><div class="stat-num">2</div><div class="stat-label">Security Layers</div></div>
  </div>
</div>

<!-- ══════════════════════════ TOC ══════════════════════════ -->
<div class="toc">
  <h3>Table of Contents</h3>
  <div class="toc-grid">
    <a href="#stack">Tech Stack</a>
    <a href="#architecture">Architecture Overview</a>
    <a href="#structure">Project Structure</a>
    <a href="#datamodel">Data Model</a>
    <a href="#flows">Process Flows</a>
    <a href="#access">Access Control</a>
    <a href="#endpoints">API Reference</a>
    <a href="#setup">Setup & Running</a>
    <a href="#tests">Unit Tests</a>
    <a href="#tradeoffs">Tradeoffs</a>
  </div>
</div>

<!-- ══════════════════════════ TECH STACK ══════════════════════════ -->
<section id="stack">
  <div class="section-label">01 — Foundation</div>
  <h2>Tech Stack</h2>
  <div class="tech-grid">
    <div class="tech-card">
      <div class="tech-icon" style="background:rgba(234,179,8,.12);">☕</div>
      <div><h4>Java 17</h4><p>LTS release with strong typing — perfect for structured, maintainable backend services.</p></div>
    </div>
    <div class="tech-card">
      <div class="tech-icon" style="background:rgba(16,185,129,.12);">🍃</div>
      <div><h4>Spring Boot 3</h4><p>Fast setup, excellent REST ecosystem. Security, validation, and auditing all built-in.</p></div>
    </div>
    <div class="tech-card">
      <div class="tech-icon" style="background:rgba(0,212,255,.1);">🍃</div>
      <div><h4>MongoDB</h4><p>Flexible document model for financial entries that evolve in structure. Unique indexes enforced at DB level.</p></div>
    </div>
    <div class="tech-card">
      <div class="tech-icon" style="background:rgba(124,58,237,.12);">🔐</div>
      <div><h4>Spring Security + JWT</h4><p>JJWT library. Tokens carry userId and role. 24-hour expiry. ThreadLocal via AuthContext.</p></div>
    </div>
    <div class="tech-card">
      <div class="tech-icon" style="background:rgba(239,68,68,.1);">🧪</div>
      <div><h4>JUnit 5 + Mockito</h4><p>18 service-layer unit tests. Repositories mocked so tests run without a live DB instance.</p></div>
    </div>
    <div class="tech-card">
      <div class="tech-icon" style="background:rgba(245,158,11,.1);">🏗️</div>
      <div><h4>Lombok + Jakarta BV</h4><p>@Data, @RequiredArgsConstructor eliminate boilerplate. @Valid + @NotNull for declarative validation.</p></div>
    </div>
  </div>
</section>

<!-- ══════════════════════════ ARCHITECTURE ══════════════════════════ -->
<section id="architecture">
  <div class="section-label">02 — Design</div>
  <h2>Architecture Overview</h2>
  <p>Dual-layer security is a deliberate design choice. <code>@PreAuthorize</code> enforces roles at the HTTP boundary; <code>resolveCaller()</code> + <code>assertAdmin()</code> enforce them again at the domain level — so even if the filter were misconfigured, service logic still rejects unauthorized calls.</p>

  <div class="diagram-wrap">
    <svg viewBox="0 0 900 520" xmlns="http://www.w3.org/2000/svg" width="860">
      <defs>
        <linearGradient id="gAccent" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" style="stop-color:#00d4ff;stop-opacity:1"/>
          <stop offset="100%" style="stop-color:#7c3aed;stop-opacity:1"/>
        </linearGradient>
        <linearGradient id="gGreen" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" style="stop-color:#10b981;stop-opacity:1"/>
          <stop offset="100%" style="stop-color:#00d4ff;stop-opacity:1"/>
        </linearGradient>
        <filter id="glow">
          <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
          <feMerge><feMergeNode in="coloredBlur"/><feMergeNode in="SourceGraphic"/></feMerge>
        </filter>
        <marker id="arrowBlue" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
          <path d="M0,0 L0,6 L8,3 z" fill="#00d4ff" opacity=".8"/>
        </marker>
        <marker id="arrowGray" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
          <path d="M0,0 L0,6 L8,3 z" fill="#1e2d45"/>
        </marker>
      </defs>

      <!-- background grid subtle -->
      <rect width="900" height="520" fill="#070c15" rx="12"/>
      <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
        <path d="M 40 0 L 0 0 0 40" fill="none" stroke="rgba(0,212,255,.04)" stroke-width="1"/>
      </pattern>
      <rect width="900" height="520" fill="url(#grid)" rx="12"/>

      <!-- CLIENT -->
      <rect x="30" y="210" width="120" height="50" rx="8" fill="#0d1b2e" stroke="#1e2d45" stroke-width="1.5"/>
      <text x="90" y="230" text-anchor="middle" fill="#94a3b8" font-family="JetBrains Mono" font-size="10" font-weight="600">CLIENT</text>
      <text x="90" y="248" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">Postman / HTTP</text>

      <!-- Arrow: Client → AuthFilter -->
      <line x1="150" y1="235" x2="200" y2="235" stroke="#1e2d45" stroke-width="1.5" marker-end="url(#arrowGray)"/>
      <text x="175" y="228" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="9">JWT Bearer</text>

      <!-- LAYER 1: Auth Filter -->
      <rect x="200" y="160" width="155" height="150" rx="10" fill="#0d1422" stroke="url(#gAccent)" stroke-width="1.5"/>
      <text x="277" y="182" text-anchor="middle" fill="#00d4ff" font-family="JetBrains Mono" font-size="10" font-weight="700" filter="url(#glow)">AUTH FILTER</text>
      <line x1="215" y1="190" x2="340" y2="190" stroke="#1e2d45" stroke-width="1"/>
      <text x="277" y="208" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">Rate Limit (100/IP)</text>
      <text x="277" y="223" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">Validate JWT token</text>
      <text x="277" y="238" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">Extract userId + role</text>
      <text x="277" y="253" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">Set SecurityContext</text>
      <text x="277" y="268" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">AuthContext (ThreadLocal)</text>
      <text x="277" y="295" text-anchor="middle" fill="#ef4444" font-family="JetBrains Mono" font-size="9">→ 429 if rate exceeded</text>

      <!-- Arrow: AuthFilter → Controller -->
      <line x1="355" y1="235" x2="405" y2="235" stroke="#1e2d45" stroke-width="1.5" marker-end="url(#arrowGray)"/>

      <!-- LAYER 2: Controller -->
      <rect x="405" y="185" width="140" height="100" rx="10" fill="#0d1422" stroke="#1e2d45" stroke-width="1.5"/>
      <text x="475" y="206" text-anchor="middle" fill="#e2e8f0" font-family="JetBrains Mono" font-size="10" font-weight="700">CONTROLLER</text>
      <line x1="418" y1="214" x2="532" y2="214" stroke="#1e2d45" stroke-width="1"/>
      <text x="475" y="230" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">Route mapping</text>
      <text x="475" y="245" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">@PreAuthorize</text>
      <text x="475" y="260" text-anchor="middle" fill="#7c3aed" font-family="JetBrains Mono" font-size="9">LAYER 1 SECURITY</text>
      <text x="475" y="273" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">@Valid on body</text>

      <!-- Arrow: Controller → Service -->
      <line x1="545" y1="235" x2="600" y2="235" stroke="#1e2d45" stroke-width="1.5" marker-end="url(#arrowGray)"/>

      <!-- LAYER 3: Service -->
      <rect x="600" y="155" width="155" height="180" rx="10" fill="#0d1422" stroke="#7c3aed" stroke-width="1.5"/>
      <text x="677" y="176" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="10" font-weight="700" filter="url(#glow)">SERVICE</text>
      <line x1="615" y1="184" x2="740" y2="184" stroke="#1e2d45" stroke-width="1"/>
      <text x="677" y="201" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">resolveCaller()</text>
      <text x="677" y="216" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">assertAdmin()</text>
      <text x="677" y="231" text-anchor="middle" fill="#7c3aed" font-family="JetBrains Mono" font-size="9">LAYER 2 SECURITY</text>
      <text x="677" y="248" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">isActive() check</text>
      <text x="677" y="263" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">Business logic</text>
      <text x="677" y="278" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">Soft delete mgmt</text>
      <text x="677" y="293" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">Aggregation calls</text>
      <text x="677" y="320" text-anchor="middle" fill="#ef4444" font-family="JetBrains Mono" font-size="9">→ 401/403 if denied</text>

      <!-- Arrow: Service → Repository -->
      <line x1="755" y1="235" x2="800" y2="235" stroke="#1e2d45" stroke-width="1.5" marker-end="url(#arrowGray)"/>

      <!-- LAYER 4: Repository / MongoDB -->
      <rect x="800" y="190" width="85" height="90" rx="10" fill="#0d1422" stroke="#10b981" stroke-width="1.5"/>
      <text x="842" y="213" text-anchor="middle" fill="#34d399" font-family="JetBrains Mono" font-size="9" font-weight="700">MONGODB</text>
      <line x1="810" y1="220" x2="875" y2="220" stroke="#1e2d45" stroke-width="1"/>
      <text x="842" y="235" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">Spring Data</text>
      <text x="842" y="248" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">MongoTemplate</text>
      <text x="842" y="262" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">Aggregation</text>

      <!-- Global Exception Handler bubble -->
      <rect x="200" y="380" width="530" height="60" rx="10" fill="#150d0d" stroke="#ef4444" stroke-width="1.5" stroke-dasharray="5,3"/>
      <text x="465" y="404" text-anchor="middle" fill="#f87171" font-family="JetBrains Mono" font-size="10" font-weight="700">GLOBAL EXCEPTION HANDLER  (@ControllerAdvice)</text>
      <text x="465" y="424" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">400 ValidationError  |  401 Unauthorized  |  403 Forbidden  |  404 NotFound  |  429 RateLimit  |  500 Fallback</text>

      <!-- dashed lines from layers down to exception handler -->
      <line x1="277" y1="310" x2="277" y2="380" stroke="#ef4444" stroke-width="1" stroke-dasharray="4,3" opacity=".4"/>
      <line x1="475" y1="285" x2="475" y2="380" stroke="#ef4444" stroke-width="1" stroke-dasharray="4,3" opacity=".4"/>
      <line x1="677" y1="335" x2="677" y2="380" stroke="#ef4444" stroke-width="1" stroke-dasharray="4,3" opacity=".4"/>

      <!-- Legend -->
      <text x="30" y="470" fill="#4b5e78" font-family="JetBrains Mono" font-size="9">■</text>
      <rect x="30" y="462" width="10" height="10" fill="#00d4ff" opacity=".6" rx="2"/>
      <text x="46" y="471" fill="#64748b" font-family="JetBrains Mono" font-size="9">Layer 1: HTTP Security</text>
      <rect x="165" y="462" width="10" height="10" fill="#7c3aed" opacity=".6" rx="2"/>
      <text x="181" y="471" fill="#64748b" font-family="JetBrains Mono" font-size="9">Layer 2: Domain Security</text>
      <rect x="310" y="462" width="10" height="10" fill="#10b981" opacity=".6" rx="2"/>
      <text x="326" y="471" fill="#64748b" font-family="JetBrains Mono" font-size="9">Data Layer</text>
      <rect x="395" y="462" width="10" height="10" fill="#ef4444" opacity=".6" rx="2"/>
      <text x="411" y="471" fill="#64748b" font-family="JetBrains Mono" font-size="9">Error Boundary</text>

      <!-- Title -->
      <text x="450" y="30" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="11" letter-spacing="2">REQUEST LIFECYCLE — DUAL SECURITY ARCHITECTURE</text>
    </svg>
  </div>
</section>

<!-- ══════════════════════════ PROJECT STRUCTURE ══════════════════════════ -->
<section id="structure">
  <div class="section-label">03 — Codebase</div>
  <h2>Project Structure</h2>
  <div class="dir-tree">
<span class="folder">src/main/java/com/zorvyn/finance/</span>
│
├── <span class="folder">controller/</span>          <span class="comment">HTTP layer — routes, delegates to services, @PreAuthorize gates</span>
│   ├── <span class="file">AuthController.java</span>            <span class="comment">POST /api/auth/login</span>
│   ├── <span class="file">UserController.java</span>            <span class="comment">CRUD for users, ADMIN-guarded</span>
│   ├── <span class="file">FinancialRecordController.java</span> <span class="comment">CRUD + filter + pagination for records</span>
│   └── <span class="file">DashboardController.java</span>       <span class="comment">GET /api/dashboard/summary (all roles)</span>
│
├── <span class="folder">service/</span>             <span class="comment">Business logic, role enforcement, aggregation</span>
│   ├── <span class="file">UserService.java</span>               <span class="comment">resolveCaller(), assertAdmin(), createUser()</span>
│   ├── <span class="file">FinancialRecordService.java</span>    <span class="comment">filterRecords(), soft delete, pagination</span>
│   └── <span class="file">DashboardService.java</span>          <span class="comment">MongoDB aggregation pipelines</span>
│
├── <span class="folder">repository/</span>          <span class="comment">MongoDB data access — Spring Data derived queries</span>
│   ├── <span class="file">UserRepository.java</span>
│   └── <span class="file">FinancialRecordRepository.java</span> <span class="comment">extends MongoRepository + CustomRepository</span>
│
├── <span class="folder">entity/</span>              <span class="comment">MongoDB document models with Spring Auditing</span>
│   ├── <span class="file">User.java</span>                      <span class="comment">@Document, @Indexed(unique) on email</span>
│   ├── <span class="file">FinancialRecord.java</span>           <span class="comment">@JsonIgnore on deleted field</span>
│   ├── <span class="file">Role.java</span>                      <span class="comment">enum: VIEWER | ANALYST | ADMIN</span>
│   └── <span class="file">RecordType.java</span>               <span class="comment">enum: INCOME | EXPENSE</span>
│
├── <span class="folder">security/</span>            <span class="comment">JWT auth mechanism</span>
│   ├── <span class="file">AuthFilter.java</span>               <span class="comment">Rate limit + JWT parse + SecurityContext + ThreadLocal</span>
│   ├── <span class="file">AuthContext.java</span>              <span class="comment">ThreadLocal&lt;String&gt; — holds userId per request</span>
│   └── <span class="file">JwtUtil.java</span>                  <span class="comment">generateToken(), extractUserId(), extractRole()</span>
│
├── <span class="folder">exception/</span>           <span class="comment">Custom exceptions + global error handler</span>
│   ├── <span class="file">GlobalExceptionHandler.java</span>   <span class="comment">@ControllerAdvice — maps all exceptions to HTTP</span>
│   ├── <span class="file">AccessDeniedException.java</span>    <span class="comment">→ 403</span>
│   ├── <span class="file">UnauthorizedException.java</span>    <span class="comment">→ 401</span>
│   └── <span class="file">ResourceNotFoundException.java</span><span class="comment">→ 404</span>
│
└── <span class="file">FinanceApplication.java</span>        <span class="comment">@SpringBootApplication + @EnableMongoAuditing</span>
  </div>
</section>

<!-- ══════════════════════════ DATA MODEL ══════════════════════════ -->
<section id="datamodel">
  <div class="section-label">04 — Data Model</div>
  <h2>Database Schema</h2>
  <p>Two MongoDB collections. Relationships managed in application logic rather than database-level constraints.</p>

  <div class="diagram-wrap">
    <svg viewBox="0 0 820 340" xmlns="http://www.w3.org/2000/svg" width="780">
      <rect width="820" height="340" fill="#070c15" rx="12"/>
      <pattern id="grid2" width="40" height="40" patternUnits="userSpaceOnUse">
        <path d="M 40 0 L 0 0 0 40" fill="none" stroke="rgba(0,212,255,.03)" stroke-width="1"/>
      </pattern>
      <rect width="820" height="340" fill="url(#grid2)" rx="12"/>

      <!-- USERS collection -->
      <rect x="60" y="40" width="260" height="255" rx="10" fill="#0d1422" stroke="url(#gAccent)" stroke-width="1.5"/>
      <rect x="60" y="40" width="260" height="36" rx="10" fill="rgba(0,212,255,.08)"/>
      <rect x="60" y="64" width="260" height="12" rx="0" fill="rgba(0,212,255,.08)"/>
      <text x="190" y="63" text-anchor="middle" fill="#00d4ff" font-family="JetBrains Mono" font-size="12" font-weight="700">users</text>
      <text x="248" y="63" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="9">collection</text>

      <!-- users fields -->
      <text x="80" y="100" fill="#f59e0b" font-family="JetBrains Mono" font-size="11">_id</text>
      <text x="200" y="100" fill="#64748b" font-family="JetBrains Mono" font-size="11">ObjectId  PK</text>

      <text x="80" y="122" fill="#94a3b8" font-family="JetBrains Mono" font-size="11">name</text>
      <text x="200" y="122" fill="#64748b" font-family="JetBrains Mono" font-size="11">String  required</text>

      <text x="80" y="144" fill="#94a3b8" font-family="JetBrains Mono" font-size="11">email</text>
      <text x="200" y="144" fill="#64748b" font-family="JetBrains Mono" font-size="11">String  unique ★</text>

      <text x="80" y="166" fill="#94a3b8" font-family="JetBrains Mono" font-size="11">password</text>
      <text x="200" y="166" fill="#64748b" font-family="JetBrains Mono" font-size="11">String  BCrypt</text>

      <text x="80" y="188" fill="#10b981" font-family="JetBrains Mono" font-size="11">role</text>
      <text x="200" y="188" fill="#64748b" font-family="JetBrains Mono" font-size="11">VIEWER|ANALYST|ADMIN</text>

      <text x="80" y="210" fill="#94a3b8" font-family="JetBrains Mono" font-size="11">active</text>
      <text x="200" y="210" fill="#64748b" font-family="JetBrains Mono" font-size="11">Boolean  default: true</text>

      <text x="80" y="232" fill="#94a3b8" font-family="JetBrains Mono" font-size="11">createdAt</text>
      <text x="200" y="232" fill="#64748b" font-family="JetBrains Mono" font-size="11">@CreatedDate</text>

      <text x="80" y="254" fill="#94a3b8" font-family="JetBrains Mono" font-size="11">updatedAt</text>
      <text x="200" y="254" fill="#64748b" font-family="JetBrains Mono" font-size="11">@LastModifiedDate</text>

      <!-- line -->
      <line x1="80" y1="90" x2="305" y2="90" stroke="#1e2d45" stroke-width="1"/>

      <!-- RECORDS collection -->
      <rect x="500" y="40" width="265" height="275" rx="10" fill="#0d1422" stroke="#7c3aed" stroke-width="1.5"/>
      <rect x="500" y="40" width="265" height="36" rx="10" fill="rgba(124,58,237,.08)"/>
      <rect x="500" y="64" width="265" height="12" rx="0" fill="rgba(124,58,237,.08)"/>
      <text x="632" y="63" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="12" font-weight="700">records</text>
      <text x="718" y="63" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="9">collection</text>

      <text x="520" y="100" fill="#f59e0b" font-family="JetBrains Mono" font-size="11">_id</text>
      <text x="630" y="100" fill="#64748b" font-family="JetBrains Mono" font-size="11">ObjectId  PK</text>

      <text x="520" y="122" fill="#94a3b8" font-family="JetBrains Mono" font-size="11">amount</text>
      <text x="630" y="122" fill="#64748b" font-family="JetBrains Mono" font-size="11">Double  @Positive</text>

      <text x="520" y="144" fill="#10b981" font-family="JetBrains Mono" font-size="11">type</text>
      <text x="630" y="144" fill="#64748b" font-family="JetBrains Mono" font-size="11">INCOME | EXPENSE</text>

      <text x="520" y="166" fill="#94a3b8" font-family="JetBrains Mono" font-size="11">category</text>
      <text x="630" y="166" fill="#64748b" font-family="JetBrains Mono" font-size="11">String  required</text>

      <text x="520" y="188" fill="#94a3b8" font-family="JetBrains Mono" font-size="11">date</text>
      <text x="630" y="188" fill="#64748b" font-family="JetBrains Mono" font-size="11">LocalDateTime  optional</text>

      <text x="520" y="210" fill="#94a3b8" font-family="JetBrains Mono" font-size="11">notes</text>
      <text x="630" y="210" fill="#64748b" font-family="JetBrains Mono" font-size="11">String  optional</text>

      <text x="520" y="232" fill="#00d4ff" font-family="JetBrains Mono" font-size="11">userId</text>
      <text x="630" y="232" fill="#64748b" font-family="JetBrains Mono" font-size="11">String  FK → users._id</text>

      <text x="520" y="254" fill="#ef4444" font-family="JetBrains Mono" font-size="11">deleted</text>
      <text x="630" y="254" fill="#64748b" font-family="JetBrains Mono" font-size="11">Boolean  @JsonIgnore</text>

      <text x="520" y="276" fill="#94a3b8" font-family="JetBrains Mono" font-size="11">createdAt</text>
      <text x="630" y="276" fill="#64748b" font-family="JetBrains Mono" font-size="11">@CreatedDate</text>

      <text x="520" y="298" fill="#94a3b8" font-family="JetBrains Mono" font-size="11">updatedAt</text>
      <text x="630" y="298" fill="#64748b" font-family="JetBrains Mono" font-size="11">@LastModifiedDate</text>

      <line x1="520" y1="90" x2="750" y2="90" stroke="#1e2d45" stroke-width="1"/>

      <!-- Relation line -->
      <line x1="320" y1="165" x2="500" y2="232" stroke="#00d4ff" stroke-width="1.5" stroke-dasharray="6,4" opacity=".5" marker-end="url(#arrowBlue)"/>
      <rect x="355" y="185" width="100" height="22" rx="5" fill="#0a0d14"/>
      <text x="405" y="199" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="9">1  ──────  many</text>

      <!-- annotation -->
      <text x="410" y="310" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="9">★ unique index enforced at MongoDB level + service layer check</text>
    </svg>
  </div>
</section>

<!-- ══════════════════════════ PROCESS FLOWS ══════════════════════════ -->
<section id="flows">
  <div class="section-label">05 — Process Flows</div>
  <h2>End-to-End Request Flows</h2>

  <!-- AUTH FLOW -->
  <h3>Authentication Flow</h3>
  <div class="diagram-wrap">
    <svg viewBox="0 0 820 200" xmlns="http://www.w3.org/2000/svg" width="780">
      <rect width="820" height="200" fill="#070c15" rx="10"/>

      <!-- Steps -->
      <!-- Step boxes -->
      <rect x="20" y="70" width="110" height="60" rx="8" fill="#0d1422" stroke="#1e2d45" stroke-width="1.5"/>
      <text x="75" y="97" text-anchor="middle" fill="#94a3b8" font-family="JetBrains Mono" font-size="9" font-weight="700">POST</text>
      <text x="75" y="111" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">/api/auth/login</text>
      <text x="75" y="123" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="8">{email, password}</text>

      <line x1="130" y1="100" x2="165" y2="100" stroke="#1e2d45" stroke-width="1.5" marker-end="url(#arrowGray)"/>

      <rect x="165" y="70" width="130" height="60" rx="8" fill="#0d1422" stroke="#1e2d45" stroke-width="1.5"/>
      <text x="230" y="97" text-anchor="middle" fill="#94a3b8" font-family="JetBrains Mono" font-size="9" font-weight="700">Lookup user</text>
      <text x="230" y="111" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">findByEmail()</text>
      <text x="230" y="123" text-anchor="middle" fill="#ef4444" font-family="JetBrains Mono" font-size="8">404 if not found</text>

      <line x1="295" y1="100" x2="330" y2="100" stroke="#1e2d45" stroke-width="1.5" marker-end="url(#arrowGray)"/>

      <rect x="330" y="70" width="130" height="60" rx="8" fill="#0d1422" stroke="#1e2d45" stroke-width="1.5"/>
      <text x="395" y="97" text-anchor="middle" fill="#94a3b8" font-family="JetBrains Mono" font-size="9" font-weight="700">BCrypt.matches()</text>
      <text x="395" y="111" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">raw vs hashed</text>
      <text x="395" y="123" text-anchor="middle" fill="#ef4444" font-family="JetBrains Mono" font-size="8">401 if mismatch</text>

      <line x1="460" y1="100" x2="495" y2="100" stroke="#1e2d45" stroke-width="1.5" marker-end="url(#arrowGray)"/>

      <rect x="495" y="70" width="145" height="60" rx="8" fill="#0d1422" stroke="#7c3aed" stroke-width="1.5"/>
      <text x="567" y="92" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="9" font-weight="700">generateToken()</text>
      <text x="567" y="106" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="8">sub: userId</text>
      <text x="567" y="118" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="8">role: ADMIN|ANALYST|VIEWER</text>
      <text x="567" y="130" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="8">exp: +24h</text>

      <line x1="640" y1="100" x2="675" y2="100" stroke="url(#gAccent)" stroke-width="1.5" marker-end="url(#arrowBlue)"/>

      <rect x="675" y="75" width="120" height="50" rx="8" fill="rgba(0,212,255,.06)" stroke="url(#gAccent)" stroke-width="1.5"/>
      <text x="735" y="97" text-anchor="middle" fill="#00d4ff" font-family="JetBrains Mono" font-size="10" font-weight="700">200 OK</text>
      <text x="735" y="113" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">JWT token string</text>

      <!-- labels -->
      <text x="75" y="50" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="9">① Request</text>
      <text x="230" y="50" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="9">② DB Lookup</text>
      <text x="395" y="50" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="9">③ Verify Password</text>
      <text x="567" y="50" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="9">④ Sign JWT</text>
      <text x="735" y="50" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="9">⑤ Response</text>

      <!-- JWT payload note -->
      <text x="410" y="175" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="9">JWT payload: { sub: userId, role: ROLE, iat: timestamp, exp: now+86400000ms }</text>
    </svg>
  </div>

  <!-- FILTER FLOW -->
  <h3>Record Filter Logic — 8 Query Paths</h3>
  <div class="diagram-wrap">
    <svg viewBox="0 0 820 420" xmlns="http://www.w3.org/2000/svg" width="780">
      <rect width="820" height="420" fill="#070c15" rx="10"/>

      <!-- Entry -->
      <rect x="290" y="20" width="240" height="44" rx="8" fill="#0d1422" stroke="#00d4ff" stroke-width="1.5"/>
      <text x="410" y="39" text-anchor="middle" fill="#00d4ff" font-family="JetBrains Mono" font-size="10" font-weight="700">GET /api/records/filter</text>
      <text x="410" y="55" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">?type  ?category  ?from  ?to  ?search</text>

      <!-- Decision: search? -->
      <line x1="410" y1="64" x2="410" y2="98" stroke="#1e2d45" stroke-width="1.5"/>
      <polygon points="410,98 370,128 410,158 450,128" fill="#0d1422" stroke="#f59e0b" stroke-width="1.5"/>
      <text x="410" y="132" text-anchor="middle" fill="#f59e0b" font-family="JetBrains Mono" font-size="9" font-weight="700">search?</text>

      <!-- YES path -->
      <line x1="450" y1="128" x2="600" y2="128" stroke="#10b981" stroke-width="1.5" marker-end="url(#arrowBlue)"/>
      <text x="525" y="120" text-anchor="middle" fill="#10b981" font-family="JetBrains Mono" font-size="9">YES</text>
      <rect x="600" y="102" width="185" height="52" rx="8" fill="#0d1422" stroke="#10b981" stroke-width="1.5"/>
      <text x="692" y="124" text-anchor="middle" fill="#34d399" font-family="JetBrains Mono" font-size="9" font-weight="700">Regex Search</text>
      <text x="692" y="139" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="9">category + notes</text>
      <text x="692" y="152" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="8">case-insensitive</text>

      <!-- NO path -->
      <line x1="410" y1="158" x2="410" y2="192" stroke="#1e2d45" stroke-width="1.5"/>
      <text x="422" y="180" fill="#ef4444" font-family="JetBrains Mono" font-size="9">NO</text>

      <!-- Second decision: combinations -->
      <rect x="265" y="192" width="290" height="30" rx="6" fill="#0d1422" stroke="#1e2d45" stroke-width="1"/>
      <text x="410" y="211" text-anchor="middle" fill="#94a3b8" font-family="JetBrains Mono" font-size="9">Evaluate filter combination</text>

      <!-- 8 query branches -->
      <line x1="410" y1="222" x2="410" y2="245" stroke="#1e2d45" stroke-width="1.5"/>
      <!-- fan out -->
      <line x1="70" y1="245" x2="760" y2="245" stroke="#1e2d45" stroke-width="1.5"/>

      <!-- branch lines -->
      <line x1="70" y1="245" x2="70" y2="270" stroke="#1e2d45" stroke-width="1"/>
      <line x1="175" y1="245" x2="175" y2="270" stroke="#1e2d45" stroke-width="1"/>
      <line x1="280" y1="245" x2="280" y2="270" stroke="#1e2d45" stroke-width="1"/>
      <line x1="375" y1="245" x2="375" y2="270" stroke="#1e2d45" stroke-width="1"/>
      <line x1="460" y1="245" x2="460" y2="270" stroke="#1e2d45" stroke-width="1"/>
      <line x1="555" y1="245" x2="555" y2="270" stroke="#1e2d45" stroke-width="1"/>
      <line x1="650" y1="245" x2="650" y2="270" stroke="#1e2d45" stroke-width="1"/>
      <line x1="745" y1="245" x2="745" y2="270" stroke="#1e2d45" stroke-width="1"/>

      <!-- 8 query boxes -->
      <rect x="20" y="270" width="100" height="55" rx="6" fill="#0d1422" stroke="#1e2d45" stroke-width="1"/>
      <text x="70" y="288" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="8">type</text>
      <text x="70" y="300" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="8">+ category</text>
      <text x="70" y="312" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="8">+ dateRange</text>
      <text x="70" y="325" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="7">all 3</text>

      <rect x="125" y="270" width="100" height="55" rx="6" fill="#0d1422" stroke="#1e2d45" stroke-width="1"/>
      <text x="175" y="291" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="8">type</text>
      <text x="175" y="305" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="8">+ dateRange</text>
      <text x="175" y="319" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="7">no category</text>

      <rect x="230" y="270" width="100" height="55" rx="6" fill="#0d1422" stroke="#1e2d45" stroke-width="1"/>
      <text x="280" y="291" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="8">category</text>
      <text x="280" y="305" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="8">+ dateRange</text>
      <text x="280" y="319" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="7">no type</text>

      <rect x="325" y="270" width="100" height="55" rx="6" fill="#0d1422" stroke="#1e2d45" stroke-width="1"/>
      <text x="375" y="291" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="8">type</text>
      <text x="375" y="305" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="8">+ category</text>
      <text x="375" y="319" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="7">no date</text>

      <rect x="410" y="270" width="100" height="55" rx="6" fill="#0d1422" stroke="#1e2d45" stroke-width="1"/>
      <text x="460" y="291" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="8">dateRange</text>
      <text x="460" y="305" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="8">only</text>
      <text x="460" y="319" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="7">no type/cat</text>

      <rect x="507" y="270" width="96" height="55" rx="6" fill="#0d1422" stroke="#1e2d45" stroke-width="1"/>
      <text x="555" y="291" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="8">type</text>
      <text x="555" y="305" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="8">only</text>
      <text x="555" y="319" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="7">no cat/date</text>

      <rect x="602" y="270" width="96" height="55" rx="6" fill="#0d1422" stroke="#1e2d45" stroke-width="1"/>
      <text x="650" y="291" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="8">category</text>
      <text x="650" y="305" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="8">only</text>
      <text x="650" y="319" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="7">no type/date</text>

      <rect x="697" y="270" width="96" height="55" rx="6" fill="#0d1422" stroke="#1e2d45" stroke-width="1"/>
      <text x="745" y="291" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="8">findAll</text>
      <text x="745" y="305" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="8">non-deleted</text>
      <text x="745" y="319" text-anchor="middle" fill="#4b5e78" font-family="JetBrains Mono" font-size="7">no params</text>

      <!-- all converge to result -->
      <line x1="20" y1="370" x2="793" y2="370" stroke="#1e2d45" stroke-width="1"/>
      <line x1="70" y1="325" x2="70" y2="370" stroke="#1e2d45" stroke-width="1"/>
      <line x1="175" y1="325" x2="175" y2="370" stroke="#1e2d45" stroke-width="1"/>
      <line x1="280" y1="325" x2="280" y2="370" stroke="#1e2d45" stroke-width="1"/>
      <line x1="375" y1="325" x2="375" y2="370" stroke="#1e2d45" stroke-width="1"/>
      <line x1="460" y1="325" x2="460" y2="370" stroke="#1e2d45" stroke-width="1"/>
      <line x1="555" y1="325" x2="555" y2="370" stroke="#1e2d45" stroke-width="1"/>
      <line x1="650" y1="325" x2="650" y2="370" stroke="#1e2d45" stroke-width="1"/>
      <line x1="745" y1="325" x2="745" y2="370" stroke="#1e2d45" stroke-width="1"/>
      <line x1="410" y1="370" x2="410" y2="390" stroke="url(#gAccent)" stroke-width="1.5" marker-end="url(#arrowBlue)"/>

      <rect x="280" y="390" width="260" height="22" rx="6" fill="rgba(0,212,255,.06)" stroke="#00d4ff" stroke-width="1"/>
      <text x="410" y="405" text-anchor="middle" fill="#00d4ff" font-family="JetBrains Mono" font-size="9" font-weight="700">200 OK — filtered records (deleted=false)</text>
    </svg>
  </div>

  <!-- DASHBOARD FLOW -->
  <h3>Dashboard Summary Aggregation</h3>
  <div class="diagram-wrap">
    <svg viewBox="0 0 820 260" xmlns="http://www.w3.org/2000/svg" width="780">
      <rect width="820" height="260" fill="#070c15" rx="10"/>

      <!-- entry -->
      <rect x="290" y="20" width="240" height="36" rx="8" fill="#0d1422" stroke="#00d4ff" stroke-width="1.5"/>
      <text x="410" y="43" text-anchor="middle" fill="#00d4ff" font-family="JetBrains Mono" font-size="10" font-weight="700">GET /api/dashboard/summary</text>

      <line x1="410" y1="56" x2="410" y2="80" stroke="#1e2d45" stroke-width="1.5"/>

      <!-- resolveCaller -->
      <rect x="290" y="80" width="240" height="30" rx="6" fill="#0d1422" stroke="#7c3aed" stroke-width="1.5"/>
      <text x="410" y="99" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="9">resolveCaller() → validate active user</text>

      <line x1="410" y1="110" x2="410" y2="130" stroke="#1e2d45" stroke-width="1.5"/>
      <line x1="90" y1="130" x2="740" y2="130" stroke="#1e2d45" stroke-width="1.5"/>

      <!-- 6 aggregation branches -->
      <line x1="90" y1="130" x2="90" y2="148" stroke="#1e2d45" stroke-width="1"/>
      <line x1="215" y1="130" x2="215" y2="148" stroke="#1e2d45" stroke-width="1"/>
      <line x1="340" y1="130" x2="340" y2="148" stroke="#1e2d45" stroke-width="1"/>
      <line x1="456" y1="130" x2="456" y2="148" stroke="#1e2d45" stroke-width="1"/>
      <line x1="578" y1="130" x2="578" y2="148" stroke="#1e2d45" stroke-width="1"/>
      <line x1="696" y1="130" x2="696" y2="148" stroke="#1e2d45" stroke-width="1"/>

      <rect x="25" y="148" width="130" height="50" rx="6" fill="#0d1422" stroke="#10b981" stroke-width="1"/>
      <text x="90" y="167" text-anchor="middle" fill="#34d399" font-family="JetBrains Mono" font-size="8" font-weight="700">totalIncome</text>
      <text x="90" y="180" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="8">match INCOME</text>
      <text x="90" y="192" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="8">$sum amount</text>

      <rect x="150" y="148" width="130" height="50" rx="6" fill="#0d1422" stroke="#ef4444" stroke-width="1"/>
      <text x="215" y="167" text-anchor="middle" fill="#f87171" font-family="JetBrains Mono" font-size="8" font-weight="700">totalExpense</text>
      <text x="215" y="180" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="8">match EXPENSE</text>
      <text x="215" y="192" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="8">$sum amount</text>

      <rect x="275" y="148" width="130" height="50" rx="6" fill="#0d1422" stroke="#f59e0b" stroke-width="1"/>
      <text x="340" y="167" text-anchor="middle" fill="#fbbf24" font-family="JetBrains Mono" font-size="8" font-weight="700">netBalance</text>
      <text x="340" y="180" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="8">income − expense</text>
      <text x="340" y="192" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="8">computed in Java</text>

      <rect x="392" y="148" width="130" height="50" rx="6" fill="#0d1422" stroke="#7c3aed" stroke-width="1"/>
      <text x="456" y="167" text-anchor="middle" fill="#a78bfa" font-family="JetBrains Mono" font-size="8" font-weight="700">categoryTotals</text>
      <text x="456" y="180" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="8">$group category</text>
      <text x="456" y="192" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="8">$sum amount</text>

      <rect x="513" y="148" width="130" height="50" rx="6" fill="#0d1422" stroke="#00d4ff" stroke-width="1"/>
      <text x="578" y="167" text-anchor="middle" fill="#00d4ff" font-family="JetBrains Mono" font-size="8" font-weight="700">monthlyTrends</text>
      <text x="578" y="180" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="8">$project year+month</text>
      <text x="578" y="192" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="8">LinkedHashMap ordered</text>

      <rect x="631" y="148" width="130" height="50" rx="6" fill="#0d1422" stroke="#94a3b8" stroke-width="1"/>
      <text x="696" y="167" text-anchor="middle" fill="#94a3b8" font-family="JetBrains Mono" font-size="8" font-weight="700">recentActivity</text>
      <text x="696" y="180" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="8">top 5 by date</text>
      <text x="696" y="192" text-anchor="middle" fill="#64748b" font-family="JetBrains Mono" font-size="8">OrderByDateDesc</text>

      <!-- converge -->
      <line x1="90" y1="198" x2="90" y2="220" stroke="#1e2d45" stroke-width="1"/>
      <line x1="215" y1="198" x2="215" y2="220" stroke="#1e2d45" stroke-width="1"/>
      <line x1="340" y1="198" x2="340" y2="220" stroke="#1e2d45" stroke-width="1"/>
      <line x1="456" y1="198" x2="456" y2="220" stroke="#1e2d45" stroke-width="1"/>
      <line x1="578" y1="198" x2="578" y2="220" stroke="#1e2d45" stroke-width="1"/>
      <line x1="696" y1="198" x2="696" y2="220" stroke="#1e2d45" stroke-width="1"/>
      <line x1="90" y1="220" x2="696" y2="220" stroke="#1e2d45" stroke-width="1"/>
      <line x1="393" y1="220" x2="393" y2="238" stroke="url(#gAccent)" stroke-width="1.5" marker-end="url(#arrowBlue)"/>

      <rect x="263" y="238" width="260" height="16" rx="5" fill="rgba(0,212,255,.06)" stroke="#00d4ff" stroke-width="1"/>
      <text x="393" y="250" text-anchor="middle" fill="#00d4ff" font-family="JetBrains Mono" font-size="8" font-weight="700">DashboardSummary JSON response</text>
    </svg>
  </div>
</section>

<!-- ══════════════════════════ ACCESS CONTROL ══════════════════════════ -->
<section id="access">
  <div class="section-label">06 — Security</div>
  <h2>Role Permission Matrix</h2>
  <p>Enforced at two independent layers: <code>@PreAuthorize</code> at the controller and <code>assertAdmin()</code> / <code>assertNotViewer()</code> in every service method.</p>

  <table class="matrix-table">
    <thead>
      <tr>
        <th>Action</th>
        <th class="col-viewer">VIEWER</th>
        <th class="col-analyst">ANALYST</th>
        <th class="col-admin">ADMIN</th>
      </tr>
    </thead>
    <tbody>
      <tr><td class="action">Login</td><td><span class="check">✓</span></td><td><span class="check">✓</span></td><td><span class="check">✓</span></td></tr>
      <tr><td class="action">View Dashboard Summary</td><td><span class="check">✓</span></td><td><span class="check">✓</span></td><td><span class="check">✓</span></td></tr>
      <tr><td class="action">Paginated Records</td><td><span class="check">✓</span></td><td><span class="check">✓</span></td><td><span class="check">✓</span></td></tr>
      <tr><td class="action">View &amp; Filter Records</td><td><span class="cross">—</span></td><td><span class="check">✓</span></td><td><span class="check">✓</span></td></tr>
      <tr><td class="action">Create Records</td><td><span class="cross">—</span></td><td><span class="cross">—</span></td><td><span class="check">✓</span></td></tr>
      <tr><td class="action">Update Records</td><td><span class="cross">—</span></td><td><span class="cross">—</span></td><td><span class="check">✓</span></td></tr>
      <tr><td class="action">Delete Records (Soft)</td><td><span class="cross">—</span></td><td><span class="cross">—</span></td><td><span class="check">✓</span></td></tr>
      <tr><td class="action">Manage Users</td><td><span class="cross">—</span></td><td><span class="cross">—</span></td><td><span class="check">✓</span></td></tr>
    </tbody>
  </table>

  <div class="callout-grid">
    <div class="callout blue">
      <h4>⚡ Rate Limiting</h4>
      <p>100 requests per IP in AuthFilter via ConcurrentHashMap. Returns HTTP 429. Planned: Redis sliding window for multi-instance support.</p>
    </div>
    <div class="callout purple">
      <h4>🔐 JWT Token</h4>
      <p>24-hour expiry. Claims: userId (sub), role. ThreadLocal cleared in finally block — zero cross-request leakage.</p>
    </div>
    <div class="callout red">
      <h4>🚫 Inactive Users</h4>
      <p>Deactivated users are cut off on their very next request — resolveCaller() checks isActive() before any logic runs.</p>
    </div>
    <div class="callout green">
      <h4>👻 Soft Delete</h4>
      <p>Records set deleted=true, never physically removed. All queries carry AndDeletedFalse. @JsonIgnore hides the flag from API consumers.</p>
    </div>
  </div>
</section>

<!-- ══════════════════════════ API REFERENCE ══════════════════════════ -->
<section id="endpoints">
  <div class="section-label">07 — API</div>
  <h2>API Reference</h2>
  <p>All protected endpoints require: <code>Authorization: Bearer &lt;token&gt;</code></p>

  <table class="endpoint-table">
    <thead>
      <tr><th>Method</th><th>Endpoint</th><th>Access</th><th>Description</th></tr>
    </thead>
    <tbody>
      <tr>
        <td><span class="method POST">POST</span></td>
        <td><code>/api/auth/login</code></td>
        <td><span class="role-pill rp-open">PUBLIC</span></td>
        <td>Get JWT token</td>
      </tr>
      <tr>
        <td><span class="method POST">POST</span></td>
        <td><code>/api/users</code></td>
        <td><span class="role-pill rp-open">PUBLIC*</span><span class="role-pill rp-admin">ADMIN</span></td>
        <td>Create user (*open for first user bootstrap)</td>
      </tr>
      <tr>
        <td><span class="method GET">GET</span></td>
        <td><code>/api/users</code></td>
        <td><span class="role-pill rp-admin">ADMIN</span></td>
        <td>List all users</td>
      </tr>
      <tr>
        <td><span class="method GET">GET</span></td>
        <td><code>/api/users/{id}</code></td>
        <td><span class="role-pill rp-admin">ADMIN</span></td>
        <td>Get user by ID</td>
      </tr>
      <tr>
        <td><span class="method PATCH">PATCH</span></td>
        <td><code>/api/users/{id}</code></td>
        <td><span class="role-pill rp-admin">ADMIN</span></td>
        <td>Update role or active status (partial)</td>
      </tr>
      <tr>
        <td><span class="method GET">GET</span></td>
        <td><code>/api/records</code></td>
        <td><span class="role-pill rp-analyst">ANALYST</span><span class="role-pill rp-admin">ADMIN</span></td>
        <td>All non-deleted records</td>
      </tr>
      <tr>
        <td><span class="method POST">POST</span></td>
        <td><code>/api/records</code></td>
        <td><span class="role-pill rp-admin">ADMIN</span></td>
        <td>Create financial record</td>
      </tr>
      <tr>
        <td><span class="method PUT">PUT</span></td>
        <td><code>/api/records/{id}</code></td>
        <td><span class="role-pill rp-admin">ADMIN</span></td>
        <td>Full record update</td>
      </tr>
      <tr>
        <td><span class="method DELETE">DELETE</span></td>
        <td><code>/api/records/{id}</code></td>
        <td><span class="role-pill rp-admin">ADMIN</span></td>
        <td>Soft delete (deleted=true)</td>
      </tr>
      <tr>
        <td><span class="method GET">GET</span></td>
        <td><code>/api/records/filter</code></td>
        <td><span class="role-pill rp-analyst">ANALYST</span><span class="role-pill rp-admin">ADMIN</span></td>
        <td>Filter: ?type ?category ?from ?to ?search</td>
      </tr>
      <tr>
        <td><span class="method GET">GET</span></td>
        <td><code>/api/records/paginated</code></td>
        <td><span class="role-pill rp-viewer">VIEWER</span><span class="role-pill rp-analyst">ANALYST</span><span class="role-pill rp-admin">ADMIN</span></td>
        <td>Paginated records ?page=0&amp;size=10</td>
      </tr>
      <tr>
        <td><span class="method GET">GET</span></td>
        <td><code>/api/dashboard/summary</code></td>
        <td><span class="role-pill rp-viewer">VIEWER</span><span class="role-pill rp-analyst">ANALYST</span><span class="role-pill rp-admin">ADMIN</span></td>
        <td>Aggregated financial summary (all 6 KPIs)</td>
      </tr>
    </tbody>
  </table>

  <!-- Example responses tabs -->
  <h3>Example Response — Dashboard Summary</h3>
  <pre><span class="kw">{</span>
  <span class="str">"totalIncome"</span>:   <span class="num">150000.0</span>,
  <span class="str">"totalExpense"</span>:  <span class="num">45000.0</span>,
  <span class="str">"netBalance"</span>:    <span class="num">105000.0</span>,
  <span class="str">"categoryTotals"</span>: <span class="kw">{</span>
    <span class="str">"Salary"</span>:    <span class="num">150000.0</span>,
    <span class="str">"Utilities"</span>: <span class="num">12000.0</span>,
    <span class="str">"Rent"</span>:      <span class="num">33000.0</span>
  <span class="kw">}</span>,
  <span class="str">"recentActivity"</span>: <span class="kw">[</span>
    <span class="kw">{</span> <span class="str">"id"</span>: <span class="str">"rec-abc123"</span>, <span class="str">"type"</span>: <span class="str">"INCOME"</span>, <span class="str">"amount"</span>: <span class="num">75000.0</span>, <span class="str">"category"</span>: <span class="str">"Salary"</span> <span class="kw">}</span>
  <span class="kw">]</span>,
  <span class="str">"monthlyTrends"</span>: <span class="kw">{</span>  <span class="com">// LinkedHashMap — insertion-ordered by month</span>
    <span class="str">"2025-01"</span>: <span class="num">55000.0</span>,  <span class="com">// net (INCOME positive, EXPENSE negative)</span>
    <span class="str">"2025-02"</span>: <span class="num">50000.0</span>
  <span class="kw">}</span>
<span class="kw">}</span></pre>
</section>

<!-- ══════════════════════════ SETUP ══════════════════════════ -->
<section id="setup">
  <div class="section-label">08 — Setup</div>
  <h2>Setup &amp; Running Locally</h2>

  <div class="callout-grid" style="margin-bottom:24px;">
    <div class="callout blue"><h4>Prerequisites</h4><p>Java 17+ · Maven 3.8+ · MongoDB on port 27017 (or Atlas URI)</p></div>
    <div class="callout green"><h4>No local Maven required</h4><p>Use the included mvnw wrapper — handles Maven automatically.</p></div>
  </div>

  <pre><span class="com"># 1. Clone the repository</span>
git clone https://github.com/pranavsaai/finance_dashboard_backend.git
cd finance_dashboard_backend

<span class="com"># 2. Set required environment variables</span>
<span class="kw">export</span> MONGO_URI_FINANCE=<span class="str">mongodb://localhost:27017/finance_db</span>
<span class="kw">export</span> JWT_SECRET_FINANCE=<span class="str">your-very-long-secret-key-at-least-32-chars</span>

<span class="com"># 3. Run the server (starts on :8080)</span>
./mvnw spring-boot:run

<span class="com"># 4. Run unit tests (no live MongoDB needed)</span>
./mvnw test</pre>

  <h3>Environment Variables</h3>
  <table class="env-table">
    <tr>
      <td>MONGO_URI_FINANCE</td>
      <td>MongoDB connection URI</td>
      <td>mongodb://localhost:27017/finance_db</td>
    </tr>
    <tr>
      <td>JWT_SECRET_FINANCE</td>
      <td>JWT signing key (min 32 chars)</td>
      <td>my-super-secret-key-2025</td>
    </tr>
  </table>

  <p style="font-size:13px;"><code>jwt.expiration=86400000</code> — tokens expire after <strong style="color:#fff;">24 hours</strong>. Auto-index creation enabled: <code>spring.data.mongodb.auto-index-creation=true</code></p>
</section>

<!-- ══════════════════════════ TESTS ══════════════════════════ -->
<section id="tests">
  <div class="section-label">09 — Testing</div>
  <h2>Unit Tests — 18 Cases</h2>
  <p>All tests run without a live MongoDB. Repositories are mocked with Mockito. <code>AuthContext.clear()</code> called in @AfterEach to prevent ThreadLocal leakage between tests.</p>

  <h3>UserServiceTest (9 tests)</h3>
  <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin:16px 0;">
    <div style="background:#0d1422;border:1px solid #1e2d45;border-radius:8px;padding:12px 16px;font-family:'JetBrains Mono',monospace;font-size:12px;color:#64748b;"><span style="color:#10b981;">✓</span> Missing X-User-Id → UnauthorizedException</div>
    <div style="background:#0d1422;border:1px solid #1e2d45;border-radius:8px;padding:12px 16px;font-family:'JetBrains Mono',monospace;font-size:12px;color:#64748b;"><span style="color:#10b981;">✓</span> Inactive user → UnauthorizedException</div>
    <div style="background:#0d1422;border:1px solid #1e2d45;border-radius:8px;padding:12px 16px;font-family:'JetBrains Mono',monospace;font-size:12px;color:#64748b;"><span style="color:#10b981;">✓</span> Active user resolves correctly</div>
    <div style="background:#0d1422;border:1px solid #1e2d45;border-radius:8px;padding:12px 16px;font-family:'JetBrains Mono',monospace;font-size:12px;color:#64748b;"><span style="color:#10b981;">✓</span> Non-admin getAllUsers → AccessDeniedException</div>
    <div style="background:#0d1422;border:1px solid #1e2d45;border-radius:8px;padding:12px 16px;font-family:'JetBrains Mono',monospace;font-size:12px;color:#64748b;"><span style="color:#10b981;">✓</span> Admin getAllUsers → full list</div>
    <div style="background:#0d1422;border:1px solid #1e2d45;border-radius:8px;padding:12px 16px;font-family:'JetBrains Mono',monospace;font-size:12px;color:#64748b;"><span style="color:#10b981;">✓</span> Admin updateUser role change</div>
    <div style="background:#0d1422;border:1px solid #1e2d45;border-radius:8px;padding:12px 16px;font-family:'JetBrains Mono',monospace;font-size:12px;color:#64748b;"><span style="color:#10b981;">✓</span> Admin deactivate user</div>
    <div style="background:#0d1422;border:1px solid #1e2d45;border-radius:8px;padding:12px 16px;font-family:'JetBrains Mono',monospace;font-size:12px;color:#64748b;"><span style="color:#10b981;">✓</span> Duplicate email → IllegalArgumentException</div>
    <div style="background:#0d1422;border:1px solid #1e2d45;border-radius:8px;padding:12px 16px;font-family:'JetBrains Mono',monospace;font-size:12px;color:#64748b;"><span style="color:#10b981;">✓</span> Valid new user saved and returned</div>
  </div>

  <h3>FinancialRecordServiceTest (9 tests)</h3>
  <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin:16px 0;">
    <div style="background:#0d1422;border:1px solid #1e2d45;border-radius:8px;padding:12px 16px;font-family:'JetBrains Mono',monospace;font-size:12px;color:#64748b;"><span style="color:#10b981;">✓</span> VIEWER createRecord → AccessDeniedException</div>
    <div style="background:#0d1422;border:1px solid #1e2d45;border-radius:8px;padding:12px 16px;font-family:'JetBrains Mono',monospace;font-size:12px;color:#64748b;"><span style="color:#10b981;">✓</span> ANALYST createRecord → AccessDeniedException</div>
    <div style="background:#0d1422;border:1px solid #1e2d45;border-radius:8px;padding:12px 16px;font-family:'JetBrains Mono',monospace;font-size:12px;color:#64748b;"><span style="color:#10b981;">✓</span> ADMIN createRecord → saved with userId</div>
    <div style="background:#0d1422;border:1px solid #1e2d45;border-radius:8px;padding:12px 16px;font-family:'JetBrains Mono',monospace;font-size:12px;color:#64748b;"><span style="color:#10b981;">✓</span> VIEWER getAllRecords → AccessDeniedException</div>
    <div style="background:#0d1422;border:1px solid #1e2d45;border-radius:8px;padding:12px 16px;font-family:'JetBrains Mono',monospace;font-size:12px;color:#64748b;"><span style="color:#10b981;">✓</span> ANALYST getAllRecords → returns list</div>
    <div style="background:#0d1422;border:1px solid #1e2d45;border-radius:8px;padding:12px 16px;font-family:'JetBrains Mono',monospace;font-size:12px;color:#64748b;"><span style="color:#10b981;">✓</span> deleteRecord bad ID → ResourceNotFoundException</div>
    <div style="background:#0d1422;border:1px solid #1e2d45;border-radius:8px;padding:12px 16px;font-family:'JetBrains Mono',monospace;font-size:12px;color:#64748b;"><span style="color:#10b981;">✓</span> ADMIN deleteRecord → deleted=true, not deleteById</div>
    <div style="background:#0d1422;border:1px solid #1e2d45;border-radius:8px;padding:12px 16px;font-family:'JetBrains Mono',monospace;font-size:12px;color:#64748b;"><span style="color:#10b981;">✓</span> filterRecords type+date → correct query path</div>
    <div style="background:#0d1422;border:1px solid #1e2d45;border-radius:8px;padding:12px 16px;font-family:'JetBrains Mono',monospace;font-size:12px;color:#64748b;"><span style="color:#10b981;">✓</span> filterRecords ?search → regex query path</div>
  </div>

  <pre><span class="com"># Run all 18 tests</span>
./mvnw test

<span class="com"># Run only service tests</span>
./mvnw test -Dtest=<span class="str">"UserServiceTest,FinancialRecordServiceTest"</span>

<span class="com"># Expected output</span>
Tests run: <span class="num">18</span>, Failures: <span class="num">0</span>, Errors: <span class="num">0</span>, Skipped: <span class="num">0</span>
BUILD SUCCESS</pre>
</section>

<!-- ══════════════════════════ TRADEOFFS ══════════════════════════ -->
<section id="tradeoffs">
  <div class="section-label">10 — Tradeoffs &amp; Roadmap</div>
  <h2>Known Tradeoffs &amp; Future Improvements</h2>

  <div class="callout-grid">
    <div class="callout amber">
      <h4>Rate Limiting — In-Memory</h4>
      <p>ConcurrentHashMap resets on restart and doesn't work across multiple instances. Planned: Redis-backed sliding window with Bucket4j.</p>
    </div>
    <div class="callout blue">
      <h4>Bootstrap User Creation</h4>
      <p>POST /api/users is permitAll() for first-run. Planned: auto-disable after first admin, or invite-based onboarding.</p>
    </div>
    <div class="callout purple">
      <h4>Dual Security Enforcement</h4>
      <p>@PreAuthorize + service-layer checks have slight duplication. Planned: consolidate into a centralized policy layer.</p>
    </div>
    <div class="callout green">
      <h4>Search + Filter Combination</h4>
      <p>search param currently overrides other filters. Planned: allow keyword + date range + type to combine freely.</p>
    </div>
    <div class="callout red">
      <h4>Soft Delete Archival</h4>
      <p>Deleted records stay in the primary collection. Planned: move to an archive collection or TTL-based cleanup strategy.</p>
    </div>
    <div class="callout blue">
      <h4>Pagination Size Cap</h4>
      <p>Page size validated but not capped. Planned: enforce max 100 records per page to prevent large data pulls.</p>
    </div>
  </div>
</section>

<!-- ══════════════════════════ FOOTER ══════════════════════════ -->
<footer>
  <div style="margin-bottom:8px;">
    <a href="https://github.com/pranavsaai/finance_dashboard_backend">github.com/pranavsaai/finance_dashboard_backend</a>
  </div>
  <div>Java 17 · Spring Boot 3 · MongoDB · JWT · JUnit 5</div>
</footer>

</body>
</html>
