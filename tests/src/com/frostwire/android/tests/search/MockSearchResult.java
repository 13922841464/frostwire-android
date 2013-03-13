package com.frostwire.android.tests.search;

import com.frostwire.licences.Licence;
import com.frostwire.search.CompleteSearchResult;

public class MockSearchResult implements CompleteSearchResult {

    @Override
    public String getSource() {
        return "Tests";
    }

    @Override
    public String getDetailsUrl() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }
    
    @Override
    public Licence getLicence() {
        return Licence.UNKNOWN;
    }
}
