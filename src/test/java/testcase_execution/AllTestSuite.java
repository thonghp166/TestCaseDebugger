package testcase_execution;

import org.junit.Rule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        ArrayParameterTest.class,
        ClassParameterTest.class,
        CompoundTest.class,
        PointerParameterTest.class,
        PrimitiveParameterTest.class,
        StructParameterTest.class
})

public class AllTestSuite {
    @Rule
    public Timeout globalTimeout = Timeout.seconds(12);
}