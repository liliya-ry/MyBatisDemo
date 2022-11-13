package dto;

import dto.queries.Query;
import session.SqlSessionFactory;

import java.util.*;


public class Configuration {
    Map<String, Environment> environments;
    Environment defaultEnvironment;
    Properties properties;
    Map<String, TypeAlias> typeAliases;
    Map<String, Mapper> mappers;
    Set<Class<?>> classMappers;
    Map<String, String> queriesWithNamespace;
    Map<String, String> resultMapsWithNamespace;
    Map<String, SqlSessionFactory> environmentFactories;

    public Configuration(Map<String, Environment> environments, Environment defaultEnvironment, Properties properties, Map<String, TypeAlias> typeAliases) {
        this.environments = environments;
        this.defaultEnvironment = defaultEnvironment;
        this.properties = properties;
        this.typeAliases = typeAliases;
        this.queriesWithNamespace = new HashMap<>();
    }

    public Mapper getMapperByNamespace(String namespace) {
        return mappers.get(namespace);
    }

    public Map<String, TypeAlias> getTypeAliases() {
        return typeAliases;
    }

    public Environment getDefaultEnvironment() {
        return defaultEnvironment;
    }

    public void setDefaultEnvironment(Environment defaultEnvironment) {
        this.defaultEnvironment = defaultEnvironment;
    }

    public Map<String, Mapper> getMappers() {
        return mappers;
    }

    private String getNamespaceByQueryId(String queryId) {
        return this.queriesWithNamespace.get(queryId);
    }
    private String getNamespaceByResultMapId(String resultMapId) {
        return this.queriesWithNamespace.get(resultMapId);
    }

    public Map<String, SqlSessionFactory> getEnvironmentFactories() {
        return environmentFactories;
    }

    public Query getQueryById(String queryId) {
        String namespace = getNamespaceByQueryId(queryId);
        Mapper m = getMapperByNamespace(namespace);
        return m.getQueryById(queryId);
    }

    public ResultMap getResultMapById(String resultMapId) {
        String namespace = getNamespaceByResultMapId(resultMapId);
        Mapper m = getMapperByNamespace(namespace);
        return m.getResultMapById(resultMapId);
    }

    public Map<String, Environment> getEnvironments() {
        return environments;
    }

    public Set<Class<?>> getClassMappers() {
        return classMappers;
    }

    public void setMappers(Map<String, Mapper> mappers) {
        this.mappers = mappers;
    }

    public void setEnvironmentFactories(Map<String, SqlSessionFactory> environmentFactories) {
        this.environmentFactories = environmentFactories;
    }

    public void setQueriesWithNamespace() {
        for (Map.Entry<String, Mapper> entry : mappers.entrySet()) {
            String namespace = entry.getKey();
            Mapper m = entry.getValue();
            Map<String, Query> queries = m.queries;
            for (String queryId : queries.keySet()) {
                this.queriesWithNamespace.put(queryId, namespace);
            }
        }
    }

    public void setClassMappers(Set<Class<?>> classMappers) {
        this.classMappers = classMappers;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }


}
