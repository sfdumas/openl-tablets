package org.openl.binding.impl;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundNode;
import org.openl.message.OpenLMessagesUtils;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.CastingMethodCaller;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenConstructor;
import org.openl.types.java.JavaOpenField;
import org.openl.types.java.JavaOpenMethod;

public class BindHelper {

	private BindHelper() {
	}

	public static void processError(ISyntaxNode syntaxNode,
			Throwable throwable, IBindingContext bindingContext) {

		SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(
				throwable, syntaxNode);
		processError(error, bindingContext);
	}

	public static void processError(String message, ISyntaxNode syntaxNode,
			Throwable throwable, IBindingContext bindingContext) {

		SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(
				message, throwable, syntaxNode);
		processError(error, bindingContext);
	}

	public static void processError(CompositeSyntaxNodeException error,
			IBindingContext bindingContext) {
		SyntaxNodeException[] errors = error.getErrors();
		for (SyntaxNodeException e : errors) {
			processError(e, bindingContext);
		}
	}

	public static void processError(SyntaxNodeException error,
			IBindingContext bindingContext) {

		bindingContext.addError(error);
		processError(error);
	}

	public static void processError(Throwable error, ISyntaxNode syntaxNode,
			IBindingContext bindingContext) {
		processError(error, syntaxNode, bindingContext, true);
	}

	public static void processError(Throwable error, ISyntaxNode syntaxNode,
			IBindingContext bindingContext, boolean storeGlobal) {
		SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils
				.createError(error, syntaxNode);
		processSyntaxNodeException(syntaxNodeException, storeGlobal,
				bindingContext);
	}

	public static void processError(String message, ISyntaxNode syntaxNode,
			IBindingContext bindingContext) {
		processError(message, syntaxNode, bindingContext, true);
	}

	public static void processError(String message, ISyntaxNode syntaxNode,
			IBindingContext bindingContext, boolean storeGlobal) {
		SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(
				message, syntaxNode);
		processSyntaxNodeException(error, storeGlobal, bindingContext);
	}

	private static void processSyntaxNodeException(SyntaxNodeException error,
			boolean storeGlobal, IBindingContext bindingContext) {
		bindingContext.addError(error);
		if (storeGlobal) {
			processError(error);
		}
	}

	public static void processError(String message, ISyntaxNode syntaxNode,
			Throwable throwable) {

		SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(
				message, throwable, syntaxNode);
		processError(error);
	}

	public static void processError(String message, ISyntaxNode syntaxNode) {

		SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(
				message, syntaxNode);
		processError(error);
	}

	public static void processError(SyntaxNodeException error) {
		OpenLMessagesUtils.addError(error);
	}

	public static final String CONDITION_TYPE_MESSAGE = "Condition must have boolean type";
	private static final Collection<String> EQUAL_OPERATORS = Collections.unmodifiableCollection(Arrays.asList(
			"op.binary.eq",
			"op.binary.strict_eq",
			"op.binary.le",
			"op.binary.strict_le",
			"op.binary.ge",
			"op.binary.strict_ge"
	));
	private static final Collection<String> NOT_EQUAL_OPERATORS = Collections.unmodifiableCollection(Arrays.asList(
			"op.binary.ne",
			"op.binary.strict_ne",
			"op.binary.lt",
			"op.binary.strict_lt",
			"op.binary.gt",
			"op.binary.strict_gt"
	));

	/**
	 * Checks the condition expression.
	 *
	 * @param conditionNode
	 *            Bound node that represents condition expression.
	 * @param bindingContext
	 *            Binding context.
	 * @return <code>conditionNode</code> in case it is correct, and
	 *         {@link ErrorBoundNode} in case condition is wrong.
	 */
	public static IBoundNode checkConditionBoundNode(IBoundNode conditionNode,
			IBindingContext bindingContext) {
		if (conditionNode != null
				&& !isBooleanType(conditionNode.getType())) {
			if (conditionNode.getType() != NullOpenClass.the) {
				BindHelper.processError(CONDITION_TYPE_MESSAGE,
						conditionNode.getSyntaxNode(), bindingContext, false);
			}
			return new ErrorBoundNode(conditionNode.getSyntaxNode());
		} else {
			if (conditionNode != null) {
				checkForSameLeftAndRightExpression(conditionNode, bindingContext);
			}

			return conditionNode;
		}
	}

    public static void checkOnDeprecation(ISyntaxNode node, IBindingContext context, IMethodCaller caller) {
        if (caller instanceof JavaOpenMethod) {
            Method javaMethod = ((JavaOpenMethod) caller).getJavaMethod();
            if (isDeprecated(javaMethod)) {
                String msg = "DEPRECATED '" + javaMethod.getName() + "' function will be removed in the next version!";
                processWarn(msg, node, context);
            }
        } else if (caller instanceof JavaOpenConstructor) {
            Constructor constr = ((JavaOpenConstructor) caller).getJavaConstructor();
            if (isDeprecated(constr)) {
                String msg = "DEPRECATED '" + constr.getName() + "' constructor will be removed in the next version!";
                processWarn(msg, node, context);
            }
        } else if (caller instanceof CastingMethodCaller) {
            checkOnDeprecation(node, context, caller.getMethod());
        }
    }

