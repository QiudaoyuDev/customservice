import i18n from 'i18next';
import { initReactI18next, useTranslation as useTranslationReact } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import type { TFunction } from 'i18next';
import en from './locales/en';
import zh from './locales/zh';

/**
 * 真正的 react-i18next 集成。
 * - 复用已有 en/zh 嵌套资源（t('a.b.c') 取值，{{var}} 占位插值，沿用 i18next 默认 keySeparator/插值）。
 * - 语言检测顺序：localStorage(app.lang) -> 浏览器语言 -> 回退 en-US；选择持久化到 localStorage。
 * - 当前提供 en-US / zh-CN 完整翻译；de-DE / fr-FR / es-ES 选项保留，回退英文。
 * - 资源随包内联，初始化同步完成，组件无需 Suspense。
 */
export const LANGS = ['en-US', 'zh-CN', 'de-DE', 'fr-FR', 'es-ES'];
export const langNames: Record<string, string> = {
  'en-US': 'English',
  'zh-CN': '中文 (简体)',
  'de-DE': 'Deutsch',
  'fr-FR': 'Français',
  'es-ES': 'Español',
};

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      'en-US': { translation: en },
      'zh-CN': { translation: zh },
      // 别名，便于浏览器仅返回语言基础码（如 en / zh）时直接命中
      en: { translation: en },
      zh: { translation: zh },
    },
    fallbackLng: 'en-US',
    nonExplicitSupportedLngs: true,
    returnNull: false,
    interpolation: {
      // React 自身负责转义，关闭 i18next 的 HTML 转义
      escapeValue: false,
    },
    detection: {
      order: ['localStorage', 'navigator', 'htmlTag'],
      caches: ['localStorage'],
      lookupLocalStorage: 'app.lang',
    },
  });

/** 语言切换时同步 <html lang>，便于无障碍与字体回退。 */
function syncHtmlLang(lng: string) {
  const base = lng.split('-')[0];
  document.documentElement.lang = base === 'zh' ? 'zh-CN' : base === 'en' ? 'en' : lng;
}
syncHtmlLang(i18n.language);
i18n.on('languageChanged', syncHtmlLang);

/** 状态码 -> 本地化标签；未配置时回退原值。 */
export function statusLabel(t: TFunction, code: string): string {
  const key = 'status.' + String(code).toLowerCase();
  const v = t(key);
  return v === key ? code : v;
}

/** 地区码 -> 本地化友好名；未配置时回退原码。 */
export function regionLabel(t: TFunction, code: string): string {
  if (!code) return '';
  const key = 'regions.' + String(code).toLowerCase();
  const v = t(key);
  return v === key ? code : v;
}

export const useTranslation = useTranslationReact;
export type { TFunction };
export default i18n;
