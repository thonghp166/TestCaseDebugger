package com.dse.testdata.object;

import com.dse.parser.object.*;
import com.dse.search.Search2;
import com.dse.testdata.Iterator;
import com.dse.testdata.object.stl.ListBaseDataNode;
import com.dse.testdata.object.stl.PairDataNode;
import com.dse.testdata.object.stl.STLArrayDataNode;
import com.dse.util.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent a variable node in the <b>variable tree</b>, example: class,
 * struct, array item, etc.
 *
 * @author DucAnh
 */
public abstract class ValueDataNode extends DataNode implements IValueDataNode {

	/**
	 * The type of variable. Ex: const int&
	 */
	private String type = "";

	/**
	 * The node contains the definition of type's variable. For example, the type of
	 * variable is "Student". This instance returns the node that defines "Student"
	 * (class Student{char* name; ...}).
	 */
	private VariableNode correspondingVar = null;

	/**
	 * global variable
	 */
	private boolean externel = false;

	private boolean isInStaticSolution = false;

	private List<Iterator> iterators;

	public ValueDataNode() {
		iterators = new ArrayList<>();
		iterators.add(new Iterator(this));
	}

	public INode getCorrespondingType() {
		VariableNode correspondingVar = getCorrespondingVar();

		if (correspondingVar == null)
			return null;

		return getCorrespondingVar().resolveCoreType();
	}

	@Override
	public boolean containGetterNode() {
		return this.getCorrespondingVar() != null && this.getCorrespondingVar().getGetterNode() != null;
	}

	@Override
	public boolean containSetterNode() {
		return this.getCorrespondingVar() != null && this.getCorrespondingVar().getSetterNode() != null;
	}

	@Override
	public VariableNode getCorrespondingVar() {
		return this.correspondingVar;
	}

	@Override
	public void setCorrespondingVar(VariableNode correspondingVar) {
		this.correspondingVar = correspondingVar;
	}

	@Override
	public String getDotGetterInStr() {
		String dotAccess = "";
		List<IDataNode> chain = this.getNodesChainFromRoot(this);

		for (IDataNode node : chain)
			if (node instanceof ValueDataNode) {
				ValueDataNode item = (ValueDataNode) node;

				if (item.isArrayElement() || item.isPassingVariable())
					dotAccess += item.getName();
				else
					dotAccess += IDataNode.DOT_ACCESS + item.getName();
			}

		return dotAccess;
	}

	@Override
	public String getDotSetterInStr(String value) {
		return this.getDotGetterInStr() + "=" + value;
	}

	@Override
	public String getGetterInStr() {
		final String METHOD = "()";

		String getter = "";
		List<IDataNode> chain = this.getNodesChainFromRoot(this);

		for (IDataNode node : chain)
			if (node instanceof ValueDataNode) {
				ValueDataNode item = (ValueDataNode) node;

				if (item.isArrayElement() || item.isPassingVariable())
					getter += item.getName();
				else if (item.containGetterNode())
					getter += IDataNode.DOT_ACCESS
							+ item.getCorrespondingVar().getGetterNode().getSingleSimpleName() + METHOD;
				else if (!item.containGetterNode())
					if (item.getCorrespondingVar().isPrivate())
						getter += IDataNode.GETTER_METHOD + Utils.toUpperFirstCharacter(item.getName() + METHOD);
					else
						getter += IDataNode.DOT_ACCESS + item.getName();
			}

		return getter;
	}

	@Override
	public String getAssertionForGoogleTest(String method, String source, String target) throws Exception {
//		if (!getVituralName().startsWith(IGTestConstant.EXPECTED_OUTPUT))
//			throw new Exception("Only expected output value can assert");

		String output = "";

		if (this instanceof ConstructorDataNode)
			return output;

		for (IDataNode child : this.getChildren()) {
			if (child instanceof ValueDataNode)
				output += ((ValueDataNode) child).getAssertionForGoogleTest(method, source, target)
						+ SpecialCharacter.LINE_BREAK;
		}

		output = output.replace(SpecialCharacter.LINE_BREAK + SpecialCharacter.LINE_BREAK, SpecialCharacter.LINE_BREAK);
		output = output.replace(";;",";");

		return output +  SpecialCharacter.LINE_BREAK;
	}

