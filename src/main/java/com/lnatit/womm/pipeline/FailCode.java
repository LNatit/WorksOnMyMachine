package com.lnatit.womm.pipeline;

public enum FailCode
{
    WORLD_ACCESS_IO("Failed to access world directory"),
    WORLD_ACCESS_VALIDATION("Failed to validate world directory"),
    WORLD_DATA_READ_FAILED("Failed to read existing world data"),
    WORLD_REQUIRES_MANUAL_CONVERSION("World requires manual conversion in an older Minecraft version"),
    WORLD_INCOMPATIBLE_VERSION("World was created by an incompatible Minecraft version"),
    WORLD_STEM_BUILD_FAILED("Failed while creating world stem"),
    WORLD_PRESET_NOT_FOUND("World preset referenced by template does not exist on this client"),
    WORLD_PREPARE_UNKNOWN("Unexpected failure while preparing world resources");

    private final String message;

    FailCode(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
