// 生成本次扫描到的静态 key 报告（.i18n/extracted-keys.json），便于人工核查与 diff。
import fs from 'node:fs';
import path from 'node:path';
import { extractSourceKeys, ROOT } from './i18n-lib.mjs';

const keys = [...(await extractSourceKeys())].sort();
const outDir = path.join(ROOT, '.i18n');
fs.mkdirSync(outDir, { recursive: true });
const outFile = path.join(outDir, 'extracted-keys.json');
fs.writeFileSync(outFile, JSON.stringify({ count: keys.length, keys }, null, 2));
console.log(
  `✓ i18n: 扫描到静态 key ${keys.length} 个，已写入 ${path.relative(ROOT, outFile)}`,
);