	@Override
	public String getSetterInStr(String nameVar) {
		String setter = "";
		List<IDataNode> chain = this.getNodesChainFromRoot(this);

		/*
		 * Get the getter of the previous variable.For example, we have "front[0]" and
		 * we need to get the setter of this variable.
		 *
		 * The first step, we get the getter of variable "front".
		 */
		String getterOfPreviousNode = "";

		final int MIN_ELEMENTS_IN_CHAIN = 2;

		if (chain.size() >= MIN_ELEMENTS_IN_CHAIN) {
			/*
			 * If the variable belongs is array item, belongs to class/struct/namespace,
			 * etc., the the size of chain is greater than 2
			 */
			IDataNode previousNode = chain.get(chain.size() - 2);
			if (previousNode instanceof ValueDataNode)
				getterOfPreviousNode = ((ValueDataNode) previousNode).getGetterInStr();
		} else {
			// nothing to do
		}
		/*
		 *
		 */
		IDataNode currentNode = chain.get(chain.size() - 1);
		if (currentNode instanceof ValueDataNode) {
			ValueDataNode dataNode = (ValueDataNode) currentNode;

			if (dataNode.isArrayElement())
				return this.getParent().getVituralName() + dataNode.getName() + "=" + nameVar;
			else if (dataNode.isPassingVariable())
				setter = getterOfPreviousNode + dataNode.getName() + "=" + nameVar;
			else if (dataNode.containSetterNode())
				setter = getterOfPreviousNode + IDataNode.DOT_ACCESS
						+ dataNode.getCorrespondingVar().getSetterNode().getSingleSimpleName() + "(" + nameVar + ")";
			else if (!dataNode.containSetterNode())
				if (dataNode.getCorrespondingVar().isPrivate())
					setter = getterOfPreviousNode + IDataNode.SETTER_METHOD
							+ Utils.toUpperFirstCharacter(dataNode.getName() + "(" + nameVar + ")");
				else if (dataNode instanceof OneDimensionCharacterDataNode) {
					String name = getterOfPreviousNode + IDataNode.DOT_ACCESS + dataNode.getName();
					setter = "strcpy(" + name + "," + nameVar + ")";

				} else
					setter = getterOfPreviousNode + IDataNode.DOT_ACCESS + dataNode.getName() + "=" + nameVar;
		}

		return setter;
	}

	@Override
	public String getType() {
		return this.type;
	}

	@Override
	public void setType(String type) {
		this.type = type;
	}

	public boolean isExternel() {
		return externel;
	}

	public void setExternel(boolean _externelVariable) {
		externel = _externelVariable;
	}

	@Override
	public boolean isArrayElement() {
		IDataNode parent = getParent();

		if (!(parent instanceof ValueDataNode))
			return false;

		if (parent instanceof ArrayDataNode || parent instanceof PointerDataNode || parent instanceof STLArrayDataNode)
			return true;

		if (this instanceof SubClassDataNode && parent instanceof ClassDataNode)
			return ((ClassDataNode) parent).isArrayElement();

		return false;
	}

	@Override
	public boolean isElementInString() {
		IDataNode parent = getParent();

		if (!(parent instanceof ValueDataNode))
			return false;

		if (parent instanceof NormalStringDataNode)
			return true;

		return false;
	}

	@Override
	public boolean isSTLListBaseElement() {
		return this.getParent() != null && this.getParent() instanceof ListBaseDataNode;
	}

	@Override
	public boolean isAttribute() {
		if (this instanceof SubClassDataNode) {
				return getParent().getParent() instanceof StructureDataNode || getParent().getParent() instanceof PairDataNode;
		} else if (this instanceof SubprogramNode) {
			if (this instanceof ConstructorDataNode)
				return getParent().getParent().getParent() instanceof StructureDataNode
						|| getParent().getParent().getParent() instanceof PairDataNode;
			else
				return false;
		} else
			return this.getParent() instanceof StructureDataNode || getParent() instanceof PairDataNode;
	}

	public boolean isExpected() {
		IDataNode parent = getParent();

		if (parent == null)
			return false;

		if (!(parent instanceof ValueDataNode))
			return false;

		if (this instanceof SubprogramNode && !(this instanceof ConstructorDataNode))
			return false;

		if (parent instanceof SubprogramNode && !(parent instanceof ConstructorDataNode)) {
			IDataNode grandParent = parent.getParent();

			boolean isReturnVar = getName().equals("RETURN");

			// case test function in sbf unit (<<SBF>>) or stub subprogram (<<STUB>>)
			if (grandParent instanceof RootDataNode)
				return !isReturnVar;
			if (grandParent instanceof UnitNode) {
				boolean isStubUnit = grandParent instanceof StubUnitNode;
				return (isStubUnit && !isReturnVar) ||(!isStubUnit && isReturnVar);
			}

		}

		return ((ValueDataNode) parent).isExpected();
	}

