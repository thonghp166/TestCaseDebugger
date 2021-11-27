package com.dse.testdata.object;

/**
 * Represent variable as array (one dimension, two dimension, etc.)
 *
 * @author ducanhnguyen
 */
public abstract class ArrayDataNode extends ValueDataNode {
	public static final int UNDEFINED_SIZE = -1;
	//Hoan
	private boolean sizeIsSet = false;
	private boolean isFixedSize = false;

	public void setSizeIsSet(boolean sizeIsSet) {
		this.sizeIsSet = sizeIsSet;
	}
//	public boolean isSizeIsSet() {
//		return this.sizeIsSet;
//	}

	public boolean isFixedSize() {
		return isFixedSize;
	}
	public boolean isSetSize() {
		return sizeIsSet;
	}

	public void setFixedSize(boolean fixedSize) {
		isFixedSize = fixedSize;
	}

	@Override
	public String generareSourcecodetoReadInputFromFile() throws Exception {
		return "";
	}

	@Override
	public ArrayDataNode clone() {
		ArrayDataNode clone = (ArrayDataNode) super.clone();
		clone.isFixedSize = isFixedSize;

		if (isFixedSize)
			clone.sizeIsSet = sizeIsSet;

		return clone;
	}
}
