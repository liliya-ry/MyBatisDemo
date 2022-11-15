package parsers;

import dto.*;
import dto.queries.*;
import org.w3c.dom.*;
import utility.FifoCache;
import utility.GenerationalCache;

import javax.xml.parsers.*;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.*;

class MapperParser {
    private static final String ILLEGAL_ATTRIBUTE = "Illegal attribute: ";
    private static final String ILLEGAL_ELEMENT = "Illegal element: ";
    private static Configuration configuration;

    static Mapper parseMapper(Configuration config, String resource) throws Exception {
        configuration = config;
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

    private static Mapper getMapper(Node mapperNode) throws Exception {
        String namespace = getAttributeValue(mapperNode, "namespace");
        Map<String, Query> queries = new HashMap<>();
        Map<String, ResultMap> resultMaps = new HashMap<>();
        int cacheIndex = -1;

        NodeList nodeList = mapperNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String nodeName = node.getNodeName();
            switch (nodeName) {
                case "cache" -> {cacheIndex = i;}
                case "resultMap" -> {
                    ResultMap resultMap = getResultMap(node);
                    resultMaps.put(resultMap.getId(), resultMap);
                }
                case "select", "insert", "update", "delete" -> {
                    Query query = getQueryByType(node, nodeName);
                    queries.put(query.getId(), query);
                }

                default -> throw new ParserConfigurationException(ILLEGAL_ELEMENT + nodeName);
            }
        }

        Node cacheNode = nodeList.item(cacheIndex);
        Map<String, utility.Cache<Object, Object>> caches = parseCaches(cacheNode, queries);

        return new Mapper(namespace, queries, resultMaps, caches);
    }

    private static Map<String, utility.Cache<Object, Object>> parseCaches(Node cacheNode, Map<String, Query> queries) throws Exception {
        Properties properties = new Properties();

        NodeList nodeList = cacheNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String nodeName = node.getNodeName();
            switch (nodeName) {
                case "property" -> setProperty(properties, node);
                default -> throw new ParserConfigurationException(ILLEGAL_ELEMENT + node);
            }
        }

