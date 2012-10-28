package org.applab.search.sflogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Class to access the configuration file. Parses XML file to HashMap and then provides access methods. Copyright (C)
 * 2012 Grameen Foundation
 */
public class Configuration {

    private static Configuration singletonValue;

    // Allows a different filepath to be used for a custom config
    private String filePath;

    // Tracker booelan to avoid parsing the config file multiple times
    private boolean isParsed = false;

    // Used to map single value configurations
    private HashMap<String, String> configurationMap;

    // Used to map multiple value configurations
    private HashMap<String, List<String>> configurationArrayMap;

    /**
     * Empty constructor as the work is done in the init proc
     */
    private Configuration() {
        filePath = "conf/configuration.xml";
    }

    /**
     * Create the singleton value and add default settings
     */
    public static Configuration getConfig() {
        if (singletonValue == null) {
            Configuration config = new Configuration();
            config.configurationMap = new HashMap<String, String>();
            config.configurationArrayMap = new HashMap<String, List<String>>();
            singletonValue = config;
        }
        return singletonValue;

    }

    /**
     * Change path to config file. Must be called before parseConfig to have any affect
     * 
     * @param filePath
     *            - File path to config file
     */
    public static void setFilePath(String filePath) {
        singletonValue.filePath = filePath;
    }

    /**
     * Initiate the parsing of the config file. Reads in file and normalises it
     */
    public void parseConfig() throws ParserConfigurationException, SAXException, IOException {

        if (!isParsed) {
            File xmlFile = new File(singletonValue.filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document xmlDocument = dBuilder.parse(xmlFile);
            xmlDocument.getDocumentElement().normalize();
            Element rootNode = xmlDocument.getDocumentElement();
            parseConfigXml(rootNode);
            isParsed = true;
        }
    }

    /**
     * Identifies each config item element
     */
    private static void parseConfigXml(Element rootElement) {

        for (Node childNode = rootElement.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                if (childNode.getNodeName().equals("configItem")) {
                    parseConfigItem((Element)childNode);
                }
            }
        }
    }

    /**
     * Parses each configItem element into its configName and configValue parts Adds config name value pair to pertinent
     * config map
     * 
     * @param configElement
     *            - The configItem element to be parsed
     */
    private static void parseConfigItem(Element configElement) {

        String configName = null;
        List<String> configValue = new ArrayList<String>();

        for (Node childNode = configElement.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                if (childNode.getNodeName().equals("configName")) {
                    configName = parseCharacterData((Element)childNode);
                }
                if (childNode.getNodeName().equals("configValue")) {
                    configValue.add(parseCharacterData((Element)childNode));
                }
            }
        }
        if (configName != null && configValue.size() == 1) {
            singletonValue.configurationMap.put(configName, configValue.get(0));
        }
        else if (configName != null && configValue.size() > 1) {
            singletonValue.configurationArrayMap.put(configName, configValue);
        }
    }

    private static String parseCharacterData(Element element) {
        Node child = element.getFirstChild();
        if (child instanceof CharacterData) {
            return ((CharacterData)child).getData();
        }
        return null;
    }

    /**
     * Retrieve a config value
     * 
     * @param name
     *            - The name of the config value required
     * @param defaultValue
     *            - value to be returned if config value is missing
     * 
     * @return - The config value matching the name or the default value if value is missing
     */
    public String getConfiguration(String name, String defaultValue) {
        if (singletonValue.configurationMap.containsKey(name)) {
            return singletonValue.configurationMap.get(name);
        }
        return defaultValue;
    }

    /**
     * Retrieve a config value as a list
     * 
     * @param name
     *            - The name of the config value required
     * 
     * @return - The config value matching the name or an empty list if it is missing
     */
    public List<String> getConfiguration(String name) {
        if (singletonValue.configurationArrayMap.containsKey(name)) {
            return singletonValue.configurationArrayMap.get(name);
        }
        String value = getConfiguration(name, "");
        List<String> values = new ArrayList<String>();
        if (!value.equalsIgnoreCase("")) {
            values.add(value);
        }
        return values;
    }
}
