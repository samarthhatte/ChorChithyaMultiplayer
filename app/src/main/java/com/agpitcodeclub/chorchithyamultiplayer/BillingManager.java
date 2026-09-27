package com.agpitcodeclub.chorchithyamultiplayer;

import android.app.Activity;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;

import java.util.ArrayList;
import java.util.List;

public class BillingManager implements PurchasesUpdatedListener {

    public static final String PRODUCT_COFFEE = "coffee_tier_1";

    private final Activity activity;
    private BillingClient billingClient;

    public BillingManager(Activity activity) {
        this.activity = activity;
        setupBillingClient();
    }

    private void setupBillingClient() {
        // Required for Billing 8.0.0+: PendingPurchasesParams must be passed
        PendingPurchasesParams pendingPurchasesParams = PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build();

        billingClient = BillingClient.newBuilder(activity)
                .setListener(this)
                .enablePendingPurchases(pendingPurchasesParams)
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                // Connected to Google Play Store
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Reconnect if needed on the next call
            }
        });
    }

    public void purchaseCoffee() {
        if (billingClient == null || !billingClient.isReady()) {
            Toast.makeText(activity, "Connecting to Play Store... please try again", Toast.LENGTH_SHORT).show();
            setupBillingClient();
            return;
        }

        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_COFFEE)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        // In 8.0.0+, the callback receives a QueryProductDetailsResult
        billingClient.queryProductDetailsAsync(params, (billingResult, result) -> {
            List<ProductDetails> detailsList = result.getProductDetailsList();

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && detailsList != null
                    && !detailsList.isEmpty()) {

                ProductDetails coffeeProduct = detailsList.get(0);

                BillingFlowParams.ProductDetailsParams productDetailsParams =
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(coffeeProduct)
                                .build();

                List<BillingFlowParams.ProductDetailsParams> flowProductList = new ArrayList<>();
                flowProductList.add(productDetailsParams);

                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(flowProductList)
                        .build();

                billingClient.launchBillingFlow(activity, billingFlowParams);
            } else {
                activity.runOnUiThread(() ->
                        Toast.makeText(activity, "Coffee item not found in Google Play yet", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // User backed out
        } else {
            Toast.makeText(activity, "Payment failed: " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            ConsumeParams consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();

            billingClient.consumeAsync(consumeParams, (billingResult, purchaseToken) -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    activity.runOnUiThread(() ->
                            Toast.makeText(activity, "☕ Thank you so much for supporting the game!", Toast.LENGTH_LONG).show()
                    );
                }
            });
        }
    }

    public void destroy() {
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
        }
    }
}