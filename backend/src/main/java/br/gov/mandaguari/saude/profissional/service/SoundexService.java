package br.gov.mandaguari.saude.profissional.service;

import org.springframework.stereotype.Service;

/**
 * Portuguese phonetic key — a verbatim port of GeneXus {@code psau_soundex.java:48-136}. ORDER of the
 * replacement table is load-bearing (earlier rules feed later ones), so the list is transcribed
 * 1:1 from the legacy procedure. Used to recompute {@code ProPesNomSoundex} from {@code PesNom} on
 * every confirm (R15) so modern search parity with the legacy grid is preserved.
 *
 * <p>Algorithm (legacy):
 * <ol>
 *   <li>trim + uppercase the name;</li>
 *   <li>apply the ordered {@code strReplace} table below;</li>
 *   <li>strip a trailing S/Z/R/M/N/L;</li>
 *   <li>strip a trailing "AO";</li>
 *   <li>replace "Ç"→"S" then "L"→"R";</li>
 *   <li>collapse consecutive duplicate letters.</li>
 * </ol>
 * GeneXus {@code substring(s, start, len)} is 1-based; this port works on the same semantics using
 * 0-based Java indexing.
 */
@Service
public class SoundexService {

    /** Ordered replacement pairs — transcribed verbatim from psau_soundex.java:60-102. */
    private static final String[][] REPLACEMENTS = {
            {"Y", "I"}, {"BR", "B"}, {"BL", "B"}, {"PH", "F"}, {"MG", "G"}, {"NG", "G"}, {"RG", "G"},
            {"GE", "J"}, {"GI", "J"}, {"RJ", "J"}, {"MJ", "J"}, {"NJ", "J"}, {"GR", "G"}, {"GL", "G"},
            {"CE", "S"}, {"CI", "S"}, {"CH", "S"}, {"CT", "T"}, {"CS", "S"}, {"Q", "K"}, {"CA", "K"},
            {"CO", "K"}, {"CU", "K"}, {"CK", "K"}, {"C", "K"}, {"LH", "L"}, {"RM", "SM"}, {"N", "M"},
            {"GM", "M"}, {"MD", "M"}, {"NH", "N"}, {"PR", "P"}, {"X", "S"}, {"TS", "S"}, {"C", "S"},
            {"Z", "S"}, {"RS", "S"}, {"TR", "T"}, {"TL", "T"}, {"LT", "T"}, {"RT", "T"}, {"ST", "T"},
            {"W", "V"},
    };

    public String compute(String nome) {
        if (nome == null) return "";
        String s = nome.trim().toUpperCase();
        if (s.isEmpty()) return "";

        for (String[] r : REPLACEMENTS) {
            s = s.replace(r[0], r[1]);
        }

        // Strip a trailing S/Z/R/M/N/L (psau_soundex.java:103-109).
        if (!s.isEmpty()) {
            char last = s.charAt(s.length() - 1);
            if (last == 'S' || last == 'Z' || last == 'R' || last == 'M' || last == 'N' || last == 'L') {
                s = s.substring(0, s.length() - 1);
            }
        }

        // Strip a trailing "AO" (psau_soundex.java:111-118).
        if (s.length() >= 2 && s.endsWith("AO")) {
            s = s.substring(0, s.length() - 2);
        }

        // Ç → S then L → R (psau_soundex.java:120-121).
        s = s.replace("Ç", "S").replace("L", "R");

        // Collapse consecutive duplicate letters (psau_soundex.java:122-136).
        if (s.length() >= 2) {
            StringBuilder out = new StringBuilder();
            char prev = s.charAt(0);
            out.append(prev);
            for (int i = 1; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c != prev) {
                    out.append(c);
                    prev = c;
                }
            }
            s = out.toString();
        }
        return s;
    }
}
