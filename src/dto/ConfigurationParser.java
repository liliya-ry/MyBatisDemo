package dto;

import mappers.EmployeeMapper;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ConfigurationParser {
    private static final String ILLEGAL_ATTRIBUTE = "Illegal attribute: ";
    private static final String ILLEGAL_ELEMENT = "Illegal element: ";
    private static final String EMPTY_TYPE = "Type can not be empty";
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("[#$]\\{([\\w\\d\\.]+)\\}");

    private final Configuration configuration;
    private Properties properties;
    private List<String> mapperNames;

    public Configuration getConfiguration() {
        return configuration;
    }
    public ConfigurationParser(String resource) throws Exception {
        this(new FileInputStream(resource));
    }

    public ConfigurationParser(InputStream in) throws Exception {
        this.configuration = parseConfiguration(in);
        this.configuration.mappers = new HashMap<>();
        parseMappers();
    }

    private Configuration parseConfiguration(String resource) throws Exception {
        Reader reader = new FileReader(resource);
        File f = new File(resource);
        FileInputStream fis = new FileInputStream(f);
        return parseConfiguration(fis);
    }

    private Configuration parseConfiguration(InputStream in) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(in);

        Element root = document.getDocumentElement();
        String rootName = root.getNodeName();
        if (!rootName.equals("configuration")) {
            throw new ParserConfigurationException("Missing configuration element");
        }

        return getConfiguration(root);
    }

    private Configuration getConfiguration(Element configurationNode) throws Exception {
        Map<String, TypeAlias> typeAliases = new HashMap<>();
        Properties properties = new Properties();
        Map<String, Environment> environments = new HashMap<>();
        Environment defaultEnvironment = null;

        NodeList nodeList = configurationNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String nodeName = node.getNodeName();
            switch (nodeName) {
                case "typeAliases" -> typeAliases = getTypeAliases(node);
                case "properties" -> properties = getProperties(node);
                case "environments" -> defaultEnvironment = setEnvironments(node, environments);
                case "mappers" -> setMapperNames(node);
                default -> throw new ParserConfigurationException(ILLEGAL_ELEMENT + nodeName);
            }
        }

        return new Configuration(environments, defaultEnvironment, properties, typeAliases);
    }

    private void setMapperNames(Node root) throws ParserConfigurationException {
        this.mapperNames = new ArrayList<>();

        NodeList nodeList = root.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String nodeName = node.getNodeName();
            if (!nodeName.equals("mapper")) {
                throw new ParserConfigurationException(ILLEGAL_ELEMENT + nodeName);
            }

            String resource = getAttributeValue(node, "resource");

            this.mapperNames.add(resource);
        }
    }

    private Properties getProperties(Node propertiesNode) throws Exception {
        Node resourceAttr = propertiesNode.getAttributes().getNamedItem("resource");
        if (resourceAttr != null) {
            return getPropertiesFromFile(resourceAttr);
        }
        return this.parseProperties(propertiesNode);
    }

    private Properties getPropertiesFromFile(Node resourceAttr) throws IOException, ParserConfigurationException {
        String resource = resourceAttr.getNodeValue();
        if (resource.equals("")) {
            throw new ParserConfigurationException("Resource can not be empty");
        }

        Properties properties = new Properties();
        try (Reader reader = new FileReader(resource)) {
            properties.load(reader);
        }

        this.properties = properties;
        return properties;
    }

    private Environment setEnvironments(Node environmentsNode, Map<String, Environment> environments) throws Exception {
        String defaultEnvironmentStr = getAttributeValue(environmentsNode, "default");

        NodeList nodeList = environmentsNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String nodeName = node.getNodeName();

            if (!nodeName.equals("environment")) {
                throw new ParserConfigurationException(ILLEGAL_ELEMENT + nodeName);
            }

            Environment environment = getEnvironment(node);
            environments.put(environment.id, environment);
        }

        return environments.get(defaultEnvironmentStr);
    }

    private Environment getEnvironment(Node environmentNode) throws Exception {
        String id = getAttributeValue(environmentNode, "id");
        String transactionManagerType = null;
        DataSource dataSource = null;

        NodeList nodeList = environmentNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String nodeName = node.getNodeName();
            switch (nodeName) {
                case "transactionManager" -> transactionManagerType = getAttributeValue(node, "type");
                case "dataSource" -> dataSource = getDataSource(node);
                default -> throw new ParserConfigurationException(ILLEGAL_ELEMENT + nodeName);
            }
        }

        return new Environment(id, transactionManagerType, dataSource);
    }

    private String getAttributeValue(Node node, String attributeStr) throws ParserConfigurationException {
        Node attribute = node.getAttributes().getNamedItem(attributeStr);
        if (attribute == null) {
            throw new ParserConfigurationException("Missing " + attributeStr + " attribute");
        }

        String attributeValue = attribute.getNodeValue();
        if (attributeValue.equals("")) {
            throw new ParserConfigurationException(attributeStr + " can not be empty");
        }

        return attributeValue;
    }

    private DataSource getDataSource(Node dataSourceNode) throws Exception {
        String type = getAttributeValue(dataSourceNode, "type");
        Properties properties = parseProperties(dataSourceNode);
        return new DataSource(type, properties);
    }

    private Properties parseProperties(Node root) throws ParserConfigurationException {
        Properties properties = new Properties();

        NodeList nodeList = root.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String nodeName = node.getNodeName();
            if (!nodeName.equals("property")) {
                throw new ParserConfigurationException(ILLEGAL_ELEMENT + nodeName);
            }

            addProperty(node, properties);
        }

        return properties;
    }

    private void addProperty(Node propertyNode, Properties properties) throws ParserConfigurationException {
        String name = null;
        String value = null;

        NamedNodeMap attributes = propertyNode.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String attributeName = attribute.getNodeName();
            switch (attributeName) {
                case "name" -> {
                    name = attribute.getNodeValue();
                    if (name.equals("")) {
                        throw new ParserConfigurationException("Name can not be empty");
                    }
                }
                case "value" -> {
                    value = attribute.getNodeValue();
                    if (value.equals("")) {
                        throw new ParserConfigurationException("Value can not be empty");
                    }
                }
                default -> throw new ParserConfigurationException(ILLEGAL_ATTRIBUTE + attributeName);
            }
        }

        Matcher matcher = PROPERTY_PATTERN.matcher(value);
        if (matcher.find()) {
            String property = matcher.group(1);
            Object p = this.properties.get(property);
            properties.put(name, p);
        }
    }

    private Map<String, TypeAlias> getTypeAliases(Node typeAliasesNode) throws Exception {
        Map<String, TypeAlias> typeAliases = new HashMap<>();

        NodeList nodeList = typeAliasesNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String nodeName = node.getNodeName();

            if (!nodeName.equals("typeAlias")) {
                throw new ParserConfigurationException(ILLEGAL_ELEMENT + nodeName);
            }

            TypeAlias typeAlias = getTypeAlias(node);
            typeAliases.put(typeAlias.alias, typeAlias);
        }

        return typeAliases;
    }

    private TypeAlias getTypeAlias(Node typeAliasNode) throws Exception {
        Class<?> type = null;
        String alias = null;

        NamedNodeMap attributes = typeAliasNode.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String attributeName = attribute.getNodeName();

            switch (attributeName) {
                case "type" -> {
                    String className = attribute.getNodeValue();
                    if (className.equals("")) {
                        throw new ParserConfigurationException(EMPTY_TYPE);
                    }
                    type = Class.forName(className);
                }
                case "alias" -> {
                    alias = attribute.getNodeValue();
                    if (alias.equals("")) {
                        throw new ParserConfigurationException("Alias can not be empty");
                    }
                }
                default -> throw new ParserConfigurationException(ILLEGAL_ATTRIBUTE + attributeName);
            }
        }

        return new TypeAlias(type, alias);
    }

    private void parseMappers() throws Exception {
        for (String mapperName : mapperNames) {
            Mapper mapper = parseMapper(mapperName);
            this.configuration.mappers.put(mapper.namespace, mapper);
        }
    }

    private Mapper parseMapper(String resource) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        File f = new File(resource);
        Document document = builder.parse(f);

        Element root = document.getDocumentElement();
        String rootName = root.getNodeName();
        if (!rootName.equals("mapper")) {
            throw new ParserConfigurationException("Missing mapper element");
        }

        return getMapper(root);
    }

    private Mapper getMapper(Node mapperNode) throws Exception {
        String className = getAttributeValue(mapperNode, "namespace");
        Class<?> namespace = Class.forName(className);
        Map<String, Query> queries = new HashMap<>();
        Map<String, ResultMap> resultMaps = new HashMap<>();

        NodeList nodeList = mapperNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String nodeName = node.getNodeName();
            switch (nodeName) {
                case "resultMap" -> {
                    ResultMap resultMap = getResultMap(node);
                    resultMaps.put(resultMap.id, resultMap);
                }
                case "select", "insert", "update", "delete" -> {
                    Query query = getQueryByType(node, nodeName);
                    queries.put(query.id, query);
                }

                default -> throw new ParserConfigurationException(ILLEGAL_ELEMENT + nodeName);
            }
        }

        return new Mapper(namespace, queries, resultMaps);
    }

    private Query getQueryByType(Node queryNode, String type) throws Exception {
        Query.QUERY_TYPE queryType = Query.QUERY_TYPE.valueOf(type.toUpperCase());

        return switch (queryType) {
            case INSERT -> getInsertQuery(queryNode);
            case SELECT -> getSelectQuery(queryNode);
            case DELETE, UPDATE -> getQuery(queryNode, queryType);
        };
    }

    private Query getQuery(Node queryNode, Query.QUERY_TYPE queryType) throws Exception {
        String id = null;
        Class<?> paramType = null;

        NamedNodeMap attributes = queryNode.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String attributeName = attribute.getNodeName();
            switch (attributeName) {
                case "id" -> {
                    id = attribute.getNodeValue();
                    if (id.equals("")) {
                        throw new ParserConfigurationException("Id can not be empty");
                    }
                }
                case "parameterType" -> {
                    String className = attribute.getNodeValue();
                    if (className.equals("")) {
                        throw new ParserConfigurationException(EMPTY_TYPE);
                    }
                    paramType = getClassByName(className);
                }
                default -> throw new ParserConfigurationException(ILLEGAL_ATTRIBUTE + attributeName);
            }
        }

        String sql = queryNode.getTextContent();

        return new Query(queryType, id, paramType, sql);
    }

    private Query getInsertQuery(Node queryNode) throws Exception {
        String id = null;
        Class<?> paramType = null;
        boolean useGeneratedKeys = false;
        String keyProperty = null;

        NamedNodeMap attributes = queryNode.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String attributeName = attribute.getNodeName();
            switch (attributeName) {
                case "id" -> {
                    id = attribute.getNodeValue();
                    if (id.equals("")) {
                        throw new ParserConfigurationException("Id can not be empty");
                    }
                }
                case "parameterType" -> {
                    String className = attribute.getNodeValue();
                    if (className.equals("")) {
                        throw new ParserConfigurationException(EMPTY_TYPE);
                    }
                    paramType = getClassByName(className);
                }
                case "useGeneratedKeys" -> {
                    String keysString = attribute.getNodeValue();
                    if (keysString.equals("")) {
                        throw new ParserConfigurationException("UseGeneratedKeys can not be empty");
                    }
                    useGeneratedKeys = Boolean.parseBoolean(keysString);
                }
                case "keyProperty" -> {
                    keyProperty = attribute.getNodeValue();
                    if (keyProperty.equals("")) {
                        throw new ParserConfigurationException("KeyProperty can not be empty");
                    }
                }
                default -> throw new ParserConfigurationException(ILLEGAL_ATTRIBUTE + attributeName);
            }
        }

        String sql = queryNode.getTextContent();

        return new InsertQuery(Query.QUERY_TYPE.INSERT, id, paramType, sql, useGeneratedKeys, keyProperty);
    }

    private Query getSelectQuery(Node queryNode) throws Exception {
        String id = null;
        Class<?> paramType = null;
        Class<?> resultType = null;
        String resultMapId = null;

        NamedNodeMap attributes = queryNode.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String attributeName = attribute.getNodeName();
            switch (attributeName) {
                case "id" -> {
                    id = attribute.getNodeValue();
                    if (id.equals("")) {
                        throw new ParserConfigurationException("Id can not be empty");
                    }
                }
                case "parameterType" -> {
                    String className = attribute.getNodeValue();
                    if (className.equals("")) {
                        throw new ParserConfigurationException(EMPTY_TYPE);
                    }
                    paramType = getClassByName(className);
                }
                case "resultType" -> {
                    String className = attribute.getNodeValue();
                    if (className.equals("")) {
                        throw new ParserConfigurationException(EMPTY_TYPE);
                    }
                    resultType = getClassByName(className);
                }

                case "resultMap" -> {
                    resultType = ResultMap.class;
                    resultMapId = attribute.getNodeValue();
                    if (resultMapId.equals("")) {
                        throw new ParserConfigurationException("Result map can  not be empty");
                    }
                }

                default -> throw new ParserConfigurationException(ILLEGAL_ATTRIBUTE + attributeName);
            }
        }

        String sql = queryNode.getTextContent();

        return new SelectQuery(Query.QUERY_TYPE.SELECT, id, paramType, sql, resultType, resultMapId);
    }

    private static final Map<String, Class<?>> PRIMITIVE_CLASSES = Map.of(
            "int", int.class,
            "float", float.class,
            "double", double.class,
            "byte", byte.class,
            "long", long.class,
            "short", short.class,
            "char", char.class,
            "boolean", boolean.class
    );

    private Class<?> getClassByName(String className) throws ClassNotFoundException {
        Class<?> cl = PRIMITIVE_CLASSES.get(className);
        if (cl != null) {
            return cl;
        }

        TypeAlias typeAlias = this.configuration.typeAliases.get(className);
        return typeAlias == null ? Class.forName(className) : typeAlias.type;
    }

    private ResultMap getResultMap(Node resultMapNode) throws Exception {
        String id = null;
        Class<?> type = null;

        NamedNodeMap attributes = resultMapNode.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String attributeName = attribute.getNodeName();
            switch (attributeName) {
                case "id" -> {
                    id = attribute.getNodeValue();
                    if (id.equals("")) {
                        throw new ParserConfigurationException("Id can not be empty");
                    }
                }
                case "type" -> {
                    String className = attribute.getNodeValue();
                    if (className.equals("")) {
                        throw new ParserConfigurationException();
                    }
                    type = getClassByName(className);
                }
                default -> throw new ParserConfigurationException(ILLEGAL_ATTRIBUTE + attributeName);
            }
        }

        Map<String, Result> results = new HashMap<>();
        Result resultId = fillResultMap(resultMapNode, results);

        return new ResultMap(id, type, results, resultId);
    }

    private Result fillResultMap(Node root, Map<String, Result> results) throws Exception {
        Result resultId = null;

        NodeList nodeList = root.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String nodeName = node.getNodeName();
            switch (nodeName) {
                case "id" -> resultId = getResult(node);
                case "result" -> {
                    Result result = getResult(node);
                    results.put(result.column, result);
                }
                default -> throw new ParserConfigurationException(ILLEGAL_ELEMENT + nodeName);
            }
        }

        return resultId;
    }

    private Result getResult(Node node) throws ParserConfigurationException {
        String property = null;
        String column = null;

        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String attributeName = attribute.getNodeName();

            switch (attributeName) {
                case "property" -> {
                    property = attribute.getNodeValue();
                    if (property.equals("")) {
                        throw new ParserConfigurationException();
                    }
                }
                case "column" -> {
                    column = attribute.getNodeValue();
                    if (column.equals("")) {
                        throw new ParserConfigurationException();
                    }
                }
                default -> throw new ParserConfigurationException(ILLEGAL_ATTRIBUTE + attributeName);
            }
        }

        return new Result(property, column);
    }

    public static void main(String[] args) throws Exception {
        String configRes = "D:\\IdeaProjects\\MyBatisDemo\\src\\resources\\mybatis-config.xml";
        ConfigurationParser configurationParser = new ConfigurationParser(configRes);
        Configuration configuration = configurationParser.getConfiguration();
        Map<Class<?>, Mapper> mappers = configuration.mappers;
        Mapper mapper = mappers.get(EmployeeMapper.class);
        Map<String, Query> queries = mapper.queries;
        for (Map.Entry<String, Query> q : queries.entrySet()) {
            System.out.println(q);
        }

        Environment defaultEnvironment = configuration.defaultEnvironment;
        Properties properties = defaultEnvironment.dataSource.properties;
        System.out.println(properties.toString());
    }
}