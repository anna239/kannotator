package inferenceData.propagation.fullPropagation;

import inferenceData.annotations.ExpectNotNull;
import inferenceData.annotations.ExpectNullable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConflictsAndPropagation {
    public interface A {
        @NotNull @ExpectNullable
        Object test(@ExpectNotNull @Nullable Object o);
    }

    public interface ConflictSource extends A {
        @Nullable
        Object test(@NotNull Object o);
    }

    public interface B extends A {
        Object test(@ExpectNotNull Object o);
    }

    public interface C extends B {
        Object test(@ExpectNotNull Object o);
    }
}
