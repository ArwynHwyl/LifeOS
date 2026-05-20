export type CourseCoverPreset = {
  id: string
  symbol: string
  bgClass: string
  textClass: string
}

export type CoverSymbol = {
  id: string
  symbol: string
  label: string
  category: string
}

export type CoverColorTheme = {
  id: string
  name: string
  bgClass: string
  textClass: string
  swatch: string
}

export const COVER_SYMBOLS: CoverSymbol[] = [
  // Calculus
  { id: 'integral',  symbol: '∫',    label: 'Integral',         category: 'Calculus' },
  { id: 'partial',   symbol: '∂',    label: 'Partial',          category: 'Calculus' },
  { id: 'nabla',     symbol: '∇',    label: 'Gradient',         category: 'Calculus' },
  { id: 'limit',     symbol: 'lim',  label: 'Limit',            category: 'Calculus' },
  { id: 'deriv',     symbol: "f'",   label: 'Derivative',       category: 'Calculus' },
  { id: 'dx',        symbol: 'dx',   label: 'Differential',     category: 'Calculus' },
  // Algebra
  { id: 'sqrt',      symbol: '√',    label: 'Square Root',      category: 'Algebra' },
  { id: 'fx',        symbol: 'ƒx',   label: 'Function',         category: 'Algebra' },
  { id: 'delta',     symbol: 'Δ',    label: 'Delta',            category: 'Algebra' },
  { id: 'xsq',       symbol: 'x²',   label: 'Quadratic',        category: 'Algebra' },
  { id: 'absx',      symbol: '|x|',  label: 'Absolute Value',   category: 'Algebra' },
  { id: 'log',       symbol: 'log',  label: 'Logarithm',        category: 'Algebra' },
  // Trigonometry
  { id: 'theta',     symbol: 'θ',    label: 'Theta',            category: 'Trigonometry' },
  { id: 'sin',       symbol: 'sin',  label: 'Sine',             category: 'Trigonometry' },
  { id: 'cos',       symbol: 'cos',  label: 'Cosine',           category: 'Trigonometry' },
  { id: 'tan',       symbol: 'tan',  label: 'Tangent',          category: 'Trigonometry' },
  { id: 'pi',        symbol: 'π',    label: 'Pi',               category: 'Trigonometry' },
  // Linear Algebra
  { id: 'matrix',    symbol: '[]',   label: 'Matrix',           category: 'Linear Algebra' },
  { id: 'lambda',    symbol: 'λ',    label: 'Eigenvalue',       category: 'Linear Algebra' },
  { id: 'sigma',     symbol: 'Σ',    label: 'Summation',        category: 'Linear Algebra' },
  { id: 'det',       symbol: 'det',  label: 'Determinant',      category: 'Linear Algebra' },
  { id: 'transpose', symbol: 'Aᵀ',   label: 'Transpose',        category: 'Linear Algebra' },
  // Number Sets
  { id: 'real',      symbol: 'ℝ',    label: 'Real Numbers',     category: 'Number Sets' },
  { id: 'natural',   symbol: 'ℕ',    label: 'Natural Numbers',  category: 'Number Sets' },
  { id: 'integer',   symbol: 'ℤ',    label: 'Integers',         category: 'Number Sets' },
  { id: 'rational',  symbol: 'ℚ',    label: 'Rational',         category: 'Number Sets' },
  { id: 'complex',   symbol: 'ℂ',    label: 'Complex Numbers',  category: 'Number Sets' },
  // Statistics
  { id: 'mu',        symbol: 'μ',    label: 'Mean',             category: 'Statistics' },
  { id: 'sigmastat', symbol: 'σ',    label: 'Std Dev',          category: 'Statistics' },
  { id: 'prob',      symbol: 'P(x)', label: 'Probability',      category: 'Statistics' },
  { id: 'chisq',     symbol: 'χ²',   label: 'Chi-squared',      category: 'Statistics' },
  { id: 'normdist',  symbol: '~N',   label: 'Normal Dist',      category: 'Statistics' },
  // Geometry
  { id: 'angle',     symbol: '∠',    label: 'Angle',            category: 'Geometry' },
  { id: 'triangle',  symbol: '△',    label: 'Triangle',         category: 'Geometry' },
  { id: 'perp',      symbol: '⊥',    label: 'Perpendicular',    category: 'Geometry' },
  { id: 'infinity',  symbol: '∞',    label: 'Infinity',         category: 'Geometry' },
  // Discrete Math
  { id: 'intersect', symbol: '∩',    label: 'Intersection',     category: 'Discrete Math' },
  { id: 'union',     symbol: '∪',    label: 'Union',            category: 'Discrete Math' },
  { id: 'emptyset',  symbol: '∅',    label: 'Empty Set',        category: 'Discrete Math' },
  { id: 'forall',    symbol: '∀',    label: 'For All',          category: 'Discrete Math' },
  { id: 'exists',    symbol: '∃',    label: 'There Exists',     category: 'Discrete Math' },
  { id: 'nfact',     symbol: 'n!',   label: 'Factorial',        category: 'Discrete Math' },
]

