package net.ccbluex.liquidbounce.launch.data.legacyui.clickgui.style.styles.newdropdown.utils.animations;

public enum Direction {
    FORWARDS,
    BACKWARDS;

    public Direction opposite() {
        if (this == Direction.FORWARDS) {
            return Direction.BACKWARDS;
        } else return Direction.FORWARDS;
    }

}
