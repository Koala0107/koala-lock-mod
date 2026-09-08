package kr.koala.crouchlock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class EvidenceTextUtil {
    private EvidenceTextUtil() { }

    public static Text parse(String raw) {
        if (raw == null || raw.isEmpty()) return Text.empty();
        String trimmed = raw.trim();

        try {
            JsonElement root = JsonParser.parseString(trimmed);
            return parseElement(root, 0);
        } catch (RuntimeException ignored) {
            return Text.literal(raw);
        }
    }

    private static MutableText parseElement(JsonElement element, int depth) {
        if (depth > 16 || element == null || element.isJsonNull()) return Text.empty();

        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isString()) {
                String value = element.getAsString();
                String nested = value.trim();
                if (depth < 3 && ((nested.startsWith("{") && nested.endsWith("}")) ||
                        (nested.startsWith("[") && nested.endsWith("]")))) {
                    try {
                        return parseElement(JsonParser.parseString(nested), depth + 1);
                    } catch (RuntimeException ignored) { }
                }
                return Text.literal(value);
            }
            return Text.literal(element.getAsString());
        }

        if (element.isJsonArray()) {
            MutableText result = Text.empty();
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) result.append(parseElement(child, depth + 1));
            return result;
        }

        JsonObject obj = element.getAsJsonObject();
        MutableText result = Text.empty();
        if (obj.has("text") && !obj.get("text").isJsonNull()) {
            result = Text.literal(obj.get("text").getAsString());
        }

        if (obj.has("color") && obj.get("color").isJsonPrimitive()) {
            Formatting color = Formatting.byName(obj.get("color").getAsString().toLowerCase());
            if (color != null) result.formatted(color);
        }
        if (bool(obj, "bold")) result.formatted(Formatting.BOLD);
        if (bool(obj, "italic")) result.formatted(Formatting.ITALIC);
        if (bool(obj, "underlined")) result.formatted(Formatting.UNDERLINE);
        if (bool(obj, "strikethrough")) result.formatted(Formatting.STRIKETHROUGH);
        if (bool(obj, "obfuscated")) result.formatted(Formatting.OBFUSCATED);

        if (obj.has("extra") && obj.get("extra").isJsonArray()) {
            for (JsonElement child : obj.getAsJsonArray("extra")) {
                result.append(parseElement(child, depth + 1));
            }
        }
        return result;
    }

    private static boolean bool(JsonObject obj, String key) {
        try {
            return obj.has(key) && obj.get(key).isJsonPrimitive() && obj.get(key).getAsBoolean();
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
