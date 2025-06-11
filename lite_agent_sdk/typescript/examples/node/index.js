/**
 * Example usage of the LiteAgent SDK
 */

import LiteAgentClient from '../../dist/index.esm.js';


// Initialize the SDK
const liteAgent = new LiteAgentClient({
  apiKey: 'sk-xxx',
  baseUrl: 'xxxx',
  enableDebugLogs: true
});

// Create a session
async function example() {
  try {
    // Initialize a session
    await liteAgent.initSession();
    console.log(`Session created with ID: ${liteAgent.getSessionId()}`);
    
    // Send a message with streaming response
    await liteAgent.chat(
      [{ type: 'text', message: 'What is LiteAgent?' }], 
      true, // enable streaming
      {
        onMessage: (message) => {
          console.log('Agent message:', message);
        },
        onChunk: (chunk) => {
          process.stdout.write(chunk.part || ''); // Print streaming text
        },
        onFunctionCall: async (funcCall) => {
          // Handle function calls
          console.log('Function call:', funcCall);
          
          // Send function call results back
          await session.sendFunctionCallResult(
            funcCall.content.id,
            { status: 'success', data: 'Function result' }
          );
        },
        onComplete: () => {
          console.log('\nStreaming completed');
        },
        onError: (error) => {
          console.error('Error in streaming:', error);
        }
      }
    );
    
    // Get history
    const history = await liteAgent.getHistory();
    console.log('Message history:', history);
    
    // Clear the session
    // await liteAgent.clear();
    // console.log('Session cleared');
    
  } catch (error) {
    console.error('Error:', error);
  }
}

// Run the example
example().catch(console.error);