/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.vm;

import org.openl.IOpenDebugger;
import org.openl.IOpenRunner;
import org.openl.IOpenVM;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.OpenLRuntimeException;
import org.openl.util.fast.FastStack;

/**
 * @author snshor
 * 
 */
public class SimpleVM implements IOpenVM {

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.IOpenVM#debug(org.openl.binding.IBoundCode)
     */
    public IOpenDebugger getDebugger() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.IOpenVM#run(org.openl.binding.IBoundCode)
     */
    public IOpenRunner getRunner() {
        return new SimpleRunner();
    }

    static class SimpleRunner implements IOpenRunner {
        SimpleRunner() {
        }

        public Object run(IBoundMethodNode node, Object[] params, IRuntimeEnv env) throws OpenLRuntimeException {
            int frameSize = node.getLocalFrameSize();

            Object[] frame = new Object[frameSize];

            if (params != null && params.length > 0)
                System.arraycopy(params, 0, frame, 0, params.length);

            try {
                env.pushLocalFrame(frame);
                return node.evaluate(env);
            } finally {
                env.popLocalFrame();
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.openl.IOpenRunner#run(java.lang.Object[])
         */
        public Object run(IBoundMethodNode node, Object[] params) throws OpenLRuntimeException {
            int frameSize = node.getLocalFrameSize();

            return node.evaluate(new SimpleRuntimeEnv(this, frameSize, params));
        }

        // public Object run2(Object[] params)
        // {
        //
        // return evaluate(node, new SimpleRuntimeEnv(frameSize));
        // }

        // Object evaluate(IBoundNode bnode, IRuntimeEnv env)
        // {
        //    	
        // IBoundNode targetNode = bnode.getTargetNode();
        // Object target = targetNode == null ? null : evaluate(targetNode,
        // env);
        //    		
        //    	
        // IBoundNode[] children = bnode.getChildren();
        // Object[] res = null;
        // if (children != null)
        // {
        // res = new Object[children.length];
        // for (int i = 0; i < res.length; i++)
        // {
        // res[i] = evaluate(children[i], env);
        // }
        // }
        // return bnode.evaluate(target != null ? target : env, res, env);
        // }

    }

    static class SimpleRuntimeEnv implements IRuntimeEnv {

        IOpenRunner runner;

        FastStack thisStack = new FastStack(100);

        FastStack frameStack = new FastStack(100);

        SimpleRuntimeEnv() {
            this(new SimpleRunner(), 0, new Object[] {});

        }

        SimpleRuntimeEnv(IOpenRunner runner, int frameSize, Object[] params) {
            Object[] aLocalFrame = new Object[frameSize];
            this.runner = runner;

            System.arraycopy(params, 0, aLocalFrame, 0, params.length);
            pushLocalFrame(aLocalFrame);

        }

        /*
         * (non-Javadoc)
         * 
         * @see org.openl.vm.IRuntimeEnv#getLocalFrame()
         */
        public Object[] getLocalFrame() {
            return (Object[]) frameStack.peek();
        }

        public IOpenRunner getRunner() {
            return runner;
        }

        public Object getThis() {
            if (thisStack.size() == 0)
                return null;
            return thisStack.peek();
        }

        public Object popThis() {
            return thisStack.pop();
        }

        public void pushThis(Object thisObject) {
            thisStack.push(thisObject);
        }

        public Object[] popLocalFrame() {
            return (Object[]) frameStack.pop();
        }

        public void pushLocalFrame(Object[] frame) {
            frameStack.push(frame);
        }

    }

    public IRuntimeEnv getRuntimeEnv() {
        return new SimpleRuntimeEnv();
    }

}
