package com.lnatit.womm.pipeline;

public final class WorldPrepareException extends Exception {
    private final FailCode reason;

    public WorldPrepareException(FailCode reason) {
        super(reason.message());
        this.reason = reason;
    }

    public WorldPrepareException(FailCode reason, Throwable cause) {
        super(reason.message(), cause);
        this.reason = reason;
    }

    public FailCode reason() {
        return reason;
    }
}

