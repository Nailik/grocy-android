/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class NfcUtil {

  private final static String TAG = NfcUtil.class.getSimpleName();

  private final static int READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A
      | NfcAdapter.FLAG_READER_NFC_B
      | NfcAdapter.FLAG_READER_NFC_F
      | NfcAdapter.FLAG_READER_NFC_V;

  public interface OnTagReadListener {

    void onTagRead(@NonNull String payload);
  }

  private final Activity activity;
  private final NfcAdapter nfcAdapter;
  private final Handler handler;
  private final boolean debug;

  public NfcUtil(Activity activity) {
    this.activity = activity;
    this.nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
    this.handler = new Handler(Looper.getMainLooper());
    this.debug = PrefsUtil.isDebuggingEnabled(
        PreferenceManager.getDefaultSharedPreferences(activity)
    );
  }

  public boolean isSupported() {
    return nfcAdapter != null;
  }

  public boolean isEnabled() {
    return nfcAdapter != null && nfcAdapter.isEnabled();
  }

  public void enableReaderMode(@NonNull OnTagReadListener listener) {
    if (nfcAdapter == null) {
      return;
    }
    nfcAdapter.enableReaderMode(activity, tag -> {
      String payload = readPayload(tag);
      if (debug) {
        Log.i(TAG, "enableReaderMode: discovered " + tag + " with payload " + payload);
      }
      if (payload != null) {
        handler.post(() -> listener.onTagRead(payload));
      }
    }, READER_FLAGS, null);
  }

  public void disableReaderMode() {
    if (nfcAdapter == null) {
      return;
    }
    nfcAdapter.disableReaderMode(activity);
  }

  // Tags scanned while the app was not in foreground are delivered as NDEF_DISCOVERED intent
  @Nullable
  public static String getPayloadFromIntent(@Nullable Intent intent) {
    if (intent == null || !NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
      return null;
    }
    Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
    if (messages != null) {
      for (Parcelable message : messages) {
        if (!(message instanceof NdefMessage)) {
          continue;
        }
        String payload = getPayloadFromMessage((NdefMessage) message);
        if (payload != null) {
          return payload;
        }
      }
    }
    // uri records are also delivered as intent data, which is what the intent filter matched
    Uri data = intent.getData();
    return data != null ? data.toString() : null;
  }

  @Nullable
  private static String readPayload(Tag tag) {
    Ndef ndef = Ndef.get(tag);
    if (ndef == null) {
      return null;
    }
    NdefMessage message = ndef.getCachedNdefMessage();
    if (message == null) {
      try {
        ndef.connect();
        message = ndef.getNdefMessage();
      } catch (Exception e) {
        Log.e(TAG, "readPayload: " + e);
      } finally {
        try {
          ndef.close();
        } catch (Exception e) {
          Log.e(TAG, "readPayload: " + e);
        }
      }
    }
    return message != null ? getPayloadFromMessage(message) : null;
  }

  @Nullable
  private static String getPayloadFromMessage(NdefMessage message) {
    for (NdefRecord record : message.getRecords()) {
      String payload = getPayloadFromRecord(record);
      if (payload != null && !payload.isEmpty()) {
        return payload;
      }
    }
    return null;
  }

  @Nullable
  private static String getPayloadFromRecord(NdefRecord record) {
    short tnf = record.getTnf();
    boolean isUriRecord = tnf == NdefRecord.TNF_ABSOLUTE_URI
        || tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.getType(), NdefRecord.RTD_URI);
    if (isUriRecord) {
      Uri uri = record.toUri();
      return uri != null ? uri.toString() : null;
    }
    if (tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
      byte[] payload = record.getPayload();
      if (payload.length == 0) {
        return null;
      }
      // first payload byte holds the encoding flag and the length of the language code
      int languageCodeLength = payload[0] & 0x3F;
      if (payload.length <= languageCodeLength) {
        return null;
      }
      Charset charset = (payload[0] & 0x80) == 0
          ? StandardCharsets.UTF_8 : StandardCharsets.UTF_16;
      return new String(
          payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, charset
      );
    }
    if (tnf == NdefRecord.TNF_MIME_MEDIA) {
      return new String(record.getPayload(), StandardCharsets.UTF_8);
    }
    return null;
  }
}
