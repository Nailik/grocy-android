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

package xyz.zedler.patrick.grocy.helper;

import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import java.util.Locale;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants.ACTION;
import xyz.zedler.patrick.grocy.Constants.NFC_ACTION;
import xyz.zedler.patrick.grocy.Constants.SETTINGS;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.fragment.InventoryFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.PurchaseFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.ShoppingListItemEditFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.TransferFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheetArgs;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.util.GrocycodeUtil;
import xyz.zedler.patrick.grocy.util.GrocycodeUtil.Grocycode;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;

public class NfcActionHelper {

  private final static String TAG = NfcActionHelper.class.getSimpleName();
  private final static String DEEP_LINK_SCHEME = "grocy://";

  private final MainActivity activity;
  private final SharedPreferences sharedPrefs;
  private final PluralUtil pluralUtil;
  private final boolean debug;
  private DownloadHelper dlHelper;

  public NfcActionHelper(MainActivity activity) {
    this.activity = activity;
    this.sharedPrefs = activity.getSharedPrefs();
    this.pluralUtil = new PluralUtil(activity);
    this.debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
  }

  // server url and api key are read once per instance, so a fresh one is needed for every tag
  private DownloadHelper newDownloadHelper() {
    destroy();
    dlHelper = new DownloadHelper(activity, TAG);
    return dlHelper;
  }

  public void destroy() {
    if (dlHelper != null) {
      dlHelper.destroy();
      dlHelper = null;
    }
  }

  public static boolean isDeepLink(@NonNull String payload) {
    return payload.toLowerCase(Locale.ROOT).startsWith(DEEP_LINK_SCHEME);
  }

  public void onTagRead(@NonNull String payload) {
    if (debug) {
      Log.i(TAG, "onTagRead: " + payload);
    }
    if (isDeepLink(payload)) {
      activity.navUtil.navigateDeepLink(payload);
      return;
    }
    Grocycode grocycode = GrocycodeUtil.getGrocycode(payload);
    if (grocycode == null) {
      activity.showSnackbar(R.string.error_nfc_no_grocycode, false);
      return;
    }
    if (!grocycode.isProduct()) {
      activity.showSnackbar(R.string.error_wrong_grocycode_type, false);
      return;
    }
    performAction(grocycode.getObjectId());
  }

  private void performAction(int productId) {
    String action = sharedPrefs.getString(SETTINGS.NFC.ACTION, SETTINGS_DEFAULT.NFC.ACTION);
    if (action == null || action.equals(NFC_ACTION.NONE)) {
      return;
    }
    switch (action) {
      case NFC_ACTION.PURCHASE:
        activity.navUtil.navigate(
            R.id.purchaseFragment,
            new PurchaseFragmentArgs.Builder()
                .setProductId(String.valueOf(productId)).build().toBundle()
        );
        break;
      case NFC_ACTION.TRANSFER:
        activity.navUtil.navigate(
            R.id.transferFragment,
            new TransferFragmentArgs.Builder()
                .setProductId(String.valueOf(productId)).build().toBundle()
        );
        break;
      case NFC_ACTION.INVENTORY:
        activity.navUtil.navigate(
            R.id.inventoryFragment,
            new InventoryFragmentArgs.Builder()
                .setProductId(String.valueOf(productId)).build().toBundle()
        );
        break;
      case NFC_ACTION.ADD_TO_SHOPPING_LIST:
        activity.navUtil.navigateDeepLink(
            R.string.deep_link_shoppingListItemEditFragment,
            new ShoppingListItemEditFragmentArgs.Builder(ACTION.CREATE)
                .setProductId(String.valueOf(productId)).build().toBundle()
        );
        break;
      default:
        loadProductDetails(productId, action);
        break;
    }
  }

  private void loadProductDetails(int productId, String action) {
    DownloadHelper dlHelper = newDownloadHelper();
    dlHelper.newQueue(
        updated -> {},
        error -> activity.showSnackbar(R.string.error_no_product_details, false)
    ).append(
        ProductDetails.getProductDetails(
            dlHelper,
            productId,
            productDetails -> onProductDetailsLoaded(dlHelper, productDetails, action)
        )
    ).start();
  }

  private void onProductDetailsLoaded(
      DownloadHelper dlHelper, ProductDetails productDetails, String action
  ) {
    switch (action) {
      case NFC_ACTION.PRODUCT_OVERVIEW:
        activity.showBottomSheet(
            new ProductOverviewBottomSheet(),
            new ProductOverviewBottomSheetArgs.Builder()
                .setProductDetails(productDetails).build().toBundle()
        );
        break;
      case NFC_ACTION.CONSUME:
        consumeProduct(
            dlHelper, productDetails,
            productDetails.getProduct().getQuickConsumeAmountDouble(), false
        );
        break;
      case NFC_ACTION.CONSUME_ALL:
        double amountAll = productDetails.getProduct().getEnableTareWeightHandlingInt() == 0
            ? productDetails.getStockAmount()
            : productDetails.getProduct().getTareWeightDouble();
        if (amountAll <= 0) {
          activity.showSnackbar(
              activity.getString(R.string.msg_not_in_stock, productDetails.getProduct().getName()),
              false
          );
          return;
        }
        consumeProduct(dlHelper, productDetails, amountAll, false);
        break;
      case NFC_ACTION.OPEN:
        consumeProduct(
            dlHelper, productDetails,
            VersionUtil.isGrocyServerMin400(sharedPrefs)
                ? productDetails.getProduct().getQuickOpenAmountDouble()
                : productDetails.getProduct().getQuickConsumeAmountDouble(),
            true
        );
        break;
    }
  }

  private void consumeProduct(
      DownloadHelper dlHelper, ProductDetails productDetails, double amount, boolean isActionOpen
  ) {
    JSONObject body = new JSONObject();
    try {
      body.put("amount", amount);
      body.put("allow_subproduct_substitution", true);
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "consumeProduct: " + e);
      }
    }
    dlHelper.postWithArray(
        isActionOpen
            ? dlHelper.grocyApi.openProduct(productDetails.getProduct().getId())
            : dlHelper.grocyApi.consumeProduct(productDetails.getProduct().getId()),
        body,
        response -> {
          double amountDone = 0;
          try {
            for (int i = 0; i < response.length(); i++) {
              amountDone += Math.abs(response.getJSONObject(i).getDouble("amount"));
            }
          } catch (JSONException e) {
            if (debug) {
              Log.e(TAG, "consumeProduct: " + e);
            }
          }
          showTransactionMessage(
              isActionOpen ? R.string.msg_opened : R.string.msg_consumed,
              productDetails,
              amountDone
          );
        },
        error -> activity.showSnackbar(R.string.error_undefined, false)
    );
  }

  private void showTransactionMessage(
      @StringRes int message, ProductDetails productDetails, double amount
  ) {
    int maxDecimalPlacesAmount = sharedPrefs.getInt(
        SETTINGS.STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    activity.showSnackbar(
        activity.getString(
            message,
            NumUtil.trimAmount(amount, maxDecimalPlacesAmount),
            pluralUtil.getQuantityUnitPlural(productDetails.getQuantityUnitStock(), amount),
            productDetails.getProduct().getName()
        ),
        false
    );
  }
}
