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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ImageFrameTextureCache {
    public record TextureInfo(Identifier id, int width, int height) { }

    private static final int MAX_BYTES = 16 * 1024 * 1024;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(7))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private static final Map<String, TextureInfo> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
    private static final Map<String, Long> RETRY_AFTER = new ConcurrentHashMap<>();

    private ImageFrameTextureCache() { }

    public static TextureInfo getInfo(String url) {
        String clean = url == null ? "" : url.trim();
        if (clean.isEmpty()) return null;

        TextureInfo existing = CACHE.get(clean);
        if (existing != null) return existing;

        long now = System.currentTimeMillis();
        Long retry = RETRY_AFTER.get(clean);
        if (retry != null && retry > now) return null;

        if (LOADING.add(clean)) loadAsync(clean);
        return null;
    }

    public static Identifier get(String url) {
        TextureInfo info = getInfo(url);
        return info == null ? null : info.id();
    }

    private static void loadAsync(String cacheKey) {
        Thread worker = new Thread(() -> {
            NativeImage image = null;
            try {
                String downloadUrl = normalizeDownloadUrl(cacheKey);
                URI uri = URI.create(downloadUrl);
                String scheme = uri.getScheme();
                if (scheme == null || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))) {
                    scheduleRetry(cacheKey);
                    return;
                }

                HttpRequest request = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(15))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/140 Safari/537.36")
                        // NativeImage can decode PNG/JPEG but Discord's media proxy may return
                        // WebP/AVIF, so prefer formats Minecraft can read directly.
                        .header("Accept", "image/png,image/jpeg,image/*;q=0.8,*/*;q=0.5")
                        .GET()
                        .build();

                HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    scheduleRetry(cacheKey);
                    return;
                }

                byte[] bytes = response.body();
                if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) {
                    scheduleRetry(cacheKey);
                    return;
                }

                image = NativeImage.read(new ByteArrayInputStream(bytes));
                if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0
                        || image.getWidth() > 4096 || image.getHeight() > 4096) {
                    if (image != null) image.close();
                    scheduleRetry(cacheKey);
                    return;
                }

                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();
                NativeImage readyImage = image;
                image = null;
                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    try {
                        NativeImageBackedTexture texture = new NativeImageBackedTexture(readyImage);
                        Identifier id = client.getTextureManager().registerDynamicTexture(
                                "image_frame_" + Integer.toHexString(cacheKey.hashCode()), texture);
                        CACHE.put(cacheKey, new TextureInfo(id, imageWidth, imageHeight));
                        RETRY_AFTER.remove(cacheKey);
                    } catch (Throwable ignored) {
                        readyImage.close();
                        scheduleRetry(cacheKey);
                    } finally {
                        LOADING.remove(cacheKey);
                    }
                });
                return;
            } catch (Throwable ignored) {
                if (image != null) image.close();
                scheduleRetry(cacheKey);
            } finally {
                if (!CACHE.containsKey(cacheKey)) LOADING.remove(cacheKey);
            }
        }, "korime-scene-image-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private static String normalizeDownloadUrl(String value) {
        String url = value.trim();
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host != null && host.equalsIgnoreCase("media.discordapp.net")) {
                // Discord media URLs often look like *.png?...&format=webp. The file name says
                // PNG but the response is actually WebP, which NativeImage cannot decode.
                // Keep Discord's signed ex/is/hm parameters intact and only request PNG output.
                String lower = url.toLowerCase(Locale.ROOT);
                if (lower.matches(".*[?&]format=[^&]*.*")) {
                    url = url.replaceAll("(?i)([?&])format=[^&]*", "$1format=png");
                } else {
                    url += url.contains("?") ? "&format=png" : "?format=png";
                }
            }
        } catch (RuntimeException ignored) {
        }
        return url;
    }

    private static void scheduleRetry(String url) {
        RETRY_AFTER.put(url, System.currentTimeMillis() + 3000L);
    }
}
