#!/usr/bin/env node
/**
 * Records a narrated-by-captions demo video of SmartCart (MP4, 1920x1080) - no dependencies.
 *
 *   node tools/demo-video/record-demo.mjs
 *
 * Prerequisites: backend on :8080 (with the dev seed loaded), frontend on :4200, Google Chrome installed,
 * and no existing account for DEMO_USER.email (the script signs up a fresh one).
 *
 * How it works
 *   1. Launches Chrome headless with a throwaway profile (your normal Chrome profile is never touched).
 *   2. Drives the app in one tab over the Chrome DevTools Protocol with real mouse/keyboard events.
 *   3. Streams that tab's screencast frames into a second tab ("encoder"), which composites them into a
 *      browser-window mockup with captions on a <canvas> and records it with MediaRecorder (H.264/MP4).
 *   4. The encoder downloads the finished file into tools/demo-video/out/.
 */
import { spawn } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const OUT_DIR = resolve(HERE, 'out');
const OUTPUT_NAME = 'SmartCart-demo.mp4';
const APP = process.env.APP_URL ?? 'http://localhost:4200';
const PORT = 9333;

const CHROME_CANDIDATES = [
  process.env.CHROME_PATH,
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  '/usr/bin/google-chrome',
].filter(Boolean);

// Fictional local-development account created by the demo itself.
const DEMO_USER = {
  name: 'Dana Demo',
  email: 'dana.demo@example.test',
  password: 'Demo#Pass2026',
  newPassword: 'Demo#Pass2027',
  dateOfBirth: '1994-06-15',
  region: 'EG',
  phone: '010 2468 1357',
};

// Video layout: a 1600x956 viewport inside a browser-window mockup on a 1920x1080 canvas.
const VIEW = { width: 1600, height: 956 };
const MOBILE = { width: 390, height: 844, scale: 2 };

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// ---------------------------------------------------------------------------------------------------
// Minimal CDP client over Node's built-in WebSocket (one connection, flattened sessions).
// ---------------------------------------------------------------------------------------------------
class Cdp {
  constructor(url) {
    this.ws = new WebSocket(url);
    this.nextId = 1;
    this.pending = new Map();
    this.listeners = new Set();
    this.ws.addEventListener('message', (event) => {
      const msg = JSON.parse(event.data);
      if (msg.id && this.pending.has(msg.id)) {
        const { resolve, reject, method } = this.pending.get(msg.id);
        this.pending.delete(msg.id);
        msg.error ? reject(new Error(`${method}: ${msg.error.message}`)) : resolve(msg.result);
      } else if (msg.method) {
        for (const listener of this.listeners) listener(msg);
      }
    });
  }

  opened() {
    return new Promise((resolve, reject) => {
      this.ws.addEventListener('open', resolve, { once: true });
      this.ws.addEventListener('error', reject, { once: true });
    });
  }

  send(method, params = {}, sessionId) {
    const id = this.nextId++;
    this.ws.send(JSON.stringify({ id, method, params, sessionId }));
    return new Promise((resolve, reject) => this.pending.set(id, { resolve, reject, method }));
  }

  on(listener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  waitFor(predicate, timeoutMs = 30000) {
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => (off(), reject(new Error('Timed out waiting for CDP event'))), timeoutMs);
      const off = this.on((msg) => {
        if (predicate(msg)) {
          clearTimeout(timer);
          off();
          resolve(msg);
        }
      });
    });
  }
}

