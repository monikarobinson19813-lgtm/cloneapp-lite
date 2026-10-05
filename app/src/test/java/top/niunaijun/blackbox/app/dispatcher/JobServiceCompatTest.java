package top.niunaijun.blackbox.app.dispatcher;

import org.junit.Test;
import static org.junit.Assert.assertSame;

public class JobServiceCompatTest {
    private interface CompatEngine {}
    private static final class Engine implements CompatEngine {}
    private static class BaseShape {
        private final CompatEngine engine;
        BaseShape(CompatEngine engine) { this.engine = engine; }
    }
    private static final class ChildShape extends BaseShape {
        ChildShape(CompatEngine engine) { super(engine); }
    }

    @Test
    public void findsAssignableRuntimeValueInPrivateSuperclassField() {
        Engine engine = new Engine();
        assertSame(engine, JobServiceCompat.findAssignableFieldValue(new ChildShape(engine), Engine.class));
    }
}
