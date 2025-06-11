type Listener = (event: any) => void;

export class MockEventSource {
  private listeners: Map<string, Listener[]>;
  public readyState: number;
  public onopen?: (event: any) => void;
  public onmessage?: (event: any) => void;
  public onerror?: (event: any) => void;

  constructor(public url: string) {
    this.listeners = new Map();
    this.readyState = 1; // OPEN
    setTimeout(() => {
      this.dispatchEvent({ type: "open" });
    }, 0);
  }

  addEventListener(type: string, listener: Listener) {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, []);
    }
    this.listeners.get(type)!.push(listener);
  }

  removeEventListener(type: string, listener: Listener) {
    const arr = this.listeners.get(type);
    if (arr) {
      const index = arr.indexOf(listener);
      if (index >= 0) arr.splice(index, 1);
    }
  }

  close() {
    this.readyState = 2; // CLOSED
  }

  dispatchEvent(event: { type: string; data?: any }) {
    const handlers = this.listeners.get(event.type);
    if (handlers) {
      handlers.forEach((fn) => {
        try {
          fn(event);
        } catch (e) {
          console.error("MockEventSource dispatch error:", e);
        }
      });
    }

    const propKey = `on${event.type}` as keyof Pick<
      MockEventSource,
      "onopen" | "onmessage" | "onerror"
    >;

    const propHandler = this[propKey];
    if (typeof propHandler === "function") {
      try {
        propHandler.call(this, event);
      } catch (e) {
        console.error("MockEventSource property dispatch error:", e);
      }
    }
  }
}
