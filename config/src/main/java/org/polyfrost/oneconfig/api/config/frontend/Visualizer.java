package org.polyfrost.oneconfig.api.config.frontend;

import org.polyfrost.oneconfig.api.config.Tree;

@FunctionalInterface
public interface Visualizer {
    boolean display(Tree tree);
}
