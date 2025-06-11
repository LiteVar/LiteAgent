export enum LogLevel {
  DEBUG = 'DEBUG',
  INFO = 'INFO',
  WARN = 'WARN',
  ERROR = 'ERROR'
}

export class Logger {
  private static instance: Logger;
  private logLevel: LogLevel = LogLevel.INFO;

  public constructor(logLevel?: LogLevel) {
    this.logLevel = logLevel || LogLevel.INFO;
  }

  static getInstance(logLevel: LogLevel): Logger {
    if (!Logger.instance) {
      Logger.instance = new Logger(logLevel);
    }
    return Logger.instance;
  }

  setLogLevel(level: LogLevel): void {
    this.logLevel = level;
  }

  private getTimeStamp(): string {
    return new Date().toISOString();
  }

  debug(message: string, ...args: any[]): void {
    if (this.shouldLog(LogLevel.DEBUG)) {
      console.debug(`\x1b[36m[${this.getTimeStamp()}] [DEBUG]\x1b[0m`, message, ...args);
    }
  }

  info(message: string, ...args: any[]): void {
    if (this.shouldLog(LogLevel.INFO)) {
      console.info(`\x1b[32m[${this.getTimeStamp()}] [INFO]\x1b[0m`, message, ...args);
    }
  }

  warn(message: string, ...args: any[]): void {
    if (this.shouldLog(LogLevel.WARN)) {
      console.warn(`\x1b[33m[${this.getTimeStamp()}] [WARN]\x1b[0m`, message, ...args);
    }
  }

  error(message: string, ...args: any[]): void {
    if (this.shouldLog(LogLevel.ERROR)) {
      console.error(`\x1b[31m[${this.getTimeStamp()}] [ERROR]\x1b[0m`, message, ...args);
    }
  }

  private shouldLog(level: LogLevel): boolean {
    const levels = Object.values(LogLevel);
    return levels.indexOf(level) >= levels.indexOf(this.logLevel);
  }
}

export const logger = Logger.getInstance(LogLevel.INFO);