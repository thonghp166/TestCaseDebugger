package com.dse.util.ast_management;

import com.dse.parser.object.INode;
import com.dse.parser.object.ProjectNode;
import com.dse.util.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class ASTManager {
	private static ASTManager instance = null;
    private List<INode> nodes = new ArrayList<>();
    private INode projectNode = null;

    private ASTManager() {}

    public static ASTManager getInstance() {
    	if(instance == null) {
    		instance = new ASTManager();
		}
    	return instance;
	}

    public INode loadASTFromJson(String path) {
		return doLoadASTFromJson(Utils.readFileContent(path));
    }

    public INode loadASTFromJson(File file) {
		return loadASTFromJson(file.getAbsolutePath());
	}

	private INode doLoadASTFromJson(String jsonContent) {
		Gson gson = new GsonBuilder()
				.excludeFieldsWithoutExposeAnnotation()
				.registerTypeAdapter(INode.class, new InterfaceAdapter<INode>())
				.setPrettyPrinting()
				.create();
		INode fromJson = gson.fromJson(jsonContent, INode.class);
		setParent(fromJson);

		// set nodes, projectNode
		projectNode = fromJson;
		nodes.clear();
		appendToNodes(projectNode);

		return fromJson;
	}

    public boolean saveASTToJson(String path) {
    	Gson gson = new GsonBuilder()
				.excludeFieldsWithoutExposeAnnotation()
				.registerTypeAdapter(INode.class, new InterfaceAdapter<INode>())
				.setPrettyPrinting()
				.create();

    	try {
    		gson.toJson(projectNode, new FileWriter(path));
    		return true;
		} catch (Exception e) {
    		 e.printStackTrace();
    		return false;
		}
	}

    private void appendToNodes(INode node) {
    	nodes.add(node);
    	for(INode child: node.getChildren()) {
    		appendToNodes(child);
		}
	}

	private void setParent(INode node) {
    	for (INode child: node.getChildren()) {
    		child.setParent(node);
    		setParent(child);
		}
	}
    public ProjectNode getProjectNode() {
    	return (ProjectNode) projectNode;
	}

	public INode getNode(int nodeId) {
		for (INode node: nodes) {
			if (node.getId() == nodeId) return node;
		}
		return null; // the Node is not found;
	}

}