export const COVER_COLOR_THEMES: CoverColorTheme[] = [
  { id: 'violet',  name: 'Violet',  bgClass: 'bg-[#ede9fe]', textClass: 'text-[#5b21b6]', swatch: '#7c3aed' },
  { id: 'blue',    name: 'Blue',    bgClass: 'bg-[#dbeafe]', textClass: 'text-[#1d4ed8]', swatch: '#2563eb' },
  { id: 'indigo',  name: 'Indigo',  bgClass: 'bg-[#e0e7ff]', textClass: 'text-[#4338ca]', swatch: '#4f46e5' },
  { id: 'sky',     name: 'Sky',     bgClass: 'bg-[#e0f2fe]', textClass: 'text-[#0369a1]', swatch: '#0ea5e9' },
  { id: 'teal',    name: 'Teal',    bgClass: 'bg-[#ccfbf1]', textClass: 'text-[#0f766e]', swatch: '#14b8a6' },
  { id: 'green',   name: 'Green',   bgClass: 'bg-[#d1fae5]', textClass: 'text-[#047857]', swatch: '#10b981' },
  { id: 'lime',    name: 'Lime',    bgClass: 'bg-[#ecfccb]', textClass: 'text-[#3f6212]', swatch: '#84cc16' },
  { id: 'amber',   name: 'Amber',   bgClass: 'bg-[#fef3c7]', textClass: 'text-[#b45309]', swatch: '#f59e0b' },
  { id: 'orange',  name: 'Orange',  bgClass: 'bg-[#ffedd5]', textClass: 'text-[#c2410c]', swatch: '#f97316' },
  { id: 'rose',    name: 'Rose',    bgClass: 'bg-[#ffe4e6]', textClass: 'text-[#be123c]', swatch: '#f43f5e' },
  { id: 'pink',    name: 'Pink',    bgClass: 'bg-[#fce7f3]', textClass: 'text-[#9d174d]', swatch: '#ec4899' },
  { id: 'fuchsia', name: 'Fuchsia', bgClass: 'bg-[#fae8ff]', textClass: 'text-[#86198f]', swatch: '#d946ef' },
]

// Legacy color mapping so old mock data keeps its original colors
const LEGACY_COLOR_MAP: Record<string, string> = {
  integral: 'violet',
  pi:       'blue',
  sigma:    'pink',
  sqrt:     'green',
  fx:       'orange',
  infinity: 'indigo',
}

export function parseCoverId(coverId: string): { symbolId: string; colorId: string } {
  const lastUnderscore = coverId.lastIndexOf('_')
  if (lastUnderscore !== -1) {
    const colorId = coverId.slice(lastUnderscore + 1)
    if (COVER_COLOR_THEMES.some((c) => c.id === colorId)) {
      return { symbolId: coverId.slice(0, lastUnderscore), colorId }
    }
  }
  // Legacy single-id format
  return { symbolId: coverId, colorId: LEGACY_COLOR_MAP[coverId] ?? COVER_COLOR_THEMES[0].id }
}

export function buildCoverId(symbolId: string, colorId: string): string {
  return `${symbolId}_${colorId}`
}

export function getCoverPreset(coverId: string): CourseCoverPreset {
  const { symbolId, colorId } = parseCoverId(coverId)
  const sym = COVER_SYMBOLS.find((s) => s.id === symbolId) ?? COVER_SYMBOLS[0]
  const col = COVER_COLOR_THEMES.find((c) => c.id === colorId) ?? COVER_COLOR_THEMES[0]
  return { id: coverId, symbol: sym.symbol, bgClass: col.bgClass, textClass: col.textClass }
}

export const DEFAULT_COVER_ID = buildCoverId(COVER_SYMBOLS[0].id, COVER_COLOR_THEMES[0].id)
