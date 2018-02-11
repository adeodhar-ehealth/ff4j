package org.ff4j.inmemory.parser.yml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Evaluate item in Yaml Tree.
 *
 * @author Cedrick LUNVEN (@clunven)
 */
public class YamlLocator implements Serializable {
    
    /** serial. */
    private static final long serialVersionUID = -3393230785214811086L;
    
    /** Adress of current node in the tree. */
    private List < String > tagPath = new ArrayList<>();
    
    /** Keep tracking adress in tree for List, tagPath : Index of List. */
    private Map < String, Integer > tagPathListIndexes = new TreeMap<>();
   
    /** Default constructor. */
    public YamlLocator() {}
    
    /**
     * Browse the Yaml tree and locate where to put value.
     * 
     * @param ymlLine
     *      current yaml Line
     * @param yamlTree
     *      current yaml tree (result)
     * @return
     *      target leaft
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public Object locate(YamlLine ymlLine, Map < String, Object > yamlTree) {
        
        if (ymlLine.getRawLine().trim().equals("- order: 2")) {
            System.out.println("OK");
            goUpInTree();
        }
        
        log("INDEXES=" + tagPathListIndexes);
        log("PATH="    + tagPath);
        
        // If current line has identation less than before go up in graph
        moveCursorBackInTree(ymlLine);
        
        // if top level tag, return tree
        if (tagPath.isEmpty()) return yamlTree;
        
        // Iterate level by level in the tree recursively (path in depth)
        Map < String, Object > cursorInTree = yamlTree;
        for(int treeLevel = 0;treeLevel < tagPath.size();treeLevel++) {
            
            // Navigate the tree
            
            Object currentTreeNode = cursorInTree.get(tagPath.get(treeLevel));
            log(treeLevel + ") Processing =" +  currentTreeNode);
            
            // Embedded object
            if (currentTreeNode instanceof Map) {
                cursorInTree = (Map<String, Object>) currentTreeNode;
            
            // Embedded List
            } else if (currentTreeNode instanceof List) {
                
                List myList = (List) currentTreeNode;
                
                // NON JE NE SUIS PAS AU BON PATH
                if (ymlLine.getLevel() == tagPath.size()) {
//                    log("Reaching max depth " + currentTreeNode);
//                    String adressTmp = buildAdressFromPath(getTagPath());
//                    log("Reaching max depth " + adressTmp + " tagPathListIndexes:" + tagPathListIndexes);
//                    if (tagPathListIndexes.containsKey(adressTmp)) {
//                        return myList.get(tagPathListIndexes.get(adressTmp));
//                    }
                    return myList;
                    
                // We need to fetch correct item in list and return the inner MAP , this is no of list
                } else {
                    Object subLevelElement = navigateSubLevel(ymlLine, yamlTree, myList);
                    if (subLevelElement instanceof Map) {
                        cursorInTree = (Map<String, Object>) subLevelElement;
                    } else {
                        // No sublevel returning current list to add something
                        return subLevelElement;
                    }
                }
            }
        }
        log("Returning " + cursorInTree);
        return cursorInTree;
    }
    
    private Object navigateSubLevel(YamlLine ymlLine, Map < String, Object > yamlTree, List myList) {
        String currentAdress = buildCurrentAddress();
        log("Picking element from list " + myList + " and adress " + currentAdress);
        
        Map < String, Object > currentCursorInTree = yamlTree;
        for (int level =  0;level < tagPath.size(); level++) {
            String nodeXTagName = getTagPath().get(level);
            Object nodeX = currentCursorInTree.get(nodeXTagName);
            log(level + ") name '" + nodeXTagName + "' value:" + nodeX);
            
            if (nodeX instanceof Map) {
                // C'est une Map on descend simple
                currentCursorInTree = ((Map <String, Object>) nodeX);
                
            } else if (nodeX instanceof List) {
                
                // On retourne la liste sauf si :
                // - Il existe encore des elements dans le breadbrumb
                // - Il existe un idem dans les éléments
                
                
                List nodeXList = (List) nodeX;
                String adressTmp = buildAdressFromPath(getTagPath(level));
                log(level + ") Value is a list picking correct index from address '" + adressTmp + "'");
                if (nodeXList.isEmpty()) return nodeXList;
                
                // Nous sommes sur le dernier element et une liste
                if ((level == tagPath.size()-1) && ymlLine.isListElement()) {
                    return nodeXList;
                }
                
                // Tant que l'on est pas sur le dernier
                if (tagPathListIndexes.containsKey(adressTmp)) {
                    Integer indexOfCurrentNode = tagPathListIndexes.get(adressTmp);
                    if (indexOfCurrentNode < nodeXList.size()) {
                        Object  nextNode = nodeXList.get(indexOfCurrentNode);
                        log(level + ") Index '" + indexOfCurrentNode + "'" + " is " + nextNode);
                        
                        // Il s'agit bien d'une map on descend d'un niveau
                        if (nextNode instanceof Map) {
                            currentCursorInTree = (Map<String, Object>) nextNode;
                        } else {
                            return nodeXList;
                        }
                    } else {
                        return currentCursorInTree;
                    }
                } else {
                    return nodeXList;
                }
            }
        }
        return currentCursorInTree;
    }
    
    public void moveCursorDeeperInTree(YamlLine ymlLine) {
        getTagPath().add(ymlLine.getTagName());
    }
    
    /***
     *  Yaml line can have  identation lower than 'current Depth'. (tagPath.size()).
     *  This means we have to go up now. 
     **/
    public void moveCursorBackInTree(YamlLine ymlLine) {
        while (ymlLine.getLevel() < tagPath.size()) { 
            goUpInTree();
        }
    }
    
    /**
     * Increment current list.
     * 
     * @param offSet
     *      increment current list
     */
    public void updateCurrentListOffset(int offSet) {
        log(tagPathListIndexes.toString());
        tagPathListIndexes.put(buildCurrentAddress(), offSet);
        log(tagPathListIndexes.toString());
    }
    
    /** Move to upper side.  */
    private void goUpInTree() {
        tagPath.remove(tagPath.size()-1);
    }
    
    /**
     * Logging what's going ON
     * @param msg
     */
    private void log(String msg) {
        System.out.println("[LOCATE]:" + msg);
    }

    /**
     * Getter accessor for attribute 'tagPath'.
     *
     * @return
     *       current value of 'tagPath'
     */
    private List<String> getTagPath() {
        return tagPath;
    }
    
    /**
     * Getter accessor for attribute 'tagPath'.
     *
     * @return
     *       current value of 'tagPath'
     */
    private List<String> getTagPath(int level) {
        return tagPath.subList(0, level+1);
    }
    
    /**
     * Concatenate all tagElementas a list with / separator
     * 
     * @param tags
     *      current tags address
     * @return
     *      a generated adress
     */
    private String buildAdressFromPath(List < String > tags) {
        return String.join("/", tags);
    }
    
    /**
     * Build current Adress.
     *
     * @return
     *      current address
     */
    private String buildCurrentAddress() {
        return buildAdressFromPath(tagPath);
    }
}
