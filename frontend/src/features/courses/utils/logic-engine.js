/* logic-engine.js — propositional-logic parser, evaluator, truth tables & laws.
   Plain JS (no React). Exposes export default Logic.
   Used by LogicCircuit / LogicSimplify / LogicFlowEditor. */

function tokenize(src) {
  const toks = [];
  let i = 0;
  const s = src;
  while (i < s.length) {
    const c = s[i];
    if (c === ' ' || c === '\t' || c === '\n') { i++; continue; }
    // multi-char operators first
    if (s.startsWith('<->', i)) { toks.push({ k: 'iff' }); i += 3; continue; }
    if (s.startsWith('->', i))  { toks.push({ k: 'imp' }); i += 2; continue; }
    if (c === '↔') { toks.push({ k: 'iff' }); i++; continue; }
    if (c === '→') { toks.push({ k: 'imp' }); i++; continue; }
    if (c === '¬' || c === '!' || c === '~') { toks.push({ k: 'not' }); i++; continue; }
    if (c === '∧' || c === '&' || c === '*' || c === '.') { toks.push({ k: 'and' }); i++; continue; }
    if (c === '∨' || c === '|' || c === '+') { toks.push({ k: 'or' }); i++; continue; }
    if (c === '⊕' || c === '^') { toks.push({ k: 'xor' }); i++; continue; }
    if (c === '(') { toks.push({ k: 'lp' }); i++; continue; }
    if (c === ')') { toks.push({ k: 'rp' }); i++; continue; }
    if (c === 'T' || c === '1' || c === '⊤') { toks.push({ k: 'const', v: true }); i++; continue; }
    if (c === 'F' || c === '0' || c === '⊥') { toks.push({ k: 'const', v: false }); i++; continue; }
    if (/[A-Za-z]/.test(c)) { toks.push({ k: 'var', v: c.toUpperCase() }); i++; continue; }
    throw new Error('Unexpected character: ' + c);
  }
  return toks;
}

function parse(src) {
  const toks = tokenize(src);
  let p = 0;
  const peek = () => toks[p];
  const eat = (k) => { const t = toks[p]; if (!t || t.k !== k) throw new Error('Expected ' + k); p++; return t; };

  function atom() {
    const t = peek();
    if (!t) throw new Error('Unexpected end');
    if (t.k === 'lp') { p++; const e = iff(); eat('rp'); return e; }
    if (t.k === 'not') { p++; return { t: 'not', a: atom() }; }
    if (t.k === 'var') { p++; return { t: 'var', name: t.v }; }
    if (t.k === 'const') { p++; return { t: 'const', val: t.v }; }
    throw new Error('Unexpected token ' + t.k);
  }
  function and() { let l = atom(); while (peek() && peek().k === 'and') { p++; l = { t: 'bin', op: 'and', a: l, b: atom() }; } return l; }
  function xor() { let l = and(); while (peek() && peek().k === 'xor') { p++; l = { t: 'bin', op: 'xor', a: l, b: and() }; } return l; }
  function or()  { let l = xor(); while (peek() && peek().k === 'or')  { p++; l = { t: 'bin', op: 'or',  a: l, b: xor() }; } return l; }
  function imp() { const l = or(); if (peek() && peek().k === 'imp') { p++; return { t: 'bin', op: 'imp', a: l, b: imp() }; } return l; }
  function iff() { let l = imp(); while (peek() && peek().k === 'iff') { p++; l = { t: 'bin', op: 'iff', a: l, b: imp() }; } return l; }

  const tree = iff();
  if (p < toks.length) throw new Error('Trailing tokens');
  return tree;
}

function tryParse(src) { try { return { ast: parse(src), error: null }; } catch (e) { return { ast: null, error: e.message }; } }

function evaluate(ast, env) {
  switch (ast.t) {
    case 'const': return ast.val;
    case 'var':   return !!env[ast.name];
    case 'not':   return !evaluate(ast.a, env);
    case 'bin': {
      const a = evaluate(ast.a, env), b = evaluate(ast.b, env);
      switch (ast.op) {
        case 'and': return a && b;
        case 'or':  return a || b;
        case 'xor': return a !== b;
        case 'imp': return !a || b;
        case 'iff': return a === b;
      }
    }
  }
  return false;
}

function variables(ast, set) {
  set = set || new Set();
  if (ast.t === 'var') set.add(ast.name);
  else if (ast.t === 'not') variables(ast.a, set);
  else if (ast.t === 'bin') { variables(ast.a, set); variables(ast.b, set); }
  return [...set].sort();
}

const GLYPH = { and: '∧', or: '∨', xor: '⊕', imp: '→', iff: '↔' };
const PREC = { iff: 1, imp: 2, or: 3, xor: 4, and: 5, not: 6, atom: 7 };
function precOf(ast) {
  if (ast.t === 'var' || ast.t === 'const') return PREC.atom;
  if (ast.t === 'not') return PREC.not;
  return PREC[ast.op];
}