// ---------------------------------------------------------------------------------------------------
// Encoder page: composites frames + captions on a canvas and records it.
// ---------------------------------------------------------------------------------------------------
const ENCODER_SCRIPT = String.raw`
(() => {
  const W = 1920, H = 1080;
  const WIN = { x: 160, y: 30, w: 1600, h: 1000, bar: 44 };
  const canvas = document.createElement('canvas');
  canvas.width = W; canvas.height = H;
  document.body.style.margin = '0';
  document.body.appendChild(canvas);
  const ctx = canvas.getContext('2d');

  const state = { frame: null, seq: 0, shown: 0, url: '', mode: 'card', caption: null, card: null, theme: 'light' };
  const FONT = '"Segoe UI", system-ui, sans-serif';
  const ease = (t) => 1 - Math.pow(1 - Math.min(Math.max(t, 0), 1), 3);

  function background() {
    const g = ctx.createLinearGradient(0, 0, W, H);
    g.addColorStop(0, '#1e1b4b'); g.addColorStop(0.55, '#312e81'); g.addColorStop(1, '#581c87');
    ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
    const blob = (x, y, r, c) => { const rg = ctx.createRadialGradient(x, y, 0, x, y, r); rg.addColorStop(0, c); rg.addColorStop(1, 'transparent'); ctx.fillStyle = rg; ctx.fillRect(0, 0, W, H); };
    blob(W * 0.85, H * 0.1, 700, 'rgba(192,38,211,0.25)');
    blob(W * 0.1, H * 0.9, 700, 'rgba(79,70,229,0.35)');
  }

  function drawWindow() {
    const { x, y, w, h, bar } = WIN;
    ctx.save();
    ctx.shadowColor = 'rgba(0,0,0,0.45)'; ctx.shadowBlur = 60; ctx.shadowOffsetY = 20;
    ctx.fillStyle = state.theme === 'dark' ? '#0f172a' : '#ffffff';
    ctx.beginPath(); ctx.roundRect(x, y, w, h, 16); ctx.fill();
    ctx.restore();

    ctx.save();
    ctx.beginPath(); ctx.roundRect(x, y, w, h, 16); ctx.clip();
    ctx.fillStyle = state.theme === 'dark' ? '#1e293b' : '#e9edf3';
    ctx.fillRect(x, y, w, bar);
    ['#ff5f57', '#febc2e', '#28c840'].forEach((c, i) => { ctx.fillStyle = c; ctx.beginPath(); ctx.arc(x + 24 + i * 22, y + bar / 2, 6.5, 0, Math.PI * 2); ctx.fill(); });
    ctx.fillStyle = state.theme === 'dark' ? '#0f172a' : '#ffffff';
    ctx.beginPath(); ctx.roundRect(x + 360, y + 8, w - 720, bar - 16, 14); ctx.fill();
    ctx.fillStyle = state.theme === 'dark' ? '#94a3b8' : '#475569';
    ctx.font = '500 15px ' + FONT; ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
    ctx.fillText(state.url, x + w / 2, y + bar / 2 + 1);
    if (state.frame) ctx.drawImage(state.frame, x, y + bar, w, h - bar);
    ctx.restore();
  }

  function drawPhone() {
    const f = state.frame;
    if (!f) return;
    const ph = 900, pw = Math.round(ph * f.width / f.height);
    const px = Math.round(W * 0.62 - pw / 2), py = Math.round((H - ph) / 2);
    ctx.save();
    ctx.shadowColor = 'rgba(0,0,0,0.5)'; ctx.shadowBlur = 70; ctx.shadowOffsetY = 24;
    ctx.fillStyle = '#0b0b10';
    ctx.beginPath(); ctx.roundRect(px - 16, py - 16, pw + 32, ph + 32, 58); ctx.fill();
    ctx.restore();
    ctx.save();
    ctx.beginPath(); ctx.roundRect(px, py, pw, ph, 44); ctx.clip();
    ctx.drawImage(f, px, py, pw, ph);
    ctx.restore();
  }

  function drawCaption(now) {
    const c = state.caption;
    if (!c) return;
    const t = ease((now - c.t0) / 450);
    ctx.save();
    ctx.globalAlpha = t;
    if (state.mode === 'mobile') {
      // Big side caption next to the phone.
      ctx.textAlign = 'left'; ctx.textBaseline = 'alphabetic';
      ctx.fillStyle = '#a5b4fc'; ctx.font = '700 22px ' + FONT;
      ctx.fillText(String(c.step).padStart(2, '0') + ' — ' + c.chapter.toUpperCase(), 200, 470 - (1 - t) * 20);
      ctx.fillStyle = '#ffffff'; ctx.font = '800 54px ' + FONT;
      wrap(c.title, 200, 540 - (1 - t) * 20, 640, 62);
      ctx.fillStyle = '#c7d2fe'; ctx.font = '400 26px ' + FONT;
      wrap(c.sub, 200, 640 - (1 - t) * 20, 600, 36);
    } else {
      ctx.font = '700 26px ' + FONT;
      const titleW = ctx.measureText(c.title).width;
      ctx.font = '400 21px ' + FONT;
      const subW = ctx.measureText(c.sub).width;
      const w = Math.max(titleW, subW) + 150, h = 92;
      const x = (W - w) / 2, y = H - 150 + (1 - t) * 24;
      ctx.shadowColor = 'rgba(0,0,0,0.35)'; ctx.shadowBlur = 30; ctx.shadowOffsetY = 10;
      ctx.fillStyle = 'rgba(15,23,42,0.92)';
      ctx.beginPath(); ctx.roundRect(x, y, w, h, 24); ctx.fill();
      ctx.shadowColor = 'transparent';
      const g = ctx.createLinearGradient(x + 22, y, x + 78, y + h);
      g.addColorStop(0, '#6366f1'); g.addColorStop(1, '#a855f7');
      ctx.fillStyle = g; ctx.beginPath(); ctx.roundRect(x + 22, y + 18, 56, 56, 16); ctx.fill();
      ctx.fillStyle = '#fff'; ctx.font = '800 24px ' + FONT; ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
      ctx.fillText(String(c.step), x + 50, y + 47);
      ctx.textAlign = 'left'; ctx.textBaseline = 'alphabetic';
      ctx.fillStyle = '#ffffff'; ctx.font = '700 26px ' + FONT; ctx.fillText(c.title, x + 100, y + 42);
      ctx.fillStyle = '#cbd5e1'; ctx.font = '400 21px ' + FONT; ctx.fillText(c.sub, x + 100, y + 72);
    }
    ctx.restore();
  }

  function wrap(text, x, y, maxW, lh) {
    let line = '';
    for (const word of text.split(' ')) {
      const test = line ? line + ' ' + word : word;
      if (ctx.measureText(test).width > maxW && line) { ctx.fillText(line, x, y); line = word; y += lh; }
      else line = test;
    }
    ctx.fillText(line, x, y);
  }

  function drawCard(now) {
    const c = state.card;
    if (!c) return;
    const t = ease((now - c.t0) / 700);
    ctx.save();
    ctx.globalAlpha = t;
    ctx.textAlign = 'center'; ctx.textBaseline = 'alphabetic';
    const g = ctx.createLinearGradient(W / 2 - 46, 330, W / 2 + 46, 420);
    g.addColorStop(0, '#818cf8'); g.addColorStop(1, '#c084fc');
    ctx.fillStyle = g; ctx.beginPath(); ctx.roundRect(W / 2 - 46, 300 - (1 - t) * 20, 92, 92, 26); ctx.fill();
    ctx.fillStyle = '#fff'; ctx.font = '800 46px ' + FONT; ctx.textBaseline = 'middle';
    ctx.fillText('S', W / 2, 347 - (1 - t) * 20);
    ctx.textBaseline = 'alphabetic';
    ctx.font = '800 84px ' + FONT; ctx.fillText(c.title, W / 2, 520 - (1 - t) * 20);
    ctx.fillStyle = '#c7d2fe'; ctx.font = '400 34px ' + FONT; ctx.fillText(c.sub, W / 2, 590 - (1 - t) * 20);
    if (c.tags) {
      ctx.font = '600 22px ' + FONT;
      const pad = 22, gap = 14;
      const widths = c.tags.map((tag) => ctx.measureText(tag).width + pad * 2);
      let x = W / 2 - (widths.reduce((a, b) => a + b, 0) + gap * (c.tags.length - 1)) / 2;
      c.tags.forEach((tag, i) => {
        ctx.fillStyle = 'rgba(255,255,255,0.12)';
        ctx.beginPath(); ctx.roundRect(x, 650, widths[i], 46, 23); ctx.fill();
        ctx.fillStyle = '#e0e7ff'; ctx.textAlign = 'left'; ctx.fillText(tag, x + pad, 681);
        x += widths[i] + gap;
      });
    }
    ctx.restore();
  }

  function draw() {
    const now = performance.now();
    background();
    if (state.mode === 'card') drawCard(now);
    else if (state.mode === 'mobile') drawPhone();
    else drawWindow();
    drawCaption(now);
  }
  setInterval(draw, 1000 / 30);

  const chunks = [];
  let recorder;

  window.__enc = {
    async frame(seq, b64) {
      const blob = await (await fetch('data:image/jpeg;base64,' + b64)).blob();
      const bitmap = await createImageBitmap(blob);
      if (seq > state.shown) { state.frame?.close?.(); state.frame = bitmap; state.shown = seq; } else bitmap.close();
    },
    url(u) { state.url = u; },
    theme(t) { state.theme = t; },
    mode(m) { state.mode = m; if (m !== 'card') state.card = null; },
    caption(step, chapter, title, sub) { state.caption = { step, chapter, title, sub, t0: performance.now() }; },
    clearCaption() { state.caption = null; },
    card(title, sub, tags) { state.mode = 'card'; state.caption = null; state.card = { title, sub, tags, t0: performance.now() }; },
    start(mimeType) {
      const stream = canvas.captureStream(30);
      recorder = new MediaRecorder(stream, { mimeType, videoBitsPerSecond: 9_000_000 });
      recorder.ondataavailable = (e) => e.data.size && chunks.push(e.data);
      recorder.start(1000);
      return recorder.mimeType;
    },
    stop(filename) {
      return new Promise((resolve) => {
        recorder.onstop = () => {
          const blob = new Blob(chunks, { type: recorder.mimeType });
          const a = document.createElement('a');
          a.href = URL.createObjectURL(blob);
          a.download = filename;
          document.body.appendChild(a);
          a.click();
          resolve(blob.size);
        };
        recorder.stop();
      });
    },
  };
})();
`;

