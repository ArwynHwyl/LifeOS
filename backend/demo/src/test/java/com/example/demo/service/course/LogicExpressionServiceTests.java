package com.example.demo.service.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.service.exception.ValidationException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LogicExpressionServiceTests {

    private final LogicExpressionService service = new LogicExpressionService();

    @Test
    void parsesUnicodeAndAsciiOperatorsToTheSameAst() {
        LogicExpressionService.Node unicode = service.parse("¬P ∨ (Q ∧ R) → P ↔ Q");
        LogicExpressionService.Node ascii = service.parse("!P | (Q & R) -> P <-> Q");

        assertThat(service.normalize(unicode)).isEqualTo(service.normalize(ascii));
    }

    @Test
    void parsesConstantsAndCaseInsensitiveVariables() {
        assertThat(service.evaluate(service.parse("T ∧ p"), Map.of("P", true))).isTrue();
        assertThat(service.evaluate(service.parse("F ∨ p"), Map.of("P", false))).isFalse();
        assertThat(service.variables(service.parse("p ∧ Q"))).containsExactly("P", "Q");
    }

    @Test
    void respectsOperatorPrecedence() {
        // AND binds tighter than OR: P ∨ Q ∧ R == P ∨ (Q ∧ R)
        LogicExpressionService.Node node = service.parse("P ∨ Q ∧ R");
        assertThat(service.evaluate(node, Map.of("P", false, "Q", true, "R", false))).isFalse();
        assertThat(service.evaluate(node, Map.of("P", true, "Q", false, "R", false))).isTrue();

        // NOT binds tighter than AND: ¬P ∧ Q == (¬P) ∧ Q
        assertThat(service.evaluate(service.parse("¬P ∧ Q"), Map.of("P", false, "Q", true))).isTrue();

        // Implication is right-associative: P → Q → R == P → (Q → R)
        assertThat(service.evaluate(service.parse("P -> Q -> R"), Map.of("P", true, "Q", false, "R", false))).isTrue();
    }

    @Test
    void evaluatesEveryOperator() {
        assertThat(service.evaluate(service.parse("P ∧ Q"), Map.of("P", true, "Q", false))).isFalse();
        assertThat(service.evaluate(service.parse("P ∨ Q"), Map.of("P", true, "Q", false))).isTrue();
        assertThat(service.evaluate(service.parse("P ⊕ Q"), Map.of("P", true, "Q", true))).isFalse();
        assertThat(service.evaluate(service.parse("P → Q"), Map.of("P", true, "Q", false))).isFalse();
        assertThat(service.evaluate(service.parse("P ↔ Q"), Map.of("P", false, "Q", false))).isTrue();
        assertThat(service.evaluate(service.parse("¬P"), Map.of("P", false))).isTrue();
    }

    @Test
    void missingVariableEvaluatesAsFalse() {
        assertThat(service.evaluate(service.parse("P ∨ Q"), Map.of("P", false))).isFalse();
    }

    @Test
    void checksTruthTableEquivalence() {
        assertThat(service.equivalent(service.parse("P -> Q"), service.parse("¬P ∨ Q"))).isTrue();
        assertThat(service.equivalent(service.parse("(P ∧ ¬Q) ∨ (P ∧ Q)"), service.parse("P"))).isTrue();
        assertThat(service.equivalent(service.parse("¬(P ∧ Q)"), service.parse("¬P ∨ ¬Q"))).isTrue();
        assertThat(service.equivalent(service.parse("P -> Q"), service.parse("Q -> P"))).isFalse();
        assertThat(service.equivalent(service.parse("P"), service.parse("Q"))).isFalse();
    }

    @Test
    void rejectsMalformedExpressions() {
        assertThatThrownBy(() -> service.parse("P ->"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("ended unexpectedly");
        assertThatThrownBy(() -> service.parse("P Q"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("trailing tokens");
        assertThatThrownBy(() -> service.parse("(P ∧ Q"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("expected RP");
        assertThatThrownBy(() -> service.parse("P # Q"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("unsupported character");
        assertThatThrownBy(() -> service.parse("  "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("required");
    }

    @Test
    void rejectsEquivalenceAboveVariableLimit() {
        assertThatThrownBy(() -> service.equivalent(
                service.parse("A ∧ B ∧ C ∧ D ∧ E"),
                service.parse("G ∧ H ∧ I ∧ J ∧ K")
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("at most 8 variables");
    }

    @Test
    void normalizeProducesCanonicalGlyphsAndParentheses() {
        assertThat(service.normalize(service.parse("!P | Q"))).isEqualTo("¬P ∨ Q");
        assertThat(service.normalize(service.parse("(P | Q) & R"))).isEqualTo("(P ∨ Q) ∧ R");
        assertThat(service.normalize(service.parse("¬(P ∧ Q)"))).isEqualTo("¬(P ∧ Q)");
    }
}
