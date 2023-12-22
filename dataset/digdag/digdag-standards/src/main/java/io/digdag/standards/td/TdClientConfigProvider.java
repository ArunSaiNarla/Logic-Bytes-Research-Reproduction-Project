package io.digdag.standards.td;

import ch.qos.logback.classic.Level;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.treasuredata.client.AbstractTDClientBuilder;
import com.treasuredata.client.TDClientConfig;
import io.digdag.core.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

public class TdClientConfigProvider
        implements Provider<TDClientConfig>
{
    private final Map<String, String> env;

    @Inject
    public TdClientConfigProvider(@Environment Map<String, String> env)
    {
        this.env = env;
    }

    @Override
    public TDClientConfig get()
    {
        Path tdConf = configPath(env);
        if (!Files.exists(tdConf)) {
            return null;
        }

        // XXX (dano): silence spam in TDClientConfig
        Logger logger = LoggerFactory.getLogger(TDClientConfig.class);
        if (logger instanceof ch.qos.logback.classic.Logger) {
            ((ch.qos.logback.classic.Logger) logger).setLevel(Level.WARN);
        }

        try {
            Properties props = TDClientConfig.readTDConf(tdConf.toFile());
            ConfigLoader configLoader = new ConfigLoader();
            configLoader.setProperties(props);
            return configLoader.buildConfig();
        }
        catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.warn("Failed to parse TD configuration file: {}", tdConf, e);
            }
            else {
                logger.warn("Failed to parse TD configuration file: {}", tdConf);
            }
            return null;
        }
    }

    private static Path configPath(Map<String, String> env)
    {
        String path;
        path = env.get("TREASURE_DATA_CONFIG_PATH");
        if (path != null) {
            return Paths.get(path);
        }
        path = env.get("TD_CONFIG_PATH");
        if (path != null) {
            return Paths.get(path);
        }
        return Paths.get(System.getProperty("user.home"), ".td", "td.conf");
    }

    // XXX (dano): Awful hack...
    static class ConfigLoader
            extends AbstractTDClientBuilder<Void, ConfigLoader>
    {

        ConfigLoader()
        {
            super(false);
        }

        @Override
        protected ConfigLoader self()
        {
            return this;
        }

        @Override
        public Void build()
        {
            throw new UnsupportedOperationException();
        }
    }
}