// Injected into every page of the app tab: a visible cursor and click ripples (headless has no cursor).
const CURSOR_SCRIPT = String.raw`
(() => {
  if (window.top !== window) return;
  const install = () => {
    if (document.getElementById('__demo_cursor')) return;
    const c = document.createElement('div');
    c.id = '__demo_cursor';
    c.innerHTML = '<svg width="30" height="30" viewBox="0 0 28 28"><path d="M5 3l16 9.5-7 1.6 4.2 8.1-3.2 1.6-4.2-8.2L5 20z" fill="#0f172a" stroke="#fff" stroke-width="1.6" stroke-linejoin="round"/></svg>';
    Object.assign(c.style, { position: 'fixed', left: '0', top: '0', zIndex: '2147483647', pointerEvents: 'none',
      transform: 'translate(' + (window.__demoX ?? -100) + 'px,' + (window.__demoY ?? -100) + 'px)', filter: 'drop-shadow(0 3px 5px rgba(0,0,0,.35))' });
    document.documentElement.appendChild(c);
    addEventListener('mousemove', (e) => { c.style.transform = 'translate(' + (e.clientX - 5) + 'px,' + (e.clientY - 3) + 'px)'; }, true);
    addEventListener('mousedown', (e) => {
      const r = document.createElement('div');
      Object.assign(r.style, { position: 'fixed', left: (e.clientX - 20) + 'px', top: (e.clientY - 20) + 'px', width: '40px', height: '40px',
        borderRadius: '50%', background: 'rgba(99,102,241,.4)', zIndex: '2147483646', pointerEvents: 'none', transition: 'transform .5s ease, opacity .5s ease' });
      document.documentElement.appendChild(r);
      requestAnimationFrame(() => { r.style.transform = 'scale(1.9)'; r.style.opacity = '0'; });
      setTimeout(() => r.remove(), 550);
    }, true);
  };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', install); else install();
})();
`;