	@Override
	public boolean isInConstructor() {
		return this.getParent() instanceof ConstructorDataNode;
	}

	@Override
	public boolean isInStaticSolution() {
		return this.isInStaticSolution;
	}

	@Override
	public void setInStaticSolution(boolean isInStaticSolution) {
		this.isInStaticSolution = isInStaticSolution;
	}

	@Override
	public boolean isPassingVariable() {
		return this.getParent() != null && !isSutExpectedValue()
				&& (/*this.getParent() instanceof RootDataNode || */this.getParent() instanceof SubprogramNode)
				&& !(this.getParent() instanceof ConstructorDataNode);
	}

	public boolean isStubArgument() {
		if (this instanceof SubprogramNode)
			return false;

		IDataNode grandParent = parent.getParent();

		if (grandParent instanceof StubUnitNode)
			return true;

		if (grandParent instanceof RootDataNode) {
			NodeType rootType = ((RootDataNode) grandParent).getLevel();

			if (rootType == NodeType.STUB || rootType == NodeType.SBF)
				return true;
		}

		return false;
	}

	public boolean isInstance() {
		if (isGlobalExpectedValue())
			return false;

		if (this instanceof ClassDataNode)
			if (correspondingVar instanceof InstanceVariableNode)
				return true;

		if (this instanceof SubClassDataNode) {
			return ((ClassDataNode) getParent()).isInstance();
		}

		return false;
	}

	public boolean isHaveExpectedValue() {
		// subprogram under test case
		if (this instanceof SubprogramNode && !(this instanceof ConstructorDataNode)) {
			IDataNode parent = getParent();
			IDataNode grandParent = parent.getParent();

			return parent instanceof StubUnitNode
					&& grandParent instanceof RootDataNode && ((RootDataNode) grandParent).getLevel() == NodeType.ROOT;
		}

		if (getCorrespondingVar() instanceof ReturnVariableNode)
			return false;

		return ((ValueDataNode) getParent()).isHaveExpectedValue();
	}

	public void setVirtualName() {
		if (this.virtualName != null)
			return;

		String virtualName = "";
		IDataNode parent = getParent();

		if (isExternel()) {
			if (getCorrespondingVar() instanceof InstanceVariableNode)
				virtualName = getName();
			else
				virtualName = getDisplayNameInParameterTree();
		}
		// parameter case
		else if (isPassingVariable()) {
			virtualName = getName();
		}
		// subprogram case
		else if (this instanceof SubprogramNode) {
			virtualName = parent.getVituralName();
		}
		// subclass data node
		else if (this instanceof SubClassDataNode){
			virtualName = parent.getVituralName();
		}
		// virtual name depend on parent's virtual name
		else if (isArrayElement() || isElementInString()) {
			String elementIndex = VariableTypeUtils.getElementIndex(getName());

			String parentVirtualName = parent.getVituralName();

			if (parent instanceof ValueDataNode && ((ValueDataNode) parent).isArrayElement())
				parentVirtualName = parentVirtualName
						.substring(0, parentVirtualName.indexOf(SpecialCharacter.OPEN_SQUARE_BRACE));

			virtualName = parentVirtualName + elementIndex;
		} else if (isAttribute()) {
			virtualName = parent.getVituralName() + SpecialCharacter.DOT + getName();

			if (parent.getVituralName().startsWith(IGTestConstant.INSTANCE_VARIABLE))
				virtualName = parent.getVituralName() + SpecialCharacter.POINT_TO + getName();
		}
		// other data node
		else {
			String parentPrefix = parent.getVituralName();
			virtualName = getName();

			if (!parentPrefix.equals(NON_VALUE))
				virtualName = parentPrefix + SpecialCharacter.UNDERSCORE_CHAR + virtualName;

			virtualName = virtualName.replace(SpecialCharacter.DOT, SpecialCharacter.UNDERSCORE_CHAR);
			virtualName = virtualName.replaceAll("[^\\w_]", SpecialCharacter.EMPTY);
		}

		// expected output
		if (name.equals("RETURN")) {
			UnitNode unit = (UnitNode) getUnit();
			if (unit != null && !(unit instanceof StubUnitNode))
				virtualName = IGTestConstant.EXPECTED_OUTPUT;
		}

		if (isStubArgument()) {
			String normalizeSubprogramName = parent.getDisplayNameInParameterTree()
					.replaceAll("[^\\w]", SpecialCharacter.UNDERSCORE);
			virtualName = IGTestConstant.STUB_PREFIX /*+ normalizeSubprogramName*/ + virtualName;
		}

		if (isSutExpectedArgument()) {
			virtualName = IGTestConstant.EXPECTED_PREFIX /*+ normalizeSubprogramName*/ + virtualName;
		} else if (isGlobalExpectedValue()) {
			virtualName = IGTestConstant.EXPECTED_PREFIX + IGTestConstant.GLOBAL_PREFIX + virtualName;
		}

		if (this instanceof NullPointerDataNode)
			virtualName = "nullptr_t";

		setVituralName(virtualName);
	}

