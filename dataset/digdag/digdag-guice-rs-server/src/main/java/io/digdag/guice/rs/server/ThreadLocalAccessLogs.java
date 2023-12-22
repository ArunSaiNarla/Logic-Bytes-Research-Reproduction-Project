package io.digdag.guice.rs.server;

import java.util.HashMap;
import java.util.Map;

public class ThreadLocalAccessLogs
{
    private static final ThreadLocal<Map<String, String>> THREAD_LOCAL =
        new ThreadLocal<Map<String, String>>()
        {
            @Override
            protected Map<String, String> initialValue()
            {
                return new HashMap<>();
            }
        };

    private ThreadLocalAccessLogs()
    { }

    public static void putAttribute(String key, String value)
    {
        THREAD_LOCAL.get().put(key, value);
    }

    public static Map<String, String> resetAttributes()
    {
        Map<String, String> map = THREAD_LOCAL.get();
        THREAD_LOCAL.remove();
        return map;
    }
}
