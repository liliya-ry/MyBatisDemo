package session;


import dto.*;
import dto.data_source.DataSource;
import parsers.ConfigurationParser;

import java.io.*;
import java.util.*;

public class SqlSessionFactoryBuilder {
    private Configuration configuration;

    private SqlSessionFactory build(Configuration configuration, Environment environment, Properties properties) throws Exception {
        this.configuration = configuration;

        if (environment != null) {
            this.configuration.setDefaultEnvironment(environment);
        }

        if (properties != null) {
            this.configuration.setProperties(properties);
        }

        return getFactoryByEnvironment(this.configuration, this.configuration.getDefaultEnvironment());
    }

    public SqlSessionFactory build(Configuration configuration) throws Exception {
        return build(configuration, null, null);
    }

    public SqlSessionFactory build(InputStream in) throws Exception {
        return build(in, null, null);
    }

    public SqlSessionFactory build(InputStream in, Environment environment) throws Exception {
        return build(in, environment, null);
    }

    public SqlSessionFactory build(InputStream in, Environment environment, Properties properties) throws Exception {
        ConfigurationParser parser = new ConfigurationParser(in);
        Configuration config = parser.getConfiguration();
        in.close();
        return build(config, environment, properties);
    }

    public SqlSessionFactory build(Reader reader) throws Exception {
        return build(reader, null, null);
    }

    public SqlSessionFactory build(Reader reader, Environment environment) throws Exception {
        return build(reader, environment, null);
    }

    public SqlSessionFactory build(Reader reader, Environment environment, Properties properties) throws Exception {
        ConfigurationParser parser = new ConfigurationParser(reader);
        Configuration config = parser.getConfiguration();
        reader.close();
        return build(config, environment, properties);
    }

    private void fillFactoriesMap() throws Exception {
        this.configuration.setEnvironmentFactories(new HashMap<>());

        for (Map.Entry<String, Environment> env : this.configuration.getEnvironments().entrySet()) {
            Environment e = env.getValue();
            DataSource dataSource = e.getDataSource();
            SqlSessionFactory factory = new SqlSessionFactory(this.configuration, dataSource);
            this.configuration.getEnvironmentFactories().put(e.getId(), factory);
        }
    }

    private SqlSessionFactory getFactoryByEnvironment(Configuration configuration, Environment environment) throws Exception {
        if (this.configuration.getEnvironmentFactories() == null) {
            fillFactoriesMap();
        }
        return configuration.getEnvironmentFactories().get(environment.getId());
    }
}
