declare module '@/features/courses/utils/logic-engine.js' {
  export interface LogicParseResult {
    ast: unknown | null
    error: string | null
  }

  export const Logic: {
    tryParse(source: string): LogicParseResult
    variables(ast: unknown): string[]
    evaluate(ast: unknown, env: Record<string, boolean>): boolean
    equivalent(left: unknown, right: unknown): boolean
  }

  export default Logic
}
