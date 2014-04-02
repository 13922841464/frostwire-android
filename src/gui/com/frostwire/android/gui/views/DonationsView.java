/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.android.gui.views;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.frostwire.android.R;
import com.frostwire.android.gui.billing.Biller;
import com.frostwire.android.gui.billing.BillerFactory;
import com.frostwire.android.gui.billing.DonationSkus;
import com.frostwire.android.gui.billing.DonationSkus.DonationSkuType;
import com.frostwire.android.gui.util.UIUtils;

/**
 * @author guabtron
 * @author aldenml
 *
 */
public class DonationsView extends LinearLayout {

    private final BitcoinButtonListener bitcoinButtonListener;

    private Biller biller;

    public DonationsView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.bitcoinButtonListener = new BitcoinButtonListener(this);
    }

    public void setBiller(Biller b) {
        biller = b;
        setupDonationButtons();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        View.inflate(getContext(), R.layout.view_donations, this);
        setupDonationButtons();
    }

    private void setupDonationButtons() {
        DonationSkus skus = BillerFactory.getDonationSkus();
        setupBitcoinDonateButton();
        setupDonateButton(R.id.fragment_about_button_donate1, skus.getSku(DonationSkuType.SKU_01_DOLLARS), "https://gumroad.com/l/pH");
        setupDonateButton(R.id.fragment_about_button_donate2, skus.getSku(DonationSkuType.SKU_05_DOLLARS), "https://gumroad.com/l/oox");
        setupDonateButton(R.id.fragment_about_button_donate3, skus.getSku(DonationSkuType.SKU_10_DOLLARS), "https://gumroad.com/l/rPl");
        setupDonateButton(R.id.fragment_about_button_donate4, skus.getSku(DonationSkuType.SKU_25_DOLLARS), "https://gumroad.com/l/XQW");
    }

    private void setupBitcoinDonateButton() {
        Button btc = (Button) findViewById(R.id.fragment_about_button_bitcoin);
        btc.setOnClickListener(bitcoinButtonListener);
    }

    protected void onBTCDonationButtonClick() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("bitcoin:14F6JPXK2fR5b4gZp3134qLRGgYtvabMWL?amount=0.0052"));
        try {
            getContext().startActivity(intent);
        } catch (Throwable t) {
            UIUtils.showLongMessage(getContext(), R.string.you_need_a_bitcoin_wallet_app);
        }
    }

    private void setupDonateButton(int id, String sku, String url) {
        Button donate = (Button) findViewById(id);
        donate.setOnClickListener(new DonateButtonListener(biller, sku, url));
    }

    private static final class BitcoinButtonListener extends ClickAdapter<DonationsView> {

        public BitcoinButtonListener(DonationsView owner) {
            super(owner);
        }

        @Override
        public void onClick(DonationsView owner, View v) {
            owner.onBTCDonationButtonClick();
        }
    }
}