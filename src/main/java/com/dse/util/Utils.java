package com.dse.util;

import auto_testcase_generation.cfg.CFGGenerationforBranchvsStatementvsBasispathCoverage;
import auto_testcase_generation.cfg.CFGGenerationforSubConditionCoverage;
import auto_testcase_generation.cfg.ICFG;
import auto_testcase_generation.testdatagen.se.ExpressionRewriterUtils;
import auto_testcase_generation.testdatagen.se.memory.IVariableNodeTable;
import com.dse.config.IProjectType;
import com.dse.config.WorkspaceConfig;
import com.dse.environment.object.EnviroCoverageTypeNode;
import com.dse.exception.OpenFileException;
import com.dse.guifx_v3.helps.UIController;
import com.dse.parser.ProjectLoader;
import com.dse.parser.SourcecodeFileParser;
import com.dse.parser.dependency.Dependency;
import com.dse.parser.object.*;
import com.dse.search.Search;
import com.dse.search.condition.FunctionNodeCondition;
import com.dse.search.condition.SourcecodeFileNodeCondition;
import com.dse.testdata.object.*;
import com.dse.testdata.object.stl.ListBaseDataNode;
import org.apache.commons.io.FileDeleteStrategy;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.*;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.*;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;
import org.reflections.Reflections;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils implements IRegex {
	/**
	 *
	 */
	public static final int UNDEFINED_TO_INT = -9999;
	public static final float UNDEFINED_TO_DOUBLE = -9999;
	final static AkaLogger logger = AkaLogger.get(Utils.class);
	public static boolean containFunction = false;
	static boolean containBlock = false;
	/**
	 *
	 */
	public static IASTNode output = null;

	public static int getTestDriverFunctionCallLine(String testCaseName, String testDriver) {
		String tag = String.format("AKA_MARK(\"<<PRE-CALLING>> Test %s\");", testCaseName);

		int line = 0;

		int startPos = testDriver.indexOf(tag);

		if (startPos > 0) {
			line = (int) testDriver
					.substring(0, startPos)
					.chars()
					.filter(c -> c == '\n')
					.count();
		}

		return line;
	}

	public static int getTestDriverFunctionCallLine(String testCaseName, File testDriverFile) {
		String testDriver = Utils.readFileContent(testDriverFile);

		String tag = String.format("AKA_MARK(\"<<PRE-CALLING>> Test %s\");", testCaseName);

		int line = 0;

		int startPos = testDriver.indexOf(tag);

		if (startPos > 0) {
			line = (int) testDriver
					.substring(0, startPos)
					.chars()
					.filter(c -> c == '\n')
					.count();
		}

		return line;
	}

	/**
	 * Add quote to the content (e.g., abs--->"abc")
	 *
	 * @param content
	 * @return
	 */
	public static String toQuote(String content) {
		return "\"" + content + "\"";
	}

	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

	public static boolean isMac() {
		return System.getProperty("os.name").toLowerCase().contains("mac");
	}

	public static boolean isUnix() {
		return System.getProperty("os.name").toLowerCase().contains("nix")
				|| System.getProperty("os.name").toLowerCase().contains("nux")
				|| System.getProperty("os.name").toLowerCase().contains("aix")
				|| System.getProperty("os.name").toLowerCase().contains("centos");
	}

	public static boolean isSolaris() {
		return System.getProperty("os.name").toLowerCase().contains("sunos");
	}

	public static String normalizePath(String path) {
		path = path.replace("\r", "").replace("\n", "");
	    String singleBackSlash = "\\";
	    String doubleBackSlash = singleBackSlash + singleBackSlash;
	    String singleSlash = "/";

		return path.replace(singleBackSlash, File.separator)
                .replace(singleSlash, File.separator)
				.replace(doubleBackSlash, File.separator);
	}

	public static String doubleNormalizePath(String path) {
		String singleBackSlash = "\\";
		String doubleBackSlash = singleBackSlash + singleBackSlash;
		String singleSlash = "/";

		String result = normalizePath(path);

		if (!File.separator.equals(singleSlash)) {
			result = result.replace(File.separator, doubleBackSlash);
		}

		return result;
	}

	public static Class<?>[] getAllSubClass(Class<?> c, String... packages) {
		List<Class<?>> subTypes = new ArrayList<>();

		if (packages.length == 0) {
			Reflections reflections = new Reflections(c);
			subTypes.addAll(reflections.getSubTypesOf(c));
		} else {
			for (String p : packages) {
				Reflections reflections = new Reflections(p, c);
				subTypes.addAll(reflections.getSubTypesOf(c));
			}
		}

		return subTypes.toArray(new Class<?>[0]);
	}

	/**
	 * Check whether the function contains do..while, while..do, for...
	 *
	 * @param fn
	 * @return
	 */
	public static boolean containsLoopBlock(IFunctionNode fn) {
		IASTFunctionDefinition fnAst = fn.getAST();
		Utils.containBlock = false;

		ASTVisitor visitor = new ASTVisitor() {

			@Override
			public int visit(IASTStatement statement) {
				if (statement instanceof IASTWhileStatement || statement instanceof IASTDoStatement
						|| statement instanceof IASTForStatement) {
					Utils.containBlock = true;
					return ASTVisitor.PROCESS_ABORT;
				} else
					return ASTVisitor.PROCESS_CONTINUE;
			}
		};

		visitor.shouldVisitStatements = true;

		fnAst.accept(visitor);

		return Utils.containBlock;
	}

	/**
	 * Check whether the function contains do..while, while..do, for...
	 *
	 * @param fn
	 * @return
	 */
	public static boolean containsIfBlock(IFunctionNode fn) {
		IASTFunctionDefinition fnAst = fn.getAST();
		Utils.containBlock = false;

		ASTVisitor visitor = new ASTVisitor() {

			@Override
			public int visit(IASTStatement statement) {
				if (statement instanceof IASTIfStatement) {
					Utils.containBlock = true;
					return ASTVisitor.PROCESS_ABORT;
				} else
					return ASTVisitor.PROCESS_CONTINUE;
			}
		};

		visitor.shouldVisitStatements = true;

		fnAst.accept(visitor);

		return Utils.containBlock;
	}

//	public static String getCastedValue(String value, ISymbolicVariable var) {
//		String castedValue = var.getType();
//
//		if (VariableTypeUtils.isNum(var.getType()) && VariableTypeUtils.isNumFloat(var.getType()))
//			castedValue = "to_int(" + value + ")";
//		return castedValue;
//	}


//	public static String getRealType(String type, INode function) {
//		String realType = "";
////
////		/*
////		 * Delete unnecessary character
////		 *
////		 * Ex: "const BigData&" ----------> ""BigData
////		 */
////		type = VariableTypeUtils.deleteStorageClasses(type);
////		type = VariableTypeUtils.deleteStructKeyword(type);
////		type = VariableTypeUtils.deleteUnionKeyword(type);
////		type = VariableTypeUtils.deleteReferenceOperator(type);
////
////		/*
////		 * Create temporary variable
////		 */
////		VariableNode var = new VariableNode();
////
////		String remaining = "";
////		String reducedType = type;
////		if (VariableTypeUtils.isOneDimension(type) || VariableTypeUtils.isTwoDimension(type)) {
////			int index = type.indexOf("[");
////
////			remaining = type.substring(index);
////			reducedType = type.substring(0, index);
////
////		} else if (VariableTypeUtils.isOneLevel(type) || VariableTypeUtils.isTwoLevel(type)) {
////			int index = type.indexOf("*");
////			remaining = type.substring(index);
////			reducedType = type.substring(0, index);
////		}
////
////		var.setRawType(reducedType);
////		var.setCoreType(reducedType);
////		var.setReducedRawType(reducedType);
////		var.setParent(function);
//
//		INode nodeType = var.resolveCoreType();
//
//		if (nodeType != null) {
//			if (nodeType instanceof PrimitiveTypeNode)
//				realType = ((PrimitiveTypeNode) nodeType).getType();
//			else if (nodeType instanceof VariableNode)
//				realType = ((IVariableNode) nodeType).getReducedRawType();
//			else if (nodeType instanceof StructureNode)
//				realType = nodeType.getNewType();
//		} else realType = reducedType;
////			realType = type;
//
//		realType += remaining;
//
//		if (realType.startsWith("inline "))
//		    realType = realType.replace("inline ", "");
//		if (realType.startsWith("virtual "))
//            realType = realType.replace("virtual ", "");
//
//        return realType;
//	}

	/*
	  Get the reduce index of array item
	  <p>
	  Ex: a[1+2][3] --------> [3][3]

	  @param arrayItem
	 * @param table
	 * @return
	 * @throws Exception
	 */
//	public static String getReducedIndex(String arrayItem, IVariableNodeTable table) throws Exception {
//		String index = "";
//		List<String> indexes = Utils.getIndexOfArray(arrayItem);
//
//		for (String indexItem : indexes) {
//			indexItem = ExpressionRewriterUtils.rewrite(table, indexItem);
//			index += Utils.asIndex(indexItem);
//		}
//		return index;
//	}

	/**
	 * Shorten ast node. <br/>
	 * Ex:"(a)" -----> "a" <br/>
	 * Ex: "(!a)" --------> "!a"
	 *
	 * @param ast
	 * @return
	 */
	public static IASTNode shortenAstNode(IASTNode ast) {
		IASTNode tmp = ast;
		/*
		 * Ex:"(a)" -----> "a"
		 *
		 * Ex: "(!a)" --------> !a
		 */
		while ((tmp instanceof CPPASTExpressionStatement || tmp instanceof ICPPASTUnaryExpression
				&& tmp.getRawSignature().startsWith("(") && tmp.getRawSignature().endsWith(")"))
				&& tmp.getChildren().length == 1 && !tmp.getRawSignature().startsWith("!"))
			tmp = tmp.getChildren()[0];

		return tmp;
	}

	/**
	 * Get all unary expression
	 * <p>
	 * Ex: "x=(a++) +1+ (--b)" -------> unary expression: {"a++", "--b}
	 *
	 * @param ast
	 * @return
	 */
	public static List<ICPPASTUnaryExpression> getUnaryExpressions(IASTNode ast) {
		List<ICPPASTUnaryExpression> unaryExpressions = new ArrayList<>();

		ASTVisitor visitor = new ASTVisitor() {

			@Override
			public int visit(IASTExpression name) {
				if (name instanceof ICPPASTUnaryExpression) {
					unaryExpressions.add((ICPPASTUnaryExpression) name);
				}
				return ASTVisitor.PROCESS_CONTINUE;
			}
		};

		visitor.shouldVisitExpressions = true;

		ast.accept(visitor);
		return unaryExpressions;
	}

	public static int countCharIn(String str, char ch) {
		return (int) str.chars().filter(c -> c == ch).count();
	}

	public static boolean isCondition(String content) {
		/*
		 * Get type of the statement
		 */
		boolean isCondition = false;

		// special case: content= "a"
		if (content.matches(IRegex.NAME_REGEX))
			isCondition = true;

		else
		/*
		 * Ex: char c = static_cast<char>(x)
		 *
		 * Ex: char c = static_cast<char>(x);
		 *
		 * Ex: cout << "A";
		 */
		if (content.endsWith(SpecialCharacter.END_OF_STATEMENT) || content.contains(" = ")
				|| Utils.containRegex(content, "\\b=\\b") || content.startsWith("cout ") || content.startsWith("cout<<")
				|| content.startsWith("std::"))
			isCondition = false;
		else {
			final String[] CONDITION_SIGNALS = new String[] { "!=", "<=", ">=", "==", ">", "<", "!" };

			for (String conditionSignal : CONDITION_SIGNALS)
				if (content.contains(conditionSignal))
					isCondition = true;
		}
		return isCondition;
	}

	/**
	 * Get ast corresponding to statement, e.g., x=y+2
	 *
	 * @param content
	 * @return
	 */
	public static IASTNode convertToIAST(String content) {
		IASTNode ast;

		/*
		 * Get type of the statement
		 */
		boolean isCondition = Utils.isCondition(content);
		/*
		 * The statement is assignment
		 */
		if (!isCondition) {
			content += content.endsWith(SpecialCharacter.END_OF_STATEMENT) ? "" : SpecialCharacter.END_OF_STATEMENT;

			ICPPASTFunctionDefinition fn = Utils
					.getFunctionsinAST(("void test(){" + content + "}").toCharArray()).get(0);

			if (fn.getBody().getChildren().length == 0)
				System.out.println();

			if (fn.getBody() instanceof IASTCompoundStatement && fn.getBody().getChildren().length == 1)
					ast = fn.getBody().getChildren()[0];
				else
					ast = fn.getBody();
		} else
		/*
		 * The statement is condition
		 */ {
			ICPPASTFunctionDefinition fn = Utils
					.getFunctionsinAST(("void test(){if (" + content + "){}}").toCharArray()).get(0);
			ast = fn.getBody().getChildren()[0].getChildren()[0];
		}
		return Utils.shortenAstNode(ast);
	}

	/**
	 * Check whether a string contain regex or not
	 *
	 * @param src
	 * @param regex
	 * @return
	 */
	public static boolean containRegex(String src, String regex) {
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(src);

		return m.find();
	}

	/**
	 * Get all expression in the assignment. Ex: "x=y=z+1"---->{x, y, z+1} in order
	 * of left side to right side
	 *
	 * @param binaryAST
	 */
	public static List<String> getAllExpressionsInBinaryExpression(IASTBinaryExpression binaryAST) {
		List<String> expression = new ArrayList<>();
		IASTNode tmpAST = binaryAST;

		while (tmpAST instanceof IASTBinaryExpression) {
			IASTNode firstChild = tmpAST.getChildren()[0];
			expression.add(firstChild.getRawSignature());

			IASTNode secondChild = tmpAST.getChildren()[1];
			tmpAST = secondChild;
		}
		expression.add(tmpAST.getRawSignature());

		return expression;
	}

	/**
	 * Convert a string into regex
	 *
	 * @param str
	 * @return
	 */
	public static String toRegex(String str) {
		str = str.replace("[", "\\[").replace("]", "\\]").replace("(", "\\(").replace(")", "\\)").replace(".", "\\.")
				.replace("*", "\\*").replace(" ", "\\s*").replace("_", "\\_");

		/*
		 * Add bound of word at the beginning
		 */
		if (str.toCharArray()[0] >= 'A' && str.toCharArray()[0] <= 'Z'
				|| str.toCharArray()[0] >= 'a' && str.toCharArray()[0] <= 'z')
			str = "\\b" + str;

		/*
		 * Add bound of word at the end
		 */
		int last = str.toCharArray().length - 1;
		if (str.toCharArray()[last] >= 'A' && str.toCharArray()[last] <= 'Z'
				|| str.toCharArray()[last] >= 'a' && str.toCharArray()[last] <= 'z')
			str += "\\b";
		return str;
	}

	public static String asIndex(int i) {
		return "[" + i + "]";
	}

	public static String asIndex(String str) {
		return "[" + str + "]";
	}

//	public static void initializeEnvironment() throws Exception {
//		Utils.getMethodName(2);
//
//		CompilerFolderParser compiler = new CompilerFolderParser(new File("C:/Dev-Cpp/"));
//		compiler.parse();
//		if (new File(compiler.getMakePath()).exists() && new File(compiler.getGccPath()).exists()
//				&& new File(compiler.getgPlusPlusPath()).exists()) {
//			AbstractSetting.setValue(ISettingv2.GNU_MAKE_PATH, compiler.getMakePath());
//			AbstractSetting.setValue(ISettingv2.GNU_GCC_PATH, compiler.getGccPath());
//			AbstractSetting.setValue(ISettingv2.GNU_GPlusPlus_PATH, compiler.getgPlusPlusPath());
//		} else
//			throw new Exception("Ä�Æ°á»�ng dáº«n biÃªn dá»‹ch sai");
//		/**
//		 *
//		 */
//		String z3SolverPath = "C:/z3/bin/z3.exe";
//		if (new File(z3SolverPath).exists())
//			AbstractSetting.setValue(ISettingv2.SOLVER_Z3_PATH, z3SolverPath);
//		else
//			throw new Exception("Ä�Æ°á»�ng dáº«n Z3 sai");
//	}

	/**
	 * Bá»• sung thÃªm ná»™i dung vÃ o tá»‡p
	 *
	 * @param path
	 * @param content
	 */
	public static void appendContentToFile(String content, String path) {
		File f = new File(path);
		if (!f.exists())
			Utils.writeContentToFile(content, path);
		else {
			String currentContent = Utils.readFileContent(path);
			Utils.writeContentToFile(currentContent + content, path);
		}
	}

	/**
	 * Kiá»ƒm tra trong biá»ƒu thá»©c cÃ³ lá»�i gá»�i hÃ m
	 * khÃ´ng
	 *
	 * @param ast
	 * @return
	 */
	public static boolean containFunctionCall(IASTNode ast) {

		ASTVisitor visitor = new ASTVisitor() {

			@Override
			public int visit(IASTExpression expression) {
				if (expression instanceof IASTFunctionCallExpression) {
					Utils.containFunction = true;
					return ASTVisitor.PROCESS_ABORT;
				} else
					return ASTVisitor.PROCESS_CONTINUE;
			}
		};
		visitor.shouldVisitStatements = true;
		visitor.shouldVisitExpressions = true;
		ast.accept(visitor);
		return Utils.containFunction;
	}

	/**
	 * Convert List<String> into String[]
	 *
	 * @param list
	 * @return
	 */
	public static String[] convertToArray(List<String> list) {
		String[] strarray = list.toArray(new String[list.size()]);
		return strarray;
	}

	public static File copy(String originalFolder) throws IOException {
		originalFolder = Utils.normalizePath(originalFolder);

		String copyFolder = originalFolder;
		if (originalFolder.endsWith(File.separator))
			copyFolder = originalFolder.substring(0, originalFolder.length() - 1);

		copyFolder += "_copy";
		while (new File(copyFolder).exists())
			copyFolder += "1";
		Utils.copyFolder(new File(originalFolder), new File(copyFolder));
		return new File(copyFolder);
	}

	public static void copyFolder(File src, File dest) throws IOException {

		if (src.isDirectory()) {

			// if directory not exists, create it
			if (!dest.exists())
				dest.mkdir();

			// list all the directory contents
			String[] files = src.list();

			for (String file : files) {
				// construct the src and dest file structure
				File srcFile = new File(src, file);
				File destFile = new File(dest, file);
				// recursive copy
				Utils.copyFolder(srcFile, destFile);
			}

		} else {
			// if file, then copy it
			// Use bytes stream to support all file types
			InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dest);

			byte[] buffer = new byte[1024];

			int length;
			// copy the file content in bytes
			while ((length = in.read(buffer)) > 0)
				out.write(buffer, 0, length);

			in.close();
			out.close();
		}
	}

	/**
	 * Tao folder
	 *
	 * @param path
	 */
	public static void createFolder(String path) {
		File destDir = new File(path);
		if (!destDir.exists())
			destDir.mkdir();
	}

	public static void deleteFileOrFolder(File path) {
		if (path != null && path.exists())
			try {
				FileDeleteStrategy.FORCE.delete(path);
				// FileUtils.deleteDirectory(new File(path));
				if (!path.exists()) {
				}
			} catch (IOException e) {
				try {
					Thread.sleep(30);
					Utils.deleteFileOrFolder(path);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
	}

//	/**
//	 * Delete all main() functions in project and update the project.
//	 *
//	 * @param root
//	 */
//	public static void deleteMain(INode root) {
//		List<INode> mainFunctionsType1 = Search.searchNodes(root, new FunctionNodeCondition(), "main()");
//		List<INode> mainFunctionsType2 = Search.searchNodes(root, new FunctionNodeCondition(), "main(int,char**)");
//		mainFunctionsType1.addAll(mainFunctionsType2);
//
//		for (INode functionMain : mainFunctionsType1) {
//			IASTFunctionDefinition ast = ((IFunctionNode) functionMain).getAST();
//			String content = Utils.readFileContent(functionMain.getParent());
//			content = content.replace(ast.getRawSignature(), "");
//			Utils.writeContentToFile(content.toString(), functionMain.getParent().getAbsolutePath());
//		}
//	}

	public static String deleteOrRenameTestedFunction(String oldContent, FunctionNode testedFunction) {
		String newContent;
		if (testedFunction.getSimpleName().contains(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS)) {
			logger.debug("The function " + testedFunction + " is in a namespace/class, we will convert its to declaration");
			String function = testedFunction.getAST().getRawSignature();
			newContent = oldContent.replace(function, ";");
		} else {
			logger.debug("The function " + testedFunction + " is not in a namespace/class, we will convert its to declaration");
			String function = testedFunction.getAST().getRawSignature();
			String functionBody = function.substring(function.indexOf("{"));
			newContent = oldContent.replace(functionBody, ";");
		}
		return newContent;
	}
	/**
	 * Delete all main() functions in project and update the project.
	 *
	 * @param root
	 */
	public static void deleteMain(INode root) {
		List<INode> mainFunctionsType1 = Search.searchNodes(root, new FunctionNodeCondition(), "main()");
		List<INode> mainFunctionsType2 = Search.searchNodes(root, new FunctionNodeCondition(), "main(int,char**)");
		mainFunctionsType1.addAll(mainFunctionsType2);

		for (INode functionMain : mainFunctionsType1) {
			IASTFunctionDefinition ast = ((IFunctionNode) functionMain).getAST();
			String content = Utils.readFileContent(functionMain.getParent());
			content = content.replace(ast.getRawSignature(), "");
			Utils.writeContentToFile(content, functionMain.getParent().getAbsolutePath());
		}
	}

	/**
	 * Ba trÆ°á»�ng há»£p cáº§n xá»­ lÃ½ gá»“m:
	 * <p>
	 * - HÃ m cáº§n kiá»ƒm thá»­ khÃ´ng thuá»™c class, struct
	 * nÃ o háº¿t
	 * <p>
	 * - HÃ m cáº§n kiá»ƒm thá»­ khai bÃ¡o trong class nhÆ°ng
	 * Ä‘á»‹nh nghÄ©a ngoÃ i class Ä‘Ã³
	 * <p>
	 * - HÃ m cáº§n kiá»ƒm thá»­ khai bÃ¡o vÃ  Ä‘á»‹nh
	 * nghÄ©a trong class
	 *
	 * @param testedFunction
	 */
	public static String deleteOrRenameTestedFunction(IFunctionNode testedFunction) {
		String parentPath = Utils.getSourcecodeFile(testedFunction).getAbsolutePath();
		String oldContent = Utils.readFileContent(parentPath);

		String newContent;
		if (testedFunction.getSimpleName().contains(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS)) {
			String function = ((FunctionNode) testedFunction).getAST().getRawSignature();
			newContent = oldContent.replace(function, ";");
		} else {
			String function = ((FunctionNode) testedFunction).getAST().getRawSignature();
			String functionBody = function.substring(function.indexOf("{"));
			newContent = oldContent.replace(functionBody, ";");
		}
		return newContent;
	}

	public static <T> String displayQueue(Queue<T> objectsQueue) {
		StringBuilder output = new StringBuilder();
		for (T item : objectsQueue)
			output.append(item.toString()).append(" # ");
		return output.toString();
	}

	public static <T> String displayStack(Stack<T> objectsStack) {
		StringBuilder output = new StringBuilder();
		for (T item : objectsStack)
			output.append(item.toString()).append(" # ");
		return output.toString();
	}

	public static String findFileExeMapWithNodeCurrent(String pathProject, INode nodeCurrent) throws IOException {
		/*
		  Find list file exe
		 */
		File dir = new File(pathProject);
		File[] listEXE = dir.listFiles((dir1, name) -> name.endsWith(".exe"));

		/*
		  Map Node with file exe
		 */
		INode nodeProject = nodeCurrent;
		while (nodeProject != null) {
			File folder = new File(nodeProject.getAbsolutePath());
			if (folder.isDirectory()) {
				String nameFolder = folder.getName();
				for (File temp : listEXE)
					if (temp.getName().equals(nameFolder))
						return temp.getCanonicalPath();
			}
			nodeProject = nodeProject.getParent();
		}

		return null;
	}

	public static IASTNode findFirstASTByName(String name, IASTNode ast) {

		ASTVisitor visitor = new ASTVisitor() {
			@Override
			public int visit(IASTStatement statement) {
				if (statement.getRawSignature().equals(name)) {
					Utils.output = statement;
					return ASTVisitor.PROCESS_ABORT;
				} else
					return ASTVisitor.PROCESS_CONTINUE;
			}
		};
		visitor.shouldVisitStatements = true;
		visitor.shouldVisitExpressions = true;
		ast.accept(visitor);

		return Utils.output;
	}

	/**
	 * Find ast by the content of condition
	 *
	 * @param name
	 *            the content of condition
	 * @param ast
	 *            the ast of source code containing the condition
	 * @return
	 */
	public static IASTNode findFirstConditionByName(String name, IASTNode ast) {
		ASTVisitor visitor = new ASTVisitor() {
			@Override
			public int visit(IASTStatement statement) {
				if (statement instanceof IASTIfStatement) {
					IASTNode con = ((IASTIfStatement) statement).getConditionExpression();

					if (con.getRawSignature().equals(name)) {
						Utils.output = con;
						return ASTVisitor.PROCESS_ABORT;
					}
				} else if (statement instanceof IASTWhileStatement) {
					IASTNode con = ((IASTWhileStatement) statement).getCondition();
					if (con.getRawSignature().equals(name)) {
						Utils.output = con;
						return ASTVisitor.PROCESS_ABORT;
					}
				} else if (statement instanceof IASTDoStatement) {
					IASTNode con = ((IASTDoStatement) statement).getCondition();
					if (con.getRawSignature().equals(name)) {
						Utils.output = con;
						return ASTVisitor.PROCESS_ABORT;
					}
				} else if (statement instanceof IASTSwitchStatement) {
					// TODO: xu ly
				}
				return ASTVisitor.PROCESS_CONTINUE;
			}
		};
		visitor.shouldVisitStatements = true;
		visitor.shouldVisitExpressions = true;
		ast.accept(visitor);

		return Utils.output;
	}

	/**
	 * Láº¥y mÃ£ ASCII cá»§a kÃ­ tá»±
	 *
	 * @param ch
	 * @return
	 */
	public static int getASCII(char ch) {
		return ch;
	}

	public static INode getClassvsStructvsNamesapceNodeParent(INode n) {
		if (n == null)
			return null;
		else if (n instanceof ClassNode || n instanceof StructNode || n instanceof NamespaceNode)
			return n;
		else {
			if (n instanceof AbstractFunctionNode)
			    if (((AbstractFunctionNode) n).getRealParent() != null)
				    return Utils.getClassvsStructvsNamesapceNodeParent(((AbstractFunctionNode) n).getRealParent());
			return Utils.getClassvsStructvsNamesapceNodeParent(n.getParent());
		}
	}

	public static INode getTopLevelClassvsStructvsNamesapceNodeParent(INode n) {
		if (n == null)
			return null;
		else if (n instanceof ClassNode || n instanceof StructNode || n instanceof NamespaceNode)
			if (n.getParent() != null && n.getParent() instanceof SourcecodeFileNode)
				return n;
			else
				return Utils.getTopLevelClassvsStructvsNamesapceNodeParent(n.getParent());
		else if (n instanceof IFunctionNode)
			return Utils.getTopLevelClassvsStructvsNamesapceNodeParent(((IFunctionNode) n).getRealParent());
		else
			return Utils.getTopLevelClassvsStructvsNamesapceNodeParent(n.getParent());
	}

	/**
	 * Láº¥y danh sÃ¡ch CPPASTName trong má»™t node AST
	 *
	 * @param ast
	 * @return
	 */
	public static List<CPPASTName> getCPPASTNames(IASTNode ast) {
		List<CPPASTName> cppASTNames = new ArrayList<>();

		ASTVisitor visitor = new ASTVisitor() {

			@Override
			public int visit(IASTName name) {
				cppASTNames.add((CPPASTName) name);
				return ASTVisitor.PROCESS_CONTINUE;
			}
		};

		visitor.shouldVisitNames = true;

		ast.accept(visitor);
		return cppASTNames;
	}

	public static List<ICPPASTFieldReference> getFieldReferences(IASTNode ast) {
		List<ICPPASTFieldReference> binaryASTs = new ArrayList<>();

		ASTVisitor visitor = new ASTVisitor() {

			@Override
			public int visit(IASTExpression name) {
				if (name instanceof ICPPASTFieldReference)
					binaryASTs.add((ICPPASTFieldReference) name);
				return ASTVisitor.PROCESS_CONTINUE;
			}
		};

		visitor.shouldVisitExpressions = true;

		ast.accept(visitor);
		return binaryASTs;
	}

	/**
	 * Get all binary expressions
	 *
	 * @param ast
	 * @return
	 */
	public static List<ICPPASTBinaryExpression> getBinaryExpressions(IASTNode ast) {
		List<ICPPASTBinaryExpression> binaryASTs = new ArrayList<>();

		ASTVisitor visitor = new ASTVisitor() {

			@Override
			public int visit(IASTExpression name) {
				if (name instanceof ICPPASTBinaryExpression)
					binaryASTs.add((ICPPASTBinaryExpression) name);
				return ASTVisitor.PROCESS_CONTINUE;
			}
		};

		visitor.shouldVisitExpressions = true;

		ast.accept(visitor);
		return binaryASTs;
	}

	/**
	 * Get all declarations in the given ast
	 *
	 * @param ast
	 * @return
	 */
	public static List<IASTSimpleDeclaration> getSimpleDeclarations(IASTNode ast) {
		List<IASTSimpleDeclaration> declarationASTs = new ArrayList<>();

		ASTVisitor visitor = new ASTVisitor() {

			@Override
			public int visit(IASTDeclaration name) {
				if (name instanceof IASTSimpleDeclaration)
					declarationASTs.add((IASTSimpleDeclaration) name);
				return ASTVisitor.PROCESS_CONTINUE;
			}
		};

		visitor.shouldVisitDeclarations = true;

		ast.accept(visitor);
		return declarationASTs;
	}

	public static List<ICPPASTLiteralExpression> getLiteralExpressions(IASTNode ast) {
		List<ICPPASTLiteralExpression> literalASTs = new ArrayList<>();

		ASTVisitor visitor = new ASTVisitor() {

			@Override
			public int visit(IASTExpression name) {
				if (name instanceof ICPPASTLiteralExpression)
					literalASTs.add((ICPPASTLiteralExpression) name);
				return ASTVisitor.PROCESS_CONTINUE;
			}
		};

		visitor.shouldVisitExpressions = true;

		ast.accept(visitor);
		return literalASTs;
	}

	public static String getFileExtension(String path) {
		String[] pathSegments = path.split(File.separator);
		String fileName = pathSegments[pathSegments.length - 1];

		int i = fileName.lastIndexOf('.');
		if (i == -1)
			return "";
		else
			return fileName.substring(i + 1);
	}

	/**
	 * Láº¥y node cha lÃ  function
	 *
	 * @param n
	 * @return
	 */
	public static INode getFunctionNodeParent(INode n) {
		if (n == null)
			return null;
		else if (n instanceof FunctionNode)
			return n;
		else
			return Utils.getFunctionNodeParent(n.getParent());

	}

	/**
	 * Láº¥y danh sÃ¡ch táº¥t cáº£ má»�i hÃ m á»Ÿ Ä‘á»‹nh
	 * dáº¡ng AST
	 *
	 * @param sourcecode
	 * @return
	 */
	public static List<ICPPASTFunctionDefinition> getFunctionsinAST(char[] sourcecode) {
		List<ICPPASTFunctionDefinition> output = new ArrayList<>();

		try {
			IASTTranslationUnit unit = Utils.getIASTTranslationUnitforCpp(sourcecode);

			if (unit.getChildren()[0] instanceof CPPASTProblemDeclaration)
				unit = Utils.getIASTTranslationUnitforC(sourcecode);

			ASTVisitor visitor = new ASTVisitor() {
				@Override
				public int visit(IASTDeclaration declaration) {
					if (declaration instanceof ICPPASTFunctionDefinition) {
						output.add((ICPPASTFunctionDefinition) declaration);
						return ASTVisitor.PROCESS_SKIP;
					}
					return ASTVisitor.PROCESS_CONTINUE;
				}
			};

			visitor.shouldVisitDeclarations = true;

			unit.accept(visitor);
		} catch (Exception e) {

		}
		return output;
	}

	public static IASTTranslationUnit getIASTTranslationUnitforCpp(char[] code) throws Exception {
		File filePath = new File("");
		FileContent fc = FileContent.create(filePath.getAbsolutePath(), code);
		Map<String, String> macroDefinitions = new HashMap<>();
		String[] includeSearchPaths = new String[0];
		IScannerInfo si = new ScannerInfo(macroDefinitions, includeSearchPaths);
		IncludeFileContentProvider ifcp = IncludeFileContentProvider.getEmptyFilesProvider();
		IIndex idx = null;
		int options = ILanguage.OPTION_IS_SOURCE_UNIT;
		IParserLogService log = new DefaultLogService();
		return GPPLanguage.getDefault().getASTTranslationUnit(fc, si, ifcp, idx, options, log);
	}

	public static IASTTranslationUnit getIASTTranslationUnitforC(char[] code) throws Exception {
		File filePath = new File("");
		FileContent fc = FileContent.create(filePath.getAbsolutePath(), code);
		Map<String, String> macroDefinitions = new HashMap<>();
		String[] includeSearchPaths = new String[0];
		IScannerInfo si = new ScannerInfo(macroDefinitions, includeSearchPaths);
		IncludeFileContentProvider ifcp = IncludeFileContentProvider.getEmptyFilesProvider();
		IIndex idx = null;
		int options = ILanguage.OPTION_IS_SOURCE_UNIT;
		IParserLogService log = new DefaultLogService();
		return GCCLanguage.getDefault().getASTTranslationUnit(fc, si, ifcp, idx, options, log);
	}

	/**
	 * Láº¥y danh sÃ¡ch id trong má»™t node AST
	 *
	 * @param ast
	 * @return
	 */
	public static List<CPPASTIdExpression> getIds(IASTNode ast) {
		List<CPPASTIdExpression> ids = new ArrayList<>();

		ASTVisitor visitor = new ASTVisitor() {
			@Override
			public int visit(IASTExpression expression) {
				if (expression instanceof CPPASTIdExpression)
					ids.add((CPPASTIdExpression) expression);
				return ASTVisitor.PROCESS_CONTINUE;
			}
		};

		visitor.shouldVisitExpressions = true;

		ast.accept(visitor);
		return ids;
	}

	public static List<ICPPASTArraySubscriptExpression> getArraySubscriptExpression(IASTNode ast) {
		List<ICPPASTArraySubscriptExpression> ids = new ArrayList<>();

		ASTVisitor visitor = new ASTVisitor() {
			@Override
			public int visit(IASTExpression expression) {
				if (expression instanceof ICPPASTArraySubscriptExpression) {
					ids.add((ICPPASTArraySubscriptExpression) expression);

					if (expression.getChildren()[0] instanceof ICPPASTArraySubscriptExpression)
						return ASTVisitor.PROCESS_SKIP;
					else
						return ASTVisitor.PROCESS_CONTINUE;
				} else
					return ASTVisitor.PROCESS_CONTINUE;
			}
		};

		visitor.shouldVisitExpressions = true;

		ast.accept(visitor);
		return ids;
	}

	/**
	 * Láº¥y danh sÃ¡ch chá»‰ sá»‘ máº£ng
	 *
	 * @param constraint
	 *            VD: a[3][2]
	 * @return VD: 3,2
	 * @problem ChÆ°a xá»­ lÃ½ chá»‰ sá»‘ máº£ng chá»©a
	 *          chá»‰ sá»‘ máº£ng khÃ¡c. VD: a[1+b[2]]
	 */
	public static List<String> getIndexOfArray(String constraint) {
		List<String> output = new ArrayList<>();

		Pattern p = Pattern.compile(IRegex.ARRAY_INDEX);
		Matcher m = p.matcher(constraint);

		while (m.find()) {
			String str = m.group(1);
			output.add(str);
		}

		return output;
	}

	/**
	 * Get the method name for a depth in call stack. <br />
	 * Utility function
	 *
	 * @param depth
	 *            depth in the call stack (0 means current method, 1 means call
	 *            method, ...)
	 * @return method name
	 */
	public static String getMethodName(final int depth) {
		final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		return ste[ste.length - 1 - depth].getMethodName();
	}

	/**
	 * Láº¥y tÃªn tá»‡p .exe
	 *
	 * @param makefilepath
	 *            Ä�Æ°á»�ng dáº«n tá»‡p make
	 * @return
	 */
	public static String getNameOfExeInDevCppMakefile(String makefilepath) {
		String makefileContent = Utils.readFileContent(Utils.normalizePath(makefilepath));

		// TH 1. Tá»‡p exe Ä‘á»‹nh nghÄ©a tÆ°á»�ng minh
		// Ex: g++ StackLinkedList.o main.o -o "main.exe"
		Pattern p = Pattern.compile("\\s+-o\\s+\"([a-zA-Z0-9\\.]+)\\.exe\"");
		Matcher m = p.matcher(makefileContent);
		if (m.find()) {
			logger.debug("Exe name: " + m.group(1));
			return m.group(1) + ".exe";
		}

		// TH 2. Tá»‡p exe Ä‘á»‹nh nghÄ©a tÆ°á»�ng minh
		// Ex: g++ StackLinkedList.o main.o -o main.exe
		p = Pattern.compile("\\s+-o\\s+([a-zA-Z0-9\\.]+)\\.exe");
		m = p.matcher(makefileContent);
		if (m.find()) {
			logger.debug("Exe name: " + m.group(1));
			return m.group(1) + ".exe";
		}

		// TH 3. Tá»‡p exe Ä‘á»‹nh nghÄ©a khÃ´ng tÆ°á»�ng
		// minh
		p = Pattern.compile("BIN\\s+=\\s+([^\\.]+)\\.exe");
		m = p.matcher(makefileContent);
		if (m.find()) {
			logger.debug("Exe name: " + m.group(1));
			return m.group(1) + ".exe";
		}
		return "";
	}

	/**
	 * Get name of variable
	 * Ex: "a[2]" ----->"a"
	 * Ex: "a.b[2]" ----->"a.b"
	 * @param variableName
	 * @return
	 */
	public static String getNameVariable(String variableName) {
		if (variableName.endsWith("]") && variableName.contains("["))
			return variableName.substring(0, variableName.lastIndexOf("["));
		else
			return variableName;
	}

	public static INode getRoot(INode n) {
		if (n == null)
			return null;
		else if (n.getParent() == null)
			return n;
		else
			return Utils.getRoot(n.getParent());

	}

	public static String getRelativePath(INode node) {
		String path = node.getAbsolutePath();

		INode root = Utils.getRoot(node);

		path = path.substring(root.getAbsolutePath().length());

		if (path.startsWith(File.separator))
			path = path.substring(1);

		return path;
	}

	/**
	 * Láº¥y danh sÃ¡ch CPPASTSimpleDeclSpecifier trong má»™t node AST
	 *
	 * @param ast
	 * @return
	 */
	public static List<CPPASTSimpleDeclSpecifier> getSimpleDeclSpecifiers(IASTNode ast) {
		List<CPPASTSimpleDeclSpecifier> cppSimpleDeclSpecifiers = new ArrayList<>();

		ASTVisitor visitor = new ASTVisitor() {
			@Override
			public int visit(IASTDeclSpecifier declSpec) {
				if (declSpec instanceof CPPASTSimpleDeclSpecifier)
					cppSimpleDeclSpecifiers.add((CPPASTSimpleDeclSpecifier) declSpec);
				return super.visit(declSpec);
			}
		};

		visitor.shouldVisitDeclSpecifiers = true;

		ast.accept(visitor);
		return cppSimpleDeclSpecifiers;
	}

	/**
	 * Get the source code file containing a specified node
	 *
	 * @param n
	 * @return
	 */
	public static ISourcecodeFileNode getSourcecodeFile(INode n) {
		if (n == null)
			return null;
		else if (n instanceof ISourcecodeFileNode)
			return (ISourcecodeFileNode) n;
		else
			return Utils.getSourcecodeFile(n.getParent());

	}

	public static INode getProjectNode(INode n) {
		if (n == null)
			return null;
		else if (n instanceof IProjectNode)
			return n;
		else
			return Utils.getProjectNode(n.getParent());
	}

	/**
	 * Get the file node contain the given node
	 *
	 * @param n
	 * @return
	 */
	public static INode getFileNode(INode n) {
		if (n == null)
			return null;
		else if (new File(n.getAbsolutePath()).exists())
			return n;
		else
			return Utils.getSourcecodeFile(n.getParent());

	}

	/**
	 * Lay node cha la class | struct
	 */
	public static INode getStructureParent(INode n) {
		if (n == null)
			return null;
		else if (n instanceof ClassNode || n instanceof StructNode)
			return n;
		else
			return Utils.getStructureParent(n.getParent());

	}

	public static <T> boolean isAvailable(List<T> l) {
		return l != null && l.size() != 0;
	}

	public static boolean isAvailable(String s) {
		return s != null && s.length() != 0;
	}

	public static boolean isSpecialChInVisibleRange(int ASCII) {
		return ASCII == 34 /* nhay kep */ || ASCII == 92 /* gach cheo */
				|| ASCII == 39 /* nhay don */;
	}

	/**
	 * Check whether the character corresponding to ASCII can be shown in screen or
	 * not
	 *
	 * @param ASCII
	 * @return
	 */
	public static boolean isVisibleCh(int ASCII) {
		return ASCII >= 32 && ASCII <= 126;
	}

	public static String putInSingleQuote(Character c) {
		return "'" + c + "'";
	}

	public static String putInString(String str) {
		return "\"" + str + "\"";
	}

	public static String readFileContent(File file) {
		return Utils.readFileContent(file.getAbsolutePath());
	}

	public static String readFileContent(INode n) {
		return Utils.readFileContent(n.getAbsolutePath());
	}

	public static String readResourceContent(String relativePath) {
		InputStream in = Utils.class.getResourceAsStream(relativePath);

		StringBuilder template = new StringBuilder();
		String line;

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			while ((line = reader.readLine()) != null)
				template.append(line).append(SpecialCharacter.LINE_BREAK);
		} catch (IOException ex) {
			logger.error("Cant read resource content from " + relativePath);
		}

		return template.toString();
	}

	/**
	 * Doc noi dung file
	 *
	 * @param filePath
	 *            duong dan tuyet doi file
	 * @return noi dung file
	 */
	public static String readFileContent(String filePath) {
		StringBuilder fileData = new StringBuilder(3000);
		try {
			BufferedReader reader;
			reader = new BufferedReader(new FileReader(filePath));
			char[] buf = new char[10];
			int numRead;
			while ((numRead = reader.read(buf)) != -1) {
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
				buf = new char[1024];
			}
			reader.close();
		} catch (Exception e) {
			 e.printStackTrace();
		} finally {
			return fileData.toString();
		}
	}

	/**
	 * Cusom splitting
	 *
	 * @param str
	 * @param delimiter
	 * @return
	 */
	public static List<String> split(String str, String delimiter) {
		List<String> output = new ArrayList<>();

		/*
		  In case we need split a path into tokens
		 */
		if (delimiter.equals("\\") || delimiter.equals("/")) {
			str = str.replace("\\", "/");
			delimiter = "/";
		}
		if (str.contains(delimiter)) {

			String[] elements = str.split(delimiter);
			for (String element : elements)
				/*
				  Nhá»¯ng xÃ¢u cÃ³ Ä‘á»™ dÃ i báº±ng 0 thÃ¬ bá»� qua
				 */

				if (element.length() > 0)
					output.add(element);

		} else
			output.add(str);
		return output;
	}

	public static int toInt(String str) {
		/*
		  Remove bracket from negative number. Ex: Convert (-2) into -2
		 */
		str = str.replaceAll("\\((" + IRegex.NUMBER_REGEX + ")\\)", "$1");

		/*

		 */
		boolean isNegative = false;
		if (str.startsWith("-")) {
			str = str.substring(1);
			isNegative = true;
		} else if (str.startsWith("+"))
			str = str.substring(1);
		/*

		 */
		int n;
		try {
			n = Integer.parseInt(str);
			if (isNegative)
				n = -n;
		} catch (Exception e) {
			n = Utils.UNDEFINED_TO_INT;
		}
		return n;
	}

	public static double toDouble(String str) {
		/*
		  Remove bracket from negative number. Ex: Convert (-2) into -2
		 */
		str = str.replaceAll("\\((" + IRegex.NUMBER_REGEX + ")\\)", "$1");

		/*

		 */
		boolean isNegative = false;
		if (str.startsWith("-")) {
			str = str.substring(1);
			isNegative = true;
		} else if (str.startsWith("+"))
			str = str.substring(1);
		/*

		 */
		double n;
		try {
			n = Double.parseDouble(str);
			if (isNegative)
				n = -n;
		} catch (Exception e) {
			n = Utils.UNDEFINED_TO_DOUBLE;
		}
		return n;
	}

	public static String toUpperFirstCharacter(String str) {
		StringBuilder output;
		char[] c = str.toCharArray();

		output = new StringBuilder((c[0] + "").toUpperCase());
		for (int i = 1; i < c.length; i++)
			output.append(c[i]);

		return output.toString();
	}

	/**
	 * Specify type of project that belong to which IDE (e.g., Eclipse, Dev-Cpp,
	 * Code block, Visual studio)
	 *
//	 * @param projectPath
	 *            : path of Project
	 * @return
	 */
	public static int getTypeOfProject(String projectPath) {
		File dir = new File(projectPath);

		/*
		  Project is created without using IDE or not. It only has a makefile.
		 */
		final String[] MAKEFILE_PROJECT_SIGNAL = new String[] { "Makefile" };
		for (String signal : MAKEFILE_PROJECT_SIGNAL) {
			if (new File(projectPath + File.separator + signal).exists()) {
				logger.debug("Is custom makefile project");
				return IProjectType.PROJECT_CUSTOMMAKEFILE;
			}
		}

		/*
		  Project is created by using IDE Dev-Cpp
		 */
		final String[] DEV_CPP_PROJECT_SIGNAL = new String[] { ".win" };
		for (String signal : DEV_CPP_PROJECT_SIGNAL)
			if (dir.listFiles((dir1, name) -> name.endsWith(signal)).length > 0) {
				logger.debug("Is DevCpp project");
				return IProjectType.PROJECT_DEV_CPP;
			}

		/*
		  Project is created by using IDE Code block
		 */
		final String[] CODE_BLOCK_PROJECT_SIGNAL = new String[] { ".cbp" };
		for (String signal : CODE_BLOCK_PROJECT_SIGNAL)
			if (dir.listFiles((dir1, name) -> name.endsWith(signal)).length > 0) {
				logger.debug("Is code block project");
				return IProjectType.PROJECT_CODEBLOCK;
			}

		/*
		  Project is created by using IDE Visual Studio
		 */
		final String[] CODE_VISUAL_STUDIO_PROJECT_SIGNAL = new String[] { ".vcxproj", ".sln" };
		for (String signal : CODE_VISUAL_STUDIO_PROJECT_SIGNAL)
			if (dir.listFiles((dir1, name) -> name.endsWith(signal)).length > 0) {
				logger.debug("Is Visual studio project");
				return IProjectType.PROJECT_VISUALSTUDIO;
			}

		/*
		  Project is created by using IDE Eclipse
		 */
		final String[] ECLIPSE_STUDIO_PROJECT_SIGNAL = new String[] { ".cproject", ".project" };
		for (String signal : ECLIPSE_STUDIO_PROJECT_SIGNAL)
			if (dir.listFiles((dir1, name) -> name.endsWith(signal)).length > 0) {
				logger.debug("Is Eclipse project");
				return IProjectType.PROJECT_ECLIPSE;
			}

		return IProjectType.PROJECT_UNKNOWN_TYPE;
	}

	public static boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
		long startTime = System.nanoTime();
		long rem = unit.toNanos(timeout);

		do {
			try {
				return true;
			} catch (IllegalThreadStateException ex) {
				if (rem > 0)
					Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
			}
			rem = unit.toNanos(timeout) - (System.nanoTime() - startTime);
		} while (rem > 0);
		return false;
	}

	public static void writeContentToFile(String content, INode n) {
		Utils.writeContentToFile(content, n.getAbsolutePath());
	}

	public static void writeContentToFile(String content, String filePath) {
		try {
			new File(filePath).getParentFile().mkdirs();
			PrintWriter out = new PrintWriter(filePath);
			out.println(content);
			out.close();
		} catch (FileNotFoundException e) {
			logger.error("Cant write content to " + filePath + " because " + e.getMessage());
			 e.printStackTrace();
		}
	}

	public static INode findRootProject(INode currentNode) {
		if (currentNode.getParent() == null)
			return currentNode;
		else if (Utils.isProjectNode(currentNode))
			return currentNode;
		else
			return Utils.findRootProject(currentNode.getParent());
	}

	public static boolean isProjectNode(INode node) {
		for (INode nodeChild : node.getChildren())
			if (nodeChild.getParent() == null || nodeChild.getNewType().endsWith(".vcxproj")
					|| nodeChild.getNewType().endsWith("win"))
				return true;
		return false;
	}

	public static List<String> findHeaderFiles(String pathProject) {
		List<String> pathSourceCodeFiles = new ArrayList<>();
		IProjectNode root = new ProjectLoader().load(new File(pathProject));

		List<INode> nodeSourceCodeFiles = Search.searchNodes(root, new SourcecodeFileNodeCondition());

		for (INode temp : nodeSourceCodeFiles)
			if (temp.getAbsolutePath().endsWith(HeaderNode.HEADER_SIGNALS))
				pathSourceCodeFiles.add(temp.getAbsolutePath().replace("\\", "/"));

		return pathSourceCodeFiles;
	}

	public static List<String> findSourcecodeFiles(String pathProject) {
		List<String> pathSourceCodeFiles = new ArrayList<>();
		IProjectNode root = new ProjectLoader().load(new File(pathProject));

		List<INode> nodeSourceCodeFiles = Search.searchNodes(root, new SourcecodeFileNodeCondition());

		for (INode temp : nodeSourceCodeFiles)
			if (temp.getAbsolutePath().endsWith(".cpp") || temp.getAbsolutePath().endsWith(".c"))
				pathSourceCodeFiles.add(temp.getAbsolutePath().replace("\\", "/"));

		return pathSourceCodeFiles;
	}

	/**
	 * @see #{CustomJevalTest.java}
	 * @param expression
	 * @return
	 */
	public static String transformFloatNegativeE(String expression) {
		Matcher m = Pattern.compile("\\d+E-\\d+").matcher(expression);
		while (m.find()) {
			String beforeE = expression.substring(0, expression.indexOf("E-"));
			String afterE = expression.substring(expression.indexOf("E-") + 2);

			StringBuilder newValue = new StringBuilder();

			if (Utils.toInt(afterE) != Utils.UNDEFINED_TO_INT) {
				int numDemicalPoint = Utils.toInt(afterE);

				if (numDemicalPoint == 0) {
					newValue = new StringBuilder(beforeE);

				} else if (beforeE.length() > numDemicalPoint) {
					for (int i = 0; i < beforeE.length() - numDemicalPoint; i++)
						newValue.append(beforeE.toCharArray()[i]);
					newValue.append(".");

					for (int i = beforeE.length() - numDemicalPoint; i < beforeE.length(); i++) {
						newValue.append(beforeE.toCharArray()[i]);
					}
				} else {
					newValue.append("0.");
					for (int i = 0; i <= numDemicalPoint - 1 - beforeE.length(); i++) {
						newValue.append("0");
					}
					newValue.append(beforeE);
				}
			}

			expression = expression.replace(m.group(0), newValue.toString());
		}
		return expression;
	}

	public static String transformFloatPositiveE(String expression) {
		Matcher m = Pattern.compile("\\d+E\\+\\d+").matcher(expression);
		while (m.find()) {
			String beforeE = expression.substring(0, expression.indexOf("E+"));
			String afterE = expression.substring(expression.indexOf("E+") + 2);

			StringBuilder newValue = new StringBuilder();

			if (Utils.toInt(afterE) != Utils.UNDEFINED_TO_INT) {
				int numDemicalPoint = Utils.toInt(afterE);

				if (numDemicalPoint == 0) {
					newValue = new StringBuilder(beforeE);

				} else {
					newValue = new StringBuilder(beforeE);
					for (int i = 0; i < numDemicalPoint; i++)
						newValue.append("0");
				}
			}

			expression = expression.replace(m.group(0), newValue.toString());
		}
		return expression;
	}

	public static void openFolderorFileOnExplorer(String path) throws OpenFileException {
		if (new File(path).exists()) {
			if (Utils.isWindows()) {
				try {
					Runtime.getRuntime().exec(new String[]{"notepad.exe", new File(path).getName()},
							null,
							new File(path).getParentFile());
				} catch (IOException e) {
					throw new OpenFileException("Unexpected error when opening the target " + path + ". Error code: " + e.getMessage());
				}

			} else if (Utils.isMac()) {
				try {
					Runtime.getRuntime().exec(new String[]{"open", new File(path).getName()},
							null,
							new File(path).getParentFile());
				} catch (IOException e) {
					throw new OpenFileException("Unexpected error when opening the target " + path + ". Error code: " + e.getMessage());
				}

			} else if (Utils.isUnix()) {
				try{
					Runtime.getRuntime().exec(new String[]{"nautilus", new File(path).getName()},
							null,
							new File(path).getParentFile());
				} catch (IOException e) {
					throw new OpenFileException("Unexpected error when opening the target " + path + ". Error code: " + e.getMessage());
				}

			} else {
				throw new OpenFileException("Does not support to open the target " + path + " on this OS");
			}
		}else{
			throw new OpenFileException("The target " + path + " does not exist!");
		}
	}

	/**
	 * Get the reduce index of array item
	 * <p>
	 * Ex: a[1+2][3] --------> [3][3]
	 *
	 * @param arrayItem
	 * @param table
	 * @return
	 * @throws Exception
	 */
	public static String getReducedIndex(String arrayItem, IVariableNodeTable table) throws Exception {
		StringBuilder index = new StringBuilder();
		List<String> indexes = Utils.getIndexOfArray(arrayItem);

		for (String indexItem : indexes) {
			indexItem = ExpressionRewriterUtils.rewrite(table, indexItem);
			index.append(Utils.asIndex(indexItem));
		}
		return index.toString();
	}

	public static String computeMd5(String message){
		String digest = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] hash = md.digest(message.getBytes("UTF-8"));

			//converting byte array to Hexadecimal String
			StringBuilder sb = new StringBuilder(2*hash.length);
			for(byte b : hash){
				sb.append(String.format("%02x", b&0xff));
			}

			digest = sb.toString();

		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace();
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		}
		return digest;
	}

	public static void viewDependency(INode node){
		// export dependency to string
		String dependencyStr = "class " + node.getClass().getSimpleName() + "\n\n";
		for (Dependency d : node.getDependencies()) {
			String content = "[" + d.getClass().getSimpleName() + "]\n";
			content += "start: " + d.getStartArrow().getAbsolutePath() + "\n";
			content += "end: " + d.getEndArrow().getAbsolutePath() + "\n\n";
			if (!dependencyStr.contains(content)) {
				dependencyStr += content;
			}
		}

		if (node instanceof IFunctionNode) {
			dependencyStr += "\n\n----------------\nDependency of arguments:\n";
			for (IVariableNode var : ((IFunctionNode) node).getArguments())
				for (Dependency d : var.getDependencies()) {
					String content = "[" + d.getClass().getSimpleName() + "]\n";
					content += "start: " + d.getStartArrow().getAbsolutePath() + "\n";
					content += "end: " + d.getEndArrow().getAbsolutePath() + "\n\n";
					if (!dependencyStr.contains(content)) {
						dependencyStr += content;
					}
				}

			dependencyStr += "\n\n----------------\nSource code:\n";
			dependencyStr += ((IFunctionNode) node).getAST().getRawSignature();
		}

		// export description of dependency to file, then load on screen
		String tmpFile = new WorkspaceConfig().fromJson().getDependencyDirectory() + File.separator + "dependency_tmp.log";
		Utils.writeContentToFile(dependencyStr, tmpFile);

		// waiting for the generation of dependency file
		int maxLoading = 10;
		while (!new File(tmpFile).exists() && maxLoading >= 0) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			maxLoading--;
		}

		if (new File(tmpFile).exists())
			try {
				logger.debug("open " + tmpFile + " to show dependency of " + node.getAbsolutePath());
				Utils.openFolderorFileOnExplorer(tmpFile);
			} catch (OpenFileException e) {
				e.printStackTrace();
				UIController.showErrorDialog("Can not load dependency of " + node.getAbsolutePath(), "Dependency loader failed", "Dependency loader");
			}
		else {
			UIController.showErrorDialog("Do not find the dependency file " + tmpFile + " of " + node.getAbsolutePath(), "Dependency loader failed", "Dependency loader");
		}
	}

	/**
	 *
	 * @param expectedStartLine the line where we want to put the content in, [1..]
	 * @param expectedNodeOffset the offset where we want to put the content in
	 * @param oldFunction the content
	 * @return
	 */
    public static String insertSpaceToFunctionContent(int expectedStartLine, int expectedNodeOffset, String oldFunction) {
        String addition = "";
        for (int i = 0; i < expectedStartLine - 2; i++) {
            addition += "\n";
        }
        int additionalOffset = expectedNodeOffset - addition.length() - 1;
        for (int i = 0; i < additionalOffset; i++) {
            addition += " ";
        }
        if (expectedStartLine > 1)
        	addition += "\n";
        return addition + oldFunction;
    }

    public static IASTFunctionDefinition disableMacroInFunction(IASTFunctionDefinition astFunctionNode, IFunctionNode functionNode){
		if (astFunctionNode.getFileLocation() == null)
			return astFunctionNode;

//    	if (functionNode != null)
//    		logger.debug("disableMacroInFunction " + functionNode.getAbsolutePath());
//    	else
//    		logger.debug("disableMacroInFunction");

		IASTFunctionDefinition output = null;
		// insert spaces to ensure that the location of new function and of the old function are the same
		try {
			int startLine = astFunctionNode.getFileLocation().getStartingLineNumber();
			int startOffset = astFunctionNode.getFileLocation().getNodeOffset();
			String content = astFunctionNode.getRawSignature();
			if (functionNode != null && (functionNode instanceof ConstructorNode || functionNode instanceof DestructorNode)
					&& functionNode.getParent() instanceof ClassNode) {
				// put the function in a class to void error when constructing ast
				String className = functionNode.getParent().getName();
				content = "class " + className + "{" + content + "};";
				String newContent = Utils.insertSpaceToFunctionContent(startLine,
						startOffset - new String("class " + className + "{").length(), content);
				IASTTranslationUnit unit = new SourcecodeFileParser().getIASTTranslationUnit(newContent.toCharArray());
				//                logger.debug("Reconstructed tree: ");
//                ASTUtils.printTreeFromAstNode(unit, "\t");
				output = (IASTFunctionDefinition) unit.getChildren()[0].
						getChildren()[0].getChildren()[1];

			} else {
				String newContent = Utils.insertSpaceToFunctionContent(startLine, startOffset, content);
				IASTTranslationUnit unit = new SourcecodeFileParser().getIASTTranslationUnit(newContent.toCharArray());
				output = (IASTFunctionDefinition) unit.getChildren()[0];
			}
		} catch (Exception e) {
//			e.printStackTrace();
			output = astFunctionNode;
		} finally {
			// log
			if (output == null || output instanceof CPPASTProblemDeclaration || output.getFileLocation() == null ||
					output.getFileLocation().getStartingLineNumber() != astFunctionNode.getFileLocation().getStartingLineNumber()
							&& output.getFileLocation().getNodeOffset() != astFunctionNode.getFileLocation().getNodeOffset()) {
				logger.error("Fail to instrument [" + astFunctionNode.getClass() + "] content = " + astFunctionNode.getRawSignature());
			}
			return output;
		}
	}
	/**
	 * Create CFG of a function.
     *
     * This function may call to a macro function or not.
     *
     * In case of a call to macro functions, CDT might parse the macro call inside the function, which
     * might lead to the incorrect CFG.
     * For example:
     * #define MACRO_CALL(a) if (a>0) return 1; else return 0;
     * int test(){return MACRO_CALL(a);}
     * Consider test(), we need to get CFG of test() only, without considering the body of MACRO_CALL(a).
     *
     *
     *
     * Therefore, to disable the problem of macro expansion in CFG generation of the function,
     * we need to disable macro.
	 */
	public static ICFG createCFG(IFunctionNode fn, String coverageType) throws Exception {
		if (fn == null)
			return null;
	    // STEP 1: Create a function with disable macro flag
		FunctionNode tmpFunction = new FunctionNode();
		tmpFunction.setAST(disableMacroInFunction(fn.getAST(), fn));
		tmpFunction.setAbsolutePath(tmpFunction.getAbsolutePath());

		// STEP 2: generate CFG of the alternative function
		ICFG cfg = null;
		switch (coverageType) {
			case EnviroCoverageTypeNode.STATEMENT:
			case EnviroCoverageTypeNode.BRANCH:
			case EnviroCoverageTypeNode.STATEMENT_AND_BRANCH:
			case EnviroCoverageTypeNode.BASIS_PATH: {
				cfg = new CFGGenerationforBranchvsStatementvsBasispathCoverage(tmpFunction).generateCFG();
				break;
			}

			case EnviroCoverageTypeNode.MCDC:
			case EnviroCoverageTypeNode.STATEMENT_AND_MCDC: {
				cfg = new CFGGenerationforSubConditionCoverage(tmpFunction).generateCFG();
				break;
			}
		}
		if (cfg != null) {
			cfg.setFunctionNode(tmpFunction);
			cfg.resetVisitedStateOfNodes();
			cfg.setIdforAllNodes();
		}
		return cfg;
	}

	public static String generateVariableDeclaration(String type, String name) {
		String parameterDeclaration;

		List<String> indexes = Utils.getIndexOfArray(type);

		if (indexes.size() > 0) {
			int idx = type.length() - 1;
			while (type.charAt(idx) == SpecialCharacter.CLOSE_SQUARE_BRACE
					|| type.charAt(idx) == SpecialCharacter.OPEN_SQUARE_BRACE
					|| Character.isDigit(type.charAt(idx)))
				idx--;
			parameterDeclaration = type.substring(0, idx + 1) + " " + name;
			for (String index : indexes)
				parameterDeclaration += "[" + index + "]";

		} else {
			parameterDeclaration = type + " " + name;
		}

		return parameterDeclaration;
	}

	public static boolean isWordIn(String sentence, boolean checkDigit, String... words) {
		for (String word : words) {
			if (sentence.equals(word))
				continue;

			if (sentence.contains(word)) {
				int start = sentence.indexOf(word) - 1;
				int end = sentence.indexOf(word) + word.length();
				char startCh = sentence.charAt(start);
				char endCh = sentence.charAt(end);

				if (sentence.startsWith(word)) {
					if ((Character.isDigit(endCh) && checkDigit) || Character.isAlphabetic(endCh))
						return false;
				} if (sentence.endsWith(word)) {
					if ((Character.isDigit(startCh) && checkDigit) || Character.isAlphabetic(startCh))
						return false;
				} else {
					if ((Character.isDigit(endCh) && checkDigit) || Character.isAlphabetic(endCh)
						|| (Character.isDigit(startCh) && checkDigit) || Character.isAlphabetic(startCh))
						return false;
				}

			} else return false;
		}

		return true;
	}


	/**
	 * Ex: "test(a, b)"
	 *
	 * @param functionNode
	 * @return
	 */
	public static StringBuilder generateCallOfArguments(ICommonFunctionNode functionNode){
		StringBuilder functionCall = new StringBuilder();
		functionCall.append("(");
		for (IVariableNode v : functionNode.getArguments())
			if (VariableTypeUtilsForStd.isUniquePtr(v.getRawType()))
				functionCall.append(String.format("std::move(%s),", v.getName()));

			else if (VariableTypeUtils.isNullPtr(v.getRawType())) {
				functionCall.append(NullPointerDataNode.NULL_PTR).append(",");

			} else
				functionCall.append(v.getName()).append(",");
		functionCall.append(")");
		functionCall = new StringBuilder(functionCall.toString().replace(",)", ")") + SpecialCharacter.END_OF_STATEMENT);
		return functionCall;
	}

	public static String getFullFunctionCall(ICommonFunctionNode functionNode) {
		INode realParent = functionNode.getParent();

		if (functionNode instanceof IFunctionNode) {
			INode tmpRealParent = ((IFunctionNode) functionNode).getRealParent();
			if (tmpRealParent != null)
				realParent = tmpRealParent;
		}

		StringBuilder functionCall = new StringBuilder();

		if (realParent instanceof SourcecodeFileNode) {
			functionCall.append(functionNode.getSimpleName())
					.append(generateCallOfArguments(functionNode));

		} else if (realParent instanceof NamespaceNode) {
			// find a list of namespace
			INode namespaceRoot = realParent;
			List<String> namespaces = new ArrayList<>();
			while (namespaceRoot.getParent() != null && namespaceRoot.getParent() instanceof NamespaceNode) {
				namespaces.add(namespaceRoot.getName());
				namespaceRoot = namespaceRoot.getParent();
			}
			namespaces.add(namespaceRoot.getName());

			// generate function call
			StringBuilder scope = new StringBuilder();
			for (String namespace : namespaces)
				scope.insert(0, namespace + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS);

			functionCall.append(scope)
					.append(functionNode.getSimpleName())
					.append(generateCallOfArguments(functionNode));

		} else if (realParent instanceof StructureNode) {
			if (functionNode instanceof ConstructorNode) {
				functionCall = new StringBuilder("new ");
				String type = Search.getScopeQualifier(realParent);
				functionCall.append(type);
			} else {
				String instanceVarName = Search.getScopeQualifier(realParent)
						.replaceAll("[^\\w]", SpecialCharacter.UNDERSCORE);
				instanceVarName = IGTestConstant.INSTANCE_VARIABLE + SpecialCharacter.UNDERSCORE + instanceVarName;

				functionCall = new StringBuilder(instanceVarName);
				functionCall.append(SpecialCharacter.POINT_TO).append(functionNode.getSingleSimpleName());
			}

			functionCall.append(generateCallOfArguments(functionNode));
		}

		return functionCall.toString();
	}

	public static String[] parseIndexesInput(DataNode node, String input) throws Exception {
		final int MAX_INDEX = 50;

		int dimensions;

		if (node instanceof MultipleDimensionDataNode)
			dimensions = ((MultipleDimensionDataNode) node).getDimensions();
		else
			dimensions = 1;

		String[] expandIndexes = new String[dimensions];

		Pattern pattern = Pattern.compile("\\[.*?\\]");
		Matcher matcher = pattern.matcher(input);

		int dim = 0;

		while (matcher.find()) {
			List<String> indexes = new ArrayList<>();
			String[] items = matcher.group().substring(1, matcher.group().length()-1).split(",");

			for (int j = 0; j< items.length; j++) {
				if (items[j].contains("..")) {
					int step = 1;

					if (items[j].contains("/")) {
						step = Integer.parseInt(items[j].substring(items[j].indexOf("/") + 1));
						items[j] = items[j].substring(0, items[j].indexOf("/"));
					}

					String[] bounds = items[j].split("\\Q..\\E");
					int start = bounds[0].isEmpty() ? 0 : Integer.parseInt(bounds[0]);
					int end = -1;

					if (bounds.length == 1) {
						if (node instanceof MultipleDimensionDataNode)
							end = ((MultipleDimensionDataNode) node).getSizes()[dim];
						else if (node instanceof OneDimensionDataNode)
							end = ((OneDimensionDataNode) node).getSize();
						else if (node instanceof PointerDataNode)
							end = ((PointerDataNode) node).getAllocatedSize();
						else if (node instanceof ListBaseDataNode)
							end = ((ListBaseDataNode) node).getSize();
						else if (node instanceof ValueDataNode)
							throw new Exception("Don't support to expand " + ((ValueDataNode) node).getType());
					} else if (bounds.length == 2) {
						end = Integer.parseInt(bounds[1]);
					} else
						throw new Exception("Invalid input");

					if (start < 0 /*|| end > MAX_INDEX*/ || end < 0)
						throw new Exception("Invalid input");

					if (end - start > MAX_INDEX)
						throw new Exception("Expand up to 50 items");

					for (int i = start; i <= end; i+=step)
						if (!indexes.contains(String.valueOf(i)))
							indexes.add(String.valueOf(i));

				} else if (!indexes.contains(items[j])) {
//					if (Integer.parseInt(items[j]) <= MAX_INDEX)
						indexes.add(items[j]);
//					else
//						throw new Exception("Invalid input");
				}
			}

			expandIndexes[dim] = String.join(",", indexes);
			dim++;
		}

		return expandIndexes;
	}

	public static List<String> getAllFiles(String folder){
		try (Stream<Path> walk = Files.walk(Paths.get(folder))) {

			List<String> result = walk.map(x -> x.toString())
					.collect(Collectors.toList());

			return result;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}

	public static double round (double value, int precision) {
		int scale = (int) Math.pow(10, precision);
		return (double) Math.round(value * scale) / scale;
	}
}
