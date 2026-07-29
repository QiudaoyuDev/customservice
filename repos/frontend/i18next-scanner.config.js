// i18next-scanner 配置文件（standalone CLI 使用：`npx i18next-scanner`）。
// 与 scripts/i18n-lib.mjs 保持同一套规则：func.list 必须包含 `t`，
// 否则扫描不到本项目 `const { t } = useTranslation(); t('key')` 的调用。
export default {
  input: ['src/**/*.{ts,tsx}'],
  // 跳过 locale 资源文件本身，避免把翻译值当成 key 反向扫入。
  output: '.i18n/$LOCALE/$NAMESPACE.json',
  options: {
    debug: false,
    func: {
      list: ['t', 'i18next.t', 'i18n.t'],
    },
    trans: {
      component: 'Trans',
      i18nKey: 'i18nKey',
    },
    lngs: ['en', 'zh'],
    ns: ['translation'],
    defaultNs: 'translation',
    keySeparator: '.',
    nsSeparator: ':',
    resource: {
      loadPath: '.i18n/$LOCALE/$NAMESPACE.json',
      savePath: '.i18n/$LOCALE/$NAMESPACE.json',
      jsonIndent: 2,
    },
  },
};
