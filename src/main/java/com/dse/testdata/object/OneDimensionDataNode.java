package com.dse.testdata.object;

import auto_testcase_generation.testdatagen.testdatainit.VariableTypes;
import com.dse.parser.object.VariableNode;
import com.dse.testdata.gen.module.TreeExpander;
import com.dse.util.IGTestConstant;
import com.dse.util.SpecialCharacter;
import com.dse.util.VariableTypeUtils;

/**
 * Represent variable as one dimension array.
 *
 * @author ducanhnguyen
 */
public abstract class OneDimensionDataNode extends ArrayDataNode {
	/**
	 * The size of array
	 */
	private int size = UNDEFINED_SIZE; // unspecified size

	public boolean canConvertToString() {
		return false;
	}

	public int getSize() {
		return this.size;
	}

	@Override
	public void setCorrespondingVar(VariableNode correspondingVar) {
		super.setCorrespondingVar(correspondingVar);
		if (VariableTypeUtils.isOneDimension(correspondingVar.getRawType()))
			size = correspondingVar.getSizeOfArray();
//		INode type = ResolveCoreTypeHelper.resolve(correspondingVar);
//		super.setCorrespondingType(type);
	}

	public void setSize(int size) {
		if (size >= 0) {
			setSizeIsSet(true);
		}
		this.size = size;
	}

	@Override
	public String generateInputToSavedInFile() {
		StringBuilder output = new StringBuilder();
		for (IDataNode child : getChildren())
			if (child instanceof NormalDataNode)
				if (((NormalDataNode) child).getValue() != null) {
					output.append(child.getName()).append("=").append(((NormalDataNode) child).getValue()).append(SpecialCharacter.LINE_BREAK);
				}
		output.append("sizeof(").append(getName()).append(")=").append(getSize()).append(SpecialCharacter.LINE_BREAK);
		return output.toString();
	}

	@Override
	public String getAssertionForGoogleTest(String method, String source, String target) throws Exception {
		if (size <= 0 && isSetSize()) {
			String assertion = "";

			String actualOutputName = getVituralName().replace(source, target);

			assertion += method + "(" + actualOutputName + "," + "nullptr" + ")" + IGTestConstant.LOG_FUNCTION_CALLS;

			return assertion;
		}

		return super.getAssertionForGoogleTest(method, source, target);
	}

	/**
	 * Example:
	 * int[3] ---> true
	 *
	 * int[] ---> false
	 * @return
	 */
	public boolean isConstrainedArray(){
		return !getType().contains("[]");
	}

	@Override
	public OneDimensionDataNode clone() {
		OneDimensionDataNode clone = (OneDimensionDataNode) super.clone();

		if (isFixedSize()) {
			clone.size = size;
			try {
				new TreeExpander().expandTree(clone);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return clone;
	}
}
