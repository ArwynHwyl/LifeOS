const FUNCTIONS: Record<string, (...values: number[]) => number> = {
  sin: Math.sin,
  cos: Math.cos,
  tan: Math.tan,
  sqrt: Math.sqrt,
  abs: Math.abs,
  log: Math.log10,
  ln: Math.log,
  exp: Math.exp,
  min: Math.min,
  max: Math.max,
  pow: Math.pow,
}

const PRECEDENCE: Record<string, number> = {
  '+': 1,
  '-': 1,
  '*': 2,
  '/': 2,
  '^': 3,
}

type Token = string | number

export function evaluateExpression(expression: string, variables: Record<string, number>) {
  const tokens = tokenize(expression)
  const rpn = toRpn(tokens)
  return evalRpn(rpn, variables)
}

function tokenize(expression: string) {
  const tokens: Token[] = []
  let index = 0
  let previous: Token | null = null
  while (index < expression.length) {
    const char = expression[index]
    if (/\s/.test(char)) {
      index += 1
      continue
    }
    if (/\d|\./.test(char)) {
      let end = index + 1
      while (end < expression.length && /[\d.]/.test(expression[end])) end += 1
      const value = Number(expression.slice(index, end))
      if (!Number.isFinite(value)) throw new Error('Invalid number')
      tokens.push(value)
      previous = value
      index = end
      continue
    }
    if (/[A-Za-z_]/.test(char)) {
      let end = index + 1
      while (end < expression.length && /[A-Za-z0-9_]/.test(expression[end])) end += 1
      const ident = expression.slice(index, end)
      tokens.push(ident)
      previous = ident
      index = end
      continue
    }
    if ('+-*/^(),'.includes(char)) {
      const unaryMinus = char === '-' && (previous == null || (typeof previous === 'string' && '+-*/^(,'.includes(previous)))
      tokens.push(unaryMinus ? 'neg' : char)
      previous = char
      index += 1
      continue
    }
    throw new Error('Unsupported character')
  }
  return tokens
}

function toRpn(tokens: Token[]) {
  const output: Token[] = []
  const operators: string[] = []
  for (let index = 0; index < tokens.length; index += 1) {
    const token = tokens[index]
    const next = tokens[index + 1]
    if (typeof token === 'number') {
      output.push(token)
    } else if (token === 'pi' || token === 'e') {
      output.push(token)
    } else if (token in FUNCTIONS || token === 'neg') {
      operators.push(token)
    } else if (/^[A-Za-z_][A-Za-z0-9_]*$/.test(token)) {
      if (next === '(') throw new Error(`Unsupported function ${token}`)
      output.push(token)
    } else if (token === ',') {
      while (operators.length && operators[operators.length - 1] !== '(') output.push(operators.pop() as string)
    } else if (token in PRECEDENCE) {
      while (operators.length) {
        const top = operators[operators.length - 1]
        const shouldPop = top in PRECEDENCE
          && (PRECEDENCE[top] > PRECEDENCE[token] || (PRECEDENCE[top] === PRECEDENCE[token] && token !== '^'))
        if (!shouldPop && top !== 'neg') break
        output.push(operators.pop() as string)
      }
      operators.push(token)
    } else if (token === '(') {
      operators.push(token)
    } else if (token === ')') {
      while (operators.length && operators[operators.length - 1] !== '(') output.push(operators.pop() as string)
      if (!operators.length) throw new Error('Mismatched parentheses')
      operators.pop()
      if (operators.length && (operators[operators.length - 1] in FUNCTIONS || operators[operators.length - 1] === 'neg')) {
        output.push(operators.pop() as string)
      }
    }
  }
  while (operators.length) {
    const operator = operators.pop() as string
    if (operator === '(' || operator === ')') throw new Error('Mismatched parentheses')
    output.push(operator)
  }
  return output
}

function evalRpn(tokens: Token[], variables: Record<string, number>) {
  const stack: number[] = []
  for (const token of tokens) {
    if (typeof token === 'number') {
      stack.push(token)
    } else if (token === 'pi') {
      stack.push(Math.PI)
    } else if (token === 'e') {
      stack.push(Math.E)
    } else if (token in variables) {
      stack.push(variables[token])
    } else if (token === 'neg') {
      const value = stack.pop()
      if (value == null) throw new Error('Invalid expression')
      stack.push(-value)
    } else if (token in FUNCTIONS) {
      const arity = token === 'min' || token === 'max' || token === 'pow' ? 2 : 1
      const args = stack.splice(-arity)
      if (args.length !== arity) throw new Error('Invalid expression')
      stack.push(FUNCTIONS[token](...args))
    } else if (token in PRECEDENCE) {
      const right = stack.pop()
      const left = stack.pop()
      if (left == null || right == null) throw new Error('Invalid expression')
      if (token === '+') stack.push(left + right)
      if (token === '-') stack.push(left - right)
      if (token === '*') stack.push(left * right)
      if (token === '/') stack.push(left / right)
      if (token === '^') stack.push(left ** right)
    } else {
      throw new Error(`Unknown identifier ${token}`)
    }
  }
  if (stack.length !== 1 || !Number.isFinite(stack[0])) throw new Error('Invalid expression')
  return stack[0]
}
