package org.openl.rules.testmethod;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class ExpectedErrorTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testUserExceptionSupport1() {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>("test/rules/testmethod/ExpectedErrorTest.xls");
        engineFactory.setExecutionMode(false);
        IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        final CompiledOpenClass compiledOpenClass = engineFactory.getCompiledOpenClass();

        assertFalse("There are compilation errors in test", compiledOpenClass.hasErrors());

        IOpenClass openClass = compiledOpenClass.getOpenClass();
        Object target = openClass.newInstance(env);

        IOpenMethod helloTest = openClass.getMethod("HelloTest", new IOpenClass[0]);
        TestUnitsResults res = (TestUnitsResults) helloTest.invoke(target, new Object[0], env);
        ArrayList<TestUnit> testUnits = res.getTestUnits();

        assertEquals("Expected Good Evening", TestUnitResultComparator.TestStatus.TR_OK.getStatus(), testUnits.get(0).compareResult());
        assertEquals("Expected user error 'Incorrect argument'", TestUnitResultComparator.TestStatus.TR_OK.getStatus(), testUnits.get(1).compareResult());
        assertEquals("Expected user error comparison failure", TestUnitResultComparator.TestStatus.TR_NEQ.getStatus(), testUnits.get(2).compareResult());
        assertEquals("Unexpected exception must be thrown. It can't be compared with user error", TestUnitResultComparator.TestStatus.TR_EXCEPTION.getStatus(), testUnits.get(3).compareResult());
        assertEquals("Unexpected exception must be thrown. It can't be compared with user error", TestUnitResultComparator.TestStatus.TR_EXCEPTION.getStatus(), testUnits.get(4).compareResult());
    }

}
