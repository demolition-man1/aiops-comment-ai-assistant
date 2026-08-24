import enElement from 'element-plus/es/locale/lang/en'
import ptBrElement from 'element-plus/es/locale/lang/pt-br'
import zhCnElement from 'element-plus/es/locale/lang/zh-cn'
import { createI18n } from 'vue-i18n'

import enUS from './locales/en-US'
import ptBR from './locales/pt-BR'
import zhCN from './locales/zh-CN'

export type AppLocale = 'zh-CN' | 'en-US' | 'pt-BR'

export const localeStorageKey = 'aiops_locale'
export const defaultLocale: AppLocale = 'zh-CN'

export const supportedLocales: Array<{ code: AppLocale; label: string; shortLabel: string }> = [
  { code: 'zh-CN', label: '简体中文', shortLabel: '中' },
  { code: 'en-US', label: 'English', shortLabel: 'EN' },
  { code: 'pt-BR', label: 'Português', shortLabel: 'PT' }
]

export const elementPlusLocales = {
  'zh-CN': zhCnElement,
  'en-US': enElement,
  'pt-BR': ptBrElement
}

export const normalizeLocale = (value: unknown): AppLocale => {
  return supportedLocales.some((item) => item.code === value) ? (value as AppLocale) : defaultLocale
}

export const i18n = createI18n({
  legacy: false,
  locale: normalizeLocale(localStorage.getItem(localeStorageKey)),
  fallbackLocale: defaultLocale,
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
    'pt-BR': ptBR
  }
})
