import 'dart:async';

extension ThrottleExtension on Function {
  void Function() throttle([int milliseconds = 1000]) {
    bool _isAllowed = true;
    Timer? _throttleTimer;
    return () {
      if (!_isAllowed) return;
      _isAllowed = false;
      this();
      _throttleTimer?.cancel();
      _throttleTimer = Timer(Duration(milliseconds: milliseconds), () {
        _isAllowed = true;
      });
    };
  }
}

extension DebounceExtension on Function {
  void Function() debounce([int milliseconds = 1000]) {
    Timer? _debounceTimer;
    return () {
      if (_debounceTimer?.isActive ?? false) _debounceTimer?.cancel();
      _debounceTimer = Timer(Duration(milliseconds: milliseconds), this());
    };
  }
}
