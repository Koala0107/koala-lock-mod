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
            .connectTimeout(Duration.ofSeconds(7))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private static final Map<String, Identifier> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
    private static final Map<String, Long> RETRY_AFTER = new ConcurrentHashMap<>();

    private ImageFrameTextureCache() { }

    public static Identifier get(String url) {
        String clean = url == null ? "" : url.trim();
        if (clean.isEmpty()) return null;

        Identifier existing = CACHE.get(clean);
        if (existing != null) return existing;

        long now = System.currentTimeMillis();
        Long retry = RETRY_AFTER.get(clean);
        if (retry != null && retry > now) return null;

        if (LOADING.add(clean)) loadAsync(clean);
        return null;
    }

    private static void loadAsync(String url) {
        Thread worker = new Thread(() -> {
            NativeImage image = null;
            try {
                URI uri = URI.create(url);
                String scheme = uri.getScheme();
                if (scheme == null || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))) {
                    scheduleRetry(url);
                    return;
                }

                HttpRequest request = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(15))
                        .header("User-Agent", "Mozilla/5.0 korime_scene-image-frame")
                        .header("Accept", "image/avif,image/webp,image/apng,image/png,image/jpeg,image/*;q=0.8,*/*;q=0.5")
                        .GET()
                        .build();

                HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    scheduleRetry(url);
                    return;
                }

                byte[] bytes = response.body();
                if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) {
                    scheduleRetry(url);
                    return;
                }

                image = NativeImage.read(new ByteArrayInputStream(bytes));
                if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0
                        || image.getWidth() > 4096 || image.getHeight() > 4096) {
                    if (image != null) image.close();
                    scheduleRetry(url);
                    return;
                }

                NativeImage readyImage = image;
                image = null;
                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    try {
                        NativeImageBackedTexture texture = new NativeImageBackedTexture(readyImage);
                        Identifier id = client.getTextureManager().registerDynamicTexture(
                                "image_frame_" + Integer.toHexString(url.hashCode()), texture);
                        CACHE.put(url, id);
                        RETRY_AFTER.remove(url);
                    } catch (Throwable ignored) {
                        readyImage.close();
                        scheduleRetry(url);
                    } finally {
                        LOADING.remove(url);
                    }
                });
                return;
            } catch (Throwable ignored) {
                if (image != null) image.close();
                scheduleRetry(url);
            } finally {
                // Successful registration removes this on the render thread. Every failure
                // must release the URL so the frame can retry instead of getting stuck forever.
                if (!CACHE.containsKey(url)) LOADING.remove(url);
            }
        }, "korime-scene-image-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private static void scheduleRetry(String url) {
        RETRY_AFTER.put(url, System.currentTimeMillis() + 5000L);
    }
}
