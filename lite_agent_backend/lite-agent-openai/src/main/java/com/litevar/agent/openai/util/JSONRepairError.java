package com.litevar.agent.openai.util;

/**
 * Custom exception for JSON repair errors
 *
 * @author uncle
 * @since 2025/6/6 11:34
 */
public class JSONRepairError extends RuntimeException {

    public JSONRepairError(String message, int position) {
        super(message + " at position " + position);
    }
}
