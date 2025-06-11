/**
 * Custom error classes for the SDK
 */
class LiteAgentError extends Error {
  code: string;
  details?: any;

  constructor(message: string, code: string, details?: any) {
    super(message);
    this.name = 'LiteAgentError';
    this.code = code;
    this.details = details;
  }
}

// Specific error types
class NetworkError extends LiteAgentError {
  constructor(message: string, details?: any) {
    super(message, 'NETWORK_ERROR', details);
    this.name = 'NetworkError';
  }
}

class AuthenticationError extends LiteAgentError {
  constructor(message: string, details?: any) {
    super(message, 'AUTH_ERROR', details);
    this.name = 'AuthenticationError';
  }
}

class ValidationError extends LiteAgentError {
  constructor(message: string, details?: any) {
    super(message, 'VALIDATION_ERROR', details);
    this.name = 'ValidationError';
  }
}

class TimeoutError extends LiteAgentError {
  constructor(message: string, details?: any) {
    super(message, 'TIMEOUT_ERROR', details);
    this.name = 'TimeoutError';
  }
}

class StreamError extends LiteAgentError {
  constructor(message: string, details?: any) {
    super(message, 'STREAM_ERROR', details);
    this.name = 'StreamError';
  }
}

export {
  LiteAgentError,
  NetworkError,
  AuthenticationError,
  ValidationError,
  TimeoutError,
  StreamError,
};