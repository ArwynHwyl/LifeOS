package com.example.demo.flashcard.config;

import com.example.demo.flashcard.entity.FlashcardCard;
import com.example.demo.flashcard.entity.FlashcardDeck;
import com.example.demo.flashcard.repository.FlashcardDeckRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class FlashcardSeedConfig {

    @Bean
    CommandLineRunner seedFlashcardData(FlashcardDeckRepository deckRepository) {
        return args -> {
            if (deckRepository.count() > 0) {
                return;
            }
            deckRepository.saveAll(java.util.List.of(
                    algebraFormulas(),
                    trigIdentities(),
                    derivativeRules(),
                    geometryFormulas(),
                    integralRules(),
                    statisticsBasics()
            ));
        };
    }

    private FlashcardDeck algebraFormulas() {
        FlashcardDeck deck = new FlashcardDeck("Algebra Formulas", "Core equations for solving and graphing.", "ALGEBRA", 1);
        int i = 1;
        deck.addCard(new FlashcardCard(
                "Quadratic formula", "x = ( −b ± √(b² − 4ac) ) / 2a",
                "Use for any quadratic ax² + bx + c = 0. The discriminant b² − 4ac tells you the number of real roots.",
                "x² − 5x + 6 = 0  →  x = 2 or x = 3", "algebra,quadratic,must-know", i++));
        deck.addCard(new FlashcardCard(
                "Slope formula", "m = (y₂ − y₁) / (x₂ − x₁)",
                "Finds the steepness of a line through two points.",
                "(1,2) and (4,8)  →  m = (8−2)/(4−1) = 2", "algebra,linear", i++));
        deck.addCard(new FlashcardCard(
                "Slope-intercept form", "y = mx + b",
                "Standard way to write a line's equation. m is the slope, b is the y-intercept.",
                "m = 3, b = −1  →  y = 3x − 1", "algebra,linear", i++));
        deck.addCard(new FlashcardCard(
                "Difference of squares", "a² − b² = (a − b)(a + b)",
                "Useful for quickly factoring binomials that are a perfect square minus another.",
                "x² − 9 = (x − 3)(x + 3)", "algebra,factoring", i++));
        deck.addCard(new FlashcardCard(
                "Distance formula", "d = √( (x₂−x₁)² + (y₂−y₁)² )",
                "Finds the straight-line distance between two points on a plane.",
                "(0,0) to (3,4)  →  d = √(9+16) = 5", "algebra,coordinate-geometry", i++));
        deck.addCard(new FlashcardCard(
                "Compound interest", "A = P(1 + r/n)^(nt)",
                "P is principal, r is annual rate, n is compounds per year, t is years.",
                "P=1000, r=0.05, n=1, t=2  →  A ≈ 1102.50", "algebra,finance", i));
        return deck;
    }

    private FlashcardDeck trigIdentities() {
        FlashcardDeck deck = new FlashcardDeck("Trig Identities", "Relationships between sine, cosine and tangent.", "TRIG", 2);
        int i = 1;
        deck.addCard(new FlashcardCard(
                "Pythagorean identity", "sin²θ + cos²θ = 1",
                "Holds for every angle θ. Derived directly from the unit circle.",
                "θ = 30°  →  (0.5)² + (0.866)² ≈ 1", "trig,identity,must-know", i++));
        deck.addCard(new FlashcardCard(
                "Tangent identity", "tanθ = sinθ / cosθ",
                "Defines tangent in terms of sine and cosine. Undefined when cosθ = 0.",
                "θ = 45°  →  tanθ = 1", "trig,identity", i++));
        deck.addCard(new FlashcardCard(
                "Double angle — sine", "sin(2θ) = 2 sinθ cosθ",
                "Expands sin of a doubled angle into single-angle terms.",
                "θ = 30°  →  sin(60°) = 2(0.5)(0.866) ≈ 0.866", "trig,double-angle", i++));
        deck.addCard(new FlashcardCard(
                "Double angle — cosine", "cos(2θ) = cos²θ − sin²θ",
                "One of three equivalent double-angle forms for cosine.",
                "θ = 45°  →  cos(90°) = 0.5 − 0.5 = 0", "trig,double-angle", i++));
        deck.addCard(new FlashcardCard(
                "Law of sines", "a/sinA = b/sinB = c/sinC",
                "Relates side lengths to opposite angles in any triangle.",
                "A=30°, a=5  →  b/sinB = 10", "trig,triangle", i++));
        deck.addCard(new FlashcardCard(
                "Law of cosines", "c² = a² + b² − 2ab·cosC",
                "Generalizes the Pythagorean theorem to any triangle, not just right triangles.",
                "a=5, b=7, C=60°  →  c² = 25+49−35 = 39", "trig,triangle", i));
        return deck;
    }

    private FlashcardDeck derivativeRules() {
        FlashcardDeck deck = new FlashcardDeck("Derivative Rules", "Differentiation shortcuts for common functions.", "CALCULUS", 3);
        int i = 1;
        deck.addCard(new FlashcardCard(
                "Power rule", "d/dx[xⁿ] = n·xⁿ⁻¹",
                "The most common derivative shortcut — bring the exponent down, subtract one.",
                "d/dx[x³] = 3x²", "calculus,derivative,must-know", i++));
        deck.addCard(new FlashcardCard(
                "Derivative of sin(x)", "d/dx[sin x] = cos x",
                "Sine and cosine derivatives cycle every four derivatives.",
                "d/dx[sin x] at x=0  →  cos(0) = 1", "calculus,derivative,trig", i++));
        deck.addCard(new FlashcardCard(
                "Derivative of cos(x)", "d/dx[cos x] = −sin x",
                "Note the negative sign — a common mistake to forget.",
                "d/dx[cos x] at x=0  →  −sin(0) = 0", "calculus,derivative,trig", i++));
        deck.addCard(new FlashcardCard(
                "Product rule", "d/dx[uv] = u’v + uv’",
                "Use when differentiating a product of two functions of x.",
                "d/dx[x² sin x] = 2x sin x + x² cos x", "calculus,derivative", i++));
        deck.addCard(new FlashcardCard(
                "Quotient rule", "d/dx[u/v] = (u’v − uv’) / v²",
                "Use when differentiating a ratio of two functions of x.",
                "d/dx[x / (x+1)] = 1 / (x+1)²", "calculus,derivative", i++));
        deck.addCard(new FlashcardCard(
                "Chain rule", "d/dx[f(g(x))] = f’(g(x))·g’(x)",
                "Use for composite functions — differentiate outer, multiply by derivative of inner.",
                "d/dx[sin(x²)] = cos(x²)·2x", "calculus,derivative,must-know", i));
        return deck;
    }

    private FlashcardDeck geometryFormulas() {
        FlashcardDeck deck = new FlashcardDeck("Geometry Formulas", "Area, perimeter and volume essentials.", "GEOMETRY", 4);
        int i = 1;
        deck.addCard(new FlashcardCard(
                "Pythagorean theorem", "a² + b² = c²",
                "Only for right triangles. c is the hypotenuse — the longest side, opposite the right angle.",
                "legs 3, 4  →  c = √(9+16) = 5", "geometry,triangle,classic", i++));
        deck.addCard(new FlashcardCard(
                "Area of a circle", "A = πr²",
                "r is the radius, the distance from center to edge.",
                "r = 4  →  A = 16π ≈ 50.27", "geometry,circle,must-know", i++));
        deck.addCard(new FlashcardCard(
                "Circumference of a circle", "C = 2πr",
                "The distance around the circle's edge.",
                "r = 4  →  C = 8π ≈ 25.13", "geometry,circle", i++));
        deck.addCard(new FlashcardCard(
                "Area of a triangle", "A = ½bh",
                "b is the base, h is the perpendicular height to that base.",
                "b=6, h=4  →  A = 12", "geometry,triangle", i++));
        deck.addCard(new FlashcardCard(
                "Volume of a sphere", "V = (4/3)πr³",
                "r is the radius of the sphere.",
                "r = 3  →  V = 36π ≈ 113.10", "geometry,solid", i++));
        deck.addCard(new FlashcardCard(
                "Sum of interior angles (n-gon)", "(n − 2) × 180°",
                "n is the number of sides of the polygon.",
                "n = 6 (hexagon)  →  4 × 180° = 720°", "geometry,polygon", i));
        return deck;
    }

    private FlashcardDeck integralRules() {
        FlashcardDeck deck = new FlashcardDeck("Integral Rules", "Antiderivative shortcuts for common functions.", "CALCULUS", 5);
        int i = 1;
        deck.addCard(new FlashcardCard(
                "Power rule for integrals", "∫xⁿ dx = xⁿ⁺¹/(n+1) + C",
                "Works for any n ≠ −1. Always remember the constant of integration C.",
                "∫x² dx = x³/3 + C", "calculus,integral,must-know", i++));
        deck.addCard(new FlashcardCard(
                "Integral of 1/x", "∫(1/x) dx = ln|x| + C",
                "The one exception to the power rule, where n = −1.",
                "∫(1/x) dx from 1 to e = 1", "calculus,integral", i++));
        deck.addCard(new FlashcardCard(
                "Integral of eˣ", "∫eˣ dx = eˣ + C",
                "eˣ is its own antiderivative, just like its own derivative.",
                "∫eˣ dx from 0 to 1 = e − 1", "calculus,integral,exponential", i++));
        deck.addCard(new FlashcardCard(
                "Integral of sin(x)", "∫sin x dx = −cos x + C",
                "Note the negative sign on the result.",
                "∫sin x dx from 0 to π = 2", "calculus,integral,trig", i++));
        deck.addCard(new FlashcardCard(
                "Integral of cos(x)", "∫cos x dx = sin x + C",
                "The antiderivative of cosine is sine, no sign flip needed.",
                "∫cos x dx from 0 to π/2 = 1", "calculus,integral,trig", i++));
        deck.addCard(new FlashcardCard(
                "Fundamental theorem of calculus", "∫[a,b] f’(x) dx = f(b) − f(a)",
                "Links derivatives and definite integrals — evaluate the antiderivative at the bounds.",
                "f(x) = x², [1,3]  →  9 − 1 = 8", "calculus,integral,theorem", i));
        return deck;
    }

    private FlashcardDeck statisticsBasics() {
        FlashcardDeck deck = new FlashcardDeck("Statistics Basics", "Core measures for describing data.", "STATS", 6);
        int i = 1;
        deck.addCard(new FlashcardCard(
                "Mean", "x̄ = Σx / n",
                "The average — sum every value, divide by how many values there are.",
                "{2,4,6}  →  mean = 12/3 = 4", "stats,central-tendency,must-know", i++));
        deck.addCard(new FlashcardCard(
                "Median", "Middle value of ordered data",
                "If n is even, average the two middle values. Less sensitive to outliers than the mean.",
                "{1,3,3,6,7,8,9}  →  median = 6", "stats,central-tendency", i++));
        deck.addCard(new FlashcardCard(
                "Standard deviation", "σ = √( Σ(x − x̄)² / n )",
                "Measures how spread out data is around the mean.",
                "{2,4,6}  →  σ = √(8/3) ≈ 1.63", "stats,spread", i++));
        deck.addCard(new FlashcardCard(
                "Variance", "σ² = Σ(x − x̄)² / n",
                "The square of the standard deviation — same idea, different units.",
                "{2,4,6}  →  σ² ≈ 2.67", "stats,spread", i++));
        deck.addCard(new FlashcardCard(
                "P(A and B), independent", "P(A∩B) = P(A) × P(B)",
                "Only valid when A and B are independent events.",
                "P(A)=0.5, P(B)=0.4  →  P(A∩B) = 0.2", "stats,probability", i++));
        deck.addCard(new FlashcardCard(
                "Combination formula", "C(n,r) = n! / (r!(n−r)!)",
                "Counts how many ways to choose r items from n when order doesn't matter.",
                "C(5,2) = 5!/(2!3!) = 10", "stats,probability,combinatorics", i));
        return deck;
    }
}
