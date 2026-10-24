package org.polyfrost.oneconfig.api.config.v1;

public interface SliderBounds {
    default float min(float annotatedMin) {
        return annotatedMin;
    }

    default float max(float annotatedMax) {
        return annotatedMax;
    }
}
