<script setup lang="ts">
withDefaults(defineProps<{
  variant?: 'white' | 'feather' | 'macaw' | 'fox' | 'beetle' | 'ghost'
  size?: 'sm' | 'md'
}>(), {
  variant: 'feather',
  size: 'md',
})

const VARIANT_CLASSES: Record<string, string> = {
  white: 'bg-white text-lx-macaw shadow-[0_4px_0_rgba(0,0,0,0.16)] active:shadow-[0_0_0_rgba(0,0,0,0)]',
  feather: 'bg-lx-feather text-white shadow-[0_4px_0_var(--color-lx-feather-dark)] active:shadow-[0_0_0_transparent]',
  macaw: 'bg-lx-macaw text-white shadow-[0_4px_0_var(--color-lx-macaw-dark)] active:shadow-[0_0_0_transparent]',
  fox: 'bg-lx-fox text-white shadow-[0_4px_0_var(--color-lx-fox-dark)] active:shadow-[0_0_0_transparent]',
  beetle: 'bg-lx-beetle text-white shadow-[0_4px_0_var(--color-lx-beetle-dark)] active:shadow-[0_0_0_transparent]',
  ghost: 'bg-lx-surface-soft text-lx-ink shadow-[0_4px_0_#dcdcdc] active:shadow-[0_0_0_transparent]',
}
</script>

<template>
  <button
    type="button"
    :class="[
      'inline-flex items-center justify-center gap-1.5 rounded-2xl font-extrabold transition-transform duration-75 active:translate-y-1 disabled:opacity-50 disabled:pointer-events-none',
      size === 'sm' ? 'px-4 py-2 text-[12.5px]' : 'px-5 py-2.5 text-[13.5px]',
      VARIANT_CLASSES[variant],
    ]"
  >
    <slot />
  </button>
</template>
