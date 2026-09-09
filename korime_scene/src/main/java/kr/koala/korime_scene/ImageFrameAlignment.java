package kr.koala.korime_scene;

public enum ImageFrameAlignment {
    TOP_LEFT("왼쪽 위", 0.0F, 1.0F),
    TOP_CENTER("가운데 위", 0.5F, 1.0F),
    TOP_RIGHT("오른쪽 위", 1.0F, 1.0F),
    MIDDLE_LEFT("왼쪽 가운데", 0.0F, 0.5F),
    CENTER("가운데", 0.5F, 0.5F),
    MIDDLE_RIGHT("오른쪽 가운데", 1.0F, 0.5F),
    BOTTOM_LEFT("왼쪽 아래", 0.0F, 0.0F),
    BOTTOM_CENTER("가운데 아래", 0.5F, 0.0F),
    BOTTOM_RIGHT("오른쪽 아래", 1.0F, 0.0F);

    private final String displayName;
    private final float horizontal;
    private final float vertical;

    ImageFrameAlignment(String displayName, float horizontal, float vertical) {
        this.displayName = displayName;
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public String getDisplayName() { return displayName; }
    public float getHorizontal() { return horizontal; }
    public float getVertical() { return vertical; }

    public ImageFrameAlignment next() {
        ImageFrameAlignment[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static ImageFrameAlignment fromId(int id) {
        ImageFrameAlignment[] values = values();
        return id >= 0 && id < values.length ? values[id] : CENTER;
    }
}
