import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { defaultLocale, elementPlusLocales, i18n, localeStorageKey, normalizeLocale, supportedLocales } from '@/i18n'
import type { AppLocale } from '@/i18n'

export const useLocaleStore = defineStore('locale', () => {
  const locale = ref<AppLocale>(normalizeLocale(localStorage.getItem(localeStorageKey) || defaultLocale))

  const setLocale = (nextLocale: AppLocale) => {
    locale.value = normalizeLocale(nextLocale)
    i18n.global.locale.value = locale.value
    localStorage.setItem(localeStorageKey, locale.value)
    document.documentElement.lang = locale.value
  }

  setLocale(locale.value)

  return {
    locale,
    languages: supportedLocales,
    elementLocale: computed(() => elementPlusLocales[locale.value]),
    setLocale
  }
})
