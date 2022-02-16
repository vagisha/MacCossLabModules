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
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.data.Container;
import org.labkey.api.targetedms.IModification;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.util.logging.LogHelper;
import org.labkey.panoramapublic.model.ExperimentAnnotations;
import org.labkey.panoramapublic.proteomexchange.UnimodModification.Terminus;
import org.labkey.panoramapublic.query.ExperimentAnnotationsManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ExperimentModificationGetter
{
    private static final Logger LOG = LogHelper.getLogger(ExperimentModificationGetter.class, "Looks up the structural and isotopic modifications for documents associated with an experiment");

    public static List<PxModification> getModifications(ExperimentAnnotations expAnnot)
    {
        List<ITargetedMSRun> runs = ExperimentAnnotationsManager.getTargetedMSRuns(expAnnot);

        Map<Long, PxModification> strModMap = new HashMap<>();
        Map<Long, PxModification> isoModMap = new HashMap<>();

        UnimodModifications uMods = getUnimodMods(); // Read the UNIMOD modifications

        for(ITargetedMSRun run: runs)
        {
            List<? extends IModification.IStructuralModification> smods = TargetedMSService.get().getStructuralModificationsUsedInRun(run.getId());
            for(IModification.IStructuralModification mod: smods)
            {
                PxModification pxMod = strModMap.get(mod.getId());
                if(pxMod == null)
                {
                    pxMod = getStructuralUnimodMod(mod, uMods);
                    strModMap.put(mod.getId(), pxMod);
                }
                pxMod.addSkylineDoc(run.getFileName());
            }

            List<? extends IModification.IIsotopeModification> iMods = TargetedMSService.get().getIsotopeModificationsUsedInRun(run.getId());
            for(IModification.IIsotopeModification mod: iMods)
            {
                PxModification pxMod = isoModMap.get(mod.getId());
                if(pxMod == null)
                {
                    pxMod = getIsotopicUnimodMod(mod, uMods, expAnnot.getContainer());
                    isoModMap.put(mod.getId(), pxMod);
                }
                pxMod.addSkylineDoc(run.getFileName());
            }
        }

        List<PxModification> allMods = new ArrayList<>();
        allMods.addAll(strModMap.values());
        allMods.addAll(isoModMap.values());
        return allMods;
    }

    private static @NotNull String[] modSites(IModification mod)
    {
        if(mod.getAminoAcid() == null)
        {
            return new String[0];
        }

        return mod.getAminoAcid().replaceAll("\\s", "").split(",");
    }

    public static PxModification getStructuralUnimodMod(IModification mod, UnimodModifications uMods)
    {
        if(mod.getUnimodId() != null)
        {
            UnimodModification uMod = uMods.getById(mod.getUnimodId());
            return uMod != null ? new PxStructuralMod(mod.getName(), mod.getId(), uMod) : new PxStructuralMod(mod.getName(), mod.getId());
        }
        else
        {
            PxStructuralMod pxMod = new PxStructuralMod(mod.getName(), mod.getId());
            String normFormula = UnimodModification.normalizeFormula(mod.getFormula());
            if(normFormula != null)
            {
                String[] sites = modSites(mod);
                Terminus term = "C".equalsIgnoreCase(mod.getTerminus()) ? Terminus.C : "N".equalsIgnoreCase(mod.getTerminus()) ? Terminus.N : null;
                // Find possible matches based on formula and modification sites (aa or term)
                List<UnimodModification> uModList = uMods.getMatches(normFormula, sites, term, true);
                if ((sites.length > 0 || term != null) && uModList.size() == 1)
                {
                    // If there was only one match for the modification formula and modification sites / terminus then assume
                    // that this is the right match.
                    pxMod.setUnimodMatch(uModList.get(0));
                }
                else
                {
                    uModList.forEach(pxMod::addPossibleUnimod);
                }
            }
            return pxMod;
        }
    }

    private static PxModification getIsotopicUnimodMod(IModification.IIsotopeModification mod, UnimodModifications uMods, Container container)
    {
        if(mod.getUnimodId() != null)
        {
            UnimodModification uMod = uMods.getById(mod.getUnimodId());
            return uMod != null ? new PxIsotopicMod(mod.getName(), mod.getId(), uMod) : new PxIsotopicMod(mod.getName(), mod.getId());
        }
        else
        {
            String formula = mod.getFormula();
            if(StringUtils.isBlank(formula))
            {
                try
                {
                    formula = buildIsotopeModFormula(mod, uMods);
                }
                catch (PxException e)
                {
                    LOG.error("Error building formula for isotopic mod (" + mod.getName() + ") in container " + container, e);
                }
            }
            PxIsotopicMod pxMod = new PxIsotopicMod(mod.getName(), mod.getId());
            String normFormula = UnimodModification.normalizeFormula(formula);
            if(normFormula != null)
            {
                String[] sites = modSites(mod);
                // Find possible matches based on formula and modification sites (aa or term)
                Terminus term = "C".equalsIgnoreCase(mod.getTerminus()) ? Terminus.C : "N".equalsIgnoreCase(mod.getTerminus()) ? Terminus.N : null;
                List<UnimodModification> uModList = uMods.getMatches(normFormula, sites, term, false);
                if ((sites.length > 0 || term != null) && uModList.size() == 1)
                {
                    // If there was only one match for the modification formula and modification sites / terminus then assume
                    // that this is the right match.
                    pxMod.setUnimodMatch(uModList.get(0));
                }
                else
                {
                    uModList.forEach(pxMod::addPossibleUnimod);
                }
            }
            return pxMod;
        }
    }

    private static String buildIsotopeModFormula(IModification.IIsotopeModification mod, UnimodModifications uMods) throws PxException
    {
        String aminoAcids = mod.getAminoAcid();
        if(StringUtils.isBlank(aminoAcids))
        {
            return null;
        }

        // On PanoramaWeb we do not have any isotopic modifications with multiple amino acids as targets.  But Skyline allows it
        String[] sites = modSites(mod);
        String formula = null;
        for(String site: sites)
        {
            String f = uMods.buildIsotopicModFormula(site.charAt(0),
                    Boolean.TRUE.equals(mod.getLabel2H()),
                    Boolean.TRUE.equals(mod.getLabel13C()),
                    Boolean.TRUE.equals(mod.getLabel15N()),
                    Boolean.TRUE.equals(mod.getLabel18O()));
            if(formula == null)
            {
                formula = f;
            }
            else if(!formula.equals(f))
            {
                throw new PxException("Multiple amino acids found for isotopic modification (" + mod.getName() +"). Formulae do not match.");
            }
        }
        return formula;
    }

    public static UnimodModifications getUnimodMods()
    {
        try
        {
            return (new UnimodParser().parse());
        }
        catch (Exception e)
        {
            LOG.error("There was an error reading UNIMOD modifications.", e);
            return new UnimodModifications();
        }
    }

    public static abstract class PxModification
    {
        private final String _skylineName;
        private final Set<String> _skylineDocs;
        private final long _dbModId; // database id from the IsotopeModification table if _isotopicMod is true, StructuralModification otherwise
        private final boolean _isotopicMod;
        private UnimodModification _match;
        private final List<UnimodModification> _unimodModifications; // List of possible Unimod modifications

        PxModification(String skylineName, boolean isIsotopic, long dbModId)
        {
            _skylineName = skylineName;

            _skylineDocs = new HashSet<>();
            _isotopicMod = isIsotopic;
            _dbModId = dbModId;

            _unimodModifications = new ArrayList<>();
        }

        public void addSkylineDoc(String skyDocName)
        {
            if(skyDocName != null)
            {
                _skylineDocs.add(skyDocName);
            }
        }

        public Set<String> getSkylineDocs()
        {
            return _skylineDocs;
        }

        public String getName()
        {
            return _match != null ? _match.getName() : null;
        }

        public String getSkylineName()
        {
            return _skylineName;
        }

        public long getDbModId()
        {
            return _dbModId;
        }

        public String getUnimodId()
        {
            return _match == null ? null : "UNIMOD:" + _match.getId();
        }

        public Integer getUnimodIdInt()
        {
            return _match == null ? null : _match.getId();
        }

        public boolean hasUnimodId()
        {
            return _match != null;
        }

        public boolean isIsotopicMod()
        {
            return _isotopicMod;
        }

        public void setUnimodMatch(UnimodModification uMod)
        {
            _match = uMod;
        }

        public void addPossibleUnimod(UnimodModification uMod)
        {
            _unimodModifications.add(uMod);
        }

        public boolean hasPossibleUnimods()
        {
            return _unimodModifications.size() > 0;
        }

        public @NotNull List<UnimodModification> getPossibleUnimodMatches()
        {
            return Collections.unmodifiableList(_unimodModifications);
        }
    }

    public static class PxStructuralMod extends PxModification
    {
        public PxStructuralMod(String skylineName, long dbModId)
        {
            super(skylineName, false, dbModId);
        }

        public PxStructuralMod(String skylineName, long dbModId, UnimodModification uMod)
        {
            super(skylineName, false, dbModId);
            if (uMod != null) setUnimodMatch(uMod);
        }
    }

    public static class PxIsotopicMod extends PxModification
    {
        public PxIsotopicMod(String skylineName, long dbModId)
        {
            super(skylineName, true, dbModId);
        }

        public PxIsotopicMod(String skylineName, long dbModId, UnimodModification uMod)
        {
            super(skylineName, true, dbModId);
            if (uMod != null) setUnimodMatch(uMod);
        }
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testStructuralMods() throws IOException
        {
            // Some modifications in PanoramaWeb that do not have a UNIMOD ID.
            List<Modification> mods = new ArrayList<>();
            Map<String, List<UnimodModification>> unimodMatches = new HashMap<>();
            int idx = 0;
            mods.add(createMod("unsaturated tryptophandione (W)", "OO-HHHH", "W", null));
            // Unimod description: Tryptophan oxidation to beta-unsaturated-2,4-bis-tryptophandione
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(1923, "Delta:H(-4)O(2)", null)));

            mods.add(createMod("oxidation (H)", "O", "H", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(35, "Oxidation", null)));
            mods.add(createMod("Met Ox", "O", "M", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(35, "Oxidation", null)));
            mods.add(createMod("Oxidation (T)", "O", "T", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(35, "Oxidation", null)));
            mods.add(createMod("Oxidation (M)", "O", "M", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(35, "Oxidation", null)));
            mods.add(createMod("try->monooxidation (W)", "O", "W", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(35, "Oxidation", null)));
            mods.add(createMod("Gln Oxidation (Q)", "O", "Q", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(35, "Oxidation", null)));
            mods.add(createMod("Ser oxidation", "O", "S", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(35, "Oxidation", null)));
            mods.add(createMod("Leu/Ile oxidation", "O", "L, I", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(35, "Oxidation", null)));
            mods.add(createMod("Methionine_sulfoxide", "O", "M", null));
            // Comment in Unimod for 'M' specificity: "methionine sulfoxide"
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(35, "Oxidation", null)));
            mods.add(createMod("Met Sulfoxide", "O", "M", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(35, "Oxidation", null)));
            mods.add(createMod("Glu oxidation (E)", "O", "E", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(35, "Oxidation", null)));
            mods.add(createMod("Met ox", "O", "M", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(35, "Oxidation", null)));
            mods.add(createMod("V oxidation", "O", "V", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(35, "Oxidation", null)));
            mods.add(createMod("mono-oxidation", "O", "M, W, H, C, F, Y", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(35, "Oxidation", null)));


            mods.add(createMod("acetylation", "C2H2O1", "K", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(1, "Acetyl", null)));
            mods.add(createMod("Acetyl-T (N-term)", "C2H2O", "T", "N"));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(1, "Acetyl", null)));
            mods.add(createMod("Acetyl (K)", "C2 H3 O -H", "K", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(1, "Acetyl", null)));

            mods.add(createMod("Acetyl-S (N-term)", "C2H2O", "S", "N"));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(1, "Acetyl", null)));

            mods.add(createMod("Asp decarboxylation (D)", "-COHH", "D", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(1915, "Decarboxylation", null)));
            mods.add(createMod("Glu decarboxylation (E)", "-COHH", "E", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(1915, "Decarboxylation", null)));

            mods.add(createMod("carbonyl (R)", "O-HH", "R", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(1918, "Carbonyl", null)));
            mods.add(createMod("carbonyl (A)", "O-HH", "A", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(1918, "Carbonyl", null)));
            mods.add(createMod("Gln Carbonyl (Q)", "O-HH", "Q", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(1918, "Carbonyl", null)));
            mods.add(createMod("carbonyl (L/I)", "O-HH", "L, I", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(1918, "Carbonyl", null)));
            mods.add(createMod("Glu carbonyl (E)", "O-HH", "E", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(1918, "Carbonyl", null)));

            mods.add(createMod("NitroY", "NO2 -H", "Y", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(354, "Nitro", null)));
            mods.add(createMod("NitroY", "N O2 -H", "Y", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(354, "Nitro", null)));

            mods.add(createMod("Phospho(S)", "HO3P", "S", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(21, "Phospho", null)));
            mods.add(createMod("Phosho (Y)", "HPO3", "Y", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(21, "Phospho", null)));
            mods.add(createMod("Phospho (S,T)", "HPO3", "S, T", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(21, "Phospho", null)));
            mods.add(createMod("Phospho", "HO3P", "T", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(21, "Phospho", null)));


            mods.add(createMod("Dimethylation (KRN)", "H4C2", "K, R, N", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(36, "Dimethyl", null)));

            mods.add(createMod("Propionamide(C)", "H5C3NO", "C", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(24, "Propionamide", null)));

            mods.add(createMod("ICAT-C (C)", "C10H17N3O3", "C", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(105, "ICAT-C", null)));

            mods.add(createMod("ring open1 (H)", "O-C2NH", "H", null));
            // Not found in UniModData.cs
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(348, "His->Asn", null)));

            mods.add(createMod("Chlorination (Y)", "Cl -H", "Y", null));
            // UnimodData.cs has the formula for UnimodId 936 as just 'Cl' - AAs = "Y", LabelAtoms = LabelAtoms.None, Formula = "Cl", ID = 936,
            // But Unimod composition for this modification is H(-1) Cl https://www.unimod.org/modifications_view.php?editid1=936
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(936, "Chlorination", null)));

            mods.add(createMod("Dihydroxyformylkynurenine (W)", "OOOO", "W", null));
            // Unimod description: Tryptophan oxidation to dihydroxy-N-formaylkynurenine
            // Not in UniModData.cs
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(1925, "Delta:O(4)", null)));

            mods.add(createMod("Tryptoline (W)", "C", "W", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(1009, "Thiazolidine", null)));

            mods.add(createMod("Methyl-ester (E)", "CH2", "E", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(34, "Methyl", null)));

            mods.add(createMod("Pyro Glu", "-OH2", "Q", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(23, "Dehydrated", null)));

            mods.add(createMod("hydroxy tryptophandione (W)", "OOO-HHHH", "W", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(1924, "Delta:H(-4)O(3)", null)));

            mods.add(createMod("HexNAc(1)dHex(1) (N)", "H23C14NO9", "N", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(142, "HexNAc(1)dHex(1)", null)));

            mods.add(createMod("C-term deamidation", "HN-O", null, "C"));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(2, "Amidated", null)));

            mods.add(createMod("ring open 4 (H)", "OO-NNCHH", "H", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(1916, "Aspartylurea", null)));
            mods.add(createMod("ring open 2(H)", "OO-C2NNHH", "H", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(349, "His->Asp", null)));
            mods.add(createMod("ring open 3 (H)", "OO-NHC", "H", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(1917, "Formylasparagine", null)));

            mods.add(createMod("Kinome-ATP-K", "C10H16N2O2", "K", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(1031, "Biotin:Thermo-88310", null)));

            mods.add(createMod("Carboxymethylcysteine", "CH2COO", "C", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(6, "Carboxymethyl", null)));

            mods.add(createMod("Carbamidomethyl Cysteine", "C2H3ON", "C", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(4, "Carbamidomethyl", null)));

            mods.add(createMod("Glycation(V)", "C6H12O6-H2O", "V", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(41, "Hex", null)));

            mods.add(createMod("trioxidation (MHWFY)", "OOO", "M, H, W, F, Y", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(new UnimodModification(345, "Trioxidation", null)));

            // Modifications with no matches
            mods.add(createMod("GlcNAc-Fuc", null, "N", null)); // No formula
            unimodMatches.put(mods.get(idx++).getName(), Collections.emptyList());
            mods.add(createMod("pyro-glu", null, "Q", "N")); // No formula
            unimodMatches.put(mods.get(idx++).getName(), Collections.emptyList());
            mods.add(createMod("GlcNAc", null, "N", null)); // No formula
            unimodMatches.put(mods.get(idx++).getName(), Collections.emptyList());
            mods.add(createMod("Pyroglutamic acid (Q)", null, "Q", "N")); // No formula
            unimodMatches.put(mods.get(idx++).getName(), Collections.emptyList());
            mods.add(createMod("Ubiquitin", null, "K", null)); // No formula
            unimodMatches.put(mods.get(idx++).getName(), Collections.emptyList());
            mods.add(createMod("Try->glycine hydroperoxide (W)", "OO-C9H7N", "W", null));
            // Could be this https://www.unimod.org/modifications_view.php?editid1=676 but formula is different.
            // Trp->Gly substitution, H(-7) C(-9) N(-1), on W.
            // UniModData.cs does not have mod 676
            unimodMatches.put(mods.get(idx++).getName(), Collections.emptyList());

            mods.add(createMod("N-Acetyl-Phospho-T", "C2H3O4P", "T", "N"));
            // Looks like a combination of Acetly (Unimod:1, H(2) C(2) O) and Phopho (Unimod:21, H O(3) P)
            unimodMatches.put(mods.get(idx++).getName(), Collections.emptyList());
            mods.add(createMod("Nacetyl_phospho(T)", "C2H3O4P", "T", "N"));
            unimodMatches.put(mods.get(idx++).getName(), Collections.emptyList());

            mods.add(createMod("Lipoyl NEM (K)", "H14C8OS2 H7C6NO2 H7C6NO2", "K", null));
            unimodMatches.put(mods.get(idx++).getName(), Collections.emptyList());
            mods.add(createMod("His-Thiolatp", "PO2", "S", null));
            unimodMatches.put(mods.get(idx++).getName(), Collections.emptyList());
            mods.add(createMod("H->hydroxy-dioxidation", "H2O2", "H", null));
            unimodMatches.put(mods.get(idx++).getName(), Collections.emptyList());
            mods.add(createMod("OOO-HH (C)", "OOO-HH", "C", null));
            unimodMatches.put(mods.get(idx++).getName(), Collections.emptyList());
            mods.add(createMod("N-term Met loss+ acetylation (S)", "-H6C3NS", "S", "N"));
            unimodMatches.put(mods.get(idx++).getName(), Collections.emptyList());

            // Modifications with multiple potential Unimod matches based on formula, modification site and terminus
            mods.add(createMod("Lys carbonyl", "O-HH", "K", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(
                    new UnimodModification(359, "Pro->pyro-Glu", null),
                    new UnimodModification(288, "Trp->Oxolactone", null),
                    new UnimodModification(1204, "Thr->Asp", null),
                    new UnimodModification(1918, "Carbonyl", null)));
            mods.add(createMod("MetOxid_NtermAcetyl", "C2H2O2", "M", "N"));
            unimodMatches.put(mods.get(idx++).getName(), List.of(
                    new UnimodModification(6, "Carboxymethyl", null),
                    new UnimodModification(545, "Ala->Glu", null),
                    new UnimodModification(576, "Gly->Asp", null)));
            mods.add(createMod("Acetyl-M (N-term)", "C2H2O", "M", "N"));
            unimodMatches.put(mods.get(idx++).getName(), List.of(
                    new UnimodModification(1, "Acetyl", null),
                    new UnimodModification(1197, "Ser->Glu", null)));
            mods.add(createMod("Acetyl-V (N-term)", "C2H2O", "V", "N"));
            unimodMatches.put(mods.get(idx++).getName(), List.of(
                    new UnimodModification(1, "Acetyl", null),
                    new UnimodModification(1197, "Ser->Glu", null)));
            mods.add(createMod("Acetyl-A (N-term)", "C2H2O", "A", "N"));
            unimodMatches.put(mods.get(idx++).getName(), List.of(
                    new UnimodModification(1, "Acetyl", null),
                    new UnimodModification(1197, "Ser->Glu", null)));
            mods.add(createMod("Methyl (TSCKRH)", "H4C2", "T, S, C, K, R, H", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(
                    new UnimodModification(36, "Dimethyl", null),
                    new UnimodModification(255, "Delta:H(4)C(2)", null),
                    new UnimodModification(280, "Ethyl", null),
                    new UnimodModification(546, "Ala->Val", null),
                    new UnimodModification(1061, "Cys->Met", null)));
            mods.add(createMod("dopa-derived quinone (Y+O-2H)", "O-HH", "Y", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(
                    new UnimodModification(359, "Pro->pyro-Glu", null),
                    new UnimodModification(288, "Trp->Oxolactone", null),
                    new UnimodModification(1204, "Thr->Asp", null),
                    new UnimodModification(1918, "Carbonyl", null)));
            mods.add(createMod("dioxidation (MHWFY)", "OO", "M, H, W, F, Y", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(
                    new UnimodModification(425, "Dioxidation", null),
                    new UnimodModification(1168, "Pro->Glu", null)));
            mods.add(createMod("mTRAQ +0 (N-term)", "H12C7N2O", null, "N"));
            unimodMatches.put(mods.get(idx++).getName(), List.of(
                    new UnimodModification(888, "mTRAQ", null),
                    new UnimodModification(1027, "DMP[140]", null)));
            mods.add(createMod("mTRAQ +0 (K)", "H12C7N2O", "K", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(
                    new UnimodModification(888, "mTRAQ", null),
                    new UnimodModification(1027, "DMP[140]", null)));
            mods.add(createMod("Methyl-ester (D)", "CH2", "D", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(
                    new UnimodModification(34, "Methyl", null),
                    new UnimodModification(558, "Asp->Glu", null)));
            mods.add(createMod("deamidate (N)", "O-NH", "N", null));
            unimodMatches.put(mods.get(idx++).getName(), List.of(
                    new UnimodModification(7, "Deamidated", null),
                    new UnimodModification(621, "Asn->Asp", null)));
            mods.add(createMod("Dimethylation (N-term)", "H4C2", null, "N"));
            unimodMatches.put(mods.get(idx++).getName(), List.of(
                    new UnimodModification(36, "Dimethyl", null),
                    new UnimodModification(255, "Delta:H(4)C(2)", null),
                    new UnimodModification(280, "Ethyl", null)));


            File unimodXml = getUnimodFile();
            UnimodModifications uMods = null;
            try
            {
                uMods = new UnimodParser().parse(unimodXml);
            }
            catch (Exception e)
            {
                fail("Failed to parse UNIMOD modifications. " + e.getMessage());
            }

            assertNotNull(uMods);

            for(Modification mod: mods)
            {
                PxModification pxMod = getStructuralUnimodMod(mod, uMods);

                List<UnimodModification> matches = unimodMatches.get(pxMod.getSkylineName());
                if (matches.size() == 0)
                {
                    assertFalse("Unexpected Unimod match for modification " + pxMod.getSkylineName(), pxMod.hasUnimodId());
                    assertTrue("Unexpected possible mods for modification " + pxMod.getSkylineName(), pxMod.getPossibleUnimodMatches().size() == 0);
                }
                else if (matches.size() == 1)
                {
                    assertTrue("Expected a Unimod Id for modification " + pxMod.getSkylineName(), pxMod.hasUnimodId());
                    assertEquals("Unexpected Unimod match Id for modification " + pxMod.getSkylineName(), matches.get(0).getId(), pxMod.getUnimodIdInt().intValue());
                    assertEquals("Unexpected Unimod match name for modification " + pxMod.getSkylineName(), matches.get(0).getName(), pxMod.getName());
                    assertFalse("modification " + pxMod.getSkylineName() + " has a Unimod Id."
                            + " Unexpected " + pxMod.getPossibleUnimodMatches().size() + " possible matches", pxMod.hasPossibleUnimods());
                }
                else if (matches.size() > 1)
                {
                    List<UnimodModification> possibleMods = pxMod.getPossibleUnimodMatches();
                    assertFalse("Unexpected Unimod Id for modification " + pxMod.getSkylineName(), pxMod.hasUnimodId());
                    assertEquals("Expected " + matches.size() + " possible matches for modification " + pxMod.getSkylineName(), matches.size(), possibleMods.size());
                    assertEquals(matches.stream().map(m -> m.getId()).collect(Collectors.toSet()), possibleMods.stream().map(m -> m.getId()).collect(Collectors.toSet()));
                }

//                if (pxMod.hasUnimodId())
//                {
//                    String term = mod.getTerminus() == null ? "" : (mod.getTerminus().equals("N") ? "N-term" : "C-term");
//                    String modInfo = pxMod.getSkylineName() + ", " + UnimodModification.normalizeFormula(mod.getFormula()) + ", " + mod.getAminoAcid() + ", TERM: " + term;
//                    System.out.print("Skyline: " + modInfo);
//                    // System.out.println(" --- " + pxMod.getUnimodId() + ", " + pxMod.getName());
//                    System.out.println(" --- List.of(new UnimodModification(" + pxMod.getUnimodIdInt() + ", \"" + pxMod.getName() + "\", null))");
//                }
//                if (pxMod.hasPossibleUnimods())
//                {
//                    String term = mod.getTerminus() == null ? "" : (mod.getTerminus().equals("N") ? "N-term" : "C-term");
//                    String modInfo = pxMod.getSkylineName() + ", " + UnimodModification.normalizeFormula(mod.getFormula()) + ", " + mod.getAminoAcid() + ", TERM: " + term;
//                    System.out.println("Skyline: " + modInfo);
//                    for (UnimodModification umod: pxMod.getPossibleUnimodMatches())
//                    {
//                        System.out.println(" --- List.of(new UnimodModification(" + umod.getId() + ", \"" + umod.getName() + "\", null))");
//                    }
//                }
            }
        }

        private File getUnimodFile() throws IOException
        {
            File root = JunitUtil.getSampleData(null, "../../../server");
            if(root == null)
            {
                root = new File(System.getProperty("user.dir"));
            }
            // /modules/MacCossLabModules/PanoramaPublic/resources/unimod_NO_NAMESPACE.xml
            return new File(root, "/modules/MacCossLabModules/PanoramaPublic/resources/unimod_NO_NAMESPACE.xml");
        }

        @Test
        public void testIsotopicMods() throws IOException
        {
            // Some modifications on PanoramaWeb that do not have a UNIMOD ID.
            List<IsotopeModification> mods = new ArrayList<>();
            mods.add(createisotopicMod("all N15",null,null,null,false,false,true,false));
            mods.add(createisotopicMod("13C V",null,"V",null,false,true,false,false));
            mods.add(createisotopicMod("Label:13C(6)15N(2) (K)",null,"K",null,false,true,true,false));
            mods.add(createisotopicMod("heavy K",null,"K","C",false,true,true,false));
            mods.add(createisotopicMod("K-8",null,"K",null,false,true,true,false));
            mods.add(createisotopicMod("HeavyK",null,"K","C",false,true,true,false));
            mods.add(createisotopicMod("Label:13C15N",null,null,null,false,true,true,false));
            mods.add(createisotopicMod("Label:13C(6)15N(4) (C-term R)",null,"R","C",false,true,true,false));
            mods.add(createisotopicMod("Label:13C(6)15N(2) (C-term K)",null,"K",null,false,true,true,false));
            mods.add(createisotopicMod("mTRAQ +8 (N-term)","C'6N'2 - C6N2",null,"N",false,false,false,false));
            mods.add(createisotopicMod("HeavyR",null,"R","C",false,true,true,false));
            mods.add(createisotopicMod("R-6",null,"R",null,false,true,false,false));
            mods.add(createisotopicMod("Leu6C13N15","C'6N' -C6N","L",null,false,false,false,false));
            mods.add(createisotopicMod("R-10",null,"R",null,false,true,true,false));
            mods.add(createisotopicMod("Label:13C(6)15N(4) (C-term R)",null,"R",null,false,true,true,false));
            mods.add(createisotopicMod("15N",null,null,null,false,false,true,false));
            mods.add(createisotopicMod("all 15N",null,null,null,false,false,true,false));
            mods.add(createisotopicMod("13C R",null,"R","C",false,true,false,false));
            mods.add(createisotopicMod("R 13C 15N",null,"R","C",false,true,true,false));
            mods.add(createisotopicMod("K-6",null,"K",null,false,true,false,false));
            mods.add(createisotopicMod("Label:15N",null,null,null,false,false,true,false));
            mods.add(createisotopicMod("mTRAQ +8 (K)","C'6N'2 - C6N2","K",null,false,false,false,false));
            mods.add(createisotopicMod("Label:13C15N",null,"V",null,false,true,true,false));
            mods.add(createisotopicMod("Label:13C",null,null,null,false,true,false,false));
            mods.add(createisotopicMod("heavyK","C6H8H'4ON2 - C6H12ON2","K",null,false,false,false,false));
            mods.add(createisotopicMod("R(+10)",null,"R",null,false,true,true,false));
            mods.add(createisotopicMod("R10",null,"R","C",false,true,true,false));
            mods.add(createisotopicMod("heavy R",null,"R","C",false,true,true,false));
            mods.add(createisotopicMod("N15",null,null,null,false,false,true,false));
            mods.add(createisotopicMod("Label:13C(6) (C-term K)",null,"K","C",false,true,false,false));
            mods.add(createisotopicMod("L-6",null,"L",null,false,true,false,false));
            mods.add(createisotopicMod("Label:13C(4)15N(2) (C-term E)",null,"E","C",false,true,true,false));
            mods.add(createisotopicMod("Label:13C(6)15N(2) (C-term K)",null,"K","C",false,true,true,false));
            mods.add(createisotopicMod("mTRAQ +4 (N-term)","C'3N'1 - C3N1",null,"N",false,false,false,false));
            mods.add(createisotopicMod("K(+08)",null,"K",null,false,true,true,false));
            mods.add(createisotopicMod("Label:13C(6) (C-term R)",null,"R","C",false,true,false,false));
            mods.add(createisotopicMod("mTRAQ +4 (K)","C'3N'1 - C3N1","K",null,false,false,false,false));

            File unimodXml = getUnimodFile();
            UnimodModifications uMods = null;
            try
            {
                uMods = new UnimodParser().parse(unimodXml);
            }
            catch (Exception e)
            {
                fail("Failed to parse UNIMOD modifications. " + e.getMessage());
            }

            assertNotNull(uMods);

            // These modifications do not match with a modification in unimod.xml
            Set<String> unknown = new HashSet<>();
            unknown.add("all N15");
            unknown.add("13C V");
            unknown.add("Label:13C15N");
            unknown.add("15N");
            unknown.add("all 15N");
            unknown.add("Label:15N");
            unknown.add("Label:13C");
            unknown.add("N15");
            unknown.add("mTRAQ +4 (K)");

            for(IsotopeModification mod: mods)
            {
                PxModification pxMod = getIsotopicUnimodMod(mod, uMods, null);

                if(!unknown.contains(pxMod.getSkylineName()) && !pxMod.hasUnimodId())
                {
                    String term = mod.getTerminus() == null ? "" : (mod.getTerminus().equals("N") ? "N-term" : "C-term");
                    String labels = mod.getLabel2H() ? "2H" : "-";
                    labels = labels + (mod.getLabel13C() ? "13C" : "-");
                    labels = labels + (mod.getLabel15N() ? "15N" : "-");
                    labels = labels + (mod.getLabel18O() ? "18O" : "-");
                    String modInfo = pxMod.getSkylineName() + ", " + UnimodModification.normalizeFormula(mod.getFormula()) + ", '" + mod.getAminoAcid() + "', TERM: " + term
                            + ", Labels: " + labels;
                    modInfo += "; Possible mods: ";
                    List<UnimodModification> possibleMods = pxMod.getPossibleUnimodMatches();
                    for (UnimodModification umod: possibleMods)
                    {
                        modInfo += umod.getId() + ": " + umod.getName() + ", ";
                    }
                    fail("Expected to get a Unimod match for modification: " + modInfo);
                }
            }
        }

        private Modification createMod(String name, String formula, String sites, String terminus)
        {
            Modification mod = new Modification();
            mod.setFormula(formula);
            mod.setTerminus(terminus);
            mod.setAminoAcid(sites);
            mod.setName(name);
            return mod;
        }

        private IsotopeModification createisotopicMod(String name, String formula, String sites, String terminus,
                                                                      boolean label2h, boolean label13c, boolean label15n, boolean label18o)
        {
            IsotopeModification mod = new IsotopeModification();
            mod.setFormula(formula);
            mod.setTerminus(terminus);
            mod.setAminoAcid(sites);
            mod.setName(name);
            mod.setLabel2H(label2h);
            mod.setLabel13C(label13c);
            mod.setLabel15N(label15n);
            mod.setLabel18O(label18o);
            return mod;
        }
    }

    static class Modification implements IModification
    {
        private long _id;
        private String _name;
        private String _aminoAcid;
        private String _terminus;
        private String _formula;
        private Double _massDiffMono;
        private Double _massDiffAvg;
        private Integer _unimodId;

        @Override
        public long getId()
        {
            return _id;
        }

        public void setId(long id)
        {
            _id = id;
        }

        @Override
        public String getName()
        {
            return _name;
        }

        public void setName(String name)
        {
            _name = name;
        }

        @Override
        public String getAminoAcid()
        {
            return _aminoAcid;
        }

        public void setAminoAcid(String aminoAcid)
        {
            _aminoAcid = aminoAcid;
        }

        @Override
        public String getTerminus()
        {
            return _terminus;
        }

        public void setTerminus(String terminus)
        {
            _terminus = terminus;
        }

        @Override
        public String getFormula()
        {
            return _formula;
        }

        public void setFormula(String formula)
        {
            _formula = formula;
        }

        @Override
        public Double getMassDiffMono()
        {
            return _massDiffMono;
        }

        public void setMassDiffMono(Double massDiffMono)
        {
            _massDiffMono = massDiffMono;
        }

        @Override
        public Double getMassDiffAvg()
        {
            return _massDiffAvg;
        }

        public void setMassDiffAvg(Double massDiffAvg)
        {
            _massDiffAvg = massDiffAvg;
        }

        @Override
        public Integer getUnimodId()
        {
            return _unimodId;
        }

        public void setUnimodId(Integer unimodId)
        {
            _unimodId = unimodId;
        }
    }

    static class IsotopeModification extends Modification implements IModification.IIsotopeModification
    {
        private Boolean _label13C;
        private Boolean _label15N;
        private Boolean _label18O;
        private Boolean _label2H;

        @Override
        public Boolean getLabel13C()
        {
            return _label13C;
        }

        public void setLabel13C(Boolean label13C)
        {
            _label13C = label13C;
        }

        @Override
        public Boolean getLabel15N()
        {
            return _label15N;
        }

        public void setLabel15N(Boolean label15N)
        {
            _label15N = label15N;
        }

        @Override
        public Boolean getLabel18O()
        {
            return _label18O;
        }

        public void setLabel18O(Boolean label18O)
        {
            _label18O = label18O;
        }

        @Override
        public Boolean getLabel2H()
        {
            return _label2H;
        }

        public void setLabel2H(Boolean label2H)
        {
            _label2H = label2H;
        }
    }
}