        return createCaches(properties, queries);
    }

    private static void setProperty(Properties properties, Node node) {
        NamedNodeMap attributes = node.getAttributes();
        Node nameNode = attributes.getNamedItem("name");
        String name = nameNode.getNodeValue();
        Node valueNode = attributes.getNamedItem("value");
        String value = valueNode.getNodeValue();
        properties.setProperty(name, value);
    }

    private static Map<String, utility.Cache<Object, Object>> createCaches(Properties properties, Map<String, Query> queries) throws Exception {
        String eviction = properties.getProperty("memoryStoreEvictionPolicy");
        String sizeStr = properties.getProperty("maxEntriesLocalHeap");
        int size = Integer.parseInt(sizeStr);
        String flushIntervalStr = properties.getProperty("timeToLiveSeconds");
        long flushInterval = Long.parseLong(flushIntervalStr);

        Class<?> cacheType = switch (eviction) {
            case "FIFO" -> FifoCache.class;
            case "LRU" -> GenerationalCache.class;
            default -> throw new Exception("Illegal cache type");
        };

        Map<String, utility.Cache<Object, Object>> caches = new HashMap<>();
        Constructor<?> cacheConstructor = cacheType.getDeclaredConstructor(int.class, long.class);

        for (Map.Entry<String, Query> queryEntry : queries.entrySet()) {
            Query query = queryEntry.getValue();
            Query.QUERY_TYPE queryType = query.getQueryType();
            if (!queryType.equals(Query.QUERY_TYPE.SELECT)) {
                continue;
            }

            SelectQuery selectQuery = (SelectQuery) query;
            boolean useCache = selectQuery.isUseCaching();
            if (useCache) {
                utility.Cache<Object, Object> cache = (utility.Cache<Object, Object>) cacheConstructor.newInstance(size, flushInterval);
                caches.put(query.getId(), cache);
            }
        }

        return caches;
    }

    private static Query getQueryByType(Node queryNode, String type) throws Exception {
        Query.QUERY_TYPE queryType = Query.QUERY_TYPE.valueOf(type.toUpperCase());

        return switch (queryType) {
            case INSERT, UPDATE -> getInsertUpdateQuery(queryNode);
            case SELECT -> getSelectQuery(queryNode);
            case DELETE -> getQuery(queryNode, queryType);
        };
    }

    private static Query getQuery(Node queryNode, Query.QUERY_TYPE queryType) throws Exception {
        String id = null;
        Class<?> paramType = null;
        boolean flushCache = false;

        NamedNodeMap attributes = queryNode.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String attributeName = attribute.getNodeName();
            switch (attributeName) {
                case "id" -> id = getAttribute(attribute, "id");
                case "parameterType" -> paramType = getClassByAttribute(attribute, "parameterType");
                case "flushCache" -> {
                    String flush = getAttribute(attribute, "flushCache");
                    flushCache = Boolean.parseBoolean(flush);
                }
                default -> throw new ParserConfigurationException(ILLEGAL_ATTRIBUTE + attributeName);
            }
        }

        String sql = queryNode.getTextContent();

        return new DeleteQuery(queryType, id, paramType, sql, flushCache);
    }

    private static Query getInsertUpdateQuery(Node queryNode) throws Exception {
        String id = null;
        Class<?> paramType = null;
        boolean useGeneratedKeys = false;
        String keyProperty = null;
        boolean flushCache = false;

        NamedNodeMap attributes = queryNode.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String attributeName = attribute.getNodeName();
            switch (attributeName) {
                case "id" -> id = getAttribute(attribute, "id");
                case "parameterType" -> paramType = getClassByAttribute(attribute, "parameterType");
                case "useGeneratedKeys" -> {
                    String keysString = getAttribute(attribute, "useGeneratedKeys");
                    useGeneratedKeys = Boolean.parseBoolean(keysString);
                }
                case "keyProperty" -> keyProperty = getAttribute(attribute, "keyProperty");
                case "flushCache" -> {
                    String flush = getAttribute(attribute, "flushCache");
                    flushCache = Boolean.parseBoolean(flush);
                }
                default -> throw new ParserConfigurationException(ILLEGAL_ATTRIBUTE + attributeName);
            }
        }

        String sql = queryNode.getTextContent();

        return new InsertQuery(Query.QUERY_TYPE.INSERT, id, paramType, sql, useGeneratedKeys, keyProperty, flushCache);
    }

    private static Query getSelectQuery(Node queryNode) throws Exception {
        String id = null;
        Class<?> paramType = null;
        Class<?> resultType = null;
        String resultMapId = null;
        boolean useCaching = false;

        NamedNodeMap attributes = queryNode.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String attributeName = attribute.getNodeName();
            switch (attributeName) {
                case "id" -> id = getAttribute(attribute, "id");
                case "parameterType" -> paramType = getClassByAttribute(attribute, "parameterType");
                case "resultType" -> resultType = getClassByAttribute(attribute, "resultType");
                case "resultMap" -> {
                    resultType = ResultMap.class;
                    resultMapId = getAttribute(attribute, "resultMap");
                }
                case "useCaching" -> {
                    String caching = getAttribute(attribute, "useCaching");
                    useCaching = Boolean.parseBoolean(caching);
                }
                default -> throw new ParserConfigurationException(ILLEGAL_ATTRIBUTE + attributeName);
            }
        }

        String sql = queryNode.getTextContent();

        return new SelectQuery(Query.QUERY_TYPE.SELECT, id, paramType, sql, resultType, resultMapId, useCaching);
    }

    private static final Map<String, Class<?>> WRAPPER_CLASSES = Map.of(
            "int", Integer.class,
            "float", Float.class,
            "double", Double.class,
            "byte", Byte.class,
            "long", Long.class,
            "short", Short.class,
            "char", Character.class,
            "boolean", Boolean.class
    );

    private static Class<?> getClassByName(String className) throws ClassNotFoundException {
        Class<?> cl = WRAPPER_CLASSES.get(className);
        if (cl != null) {
            return cl;
        }

        TypeAlias typeAlias = configuration.getTypeAliases().get(className);
        return typeAlias == null ? Class.forName(className) : typeAlias.getType();
    }

    private static ResultMap getResultMap(Node resultMapNode) throws Exception {
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

    private static Result fillResultMap(Node root, Map<String, Result> results) throws Exception {
        Result resultId = null;

        NodeList nodeList = root.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String nodeName = node.getNodeName();
            switch (nodeName) {
                case "id" -> {
                    resultId = getResult(node);
                    results.put(resultId.getColumn(), resultId);
                }
                case "result" -> {
                    Result result = getResult(node);
                    results.put(result.getColumn(), result);
                }
                default -> throw new ParserConfigurationException(ILLEGAL_ELEMENT + nodeName);
            }
        }

        return resultId;
    }

    private static Result getResult(Node node) throws ParserConfigurationException {
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

    private static String getAttributeValue(Node node, String attributeStr) throws ParserConfigurationException {
        Node attribute = node.getAttributes().getNamedItem(attributeStr);
        if (attribute == null) {
            throw new ParserConfigurationException("Missing " + attributeStr + " attribute");
        }
        return getAttribute(attribute, attributeStr);
    }

    private static String getAttribute(Node attribute, String attrName) throws ParserConfigurationException {
        String attrValue = attribute.getNodeValue();
        if (attrValue.equals("")) {
            throw new ParserConfigurationException(attrName + " can not be empty");
        }
        return attrValue;
    }

    private static Class<?> getClassByAttribute(Node attribute, String attrName) throws ParserConfigurationException, ClassNotFoundException {
        String className = getAttribute(attribute, attrName);
        return getClassByName(className);
    }
}