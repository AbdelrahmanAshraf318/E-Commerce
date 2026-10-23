// Renders roadmap.html to PDF with headless Chrome (no dependencies): node render.mjs <in.html> <out.pdf>
import { spawn } from 'node:child_process';
import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const [input, output] = process.argv.slice(2).map((p) => resolve(p));
const PORT = 9335;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const chrome = spawn('C:/Program Files/Google/Chrome/Application/chrome.exe', [
  '--headless=new', `--remote-debugging-port=${PORT}`, `--user-data-dir=${mkdtempSync(join(tmpdir(), 'pdf-'))}`,
  '--no-first-run', '--no-default-browser-check', 'about:blank',
], { stdio: 'ignore' });

try {
  let page;
  for (let i = 0; i < 50 && !page; i++) {
    const list = await fetch(`http://127.0.0.1:${PORT}/json/list`).then((r) => r.json()).catch(() => null);
    page = list?.find((t) => t.type === 'page');
    if (!page) await sleep(200);
  }
  const ws = new WebSocket(page.webSocketDebuggerUrl);
  await new Promise((r) => ws.addEventListener('open', r, { once: true }));
  let id = 0;
  const pending = new Map();
  ws.addEventListener('message', (e) => {
    const m = JSON.parse(e.data);
    if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); }
  });
  const send = (method, params = {}) => new Promise((res, rej) => {
    const i = ++id;
    pending.set(i, (m) => (m.error ? rej(new Error(`${method}: ${m.error.message}`)) : res(m.result)));
    ws.send(JSON.stringify({ id: i, method, params }));
  });

  await send('Page.enable');
  await send('Page.navigate', { url: pathToFileURL(input).href });
  // Wait for layout and web fonts, so the PDF never falls back to a system font.
  for (let i = 0; i < 60; i++) {
    const r = await send('Runtime.evaluate', {
      expression: `document.readyState === 'complete' && document.fonts.status === 'loaded'`, returnByValue: true,
    });
    if (r.result.value) break;
    await sleep(250);
  }
  const fonts = await send('Runtime.evaluate', {
    expression: `document.fonts.check('700 12px "Plus Jakarta Sans"')`, returnByValue: true,
  });
  console.log('Brand font loaded:', fonts.result.value);

  const footer = `<div style="width:100%;font-family:Segoe UI,sans-serif;font-size:7.5px;color:#94a3b8;padding:0 16mm;display:flex;justify-content:space-between">
      <span>SmartCart · Roadmap &amp; feature ideas</span><span><span class="pageNumber"></span> / <span class="totalPages"></span></span></div>`;
  const pdf = await send('Page.printToPDF', {
    printBackground: true,
    preferCSSPageSize: true,
    displayHeaderFooter: true,
    headerTemplate: '<span></span>',
    footerTemplate: footer,
  });
  writeFileSync(output, Buffer.from(pdf.data, 'base64'));
  console.log('Wrote', output);
  if (process.env.SPLIT_DIR) {
    // One PDF per page, for visual review.
    for (let n = 1; n < 100; n++) {
      try {
        const one = await send('Page.printToPDF', { printBackground: true, preferCSSPageSize: true, displayHeaderFooter: true,
          headerTemplate: '<span></span>', footerTemplate: footer, pageRanges: String(n) });
        writeFileSync(join(process.env.SPLIT_DIR, `page-${String(n).padStart(2, '0')}.pdf`), Buffer.from(one.data, 'base64'));
      } catch { console.log('Total pages:', n - 1); break; }
    }
  }
  await send('Browser.close').catch(() => {});
} finally {
  chrome.kill();
}
