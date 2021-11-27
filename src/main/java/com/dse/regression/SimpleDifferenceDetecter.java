package com.dse.regression;

import com.dse.config.WorkspaceConfig;
import com.dse.environment.SourcecodeFileTreeExporterv2;
import com.dse.environment.SourcecodeTreeImporterv2;
import com.dse.parser.object.*;
import com.dse.search.Search;
import com.dse.search.condition.NodeCondition;
import com.dse.util.AkaLogger;
import com.dse.util.Utils;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * When users open an environment, then make modification on the original source code.
 * <p>
 * <p>
 * Given a tree of nodes corresponding to the latest version + map of nodes of the previous version, this module detects these changes.
 */
public class SimpleDifferenceDetecter extends AbstractDifferenceDetecter {
    final static AkaLogger logger = AkaLogger.get(SimpleDifferenceDetecter.class);

    public void diff(INode modifiedSrcNode, String elementFolderOfOldversion) {
        logger.debug("Finding difference of " + modifiedSrcNode.getAbsolutePath());
        if (modifiedSrcNode instanceof SourcecodeFileNode)
            try {
                String relativePath = modifiedSrcNode.getAbsolutePath().replace(Utils.getRoot(modifiedSrcNode).getAbsolutePath(), "");
                String elementFile = elementFolderOfOldversion + relativePath + WorkspaceConfig.AKA_EXTENSION;

                if (new File(elementFile).exists()) {
                    SourcecodeTreeImporterv2 importerv2 = new SourcecodeTreeImporterv2();
                    Map<String, String> oldElements = importerv2.load(new File(elementFile));

                    // Analyze changed source code file again
                    List<INode> allNodesInLatestVersion = Search.searchNodes(modifiedSrcNode, new NodeCondition());

                    findAddedNodes(allNodesInLatestVersion, oldElements);
                    findDeletedNodes(oldElements, allNodesInLatestVersion);
                    findModifiedNodes(oldElements, allNodesInLatestVersion);
                }
            } catch (Exception e) {
                logger.debug("Can not parse " + modifiedSrcNode.getAbsolutePath());
                e.printStackTrace();
            }
    }

    private void findAddedNodes(List<INode> allNodesInLatestVersion,  Map<String, String> oldElements) {
        // find added nodes
        for (INode latestVersion : allNodesInLatestVersion) {
            if (!oldElements.containsKey(latestVersion.getAbsolutePath())) {
                if (addedNodes instanceof IVariableNode && ((IVariableNode) addedNodes).getParent() instanceof AbstractFunctionNode) {
                    // ignore
                    // since we add a new function, we do not need to add its parameters
                } else {
                    addedNodes.add(latestVersion);
                    logger.debug("Found added nodes: " + latestVersion.getAbsolutePath());
                }
            }
        }
    }

    private void findDeletedNodes(Map<String, String> oldElements, List<INode> allNodesInLatestVersion) {
        for (String oldNodePath : oldElements.keySet())
            if (!oldNodePath.equals(SourcecodeFileTreeExporterv2.NAME_SOURCE_COLDE_FILE_TAG)) {
            boolean foundInNewTree = false;
            for (INode latestNode : allNodesInLatestVersion)
                if (latestNode.getAbsolutePath().equals(oldNodePath)){
                    foundInNewTree = true;
                    break;
                }

            if (!foundInNewTree)
                if (!deletedPaths.contains(oldNodePath)) {
                    deletedPaths.add(oldNodePath);
                    logger.debug("Found deleted path: " + oldNodePath);
                }
        }
    }

    private void findModifiedNodes(Map<String, String> oldElements, List<INode> allNodesInLatestVersion) {
        for (INode latestNode : allNodesInLatestVersion)
            // ignore definition function node because we do not save the checksum of this node
            if (!(latestNode instanceof DefinitionFunctionNode)) {
           if (oldElements.containsKey(latestNode.getAbsolutePath())){
               if (latestNode instanceof CustomASTNode && ((CustomASTNode) latestNode).getAST()!=null) {
                   String oldMd5 = oldElements.get(latestNode.getAbsolutePath());
                   String newContent = ((CustomASTNode) latestNode).getAST().getRawSignature();
                   String newMd5 = Utils.computeMd5(newContent);
                   if (!oldMd5.equals(newMd5)) {
                       modifiedNodes.add(latestNode);
                       logger.debug("Found modified node: " + latestNode.getAbsolutePath() + "\nold check sum = " + oldMd5 + "\nnew checksum = " + newMd5 + "\nnew content = [" + newContent + "]");
                   }
               }
           }
       }
    }


}
