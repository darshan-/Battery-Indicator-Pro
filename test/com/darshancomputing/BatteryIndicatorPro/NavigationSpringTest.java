package com.darshancomputing.BatteryIndicatorPro;

/** Standalone JVM checks: compile only this test and NavigationSpring.java. */
public final class NavigationSpringTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        for (int fps : new int[] {15, 30, 60, 120, 240}) {
            NavigationSpring spring = new NavigationSpring(0, 18);
            spring.retarget(2);
            double previous = 0;
            for (int frame = 0; frame < fps * 2; frame++) {
                spring.advance(1.0 / fps);
                check(spring.position >= previous && spring.position <= 2, "Overshoot at " + fps);
                previous = spring.position;
            }
            check(spring.position == 2 && spring.isAtRest(), "Did not settle at " + fps);
        }

        NavigationSpring regular = new NavigationSpring(0, 18);
        NavigationSpring dropped = new NavigationSpring(0, 18);
        regular.retarget(2);
        dropped.retarget(2);
        for (int i = 0; i < 15; i++) regular.advance(1.0 / 60);
        dropped.advance(0.25);
        check(Math.abs(regular.position - dropped.position) < 1e-10, "Dropped-frame position changed");
        check(Math.abs(regular.velocity - dropped.velocity) < 1e-10, "Dropped-frame velocity changed");

        double presentation = regular.position, velocity = regular.velocity;
        regular.retarget(0);
        check(regular.position == presentation && regular.velocity == velocity, "Retarget reset motion");
        regular.advance(0);
        regular.advance(-0.1);
        check(regular.position == presentation && regular.velocity == velocity, "Invalid time advanced motion");
        for (int i = 0; i < 120; i++) {
            if (i % 7 == 0) regular.retarget((i / 7) % 3);
            regular.advance(1.0 / 60);
            check(Double.isFinite(regular.position) && Double.isFinite(regular.velocity), "Reversal diverged");
        }
        regular.snap(1);
        check(regular.position == 1 && regular.target == 1 && regular.velocity == 0, "Reduced-motion snap failed");

        NavigationSpring flick = new NavigationSpring(0.4, 18);
        flick.velocity = 2;
        flick.retarget(1);
        flick.advance(1.0 / 60);
        check(flick.position > 0.4, "Release velocity was not carried");
        System.out.println("Navigation spring checks passed: frame rates, dropped frames, retargets, reversal, snap and velocity.");
    }
}
