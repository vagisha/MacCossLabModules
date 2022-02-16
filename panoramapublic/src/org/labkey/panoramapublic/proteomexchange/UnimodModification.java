/*
 * Copyright (c) 2018-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.panoramapublic.proteomexchange;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UnimodModification
{
    private final int _id;
    private final String _name;
    private final String _normFormula;
    private final Set<String> _modSites;
    private boolean _isNterm;
    private boolean _isCterm;
    private boolean _isIsotopic;

    public enum Terminus
    {
        N("N-term"), C("C-term");
        private final String label;
        Terminus(String label)
        {
            this.label = label;
        }

        public String getLabel()
        {
            return label;
        }
    }

    public UnimodModification(int id, String name, String normalizedFormula)
    {
        _id = id;
        _name = name;
        _normFormula = normalizedFormula;
        _modSites = new HashSet<>();
    }

    public int getId()
    {
        return _id;
    }

    public String getName()
    {
        return _name;
    }

    public String getNormalizedFormula()
    {
        return _normFormula;
    }

    public void setNterm(boolean nterm)
    {
        _isNterm = nterm;
    }

    public void setCterm(boolean cterm)
    {
        _isCterm = cterm;
    }

    public void addSite(String site, String classification)
    {
        _modSites.add(site);
    }

    public boolean isStructural()
    {
        return !_isIsotopic;
    }

    public boolean isIsotopic()
    {
        return _isIsotopic;
    }

    public void setIsotopic(boolean isotopic)
    {
        _isIsotopic = isotopic;
    }

    /**
     * @param normFormula
     * @param sites
     * @param terminus
     * @return true if the given normalized formula matches this modification's composition, and the given sites are in the
     * allowed sites for this modification. If no sites are given then the given terminus must match. If both the given
     * sites and terminus are null or empty then return false.
     */
    public boolean matches(String normFormula, String[] sites, Terminus terminus)
    {
        if(!formulaMatches(normFormula))
        {
            return false;
        }
        boolean hasSites = sites != null && sites.length > 0;
        if (!hasSites && terminus == null)
        {
            // Cannot find an exact match based on just the formula
            return false;
        }
        return hasSites ? _modSites.containsAll(Arrays.asList(sites)) :
                          terminus != null && ((_isNterm && Terminus.N.equals(terminus)) || _isCterm && Terminus.C.equals(terminus));
    }

    public boolean formulaMatches(String normFormula)
    {
        return _normFormula.equals(normFormula);
    }

    public static String normalizeFormula(String formula)
    {
        if(StringUtils.isBlank(formula))
        {
            return formula;
        }

        // Assume formulas are of the form H'6C'8N'4 - H2C6N4.
        // The part of the formula following ' - ' are the element masses that will be subtracted
        // from the total mass.  Only one negative part is allowed. We will parse the positive and negative parts separately.
        String[] parts = formula.split("-");
        if(parts.length > 2)
        {
            throw new IllegalArgumentException("Formula inconsistent with required form: " + formula);
        }

        Map<String, Integer> composition = getComposition(parts[0]);
        if(parts.length > 1)
        {
            Map<String, Integer> negComposition = getComposition(parts[1]);
            for(String element: negComposition.keySet())
            {
                int posCount = composition.get(element) == null ? 0 : composition.get(element);
                int totalCount = posCount - negComposition.get(element);
                if(totalCount != 0)
                {
                    composition.put(element, totalCount);
                }
                else
                {
                    composition.remove(element);
                }
            }
        }

        List<String> sortedElements = new ArrayList<>(composition.keySet());
        Collections.sort(sortedElements);
        StringBuilder posForm = new StringBuilder();
        StringBuilder negForm = new StringBuilder();
        for(String element: sortedElements)
        {
            Integer count = composition.get(element);
            if(count > 0)
            {
                posForm.append(element).append(composition.get(element));
            }
            else
            {
                negForm.append(element).append(-(composition.get(element)));
            }
        }
        String totalFormula = posForm.toString();
        if(negForm.length() > 0)
        {
            totalFormula = totalFormula + (totalFormula.length() > 0 ? " - " : "-") + negForm.toString();
        }
        return totalFormula;
    }

    private static Map<String, Integer> getComposition(String formula)
    {
        Map<String, Integer> composition = new HashMap<>();

        String currElem = null;
        Integer currCount = null;
        char[] chars = formula.toCharArray();
        for (char c : chars)
        {
            if (Character.isDigit(c))
            {
                currCount = ((currCount == null ? 0 : currCount) * 10 + (c - '0'));
            }
            else if (Character.isUpperCase(c))
            {
                if (currElem != null)
                {
                    updateElementCount(composition, currElem, currCount);
                }
                currElem = "" + c;
                currCount = null;
            }
            else if (!Character.isWhitespace(c)) // e.g. Na, C'
            {
                currElem += c;
            }
        }

        // last one
        if(currElem != null)
        {
            updateElementCount(composition, currElem, currCount);
        }

        return composition;
    }

    private static void updateElementCount(Map<String, Integer> composition, String currElem, Integer currCount)
    {
        int oldCount = composition.get(currElem) == null ? 0 : composition.get(currElem);
        Integer newCount = oldCount + (currCount == null ? 1 : currCount);
        if(newCount == 0)
        {
            composition.remove(currElem);
        }
        else
        {
            composition.put(currElem, newCount);
        }
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("UNIMOD:").append(getId());
        sb.append(", ").append(getName());
        sb.append(", ").append(getNormalizedFormula());
        if(_modSites.size() > 0)
        {
            sb.append(", Sites: ").append(StringUtils.join(_modSites, ":"));
        }
        if(_isCterm)
        {
            sb.append(", C-term");
        }
        if(_isNterm)
        {
            sb.append(", N-term");
        }
        sb.append(", Isotopic: " + _isIsotopic);
        return sb.toString();
    }

    public String getModSites()
    {
        if(_modSites.size() > 0)
        {
            return StringUtils.join(_modSites, ":");
        }
        return "";
    }
}
