class PcmPlayerWorklet extends AudioWorkletProcessor {
  constructor() {
    super();
    this.queue = [];
    this.offset = 0;
    this.ended = false;
    this.drainedEmitted = false;

    this.port.onmessage = (event) => {
      const data = event.data || {};
      if (data.type === 'reset') {
        this.queue = [];
        this.offset = 0;
        this.ended = false;
        this.drainedEmitted = false;
        return;
      }
      if (data.type === 'append' && data.samples) {
        const samples = new Float32Array(data.samples);
        if (samples.length > 0) {
          this.queue.push(samples);
          this.drainedEmitted = false;
        }
        return;
      }
      if (data.type === 'end') {
        this.ended = true;
      }
    };
  }

  process(_inputs, outputs) {
    const output = outputs[0][0];
    output.fill(0);

    let writeIndex = 0;
    while (writeIndex < output.length && this.queue.length > 0) {
      const chunk = this.queue[0];
      const remain = chunk.length - this.offset;
      const canCopy = Math.min(output.length - writeIndex, remain);
      output.set(chunk.subarray(this.offset, this.offset + canCopy), writeIndex);
      writeIndex += canCopy;
      this.offset += canCopy;

      if (this.offset >= chunk.length) {
        this.queue.shift();
        this.offset = 0;
      }
    }

    if (this.ended && this.queue.length === 0 && !this.drainedEmitted) {
      this.drainedEmitted = true;
      this.port.postMessage({ type: 'drain' });
    }

    return true;
  }
}

registerProcessor('pcm-player-worklet', PcmPlayerWorklet);
