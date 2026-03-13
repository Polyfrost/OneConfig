package org.polyfrost.oneconfig.internal.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.List;

public class ComponentHelper {

    private static final String baselineIndent = indent(4);

    /**
     * Returns a prettified string representation of the given component's toString output.
     */
    public static String prettyPrint(Component component) {
        if (component == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        prettyPrintHelper(component, sb, 0);
        return sb.toString();
    }

    private static void prettyPrintHelper(Component component, StringBuilder sb, int indent) {
        String indentation = indent(indent);
        sb.append(indentation).append(component.getClass().getSimpleName()).append(" {\n");

        //#if MC >= 1.19.2
        //$$ StringBuilder textBuilder = new StringBuilder();
        //$$ component.getContents().visit((content) -> {
        //$$     textBuilder.append(content);
        //$$     return java.util.Optional.empty();
        //$$ });
        //$$
        //$$ String text = textBuilder.toString();
        //#else
        String text = component.getString();
        //#endif
        if (!text.isEmpty()) {
            sb.append(indentation).append(baselineIndent).append("text: ").append('"').append(text).append('"').append("\n");
        }

        Style style = component.getStyle();
        if (style != null) {
            sb.append(indentation)
                    .append(baselineIndent)
                    .append("style: {\n");

            sb.append(indentation)
                    .append(baselineIndent)
                    .append(baselineIndent)
                    .append("color: ")
                    //#if MC >= 1.16.5
                    //$$ .append(style.getColor() != null ? style.getColor().serialize() : "null")
                    //#else
                    .append(style.getColor() != null ? style.getColor().formatValue() : "null")
                    //#endif
                    .append("\n");

            sb.append(indentation)
                    .append(baselineIndent)
                    .append(baselineIndent)
                    .append("bold: ")
                    .append(style.isBold())
                    .append("\n");

            sb.append(indentation)
                    .append(baselineIndent)
                    .append(baselineIndent)
                    .append("italic: ")
                    .append(style.isItalic())
                    .append("\n");

            sb.append(indentation)
                    .append(baselineIndent)
                    .append(baselineIndent)
                    .append("underlined: ")
                    .append(style.isUnderlined())
                    .append("\n");

            sb.append(indentation)
                    .append(baselineIndent)
                    .append(baselineIndent)
                    .append("strikethrough: ")
                    .append(style.isStrikethrough())
                    .append("\n");

            sb.append(indentation)
                    .append(baselineIndent)
                    .append(baselineIndent)
                    .append("obfuscated: ")
                    .append(style.isObfuscated())
                    .append("\n");

            sb.append(indentation)
                    .append(baselineIndent)
                    .append("}")
                    .append("\n");
        }

        List<Component> siblings = component.getSiblings();
        if (!siblings.isEmpty()) {
            sb.append(indentation)
                    .append(baselineIndent)
                    .append("siblings: [\n");

            for (Component sibling : siblings) {
                prettyPrintHelper(sibling, sb, indent + 8);
            }

            sb.append(indentation)
                    .append(baselineIndent)
                    .append("]\n");
        }

        sb.append(indentation).append("}\n");
    }

    private static String indent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append(' ');
        }

        return sb.toString();
    }

}
