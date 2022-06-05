package cc.polyfrost.oneconfig.config.compatibility.vigilance;

import gg.essential.vigilance.data.PropertyCollector;

/**
 * Interface for accessing the {@link PropertyCollector} in a Vigilant config.
 * <p>Not recommended for non-internal OneConfig usage, as Systemless will get really angry at us</p>
 */
public interface VigilantAccessor {
    PropertyCollector getPropertyCollector();
}
