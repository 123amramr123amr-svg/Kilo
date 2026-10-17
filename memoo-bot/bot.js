require('dotenv').config();
const { Telegraf } = require('telegraf');
const { execFileSync } = require('child_process');

const TOKEN = process.env.BOT_TOKEN;
const MAUNS = '/tmp/mauns-install/node_modules/mauns/bin/mauns-linux-x64';

if (!TOKEN) {
  console.error('أضف BOT_TOKEN في ملف .env');
  process.exit(1);
}

const bot = new Telegraf(TOKEN);

bot.start((ctx) => ctx.reply('أهلاً! أنا Memoo. أرسل لي أي مهمة وسأنفّذها لك.'));

bot.help((ctx) => ctx.reply('أرسل أي نص وسأقوم بتنفيذه عبر وكيل Mauns.\nمثال: أنشئ ملف hello.txt واكتب فيه Hello World'));

bot.on('text', async (ctx) => {
  const task = ctx.message.text.trim();
  await ctx.reply('جاري التنفيذ...');
  try {
    const out = execFileSync(MAUNS, [task], {
      encoding: 'utf8',
      timeout: 120000,
      env: { ...process.env },
      maxBuffer: 10 * 1024 * 1024,
    });
    const text = out.slice(0, 4000) || '(لا مخرجات)';
    ctx.reply(text);
  } catch (err) {
    ctx.reply('خطأ: ' + (err.stderr || err.message).slice(0, 1000));
  }
});

bot.launch().then(() => console.log('Memoo bot running...'));
