package org.labkey.panoramapublic.proteomexchange;

import java.util.HashMap;
import java.util.Map;

public enum ChemElement
{
    // Source: unimod.xml
    H("H",1.007825035),
    H2("2H", "H'", 2.014101779),
    Li("Li",7.016003),
    B("B",11.0093055),
    C("C",12),
    C13("13C", "C'", 13.00335483),
    N("N",14.003074),
    N15("15N", "N'", 15.00010897),
    O("O",15.99491463),
    O18("18O", "O'", 17.9991603),
    F("F",18.99840322),
    Na("Na",22.9897677),
    Mg("Mg",23.9850423),
    Al("Al",26.9815386),
    P("P",30.973762),
    S("S",31.9720707),
    Cl("Cl",34.96885272),
    K("K",38.9637074),
    Ca("Ca",39.9625906),
    Cr("Cr",51.9405098),
    Mn("Mn",54.9380471),
    Fe("Fe",55.9349393),
    Ni("Ni",57.9353462),
    Co("Co",58.9331976),
    Cu("Cu",62.9295989),
    Zn("Zn",63.9291448),
    As("As",74.9215942),
    Br("Br",78.9183361),
    Se("Se",79.9165196),
    Mo("Mo",97.9054073),
    Ru("Ru",101.9043485),
    Pd("Pd",105.903478),
    Ag("Ag",106.905092),
    Cd("Cd",113.903357),
    I("I",126.904473),
    Pt("Pt",194.964766),
    Au("Au",196.966543),
    Hg("Hg",201.970617);


    private final String _title;
    private final String _symbol;
    private final double _avgMass;

    private static Map<String, ChemElement> symbolMap = new HashMap<>();
    static
    {
        for (ChemElement el: ChemElement.values())
        {
            symbolMap.put(el.getSymbol(), el);
        }
    }

    public static ChemElement getElementForSymbol(String symbol)
    {
        return symbolMap.get(symbol);
    }

    ChemElement(String title, double avgMass)
    {
        this(title, title, avgMass);
    }
    ChemElement(String title, String symbol, double avgMass)
    {
        _title = title;
        _symbol = symbol;
        _avgMass = avgMass;
    }

    public String getTitle()
    {
        return _title;
    }

    public String getSymbol()
    {
        return _symbol;
    }
}
