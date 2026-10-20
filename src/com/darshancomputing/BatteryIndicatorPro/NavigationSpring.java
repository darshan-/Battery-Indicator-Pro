package com.darshancomputing.BatteryIndicatorPro;

/** Exact critically damped spring; retargeting preserves presentation and velocity. */
final class NavigationSpring {
    double position, velocity, target;
    private final double frequency;

    NavigationSpring(double position, double frequency) {
        this.position = this.target = position;
        this.frequency = frequency;
    }

    void retarget(double target) {
        this.target = target;
    }

    void snap(double position) {
        this.position = this.target = position;
        velocity = 0;
    }

    boolean isAtRest() {
        return Math.abs(position - target) < 0.0005 && Math.abs(velocity) < 0.01;
    }

    void advance(double seconds) {
        if (!(seconds > 0)) return;
        double displacement = position - target;
        double coefficient = velocity + frequency * displacement;
        double decay = Math.exp(-frequency * seconds);
        position = target + (displacement + coefficient * seconds) * decay;
        velocity = (velocity - frequency * coefficient * seconds) * decay;
        if (isAtRest()) snap(target);
    }
}
