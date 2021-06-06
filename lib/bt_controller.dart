import 'package:flutter/services.dart';

class BTController {
  static const platform = const MethodChannel('flutter.native/helper');

  static Function _onData = (String str) => {};

  static init(Function data) {
    _onData(data);
    platform.setMethodCallHandler((call) => _handler(call));
  }

  static Future<dynamic> _handler(MethodCall call) {
    return Future.value(_onData(call.arguments));
  }

  static Future<List<dynamic>> enumerateDevices() async {
    print('enumerating devices');
    try {
      return await platform.invokeMethod('enumerate-devices');
    } on PlatformException catch (_) {
      return [];
    }
  }

  static Future<void> connect(String address) async {
    print('connecting to $address');
    try {
      return await platform.invokeMethod('connect', address);
    } on PlatformException catch (_) {}
  }

  static Future<void> transmit(String data) async {
    try {
      return await platform.invokeMethod('transmit', data);
    } on PlatformException catch (_) {}
  }
}
