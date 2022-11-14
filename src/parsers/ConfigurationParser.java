package parsers;

import dto.*;
import dto.data_source.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class ConfigurationParser {
    private static final String ILLEGAL_ATTRIBUTE = "Illegal attribute: ";
    private static final String ILLEGAL_ELEMENT = "Illegal element: ";
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("[#$]\\{([\\w\\d\\.]+)\\}");

    private final Configuration configuration;
    private Properties properties;
    private final Environment environment;
    private List<String> mapperNames;
    private Set<Class<?>> classMappers;


    public ConfigurationParser(InputStream in) throws Exception {
        this(new InputSource(in), null, null);
    }

    public ConfigurationParser(InputStream in, Environment environment) throws Exception {
        this(new InputSource(in), environment, null);
    }

    public ConfigurationParser(InputStream in, Environment environment, Properties properties) throws Exception {
        this(new InputSource(in), environment, properties);
    }

    public ConfigurationParser(Reader reader) throws Exception {
        this(new InputSource(reader), null, null);
    }

    public ConfigurationParser(Reader reader, Environment environment) throws Exception {
        this(new InputSource(reader), environment, null);
    }

    public ConfigurationParser(Reader reader, Environment environment, Properties properties) throws Exception {
        this(new InputSource(reader), environment, properties);
    }

    private ConfigurationParser(InputSource source, Environment environment, Properties properties) throws Exception {
        this.environment = environment;
        this.properties = properties;
        this.configuration = parseConfiguration(source);
        this.configuration.setMappers(new HashMap<>());
        parseMappers();
        this.configuration.setClassMappers(this.classMappers);
        this.configuration.setQueriesWithNamespace();
    }

    private Configuration parseConfiguration(InputSource source) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(source);

        Element root = document.getDocumentElement();
        String rootName = root.getNodeName();
        if (!rootName.equals("configuration")) {
            throw new ParserConfigurationException("Missing configuration element");
        }

        return getConfiguration(root);
    }

    private Configuration getConfiguration(Element configurationNode) throws Exception {
        Map<String, TypeAlias> typeAliases = new HashMap<>();
        Properties prop = new Properties();
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
                case "properties" -> prop = getProperties(node);
                case "environments" -> defaultEnvironment = setEnvironments(node, environments);
                case "mappers" -> setMapperNames(node);
                default -> throw new ParserConfigurationException(ILLEGAL_ELEMENT + nodeName);
            }
        }

        return new Configuration(environments, defaultEnvironment, prop, typeAliases);
    }

    private void setMapperNames(Node root) throws ParserConfigurationException, ClassNotFoundException {
        this.mapperNames = new ArrayList<>();
        this.classMappers = new HashSet<>();

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

            Node attribute = node.getAttributes().item(0);
            String attrName = attribute.getNodeName();
            switch (attrName) {
                case "resource" -> {
                    String resource = getAttribute(attribute, "resource");
                    this.mapperNames.add(resource);
                }
                case "class" -> {
                    String className = attribute.getNodeValue();
                    Class<?> mapperClass = Class.forName(className);
                    this.classMappers.add(mapperClass);
                }
                default -> throw new ParserConfigurationException(ILLEGAL_ATTRIBUTE + attrName);
            }
        }
    }

    private Properties getProperties(Node propertiesNode) throws Exception {
        if (this.properties != null) {
            return this.properties;
        }

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

        Properties prop = new Properties();
        try (Reader reader = new FileReader(resource)) {
            prop.load(reader);
        }

        this.properties = prop;
        return prop;
    }

    private Environment setEnvironments(Node environmentsNode, Map<String, Environment> environments) throws Exception {
        String defaultEnvironmentStr = this.environment != null ?
                this.environment.getId() :
                getAttributeValue(environmentsNode, "default");

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

            Environment env = getEnvironment(node);
            environments.put(env.getId(), env);
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

    private DataSource getDataSource(Node dataSourceNode) throws Exception {
        String type = getAttributeValue(dataSourceNode, "type");
        Properties prop = parseProperties(dataSourceNode);
        return switch (type) {
            case "POOLED" -> new PooledDataSource(prop);
            case "UNPOOLED" -> new UnpooledDataSource(prop);
            default -> throw new Exception("Invalid type");
        };
    }

    private Properties parseProperties(Node root) throws ParserConfigurationException {
        Properties prop = new Properties();

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

            addProperty(node, prop);
        }

        return prop;
    }

    private void addProperty(Node propertyNode, Properties properties) throws ParserConfigurationException {
        String name = null;
        String value = null;

        NamedNodeMap attributes = propertyNode.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String attributeName = attribute.getNodeName();
            switch (attributeName) {
                case "name" -> name = getAttribute(attribute, "name");
                case "value" -> value = getAttribute(attribute, "value");
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
            typeAliases.put(typeAlias.getAlias(), typeAlias);
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
                    String className = getAttribute(attribute, "type");
                    type = Class.forName(className);
                }
                case "alias" -> alias = getAttribute(attribute, "alias");
                default -> throw new ParserConfigurationException(ILLEGAL_ATTRIBUTE + attributeName);
            }
        }

        return new TypeAlias(type, alias);
    }

    private void parseMappers() throws Exception {
        for (String mapperName : mapperNames) {
            Mapper mapper = MapperParser.parseMapper(this.configuration, mapperName);
            this.configuration.getMappers().put(mapper.getNamespace(), mapper);
        }
    }

    private String getAttributeValue(Node node, String attributeStr) throws ParserConfigurationException {
        Node attribute = node.getAttributes().getNamedItem(attributeStr);
        if (attribute == null) {
            throw new ParserConfigurationException("Missing " + attributeStr + " attribute");
        }
        return getAttribute(attribute, attributeStr);
    }

    private String getAttribute(Node attribute, String attrName) throws ParserConfigurationException {
        String attrValue = attribute.getNodeValue();
        if (attrValue.equals("")) {
            throw new ParserConfigurationException(attrName + " can not be empty");
        }
        return attrValue;
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}