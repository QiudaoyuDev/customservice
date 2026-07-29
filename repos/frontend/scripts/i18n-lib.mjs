// 共享逻辑：扫描源码中的静态 i18n key，并用 esbuild 加载 locale .ts 资源。
// 被 scripts/check-i18n.mjs 与 scripts/scan-i18n.mjs 复用。
import { Parser } from 'i18next-scanner';
import esbuild from 'esbuild';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
export const ROOT = path.resolve(__dirname, '..');

// 关键：本项目统一用 `const { t } = useTranslation(); t('key')`，
// 而 scanner 默认只识别 i18next.t / i18n.t，因此必须把 `t` 加进 func.list。
const SCANNER_OPTS = {
  func: { list: ['t', 'i18next.t', 'i18n.t'] },
  lngs: ['en', 'zh'],
  ns: ['translation'],
  defaultNs: 'translation',
  keySeparator: '.',
  nsSeparator: ':',
};

// 收集 src 中所有静态 t('key') 字面量键。
// 动态拼接键（如 status.${code}、regions.${code}）不会被静态扫描捕获，
// 这类键由运行期 missingKeyHandler 兜底告警。
// 先用 esbuild 把 TS/TSX 转成纯 JS 并去掉注释与类型：
//  - 避免注释里的 t('a.b.c') 被误扫；
//  - 去掉 TS 的 `as`/类型注解，否则 scanner 内置的 esprima 解析 options 对象会报错。
export async function extractSourceKeys(srcDir = path.join(ROOT, 'src')) {
  const parser = new Parser(SCANNER_OPTS);
  const skipDir = /(node_modules|\.i18n[\\/]|i18n[\\/]locales[\\/]|dist[\\/])/;
  const testRe = /\.(test|spec)\.[tj]sx?$/;

  const walk = async (dir) => {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const full = path.join(dir, entry.name);
      if (skipDir.test(full)) continue;
      if (entry.isDirectory()) {
        await walk(full);
      } else if (/\.[tj]sx?$/.test(entry.name) && !testRe.test(entry.name)) {
        let content = fs.readFileSync(full, 'utf8');
        try {
          const loader = full.endsWith('.tsx') ? 'tsx' : 'ts';
          const out = await esbuild.transform(content, {
            loader,
            jsx: 'transform',
            legalComments: 'none',
          });
          content = out.code;
        } catch {
          // 转译失败则退回原始内容，仍尝试扫描。
        }
        parser.parseFuncFromString(content, { filename: full });
      }
    }
  };
  await walk(srcDir);

  const tree = parser.get().en?.translation ?? {};
  return flattenKeys(tree);
}

// 用 esbuild 把 locale .ts 转译为 ESM 后通过 data URL 动态导入，得到资源对象。
export async function loadLocaleTs(relPath) {
  const file = path.join(ROOT, relPath);
  const code = fs.readFileSync(file, 'utf8');
  const out = await esbuild.transform(code, {
    loader: 'ts',
    format: 'esm',
    sourcefile: file,
  });
  const dataUrl = 'data:text/javascript;base64,' + Buffer.from(out.code).toString('base64');
  const mod = await import(dataUrl);
  return mod.default;
}

// 把嵌套对象展开成点分 key 集合（只展开叶子节点，命名空间本身不算翻译键）。
export function flattenKeys(obj, prefix = '') {
  const keys = new Set();
  if (obj == null || typeof obj !== 'object' || Array.isArray(obj)) {
    if (prefix) keys.add(prefix);
    return keys;
  }
  for (const [k, v] of Object.entries(obj)) {
    const p = prefix ? `${prefix}.${k}` : k;
    if (v != null && typeof v === 'object' && !Array.isArray(v)) {
      for (const sub of flattenKeys(v, p)) keys.add(sub);
    } else {
      keys.add(p);
    }
  }
  return keys;
}