    public static void checkOnDeprecation(ISyntaxNode node, IBindingContext context, IOpenClass aClass) {
        if (aClass instanceof JavaOpenClass) {
            Class<?> javaClass = ((JavaOpenClass) aClass).getInstanceClass();
            if (javaClass.isAnnotationPresent(Deprecated.class)) {
                String msg = "DEPRECATED '" + javaClass.getName() + "' class will be removed in the next version!";
                processWarn(msg, node, context);
            }
        } else if (aClass!= null && aClass.isArray()) {
        	checkOnDeprecation(node, context, aClass.getComponentClass());
		}
    }

    public static void checkOnDeprecation(ISyntaxNode node, IBindingContext context, IOpenField filed) {
        if (filed instanceof JavaOpenField) {
            Field javaField = ((JavaOpenField) filed).getJavaField();
            if (isDeprecated(javaField)) {
                String msg = "DEPRECATED '" + javaField.getName() + "' field will be removed in the next version!";
                processWarn(msg, node, context);
            }
        }
    }

	private static <T extends Member & AnnotatedElement> boolean isDeprecated(T javaField) {
		return javaField.isAnnotationPresent(Deprecated.class) || javaField.getDeclaringClass()
                .isAnnotationPresent(Deprecated.class);
	}

	private static void checkForSameLeftAndRightExpression(IBoundNode conditionNode, IBindingContext bindingContext) {
		IBoundNode[] children = conditionNode.getChildren();
		if (children.length == 2) {
			IBoundNode left = children[0];
			IBoundNode right = children[1];
			if (isSame(left, right)) {
				String type = conditionNode.getSyntaxNode().getType();

				if (EQUAL_OPERATORS.contains(type)) {
					BindHelper.processWarn("Condition is always true",
							conditionNode.getSyntaxNode(),
							bindingContext);
				} else if (NOT_EQUAL_OPERATORS.contains(type)) {
					BindHelper.processWarn("Condition is always false",
							conditionNode.getSyntaxNode(),
							bindingContext);
				}
			}
		}
	}

	private static boolean isSame(IBoundNode left, IBoundNode right) {
		if (left instanceof FieldBoundNode && right instanceof FieldBoundNode) {
			if (((FieldBoundNode) left).getBoundField() == ((FieldBoundNode) right).getBoundField()) {
				if (left.getTargetNode() == right.getTargetNode()) {
					return true;
				}
				if (isSame(left.getTargetNode(), right.getTargetNode())) {
					return true;
				}
			}
		} else if (left instanceof LiteralBoundNode && right instanceof LiteralBoundNode) {
			Object leftValue = ((LiteralBoundNode) left).getValue();
			Object rightValue = ((LiteralBoundNode) right).getValue();
			if (leftValue == rightValue || leftValue != null && leftValue.equals(rightValue)) {
				return true;
			}
		}

		return false;
	}

	public static void processWarn(String message, ISyntaxNode source,
			IBindingContext bindingContext) {
		if (bindingContext.isExecutionMode()) {
			OpenLMessagesUtils.addWarn(message);
		} else {
			OpenLMessagesUtils.addWarn(message, source);
		}
	}

	public static IBoundCode makeInvalidCode(IParsedCode parsedCode,
			ISyntaxNode syntaxNode, IBindingContext bindingContext) {

		ErrorBoundNode boundNode = new ErrorBoundNode(syntaxNode);

		return new BoundCode(parsedCode, boundNode, bindingContext.getErrors(),
				bindingContext.getLocalVarFrameSize());
	}

	public static IBoundCode makeInvalidCode(IParsedCode parsedCode,
			ISyntaxNode syntaxNode, SyntaxNodeException[] errors) {

		ErrorBoundNode boundNode = new ErrorBoundNode(syntaxNode);

		return new BoundCode(parsedCode, boundNode, errors, 0);
	}

	public static IBindingContext delegateContext(IBindingContext context,
			IBindingContextDelegator delegator) {

		if (delegator != null) {

			delegator.setTopDelegate(context);

			return delegator;
		}

		return context;
	}

	/**
	 * Analyzes the binding context and returns the name for
	 * internal/temporary/service variable with the name: varNamePrefix + '$' +
	 * available_index.
	 */
	public static String getTemporaryVarName(IBindingContext bindingContext,
			String namespace, String varNamePrefix) {
		int index = 0;
		while (bindingContext.findVar(namespace, varNamePrefix + "$" + index,
				true) != null) {
			index++;
		}
		return varNamePrefix + "$" + index;
	}

	/**
     * Checks given type that it is boolean type.
     *
     * @param type {@link IOpenClass} instance
     * @return <code>true</code> if given type equals
     *         {@link JavaOpenClass#BOOLEAN} or
     *         JavaOpenClass.getOpenClass(Boolean.class); otherwise -
     *         <code>false</code>
     */
    private static boolean isBooleanType(IOpenClass type) {
        return type == null || JavaOpenClass.BOOLEAN == type || JavaOpenClass.getOpenClass(Boolean.class) == type;

    }
}
