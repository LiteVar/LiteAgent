package com.litevar.agent.rest.markdown_conversion.core;

/**
 * Callback for reporting progress during a conversion run.
 */
@FunctionalInterface
public interface ConversionProgressListener {

    /**
     * Reports current progress.
     *
     * @param progress value between 0 and 1 inclusive
     * @param stage    symbolic stage identifier
     * @param detail   human-readable detail of the current stage
     */
    void onProgress(double progress, String stage, String detail);
}

