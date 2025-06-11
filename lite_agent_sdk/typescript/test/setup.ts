import nock from 'nock';
import { MockEventSource } from './mocks/eventsource.mock';

beforeAll(() => {
  global.EventSource = MockEventSource as any;
});

// Clean all nock interceptors after each test
afterEach(() => {
  nock.cleanAll();
});

// Enable network connections after all tests
afterAll(() => {
  nock.enableNetConnect();
});

// Initialize nock to mock all HTTP requests by default
beforeEach(() => {
  nock.disableNetConnect();
});
