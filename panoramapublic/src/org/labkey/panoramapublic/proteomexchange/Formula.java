package org.labkey.panoramapublic.proteomexchange;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

public class Formula
{
    private final TreeMap<ChemElement, Integer> _elementCounts; // Map is sorted on natural ordering of ChemElement

    public Formula()
    {
        _elementCounts = new TreeMap<>();
    }

    public void addElement(ChemElement element, int count)
    {
        Integer currentCount = _elementCounts.computeIfAbsent(element, el -> 0);
        int newCount = currentCount + count;
        if (newCount != 0)
        {
            _elementCounts.put(element, newCount);
        }
        else
        {
            _elementCounts.remove(element);
        }
    }

    public SortedMap<ChemElement, Integer> getElementCounts()
    {
        return Collections.unmodifiableSortedMap(_elementCounts);
    }

    public String getFormula()
    {
        String positive = "";
        String negative = "";
        for (ChemElement el: _elementCounts.keySet())
        {
            int count = _elementCounts.get(el);
            boolean pos = count > 0;
            count = count < 0 ? count * -1 : count;
            String formulaPart = el.getSymbol() + (count > 1 ? count : "");
            if (pos)
            {
                positive += formulaPart;
            }
            else
            {
                negative += formulaPart;
            }
        }
        String separator = positive.length() > 0 && negative.length() > 0 ? " - " : (negative.length() > 0 ? "-" : "");
        return positive + separator + negative;
    }

    public Formula addFormula(Formula otherFormula)
    {
        Formula newFormula = new Formula();

        this._elementCounts.forEach((key, value) -> newFormula.addElement(key, value));
        otherFormula._elementCounts.forEach((key, value) -> newFormula.addElement(key, value));

        return newFormula;
    }

    public Formula subtractFormula(Formula otherFormula)
    {
        Formula newFormula = new Formula();

        this._elementCounts.forEach((key, value) -> newFormula.addElement(key, value));
        otherFormula._elementCounts.forEach((key, value) -> newFormula.addElement(key, value * -1));

        return newFormula;
    }

    public boolean isEmpty()
    {
        return _elementCounts.isEmpty();
    }

    @Override
    public String toString()
    {
        return getFormula();
    }

    public static String normalizeFormula(String input)
    {
        return parseFormula(input).getFormula();
    }

    public static Formula parseFormula(String input)
    {
        Formula formula = new Formula();
        return parseFormula(input, formula, false);
    }

    private static Formula parseFormula(String input, Formula formula, boolean negative)
    {
        if(StringUtils.isBlank(input))
        {
            return new Formula();
        }

        // Assume formulas are of the form H'6C'8N'4 - H2C6N4.
        // The part of the formula following ' - ' are the element masses that will be subtracted
        // from the total mass.  Only one negative part is allowed. We will parse the positive and negative parts separately.
        input = input.trim();

        while (input.length() > 0)
        {
            if (input.startsWith("-"))
            {
                return parseFormula(input.substring(1), formula, !negative);
            }
            String sym = getNextSymbol(input);
            ChemElement el = ChemElement.getElementForSymbol(sym);

            // Unrecognized element
            if (el == null)
            {
                throw new IllegalArgumentException("Unrecognized element in formula: " + sym);
            }

            input = input.substring(sym.length());
            int endCount = 0;
            while (endCount < input.length() && Character.isDigit(input.charAt(endCount)))
            {
                endCount++;
            }

            int count = endCount == 0 ? 1 : Integer.parseInt(input.substring(0, endCount));

            if (negative)
            {
                count = -count;
            }

            formula.addElement(el, count);
            input = input.substring(endCount).trim();
        }
        return formula;
    }

    private static String getNextSymbol(String input)
    {
        // Skip the first character, since it is always the start of
        // the symbol, and then look for the end of the symbol.
        for (int i = 1; i < input.length(); i++)
        {
            char c = input.charAt(i);
            if (!Character.isLowerCase(c) && c != '\'' && c != '"')
            {
                return input.substring(0, i);
            }
        }
        return input;
    }
}
