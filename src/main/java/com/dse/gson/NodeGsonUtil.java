package com.dse.gson;

import com.dse.parser.object.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import example.gson.polynomic.example1.RuntimeTypeAdapterFactory;

public class NodeGsonUtil {

    public static Gson create() {
        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();

        RuntimeTypeAdapterFactory<Node> nodeAdapterFactory = RuntimeTypeAdapterFactory.of(Node.class)
                .registerSubtype(FunctionNode.class)
                .registerSubtype(FolderNode.class)
                .registerSubtype(SourcecodeFileNode.class)
                .registerSubtype(HeaderNode.class)
                .registerSubtype(CppFileNode.class)
                .registerSubtype(CFileNode.class)
                .registerSubtype(NamespaceNode.class)
                .registerSubtype(ClassNode.class)
                .registerSubtype(StructNode.class)
                .registerSubtype(ProjectNode.class)
                .registerSubtype(InternalVariableNode.class)
                .registerSubtype(IncludeHeaderNode.class)
                .registerSubtype(ExternalVariableNode.class)
                .registerSubtype(ConstructorNode.class)
                .registerSubtype(DestructorNode.class)
                .registerSubtype(AttributeOfStructureVariableNode.class)
                .registerSubtype(DefinitionFunctionNode.class)
                .registerSubtype(VariableNode.class)
                .registerSubtype(UnknowObjectNode.class)
                .registerSubtype(PrimitiveTypedefDeclaration.class)
                .registerSubtype(EnumTypedefNode.class)
                .registerSubtype(SpecialEnumTypedefNode.class)
                .registerSubtype(UnionTypedefNode.class)
                .registerSubtype(UnionNode.class)
                .registerSubtype(SpecialUnionTypedefNode.class)
                ;

        Gson gson = builder.registerTypeAdapterFactory(nodeAdapterFactory).setPrettyPrinting().create();
        return gson;
    }
}
