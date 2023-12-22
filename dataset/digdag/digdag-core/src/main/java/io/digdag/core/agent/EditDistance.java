package io.digdag.core.agent;

import java.util.List;
import java.util.Collection;
import com.google.common.collect.ImmutableList;

public class EditDistance
{
    public static List<String> suggest(String key, Collection<String> candidateKeys, double thresholdRatio)
    {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (String candidate : candidateKeys) {
            int threshold = (int) Math.floor(candidate.length() * thresholdRatio);
            int editDistance = distance(key, candidate);
            if (editDistance <= threshold) {
                builder.add(candidate);
            }
        }
        return builder.build();
    }

    private EditDistance()
    { }

    static int distance(String str1, String str2)
    {
        int len1 = str1.length();
        int len2 = str2.length();

        if (len2 == 0) {
            return len1;
        }
        if (len1 == 0) {
            return len2;
        }

        int[] ds = new int[len2 + 1];
        for (int i = 0; i < ds.length; i++) {
            ds[i] = i;
        }

        int x = 0;
        for (int i1 = 0; i1 < len1; i1++) {
            int in = i1 + 1;
            for (int i2 = 0; i2 < len2; i2++) {
                int cost = (str1.charAt(i1) == str2.charAt(i2)) ? 0 : 1;
                x = min3(
                        ds[i2 + 1] + 1, // insertion
                        in + 1,        // deletion
                        ds[i2] + cost   // substitution
                    );
                ds[i2] = in;
                in = x;
            }
            ds[len2] = x;
        }

        return x;
    }

    private static int min3(int a, int b, int c)
    {
        if (a < b && a < c) {
            return a;
        }
        else if (b < c) {
            return b;
        }
        else {
            return c;
        }
    }
}
