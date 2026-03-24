package com.example;

import org.junit.Test;
import static org.junit.Assert.*;

public class CalculatorTest {

    @Test
    public void testAdd() {
        Calculator calc = new Calculator();
        assertEquals("Сложение 1 + 2 должно равняться 3", 3, calc.add(1, 2));
        assertEquals("Сложение 0 + 0 должно равняться 0", 0, calc.add(0, 0));
    }

    @Test
    public void testSubtract() {
        Calculator calc = new Calculator();
        assertEquals("Вычитание 5 - 2 должно равняться 3", 3, calc.subtract(5, 2));
    }
}

