package session;


import dto.Configuration;
import dto.ConfigurationParser;
import dto.DataSource;
import dto.Environment;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SqlSessionFactoryBuilder {
    private Configuration configuration;

    public SqlSessionFactory build(Configuration configuration) throws Exception {
        this.configuration = configuration;
        return getFactoryByEnvironment(configuration, this.configuration.defaultEnvironment);
    }

    private void fillFactoriesMap() throws Exception {
        this.configuration.environmentFactories = new HashMap<>();

        for (Map.Entry<String, Environment> env : this.configuration.environments.entrySet()) {
            Environment e = env.getValue();
            DataSource dataSource = e.dataSource;
            String dataSourceType = dataSource.type;
            SqlSessionFactory factory = dataSourceType.equals("POOLED") ?
                    new SqlSessionFactoryPooled(configuration) :
                    new SqlSessionFactoryUnpooled(configuration);
            this.configuration.environmentFactories.put(e.id, factory);
        }
    }

    private SqlSessionFactory getFactoryByEnvironment(Configuration configuration, Environment environment) throws Exception {
        if (this.configuration.environmentFactories == null) {
            fillFactoriesMap();
        }
        return configuration.environmentFactories.get(environment.id);
    }

    public SqlSessionFactory build(InputStream in) throws Exception {
        ConfigurationParser parser = new ConfigurationParser(in);
        Configuration config = parser.getConfiguration();
        return build(config);
    }

    public SqlSessionFactory build(InputStream in, Environment environment) throws Exception {
        ConfigurationParser parser = new ConfigurationParser(in);
        Configuration config = parser.getConfiguration();
        return getFactoryByEnvironment(config, environment);
    }
}
