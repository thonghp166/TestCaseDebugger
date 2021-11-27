package com.dse.testdata.object;

import com.dse.guifx_v3.helps.Environment;
import com.dse.parser.object.INode;
import com.dse.parser.object.StructNode;
import com.dse.parser.object.StructTypedefNode;
import com.dse.util.IGTestConstant;
import com.dse.util.SpecialCharacter;
import com.dse.util.Utils;
import com.dse.util.VariableTypeUtils;
import org.eclipse.cdt.core.dom.ast.IASTNode;

/**
 * Represent variable as pointer (one level, two level, etc.)
 *
 * @author ducanhnguyen
 */
public abstract class PointerDataNode extends ValueDataNode {
	public static final int NULL_VALUE = -1;

	protected int level;

	/**
	 * The allocated size, including '\0'.
	 *
	 * Ex1: node="xyz" ---> allocatedSize = 4 <br/>
	 * Ex2: node="" ---> allocatedSize = 1
	 */
	private int allocatedSize;

	private boolean sizeIsSet = false;

	public boolean isSetSize() {
		return sizeIsSet;
	}

	public void setSizeIsSet(boolean sizeIsSet) {
		this.sizeIsSet = sizeIsSet;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getAllocatedSize() {
		return this.allocatedSize;
	}

	public void setAllocatedSize(int allocatedSize) {
		this.allocatedSize = allocatedSize;
	}

	public boolean isNotNull() {
		return this.allocatedSize >= 1;
	}

	@Override
	public String generareSourcecodetoReadInputFromFile() throws Exception {
		StringBuilder output = new StringBuilder();
		for (IDataNode child : this.getChildren())
			output.append(child.generareSourcecodetoReadInputFromFile());
		return output.toString();
	}

	@Override
	public String getInputForGoogleTest() throws Exception {
		if (Environment.getInstance().isC())
			return getCInput();
		else
			return getCppInput();
	}

	private String getCInput() throws Exception {
		String input = "";

		String type = VariableTypeUtils
				.deleteStorageClasses(getType().replace(IDataNode.REFERENCE_OPERATOR, ""));

		String coreType = "";
		if (getChildren() != null && !getChildren().isEmpty())
			coreType = ((ValueDataNode) getChildren().get(0)).getType();
		else
			coreType = type.substring(0, type.lastIndexOf('*'));

		if (this instanceof PointerStructureDataNode) {
			if (type.contains(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS)) {
				int index = type.lastIndexOf(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS) + 2;
				type = type.substring(index);
			}
			if (coreType.contains(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS)) {
				int index = coreType.lastIndexOf(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS) + 2;
				coreType = coreType.substring(index);
			}

			INode correspondingType = getCorrespondingType();
			if (correspondingType instanceof StructNode && !(correspondingType instanceof StructTypedefNode)) {
				if (!type.startsWith("struct"))
					type = "struct " + type;
				if (!coreType.startsWith("struct"))
					coreType = "struct " + coreType;
			}
		}

		if (isExternel())
			type = "";

		if (isPassingVariable() || isSTLListBaseElement() || isInConstructor() || isGlobalExpectedValue() || isSutExpectedArgument()) {
			String allocation = "";

			if (this.isNotNull())
				allocation = String.format("%s %s = malloc(%d * sizeof(%s))" + SpecialCharacter.END_OF_STATEMENT,
						type, this.getVituralName(), this.getAllocatedSize(), coreType);
			else {
				allocation = String.format("%s %s = " + IDataNode.NULL_POINTER_IN_C + SpecialCharacter.END_OF_STATEMENT,
						type, this.getVituralName());
			}
			input += allocation;
		} else if (isArrayElement() || isAttribute()) {
			String allocation;

			if (this.isNotNull())
				allocation = String.format("%s = malloc(%d * sizeof(%s))" + SpecialCharacter.END_OF_STATEMENT,
						this.getVituralName(), this.getAllocatedSize(), coreType);
			else
				allocation = String.format("%s = " + IDataNode.NULL_POINTER_IN_C + SpecialCharacter.END_OF_STATEMENT
						, this.getVituralName());
			input += allocation;
		} else {
			if (this.isNotNull())
				input = String.format("%s = malloc(%d * sizeof(%s))" + SpecialCharacter.END_OF_STATEMENT,
						this.getVituralName(), this.getAllocatedSize(), coreType);
			else
				input += String.format("%s = " + IDataNode.NULL_POINTER_IN_C + SpecialCharacter.END_OF_STATEMENT
						, this.getVituralName());
		}

		return input + SpecialCharacter.LINE_BREAK + super.getInputForGoogleTest();
	}

	private String getCppInput() throws Exception {
		String input = "";

		String type = VariableTypeUtils
				.deleteStorageClasses(getType().replace(IDataNode.REFERENCE_OPERATOR, ""));

		String coreType = "";
		if (getChildren() != null && !getChildren().isEmpty())
			coreType = ((ValueDataNode) getChildren().get(0)).getType();
		else
			coreType = type.substring(0, type.lastIndexOf('*'));

		if (isExternel())
			type = "";

		if (isPassingVariable() || isSTLListBaseElement() || isInConstructor() || isGlobalExpectedValue() || isSutExpectedArgument()) {
			String allocation = "";

			if (this.isNotNull())
				allocation = String.format("%s %s = new %s[%s]" + SpecialCharacter.END_OF_STATEMENT, type,
						this.getVituralName(), coreType, this.getAllocatedSize());
			else {
				allocation = String.format("%s %s = " + IDataNode.NULL_POINTER_IN_CPP + SpecialCharacter.END_OF_STATEMENT,
						type, this.getVituralName());
			}
			input += allocation;
		} else if (isArrayElement() || isAttribute()) {
			String allocation;

			if (this.isNotNull())
				allocation = String.format("%s = new %s[%s]" + SpecialCharacter.END_OF_STATEMENT,
						this.getVituralName(), coreType, this.getAllocatedSize());
			else
				allocation = String.format("%s = " + IDataNode.NULL_POINTER_IN_CPP + SpecialCharacter.END_OF_STATEMENT
						, this.getVituralName());
			input += allocation;
		} else {
			if (this.isNotNull())
				input += getVituralName() + " = new " + coreType + Utils.asIndex(this.getAllocatedSize())
						+ SpecialCharacter.END_OF_STATEMENT;
			else
				input += getVituralName() + " = " + IDataNode.NULL_POINTER_IN_CPP + SpecialCharacter.END_OF_STATEMENT;
		}

		return input + SpecialCharacter.LINE_BREAK + super.getInputForGoogleTest();
	}

	@Override
	public String getAssertionForGoogleTest(String method, String source, String target) throws Exception {
		if (!isNotNull() && isSetSize()) {
			String assertion = "";

			String actualOutputName = getVituralName().replace(source, target);

			assertion += method + "(" + actualOutputName + "," + "nullptr" + ")" + IGTestConstant.LOG_FUNCTION_CALLS;

			return assertion;
		}

		return super.getAssertionForGoogleTest(method, source, target);
	}

	@Override
	public PointerDataNode clone() {
		PointerDataNode clone = (PointerDataNode) super.clone();
		clone.level = level;
		return clone;
	}

	protected String superSuperInputGTest() throws Exception {
		String output = "";
		boolean explanationEnabled = true;

		if (this.getChildren() != null)
			for (IDataNode child : this.getChildren()) {
				if (explanationEnabled) {
					output += SpecialCharacter.LINE_BREAK + "/* " + child.getClass().getSimpleName() + " " + child.getName() + " */" + SpecialCharacter.LINE_BREAK;
				}
				output += child.getInputForGoogleTest() + SpecialCharacter.LINE_BREAK;
			}

		if (explanationEnabled) {
			output = output.replace(SpecialCharacter.LINE_BREAK + SpecialCharacter.LINE_BREAK, SpecialCharacter.LINE_BREAK);
			output = output.replace(SpecialCharacter.LINE_BREAK + SpecialCharacter.LINE_BREAK, SpecialCharacter.LINE_BREAK);
		} else {
			//output = output.replace(SpecialCharacter.LINE_BREAK, "");
			output = output.replace(SpecialCharacter.LINE_BREAK, ""); // note: the comment in test case script must be put in '/*' and '*/', not '//'
			output = output.replace(";;",";");
		}
		return output +  SpecialCharacter.LINE_BREAK;
	}

}
