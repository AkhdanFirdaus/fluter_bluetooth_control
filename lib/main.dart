import 'package:bluetooth_control/bt_controller.dart';
import 'package:flutter/material.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: MyHomePage('Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage(this.title);

  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  String sensorValue = "N/A";
  bool ledState = false;

  @override
  void initState() {
    super.initState();
    BTController.init(onData);
    scanDevices();
  }

  void onData(dynamic data) {
    setState(() {
      sensorValue = data;
    });
  }

  void switchLed() {
    setState(() {
      ledState = !ledState;
    });

    BTController.transmit(ledState ? '0' : '1');
  }

  Future<void> scanDevices() async {
    BTController.enumerateDevices().then((value) {
      onGetDevices(value);
    });
  }

  void onGetDevices(List<dynamic> devices) {
    Iterable<SimpleDialogOption> options = devices.map((device) {
      return SimpleDialogOption(
        child: Text(device.keys.first),
        onPressed: () {
          selectDevice(device.values.first);
        },
      );
    });

    SimpleDialog dialog = SimpleDialog(
      title: Text("Choose device"),
      children: options.toList(),
    );

    showDialog(
      context: context,
      barrierDismissible: true,
      builder: (BuildContext context) {
        return dialog;
      },
    );
  }

  void selectDevice(String deviceAddress) {
    Navigator.of(context, rootNavigator: true).pop('dialog');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Center(
        child: ledState
            ? Icon(
                Icons.lightbulb,
                color: Colors.yellow,
              )
            : Icon(Icons.lightbulb_outline),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          switchLed();
        },
        child: Icon(Icons.power_settings_new),
      ),
    );
  }
}
