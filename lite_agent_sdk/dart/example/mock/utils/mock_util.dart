class MockUtil {
  List<String> storage = ["Hello", "World"];

  int count() {
    return storage.length;
  }

  int create(String text) {
    storage.add(text);
    return storage.length - 1;
  }

  String read(int id) {
    return storage[id];
  }

  void update(int id, String text) {
    storage[id] = text;
  }

  void delete(int id) {
    storage.removeAt(id);
  }
}