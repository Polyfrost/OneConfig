package org.polyfrost.oneconfig.internal.ui.components.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpinnerFormatTest {
    @Test
    void keepsPrecisionOfFineSteps() {
        assertEquals("0.001", NumberOptionKt.formatSpinnerValue(0.001f, 0.001f));
        assertEquals("0.50", NumberOptionKt.formatSpinnerValue(0.5f, 0.1f));
        assertEquals("3", NumberOptionKt.formatSpinnerValue(3f, 0.001f));
        assertEquals("1.23", NumberOptionKt.formatSpinnerValue(1.234f, 0f));
    }

    @Test
    void integerPropertiesUseWholeSteps() {
        assertEquals(1f, NumberOptionKt.spinnerStep(0.5f, 0f, 10f, NumberOptionKt.isIntegralType(int.class)));
        assertEquals(2f, NumberOptionKt.spinnerStep(2.4f, 0f, 10f, NumberOptionKt.isIntegralType(Integer.class)));
        assertEquals(0.5f, NumberOptionKt.spinnerStep(0.5f, 0f, 10f, NumberOptionKt.isIntegralType(float.class)));
    }
}
