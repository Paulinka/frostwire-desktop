package com.frostwire.gui.bittorrent;

import java.io.File;
import java.util.Date;

import com.frostwire.torrent.CopyrightLicenseBroker;
import com.frostwire.torrent.PaymentOptions;
import com.limegroup.gnutella.gui.I18n;

public class DuplicateDownload implements BTDownload {

    private static final String STATE_DUPLICATED = I18n.tr("Duplicated");
    
    private final BTDownload _delegate;
    
    public DuplicateDownload(BTDownload delegate) {
        _delegate = delegate;
    }

    public long getSize() {
        return _delegate.getSize();
    }
    
    public long getSize(boolean update) {
    	return _delegate.getSize(update);
    }

    public String getDisplayName() {
        return _delegate.getDisplayName();
    }

    public boolean isResumable() {
        return false;
    }

    public boolean isPausable() {
        return false;
    }

    public boolean isCompleted() {
        return false;
    }

    public int getState() {
        return -1;
    }

    public void remove() {
    }

    public void pause() {
    }

    public File getSaveLocation() {
        return null;
    }

    public void resume() {
    }

    public int getProgress() {
        return 0;
    }

    public String getStateString() {
        return STATE_DUPLICATED;
    }

    public long getBytesReceived() {
        return 0;
    }

    public long getBytesSent() {
        return 0;
    }

    public double getDownloadSpeed() {
        return 0;
    }

    public double getUploadSpeed() {
        return 0;
    }

    public long getETA() {
        return 0;
    }

    public String getPeersString() {
        return "";
    }

    public String getSeedsString() {
        return "";
    }

    public boolean isDeleteTorrentWhenRemove() {
        return false;
    }

    public void setDeleteTorrentWhenRemove(boolean deleteTorrentWhenRemove) {
    }

    public boolean isDeleteDataWhenRemove() {
        return false;
    }

    public void setDeleteDataWhenRemove(boolean deleteDataWhenRemove) {
    }

    public String getHash() {
        return _delegate.getHash();
    }

    public String getSeedToPeerRatio() {
        return "";
    }

    public String getShareRatio() {
        return "";
    }
    
    public Date getDateCreated() {
        return _delegate.getDateCreated();
    }

    public boolean isPartialDownload() {
        return false;
    }

    @Override
    public PaymentOptions getPaymentOptions() {
        return _delegate.getPaymentOptions();
    }

    @Override
    public CopyrightLicenseBroker getCopyrightLicenseBroker() {
        return _delegate.getCopyrightLicenseBroker();
    }
}
