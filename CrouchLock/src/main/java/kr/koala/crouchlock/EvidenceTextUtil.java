package kr.koala.crouchlock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class EvidenceTextUtil {
    private EvidenceTextUtil() { }

    public static Text parse(String raw) {
        if (raw == null) return Text.empty();
        String trimmed = raw.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return Text.literal(raw);
        }

        try {
            JsonObject obj = JsonParser.parseString(trimmed).getAsJsonObject();
            if (!obj.has("text")) return Text.literal(raw);

            MutableText text = Text.literal(obj.get("text").getAsString());
            if (obj.has("color")) {
                Formatting color = Formatting.byName(obj.get("color").getAsString().toLowerCase());
                if (color != null) text.formatted(color);
            }
            if (obj.has("bold") && obj.get("bold").getAsBoolean()) text.formatted(Formatting.BOLD);
            if (obj.has("italic") && obj.get("italic").getAsBoolean()) text.formatted(Formatting.ITALIC);
            if (obj.has("underlined") && obj.get("underlined").getAsBoolean()) text.formatted(Formatting.UNDERLINE);
            if (obj.has("strikethrough") && obj.get("strikethrough").getAsBoolean()) text.formatted(Formatting.STRIKETHROUGH);
            if (obj.has("obfuscated") && obj.get("obfuscated").getAsBoolean()) text.formatted(Formatting.OBFUSCATED);
            return text;
        } catch (RuntimeException ignored) {
            return Text.literal(raw);
        }
    }
}
