const express = require('express');
const cors = require('cors');
const { execFileSync } = require('child_process');
const path = require('path');

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

const MAUNS = '/tmp/mauns-install/node_modules/mauns/bin/mauns-linux-x64';

app.post('/run', (req, res) => {
  const { task } = req.body;
  if (!task) return res.status(400).json({ error: 'task required' });
  try {
    const out = execFileSync(MAUNS, [task], {
      encoding: 'utf8',
      timeout: 120000,
      env: { ...process.env },
      maxBuffer: 10 * 1024 * 1024,
    });
    res.json({ output: out });
  } catch (err) {
    res.status(500).json({ error: err.stderr || err.message });
  }
});

app.get('/health', (req, res) => res.json({ ok: true }));

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Mauns web listening on :${PORT}`));
