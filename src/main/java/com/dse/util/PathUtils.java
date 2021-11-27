package com.dse.util;

import com.dse.config.AkaConfig;
import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.object.ProjectNode;

public class PathUtils {
    public static String toAbsolute(String relativePath) {
        String absolutePath = relativePath;

        int offset = 0;

        if (relativePath.startsWith("..")) {
            offset = 2;
        } else if (relativePath.startsWith(".")) {
            offset = 1;
        }

        if (offset > 0) {
//            ProjectNode root = Environment.getInstance().getProjectNode();
//            if (root != null) {
//                absolutePath = root.getFile().getParent() + relativePath.substring(offset);
//            } else {
                String workspace = new AkaConfig().fromJson().getOpeningWorkspaceDirectory();
                int index = workspace.indexOf("/aka-working-space");
                if (index < 0) absolutePath = relativePath;
                else {
                    String prefix = workspace.substring(0, index);
                    absolutePath = prefix + relativePath.substring(offset);
//                }
            }
        }

        return absolutePath;
    }

    public static String toRelative(String absolutePath) {
        String workspace = new AkaConfig().fromJson().getOpeningWorkspaceDirectory();
        int index = workspace.indexOf("/aka-working-space");

        if (index < 0) return absolutePath;

        String prefix = workspace.substring(0, index);
        return absolutePath.replaceFirst("\\Q" + prefix + "\\E", ".");
    }
}