	public boolean isGlobalExpectedValue() {
		RootDataNode globalRoot = Search2.findGlobalRoot(getTestCaseRoot());

		assert globalRoot != null;
		return (globalRoot.getGlobalInputExpOutputMap().containsValue(this));
	}

	public boolean isSutExpectedValue() {
		IDataNode parent = getParent();

		if (!(parent instanceof ValueDataNode))
			return false;

		if (parent instanceof SubprogramNode) {
			SubprogramNode sut = Search2
					.findSubprogramUnderTest(((SubprogramNode) parent).getTestCaseRoot());

			if (sut == parent) {
				if (sut.getParamExpectedOuputs().contains(this))
					return true;
			}
		}

		return ((ValueDataNode) parent).isSutExpectedValue();
	}

	public boolean isSutExpectedArgument() {
		IDataNode parent = getParent();

		if (!(parent instanceof ValueDataNode))
			return false;

		if (parent instanceof SubprogramNode) {
			SubprogramNode sut = Search2
					.findSubprogramUnderTest(((SubprogramNode) parent).getTestCaseRoot());

			if (sut == parent) {
				if (sut.getParamExpectedOuputs().contains(this))
					return true;
			}
		}

		return false;
	}

	public String getDisplayNameInParameterTree() {
		String prefixPath = null;

		Iterator firstIterator = iterators.get(0);

		if (getName().startsWith(IGTestConstant.INSTANCE_VARIABLE))
			return getType() + " Instance";

		prefixPath = getName() + "";

		INode originalVar = getCorrespondingVar();

		if (originalVar instanceof ReturnVariableNode) {
			prefixPath = "return";
		}

		if (originalVar instanceof ExternalVariableNode) {
			INode currentVar = originalVar.getParent();

			while ((currentVar instanceof StructureNode || currentVar instanceof NamespaceNode)) {
				prefixPath = currentVar.getNewType() + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + prefixPath;
				currentVar = currentVar.getParent();
			}
		}

		for (int i = 0; i < iterators.size(); i++) {
			Iterator iterator = iterators.get(i);

			if (iterator.getDataNode() == this) {
				if (i != 0 || iterator.getRepeat() != Iterator.FILL_ALL) {
					prefixPath += String.format(" [%s]", iterator.getDisplayName());
				}

				break;
			}
		}

		return prefixPath;
	}



	public List<Iterator> getIterators() {
		return iterators;
	}

	public void setIterators(List<Iterator> iterators) {
		this.iterators = iterators;
	}

	public Iterator getCorrespondingIterator() {
		if (isStubArgument()) {
			return iterators.stream().filter(i -> i.getDataNode() == this).findFirst().orElse(null);
		} else {
			if (parent instanceof ValueDataNode) {
				return ((ValueDataNode) parent).getCorrespondingIterator();
			} else
				return null;
		}
	}

	@Override
	public ValueDataNode clone() {
		ValueDataNode clone = null;

		try {
			clone = getClass().newInstance();
			clone.setName(getName()+"");
			clone.setParent(getParent());
			clone.setType(getType()+"");
			clone.setCorrespondingVar(getCorrespondingVar());
			clone.setExternel(isExternel());
			clone.setInStaticSolution(isInStaticSolution());
			clone.setIterators(iterators);
		} catch (IllegalAccessException | InstantiationException e) {
			e.printStackTrace();
		}

		return clone;
	}
}
