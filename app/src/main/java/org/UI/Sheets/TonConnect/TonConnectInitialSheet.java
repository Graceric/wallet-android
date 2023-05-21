package org.UI.Sheets.TonConnect;

import org.TonController.TonConnect.TonConnectController;
import org.UI.Sheets.Templates.BottomSheetPageLoading;
import org.UI.Sheets.Templates.BottomSheetPaginated;
import org.telegram.ui.ActionBar.BaseFragment;

public class TonConnectInitialSheet extends BottomSheetPaginated {
    private TonConnectController.ConnectedApplication application;

    public TonConnectInitialSheet (BaseFragment parent) {
        super(parent);
    }

    public void setLoadedApplication (TonConnectController.ConnectedApplication application) {
        this.application = application;
        if (pages[1] instanceof TonConnectInitialPage) {
            ((TonConnectInitialPage) pages[1]).setApplication(application);
        }
        viewPager.setCurrentItem(1);
    }

    @Override
    protected int getPagesCount () {
        return 2;
    }

    @Override
    protected Page createPageAtPosition (int position) {
        if (position == 1) {
            TonConnectInitialPage p = new TonConnectInitialPage();
            if (application != null) p.setApplication(application);
            return p;
        } else {
            return new BottomSheetPageLoading(384);
        }
    }

    @Override
    protected boolean useNonPagerMode () {
        return true;
    }
}
