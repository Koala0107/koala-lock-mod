package kr.koala.korime_scene;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ImageFrameTextureCache {
    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final Map<String, Identifier> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();

    private ImageFrameTextureCache() { }

    public static Identifier get(String url) {
        if (url == null || url.isBlank()) return null;
        Identifier existing = CACHE.get(url);
        if (existing != null) return existing;
        if (LOADING.add(url)) loadAsync(url);
        return null;
    }

    private static void loadAsync(String url) {
        Thread.startVirtualThread(() -> {
            try {
                URI uri = URI.create(url.trim());
                String scheme = uri.getScheme();
                if (scheme == null || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))) return;

                HttpRequest request = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(10))
                        .header("User-Agent", "korime_scene/1.8 image-frame")
                        .GET()
                        .build();
                HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() < 200 || response.statusCode() >= 300) return;
                byte[] bytes = response.body();
                if (bytes.length == 0 || bytes.length > MAX_BYTES) return;

                NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
                if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) return;
                if (image.getWidth() > 4096 || image.getHeight() > 4096) {
                    image.close();
                    return;
                }

                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                    Identifier id = client.getTextureManager().registerDynamicTexture("image_frame_" + Integer.toHexString(url.hashCode()), texture);
                    CACHE.put(url, id);
                    LOADING.remove(url);
                });
                return;
            } catch (Throwable ignored) {
            }
            LOADING.remove(url);
        });
    }
}