function toString(ast) {
  if (ast.t === 'const') return ast.val ? 'T' : 'F';
  if (ast.t === 'var') return ast.name;
  if (ast.t === 'not') {
    const inner = ast.a;
    const s = toString(inner);
    return '¬' + (precOf(inner) < PREC.not ? '(' + s + ')' : s);
  }
  // bin
  const myPrec = PREC[ast.op];
  const wrap = (child, right) => {
    const s = toString(child);
    const cp = precOf(child);
    if (cp < myPrec) return '(' + s + ')';
    if (cp === myPrec && right && (ast.op === 'imp')) return '(' + s + ')';
    return s;
  };
  return wrap(ast.a, false) + ' ' + GLYPH[ast.op] + ' ' + wrap(ast.b, true);
}

function truthTable(ast, varsOverride) {
  const vars = varsOverride || variables(ast);
  const rows = [];
  const n = vars.length;
  for (let m = 0; m < (1 << n); m++) {
    const env = {};
    vars.forEach((v, idx) => { env[v] = !!(m & (1 << (n - 1 - idx))); });
    rows.push({ env, val: evaluate(ast, env) });
  }
  return { vars, rows };
}

function equivalent(a, b) {
  const vars = [...new Set([...variables(a), ...variables(b)])].sort();
  const n = vars.length;
  for (let m = 0; m < (1 << n); m++) {
    const env = {};
    vars.forEach((v, idx) => { env[v] = !!(m & (1 << (n - 1 - idx))); });
    if (evaluate(a, env) !== evaluate(b, env)) return false;
  }
  return true;
}

function isTautology(ast) { return truthTable(ast).rows.every(r => r.val); }
function isContradiction(ast) { return truthTable(ast).rows.every(r => !r.val); }

const LAWS = [
  { id: 'DOUBLE_NEGATION', name: 'Double negation', short: '¬¬', forms: '¬(¬P) ≡ P',
    note: 'Two “nots” cancel each other out.', color: '#6b4ec1' },
  { id: 'DE_MORGAN', name: "De Morgan's", short: 'DM', forms: '¬(P∧Q) ≡ ¬P∨¬Q   ·   ¬(P∨Q) ≡ ¬P∧¬Q',
    note: 'Push the “not” inside and flip ∧ ↔ ∨.', color: '#3b6cb5' },
  { id: 'DISTRIBUTIVE', name: 'Distributive', short: 'Dist', forms: 'P∧(Q∨R) ≡ (P∧Q)∨(P∧R)',
    note: 'Factor out (or multiply through) the shared term.', color: '#c44a1a' },
  { id: 'IDENTITY', name: 'Identity', short: 'Id', forms: 'P∧T ≡ P   ·   P∨F ≡ P',
    note: 'AND-with-true / OR-with-false leaves it unchanged.', color: '#3a7d44' },
  { id: 'DOMINATION', name: 'Domination', short: 'Dom', forms: 'P∨T ≡ T   ·   P∧F ≡ F',
    note: 'OR-with-true is always true; AND-with-false always false.', color: '#3a7d44' },
  { id: 'IDEMPOTENT', name: 'Idempotent', short: 'Idem', forms: 'P∧P ≡ P   ·   P∨P ≡ P',
    note: 'A term combined with itself is just itself.', color: '#6b4ec1' },
  { id: 'COMPLEMENT', name: 'Complement', short: 'Comp', forms: 'P∧¬P ≡ F   ·   P∨¬P ≡ T',
    note: 'A term and its opposite cover every case.', color: '#c44a1a' },
  { id: 'ABSORPTION', name: 'Absorption', short: 'Abs', forms: 'P∨(P∧Q) ≡ P   ·   P∧(P∨Q) ≡ P',
    note: 'The bigger piece swallows the smaller one.', color: '#3b6cb5' },
  { id: 'COMMUTATIVE', name: 'Commutative', short: 'Comm', forms: 'P∧Q ≡ Q∧P',
    note: 'Order doesn’t matter — swap the two sides.', color: '#8a8276' },
  { id: 'ASSOCIATIVE', name: 'Associative', short: 'Assoc', forms: '(P∧Q)∧R ≡ P∧(Q∧R)',
    note: 'Regroup the brackets — grouping doesn’t matter.', color: '#8a8276' },
  { id: 'IMPLICATION', name: 'Implication', short: 'Impl', forms: 'P→Q ≡ ¬P∨Q',
    note: 'Rewrite an arrow as “not P, or Q”.', color: '#3b6cb5' },
];
const LAW_BY_ID = {};
LAWS.forEach(l => { LAW_BY_ID[l.id] = l; });

export const Logic = {
  parse, tryParse, evaluate, variables, toString,
  truthTable, equivalent, isTautology, isContradiction,
  LAWS, LAW_BY_ID,
};

export default Logic;
