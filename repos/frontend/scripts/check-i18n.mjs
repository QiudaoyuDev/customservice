// CI 防回归门禁：校验「源码静态 key ⊆ en 资源」「源码静态 key ⊆ zh 资源」「en 资源 ⊆ zh 资源」。
// 任一项不满足则以非零退出码失败，防止新串漏翻或拼写错误。
import { extractSourceKeys, loadLocaleTs, flattenKeys } from './i18n-lib.mjs';

const sourceKeys = [...(await extractSourceKeys())].sort();
const enKeys = flattenKeys(await loadLocaleTs('src/i18n/locales/en.ts'));
const zhKeys = flattenKeys(await loadLocaleTs('src/i18n/locales/zh.ts'));

// i18next 在传入 count 时会查找 key_one / key_other 等复数形式。
// 若复数键缺失但其基础键 key 已存在，视为可接受（安全网，真实复数键仍应补全）。
const PLURAL_SUFFIXES = ['_zero', '_one', '_two', '_few', '_many', '_other'];
const baseOf = (k) => {
  for (const s of PLURAL_SUFFIXES) if (k.endsWith(s)) return k.slice(0, -s.length);
  return null;
};
const satisfiedIn = (k, set) => set.has(k) || (baseOf(k) != null && set.has(baseOf(k)));

const missingEn = sourceKeys.filter((k) => !satisfiedIn(k, enKeys)); // 源码引用但 en 缺失（多为拼写错误）
const missingZh = sourceKeys.filter((k) => !satisfiedIn(k, zhKeys)); // 源码引用但 zh 缺失
const untranslated = [...enKeys].filter((k) => !zhKeys.has(k)).sort(); // en 有但 zh 漏翻

let ok = true;
if (missingEn.length) {
  ok = false;
  console.error('✗ i18n: 源码引用了但 en 资源缺失的 key（疑似拼写错误）：');
  missingEn.forEach((k) => console.error('    - ' + k));
}
if (missingZh.length) {
  ok = false;
  console.error('✗ i18n: 源码引用了但 zh 资源缺失的 key：');
  missingZh.forEach((k) => console.error('    - ' + k));
}
if (untranslated.length) {
  ok = false;
  console.error('✗ i18n: en 中存在但 zh 未翻译的 key（漏翻）：');
  untranslated.forEach((k) => console.error('    - ' + k));
}

if (ok) {
  console.log(
    `✓ i18n: 校验通过。源码静态 key ${sourceKeys.length} 个，en/zh 资源完整一致。`,
  );
  process.exit(0);
} else {
  console.error('\n请补充缺失的翻译后重新运行 `npm run check:i18n`。');
  process.exit(1);
}
