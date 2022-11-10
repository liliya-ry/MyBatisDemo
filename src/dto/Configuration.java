package dto;

import session.SqlSessionFactory;

import java.util.Map;
import java.util.Properties;

public class Configuration {
    public Map<String, Environment> environments;
    public Environment defaultEnvironment;
    public Properties properties;
    public Map<String, TypeAlias> typeAliases;
    public Map<Class<?>, Mapper> mappers;
    public Map<String, SqlSessionFactory> environmentFactories;

    public Configuration(Map<String, Environment> environments, Environment defaultEnvironment, Properties properties, Map<String, TypeAlias> typeAliases) {
        this.environments = environments;
        this.defaultEnvironment = defaultEnvironment;
        this.properties = properties;
        this.typeAliases = typeAliases;
    }

    public Mapper getMapperByType(Class<?> type) {
        return mappers.get(type);
    }

    public Properties getProperties() {
        return properties;
    }
}
