// Tests for OCR service model classes
// (lib/features/invoices/services/ocr_service.dart).
// Tests BBox, OcrLine, OcrQr, OcrRichResult factory constructors only.
// OcrService.analyzeImages uses a MethodChannel and is skipped.

import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/invoices/services/ocr_service.dart';

void main() {
  group('BBox', () {
    test('stores l, t, w, h', () {
      const b = BBox(l: 1.0, t: 2.0, w: 3.0, h: 4.0);
      expect(b.l, 1.0);
      expect(b.t, 2.0);
      expect(b.w, 3.0);
      expect(b.h, 4.0);
    });

    test('BBox.from parses map with num values', () {
      final b = BBox.from({'l': 10, 't': 20, 'w': 30, 'h': 40});
      expect(b.l, 10.0);
      expect(b.t, 20.0);
      expect(b.w, 30.0);
      expect(b.h, 40.0);
    });

    test('BBox.from parses map with double values', () {
      final b = BBox.from({'l': 1.5, 't': 2.5, 'w': 3.5, 'h': 4.5});
      expect(b.l, 1.5);
      expect(b.t, 2.5);
    });
  });

  group('OcrLine', () {
    test('stores text and box', () {
      const b = BBox(l: 0, t: 0, w: 100, h: 20);
      final line = OcrLine(text: 'Hello', box: b);
      expect(line.text, 'Hello');
      expect(line.box.w, 100.0);
    });

    test('OcrLine.from parses text and box from map', () {
      final line = OcrLine.from({
        'text': 'Invoice',
        'box': {'l': 5, 't': 10, 'w': 80, 'h': 15},
      });
      expect(line.text, 'Invoice');
      expect(line.box.l, 5.0);
      expect(line.box.h, 15.0);
    });

    test('OcrLine.from uses empty string when text is null', () {
      final line = OcrLine.from({
        'text': null,
        'box': {'l': 0, 't': 0, 'w': 1, 'h': 1},
      });
      expect(line.text, '');
    });
  });

  group('OcrQr', () {
    test('stores value and optional box', () {
      final qr = OcrQr(value: 'https://example.com');
      expect(qr.value, 'https://example.com');
      expect(qr.box, isNull);
    });

    test('OcrQr.from parses value without box', () {
      final qr = OcrQr.from({'value': 'ABC123', 'box': null});
      expect(qr.value, 'ABC123');
      expect(qr.box, isNull);
    });

    test('OcrQr.from parses value with box', () {
      final qr = OcrQr.from({
        'value': 'QR-DATA',
        'box': {'l': 0, 't': 0, 'w': 50, 'h': 50},
      });
      expect(qr.value, 'QR-DATA');
      expect(qr.box, isNotNull);
      expect(qr.box!.w, 50.0);
    });

    test('OcrQr.from uses empty string when value is null', () {
      final qr = OcrQr.from({'value': null, 'box': null});
      expect(qr.value, '');
    });
  });

  group('OcrRichResult', () {
    test('stores path, text, lines, qrcodes', () {
      final result = OcrRichResult(
        path: '/tmp/img.jpg',
        text: 'full text',
        lines: [],
        qrcodes: [],
      );
      expect(result.path, '/tmp/img.jpg');
      expect(result.text, 'full text');
      expect(result.lines, isEmpty);
      expect(result.qrcodes, isEmpty);
    });

    test('OcrRichResult.from parses full map', () {
      final result = OcrRichResult.from({
        'path': '/docs/scan.png',
        'text': 'OCR output',
        'lines': [
          {
            'text': 'Line one',
            'box': {'l': 0, 't': 0, 'w': 100, 'h': 12},
          },
        ],
        'qrcodes': [
          {'value': 'QR-VAL', 'box': null},
        ],
      });
      expect(result.path, '/docs/scan.png');
      expect(result.text, 'OCR output');
      expect(result.lines.length, 1);
      expect(result.lines[0].text, 'Line one');
      expect(result.qrcodes.length, 1);
      expect(result.qrcodes[0].value, 'QR-VAL');
    });

    test('OcrRichResult.from handles null lines and qrcodes', () {
      final result = OcrRichResult.from({
        'path': '/a.png',
        'text': null,
        'lines': null,
        'qrcodes': null,
      });
      expect(result.text, '');
      expect(result.lines, isEmpty);
      expect(result.qrcodes, isEmpty);
    });
  });
}
