export type CourseCoverPreset = {
  id: string
  symbol: string
  bgClass: string
  textClass: string
}

export const COURSE_COVER_PRESETS: CourseCoverPreset[] = [
  { id: 'integral', symbol: '∫', bgClass: 'bg-[#ede9fe]', textClass: 'text-[#5b21b6]' },
  { id: 'pi', symbol: 'π', bgClass: 'bg-[#dbeafe]', textClass: 'text-[#1d4ed8]' },
  { id: 'sigma', symbol: 'Σ', bgClass: 'bg-[#fce7f3]', textClass: 'text-[#9d174d]' },
  { id: 'sqrt', symbol: '√', bgClass: 'bg-[#d1fae5]', textClass: 'text-[#047857]' },
  { id: 'fx', symbol: 'ƒx', bgClass: 'bg-[#ffedd5]', textClass: 'text-[#c2410c]' },
  { id: 'infinity', symbol: '∞', bgClass: 'bg-[#e0e7ff]', textClass: 'text-[#4338ca]' },
]

export const DEFAULT_COVER_ID = COURSE_COVER_PRESETS[0].id

export function getCoverPreset(coverId: string): CourseCoverPreset {
  return COURSE_COVER_PRESETS.find((p) => p.id === coverId) ?? COURSE_COVER_PRESETS[0]
}