// ---------------------------------------------------------------------------------------------------
// Director: drives the app tab.
// ---------------------------------------------------------------------------------------------------
class Director {
  constructor(cdp, appSession, encSession) {
    this.cdp = cdp;
    this.app = appSession;
    this.enc = encSession;
    this.mouse = { x: 800, y: 400 };
    this.frameSeq = 0;
    this.step = 0;
  }

  appCmd(method, params) {
    return this.cdp.send(method, params, this.app);
  }

  encEval(expression) {
    return this.cdp.send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true }, this.enc);
  }

  async eval(expression) {
    const result = await this.appCmd('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true });
    if (result.exceptionDetails) throw new Error(result.exceptionDetails.exception?.description ?? 'evaluate failed');
    return result.result.value;
  }

  // ----- screencast -----
  async startScreencast(maxWidth, maxHeight) {
    if (!this.unsubscribeFrames) {
      this.unsubscribeFrames = this.cdp.on((msg) => {
        if (msg.sessionId !== this.app) return;
        if (msg.method === 'Page.screencastFrame') {
          this.cdp.send('Page.screencastFrameAck', { sessionId: msg.params.sessionId }, this.app).catch(() => {});
          this.relayFrame(msg.params.data);
        } else if (msg.method === 'Page.frameNavigated' && !msg.params.frame.parentId) {
          this.showUrl(msg.params.frame.url);
        } else if (msg.method === 'Page.navigatedWithinDocument') {
          this.showUrl(msg.params.url); // Angular router (history.pushState) navigations
        }
      });
    }
    await this.appCmd('Page.startScreencast', { format: 'jpeg', quality: 88, maxWidth, maxHeight, everyNthFrame: 1 });
  }

  /**
   * Latest-frame-wins backpressure. Frames share the WebSocket with the mouse/keyboard commands; queueing every
   * ~150 KB frame made the commands wait behind them and slowed the whole demo. While one frame is in flight,
   * keep only the newest and send it next.
   */
  relayFrame(data) {
    if (this.frameInFlight) {
      this.nextFrame = data;
      return;
    }
    this.frameInFlight = true;
    const seq = ++this.frameSeq;
    this.encEval(`__enc.frame(${seq}, ${JSON.stringify(data)})`)
      .catch(() => {})
      .finally(() => {
        this.frameInFlight = false;
        if (this.nextFrame) {
          const next = this.nextFrame;
          this.nextFrame = null;
          this.relayFrame(next);
        }
      });
  }

  /** Host + path only: never put query strings (OAuth state, tokens) in the video. */
  showUrl(url) {
    let shown;
    try {
      const u = new URL(url);
      shown = u.host + (u.pathname === '/' ? '' : u.pathname);
    } catch {
      shown = url;
    }
    if (shown.length > 80) shown = shown.slice(0, 77) + '…';
    this.encEval(`__enc.url(${JSON.stringify(shown)})`).catch(() => {});
  }

  async stopScreencast() {
    await this.appCmd('Page.stopScreencast');
  }

  // ----- captions -----
  async chapter(chapter, title, sub) {
    this.step++;
    await this.encEval(`__enc.caption(${this.step}, ${JSON.stringify(chapter)}, ${JSON.stringify(title)}, ${JSON.stringify(sub)})`);
  }

  async card(title, sub, tags, holdMs) {
    await this.encEval(`__enc.card(${JSON.stringify(title)}, ${JSON.stringify(sub)}, ${JSON.stringify(tags ?? null)})`);
    await sleep(holdMs);
  }

  async show(mode) {
    await this.encEval(`__enc.mode(${JSON.stringify(mode)})`);
  }

  async syncTheme() {
    const theme = await this.eval(`document.documentElement.getAttribute('data-bs-theme') || 'light'`);
    await this.encEval(`__enc.theme(${JSON.stringify(theme)})`);
  }

  // ----- navigation -----
  async goto(path, waitSelector) {
    await this.appCmd('Page.navigate', { url: path.startsWith('http') ? path : APP + path });
    if (waitSelector) await this.waitFor(waitSelector);
    await this.restoreCursor();
  }

  async restoreCursor() {
    await this.appCmd('Input.dispatchMouseEvent', { type: 'mouseMoved', x: this.mouse.x, y: this.mouse.y });
  }

  async waitFor(selector, text, timeoutMs = 15000) {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
      const found = await this.eval(`(() => { const t = ${JSON.stringify(text ?? '')};
        return [...document.querySelectorAll(${JSON.stringify(selector)})].some(e => e.getClientRects().length && (!t || e.textContent.includes(t))); })()`).catch(() => false);
      if (found) return;
      await sleep(150);
    }
    throw new Error(`Timed out waiting for ${selector}${text ? ` containing "${text}"` : ''}`);
  }

  /** Centre of the element, scrolled into comfortable view first. */
  async locate(selector, text, inner) {
    await this.waitFor(selector, text);
    return this.eval(`(async () => {
      const t = ${JSON.stringify(text ?? '')};
      const box = [...document.querySelectorAll(${JSON.stringify(selector)})].find(e => e.getClientRects().length && (!t || e.textContent.includes(t)));
      const el = ${JSON.stringify(inner ?? '')} ? box.querySelector(${JSON.stringify(inner ?? '')}) : box;
      let r = el.getBoundingClientRect();
      if (r.top < 90 || r.bottom > innerHeight - 170) {
        el.scrollIntoView({ block: 'center', behavior: 'smooth' });
        await new Promise(res => setTimeout(res, 800));
        r = el.getBoundingClientRect();
      }
      return { x: r.left + r.width / 2, y: r.top + r.height / 2 };
    })()`);
  }

  // ----- mouse & keyboard -----
  async moveTo(x, y) {
    const from = { ...this.mouse };
    const distance = Math.hypot(x - from.x, y - from.y);
    const duration = Math.min(900, 250 + distance * 0.6);
    const steps = Math.max(8, Math.round(duration / 16));
    for (let i = 1; i <= steps; i++) {
      const t = i / steps;
      const e = t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
      this.mouse = { x: from.x + (x - from.x) * e, y: from.y + (y - from.y) * e };
      await this.appCmd('Input.dispatchMouseEvent', { type: 'mouseMoved', x: this.mouse.x, y: this.mouse.y });
      await sleep(16);
    }
  }

  async hover(selector, text, holdMs = 700) {
    const p = await this.locate(selector, text);
    await this.moveTo(p.x, p.y);
    await sleep(holdMs);
  }

  async click(selector, text, { pauseAfter = 600, inner } = {}) {
    const p = await this.locate(selector, text, inner);
    await this.moveTo(p.x, p.y);
    await sleep(180);
    const base = { x: p.x, y: p.y, button: 'left', clickCount: 1 };
    await this.appCmd('Input.dispatchMouseEvent', { ...base, type: 'mousePressed' });
    await sleep(70);
    await this.appCmd('Input.dispatchMouseEvent', { ...base, type: 'mouseReleased' });
    await sleep(pauseAfter);
  }

  async type(selector, text, { clear = false, delay = 55 } = {}) {
    await this.click(selector, undefined, { pauseAfter: 150 });
    if (clear) {
      await this.eval(`(() => { const el = document.querySelector(${JSON.stringify(selector)}); el.select?.(); })()`);
      await this.key('Backspace', 8);
    } else {
      await this.key('End', 35); // the click may have put the caret mid-text
    }
    for (const ch of text) {
      await this.appCmd('Input.insertText', { text: ch });
      await sleep(delay);
    }
    await sleep(250);
  }

  async key(key, keyCode) {
    await this.appCmd('Input.dispatchKeyEvent', { type: 'keyDown', key, code: key, windowsVirtualKeyCode: keyCode });
    await this.appCmd('Input.dispatchKeyEvent', { type: 'keyUp', key, code: key, windowsVirtualKeyCode: keyCode });
  }

  async setCursorVisible(visible) {
    await this.eval(`(() => { window.__demoHidden = ${!visible}; const c = document.getElementById('__demo_cursor'); if (c) c.style.display = ${visible ? "''" : "'none'"}; })()`);
  }

  /** For controls whose native pickers don't render in a screencast (date, select). */
  async setValue(selector, value) {
    await this.click(selector, undefined, { pauseAfter: 200 });
    await this.eval(`(() => { const el = document.querySelector(${JSON.stringify(selector)});
      el.value = ${JSON.stringify(value)};
      el.dispatchEvent(new Event('input', { bubbles: true }));
      el.dispatchEvent(new Event('change', { bubbles: true }));
      el.blur(); })()`);
    await this.appCmd('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
    await sleep(350);
  }

  async scroll(deltaY, waitMs = 1100) {
    await this.eval(`window.scrollBy({ top: ${deltaY}, behavior: 'smooth' })`);
    await sleep(waitMs);
  }

  async scrollTop(waitMs = 900) {
    await this.eval(`window.scrollTo({ top: 0, behavior: 'smooth' })`);
    await sleep(waitMs);
  }

  async setViewport({ width, height, scale = 1, mobile = false }) {
    await this.appCmd('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: scale, mobile });
    await this.appCmd('Emulation.setTouchEmulationEnabled', { enabled: mobile, maxTouchPoints: mobile ? 5 : 1 });
  }
}

// ---------------------------------------------------------------------------------------------------
// The storyboard.
// ---------------------------------------------------------------------------------------------------
async function storyboard(d) {
  const u = DEMO_USER;

  await d.card('SmartCart', 'A full-stack e-commerce demo', ['Spring Boot 4', 'Angular 22', 'PostgreSQL', 'JWT + Google OAuth2'], 4200);

  // 1. Storefront
  await d.goto('/products', 'article.product-card');
  await d.syncTheme();
  await sleep(900);
  await d.show('app');
  await d.chapter('Storefront', 'A modern storefront with real product photos', 'Hero search, responsive grid, live stock badges');
  await sleep(2200);
  await d.scroll(520);
  // Page 1 is sorted by name; these are all on it.
  await d.hover('article.product-card', 'Ceramic Pour-Over', 900);
  await d.hover('article.product-card', 'Clean Architecture', 900);
  await d.hover('article.product-card', 'Domain-Driven Design', 1100);
  await d.scroll(700, 1300);

  // 2. Pagination & search
  await d.chapter('Catalogue', 'Server-side pagination and search', 'Spring Data paging, inactive products hidden');
  await d.click('button.page-link', 'Next', { pauseAfter: 1400 });
  await d.scroll(450, 1200);
  await d.scrollTop(900);
  await d.type('#search', 'lamp', { delay: 110 });
  await sleep(1800);
  await d.scroll(380, 1400);
  await d.scrollTop(700);
  await d.click('.hero-search-clear', undefined, { pauseAfter: 1300 });

  // 3. Product detail + guest redirect
  await d.chapter('Product page', 'Product details with live availability', 'Low-stock warning, quantity limits, breadcrumb');
  await d.click('.product-title a', 'Indoor Herb Garden Kit', { pauseAfter: 400 });
  await d.waitFor('.detail-title');
  await sleep(2000);
  await d.chapter('Auth guard', 'Guests are asked to sign in…', '…and are brought straight back afterwards');
  await d.click('button', 'Add to cart', { pauseAfter: 400 });
  await d.waitFor('form');
  await sleep(1800);

  // 4. Sign up (validation, then success)
  await d.chapter('Sign up', 'Email sign-up with validation', 'Client-side + server-side rules, or one click with Google');
  await d.click('a', 'Create an account', { pauseAfter: 300 });
  await d.waitFor('#confirmPassword');
  await sleep(900);
  await d.click('form button[type=submit]', undefined, { pauseAfter: 1600 });
  await d.type('#name', u.name);
  await d.type('#email', u.email);
  await d.type('#password', 'weak', { delay: 90 });
  await d.click('#confirmPassword', undefined, { pauseAfter: 1300 });
  await d.type('#password', u.password, { clear: true });
  await d.type('#confirmPassword', u.password);
  await d.setValue('#dateOfBirth', u.dateOfBirth);
  await d.setValue('#region', u.region);
  await d.type('#phoneNumber', u.phone);
  await sleep(500);
  await d.click('form button[type=submit]', undefined, { pauseAfter: 400 });
  await d.waitFor('a.account-chip');
  await sleep(1500);

  // 5. Add to cart from the product page
  await d.chapter('Cart', 'Add to cart with a quantity stepper', 'Limits follow stock and the per-line maximum');
  await d.scrollTop(500);
  await d.type('#search', 'headphones', { delay: 70 });
  await sleep(1300);
  await d.click('.product-title a', 'Wireless Headphones', { pauseAfter: 400 });
  await d.waitFor('.detail-title');
  await sleep(800);
  await d.click('app-quantity-stepper button[aria-label="Increase quantity"]', undefined, { pauseAfter: 500 });
  await d.click('app-quantity-stepper button[aria-label="Increase quantity"]', undefined, { pauseAfter: 600 });
  await d.click('button', 'Add to cart', { pauseAfter: 2200 });

  // 6. Quick add from the grid
  await d.chapter('Cart', 'Quick add straight from the grid', 'Toast confirmation and a live cart badge');
  await d.click('.crumbs a', 'Shop', { pauseAfter: 400 });
  await d.waitFor('article.product-card');
  await sleep(600);
  await d.scroll(520);
  await d.hover('article.product-card', 'Ceramic Pour-Over', 600);
  await d.click('article.product-card', 'Ceramic Pour-Over', { inner: '.quick-add', pauseAfter: 1800 });
  await d.hover('article.product-card', 'Clean Architecture', 500);
  await d.click('article.product-card', 'Clean Architecture', { inner: '.quick-add', pauseAfter: 2000 });

  // 7. Cart page
  await d.chapter('Cart', 'Cart with live totals', 'Server-validated quantities, remove, empty-cart confirmation');
  await d.click('a[aria-label^="Cart"]', undefined, { pauseAfter: 400 });
  await d.waitFor('.cart-line');
  await sleep(1500);
  await d.click('.cart-line:first-child button[aria-label="Increase quantity"]', undefined, { pauseAfter: 1300 });
  await d.click('.cart-line:first-child button[aria-label="Decrease quantity"]', undefined, { pauseAfter: 1300 });
  await d.click('.cart-line:last-child .remove-btn', undefined, { pauseAfter: 1700 });
  await d.hover('.summary-card', undefined, 1200);

  // 8. Dark mode
  await d.chapter('Theme', 'Dark mode — Light, Dark or follow the system', 'Saved per browser, no flash on reload');
  await d.click('app-theme-toggle button.nav-icon-btn', undefined, { pauseAfter: 900 });
  await d.click('.theme-option', 'Dark', { pauseAfter: 300 });
  await d.syncTheme();
  await sleep(1600);
  await d.click('a.nav-link', 'Shop', { pauseAfter: 400 });
  await d.waitFor('article.product-card');
  await sleep(1200);
  await d.scroll(600, 1400);
  await d.hover('article.product-card', 'Bamboo Makeup', 900);
  await d.scroll(700, 1400);

  // 9. Account
  await d.chapter('Account', 'Manage your profile', 'Name, birthday, country and phone (normalised to E.164)');
  await d.click('a.account-chip', undefined, { pauseAfter: 400 });
  await d.waitFor('#profileHeading');
  await sleep(1300);
  await d.click('button', 'Edit', { pauseAfter: 600 });
  await d.type('#name', ' Smith');
  await d.click('button', 'Save changes', { pauseAfter: 1800 });

  await d.chapter('Account', 'Change password', 'The current password is verified with BCrypt');
  await d.type('#currentPassword', u.password);
  await d.type('#newPassword', u.newPassword);
  await d.type('#confirmedNewPassword', u.newPassword);
  await d.click('button', 'Update password', { pauseAfter: 2000 });

  // 10. Deactivate & reactivate
  await d.chapter('Account', 'Deactivate — and come back any time', 'Tokens stop working immediately; reactivation re-checks the password');
  await d.click('button', 'Deactivate', { pauseAfter: 900 });
  await d.click('button', 'Yes, deactivate', { pauseAfter: 400 });
  await d.waitFor('form');
  await sleep(1600);
  await d.type('#email', u.email, { delay: 35 });
  await d.type('#password', u.newPassword, { delay: 35 });
  await d.click('form button[type=submit]', undefined, { pauseAfter: 1700 });
  await d.click('button', 'Reactivate my account', { pauseAfter: 400 });
  await d.waitFor('a.account-chip');
  await sleep(1600);

  // 11. Google sign-in
  await d.chapter('Google', 'Or sign in with Google (OAuth2)', 'Spring Security handles the flow, the app issues its own JWT');
  await d.click('button', 'Sign out', { pauseAfter: 400 });
  await d.waitFor('app-google-button a');
  await sleep(1200);
  // Stop at the button: the next screen is Google's own consent page, which needs a real Google account.
  await d.hover('app-google-button a', undefined, 2600);

  // 12. Mobile
  await d.goto('/products', 'article.product-card');
  await d.stopScreencast();
  await d.setViewport({ width: MOBILE.width, height: MOBILE.height, scale: MOBILE.scale, mobile: true });
  await d.startScreencast(MOBILE.width * MOBILE.scale, MOBILE.height * MOBILE.scale);
  await d.eval(`window.scrollTo(0, 0)`);
  await d.setCursorVisible(false);
  await sleep(1200);
  await d.show('mobile');
  await d.chapter('Responsive', 'Built mobile-first', 'Same app, touch-friendly on a phone');
  await sleep(2200);
  await d.scroll(700, 1500);
  await d.scroll(900, 1500);
  await d.scrollTop(1000);
  await d.click('button.navbar-toggler', undefined, { pauseAfter: 1800 });
  await d.click('button.navbar-toggler', undefined, { pauseAfter: 900 });

  await d.card('SmartCart', 'Thanks for watching', ['Secure auth', 'Cart', 'Dark mode', 'Responsive'], 3800);
}

// ---------------------------------------------------------------------------------------------------
// Main.
// ---------------------------------------------------------------------------------------------------
async function main() {
  const chromePath = CHROME_CANDIDATES.find((p) => existsSync(p));
  if (!chromePath) throw new Error('Google Chrome not found - set CHROME_PATH');
  mkdirSync(OUT_DIR, { recursive: true });
  rmSync(join(OUT_DIR, OUTPUT_NAME), { force: true });

  const profile = mkdtempSync(join(tmpdir(), 'smartcart-demo-'));
  const chrome = spawn(chromePath, [
    '--headless=new', `--remote-debugging-port=${PORT}`, `--user-data-dir=${profile}`,
    '--no-first-run', '--no-default-browser-check', '--hide-scrollbars', '--mute-audio',
    '--disable-background-timer-throttling', '--disable-renderer-backgrounding', '--disable-backgrounding-occluded-windows',
    '--window-size=1920,1080', 'about:blank',
  ], { stdio: 'ignore' });

  let cdp;
  try {
    let version;
    for (let i = 0; i < 50 && !version; i++) {
      version = await fetch(`http://127.0.0.1:${PORT}/json/version`).then((r) => r.json()).catch(() => null);
      if (!version) await sleep(200);
    }
    if (!version) throw new Error('Chrome DevTools endpoint did not come up');
    console.log(`Chrome ${version.Browser}`);

    cdp = new Cdp(version.webSocketDebuggerUrl);
    await cdp.opened();

    const attach = async (url) => {
      const { targetId } = await cdp.send('Target.createTarget', { url, newWindow: true });
      const { sessionId } = await cdp.send('Target.attachToTarget', { targetId, flatten: true });
      return sessionId;
    };

    // Encoder tab.
    const enc = await attach('about:blank');
    await cdp.send('Emulation.setDeviceMetricsOverride', { width: 1920, height: 1080, deviceScaleFactor: 1, mobile: false }, enc);
    await cdp.send('Emulation.setFocusEmulationEnabled', { enabled: true }, enc);
    await cdp.send('Runtime.evaluate', { expression: ENCODER_SCRIPT }, enc);
    await cdp.send('Browser.setDownloadBehavior', { behavior: 'allow', downloadPath: OUT_DIR, eventsEnabled: true });
    const supported = (await cdp.send('Runtime.evaluate', {
      expression: `['video/mp4;codecs=avc1.640028','video/mp4;codecs=avc1.42E01F','video/webm;codecs=vp9'].find(t => MediaRecorder.isTypeSupported(t))`,
      returnByValue: true,
    }, enc)).result.value;
    if (!supported) throw new Error('This Chrome cannot record MP4 or WebM');
    console.log(`Encoding as ${supported}`);

    // App tab.
    const app = await attach('about:blank');
    await cdp.send('Page.enable', {}, app);
    await cdp.send('Runtime.enable', {}, app);
    await cdp.send('Emulation.setFocusEmulationEnabled', { enabled: true }, app);
    await cdp.send('Page.addScriptToEvaluateOnNewDocument', { source: CURSOR_SCRIPT }, app);

    const director = new Director(cdp, app, enc);
    await director.setViewport({ width: VIEW.width, height: VIEW.height });
    await director.startScreencast(VIEW.width, VIEW.height);

    const started = await cdp.send('Runtime.evaluate', { expression: `__enc.start(${JSON.stringify(supported)})`, returnByValue: true }, enc);
    console.log(`Recording (${started.result.value})...`);
    const t0 = Date.now();

    try {
      await storyboard(director);
    } catch (error) {
      // Leave evidence: what the app tab looked like when the step failed.
      const shot = await cdp.send('Page.captureScreenshot', { format: 'png' }, app).catch(() => null);
      if (shot) writeFileSync(join(OUT_DIR, 'failure.png'), Buffer.from(shot.data, 'base64'));
      const url = await director.eval('location.href').catch(() => '?');
      console.error(`Step failed on ${url} (screenshot: ${join(OUT_DIR, 'failure.png')})`);
      throw error;
    }

    await director.stopScreencast();
    const filename = supported.startsWith('video/mp4') ? OUTPUT_NAME : OUTPUT_NAME.replace('.mp4', '.webm');
    const done = cdp.waitFor((m) => m.method === 'Browser.downloadProgress' && m.params.state === 'completed', 120000);
    const size = (await cdp.send('Runtime.evaluate', { expression: `__enc.stop(${JSON.stringify(filename)})`, awaitPromise: true, returnByValue: true }, enc)).result.value;
    await done;
    console.log(`Saved ${join(OUT_DIR, filename)} (${(size / 1024 / 1024).toFixed(1)} MB, ${Math.round((Date.now() - t0) / 1000)} s)`);
  } finally {
    try { await cdp?.send('Browser.close'); } catch { /* already closed */ }
    await sleep(800);
    chrome.kill();
    try { rmSync(profile, { recursive: true, force: true }); } catch { /* Chrome may still hold files briefly */ }
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
