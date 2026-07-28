import {createContext, ReactNode, useCallback, useContext, useEffect, useState} from 'react';
import en from './locales/en';
import zh from './locales/zh';

/**
 * 轻量国际化模块（遵循 i18next / react-i18next 约定：useTranslation() -> { t, i18n }）。
 * - 零运行时依赖，开箱即跑；后续如需更完整能力（复数、格式化）可平滑替换为 react-i18next。
 * - 资源以嵌套 key 组织，t('a.b.c') 取值，{{var}} 占位插值。
 * - 语言检测顺序：localStorage(app.lang) -> 浏览器语言 -> 回退 en-US；选择持久化到 localStorage。
 * - 当前提供 en-US / zh-CN 完整翻译；de-DE / fr-FR / es-ES 选项保留，回退英文。
 */
type Dict = Record<string, any>;
const resources: Record<string, Dict> = {
    'en-US': en, en, 'zh-CN': zh, zh,
};
export const LANGS = ['en-US', 'zh-CN', 'de-DE', 'fr-FR', 'es-ES'];
export const langNames: Record<string, string> = {
    'en-US': 'English', 'zh-CN': '中文 (简体)', 'de-DE': 'Deutsch', 'fr-FR': 'Français', 'es-ES': 'Español',
};
const fallbackLng = 'en-US';
const STORAGE_KEY = 'app.lang';

function resolve(lng: string): string {
    if (!lng) return fallbackLng;
    if (resources[lng]) return lng;
    const base = lng.split('-')[0].toLowerCase();
    if (resources[base]) return base;
    const hit = LANGS.find((s) => s.split('-')[0].toLowerCase() === base);
    return hit ? hit.split('-')[0].toLowerCase() : fallbackLng;
}

function getDict(lng: string): Dict {
    return resources[resolve(lng)] ?? resources[fallbackLng];
}

function lookup(dict: Dict, key: string): string | undefined {
    return key.split('.').reduce<any>((o, k) => (o == null ? undefined : o[k]), dict);
}

function interpolate(template: string, params?: Record<string, any>): string {
    if (!params) return template;
    return template.replace(/\{\{(\w+)\}\}/g, (_, k) => (params[k] !== undefined ? String(params[k]) : `{{${k}}}`));
}

export type TFunction = (key: string, params?: Record<string, any>) => string;

export function createT(lng: string): TFunction {
    const dict = getDict(lng);
    const fb = resources[fallbackLng];
    return (key, params) => {
        let val = lookup(dict, key);
        if (val === undefined) val = lookup(fb, key);
        if (typeof val !== 'string') return key;
        return interpolate(val, params);
    };
}

/** 状态码 -> 本地化标签；未配置时回退原值。 */
export function statusLabel(t: TFunction, code: string): string {
    const key = 'status.' + String(code).toLowerCase();
    const v = t(key);
    return v === key ? code : v;
}

interface I18nValue {
    language: string;
    t: TFunction;
    changeLanguage: (l: string) => void;
}

const I18nContext = createContext<I18nValue | null>(null);

export function I18nProvider({children}: { children: ReactNode }) {
    const [language, setLanguage] = useState<string>(() => {
        const saved = typeof localStorage !== 'undefined' ? localStorage.getItem(STORAGE_KEY) : null;
        if (saved) return resolve(saved);
        const nav = typeof navigator !== 'undefined' ? navigator.language : fallbackLng;
        return resolve(nav);
    });
    useEffect(() => {
        try {
            localStorage.setItem(STORAGE_KEY, language);
        } catch {
            /* ignore */
        }
    }, [language]);
    const t = createT(language);
    const changeLanguage = useCallback((l: string) => setLanguage(resolve(l)), []);
    return <I18nContext.Provider value={{language, t, changeLanguage}}>{children}</I18nContext.Provider>;
}

export function useTranslation() {
    const ctx = useContext(I18nContext);
    if (!ctx) throw new Error('useTranslation must be used within <I18nProvider>');
    return {t: ctx.t, i18n: {language: ctx.language, changeLanguage: ctx.changeLanguage}};
}
