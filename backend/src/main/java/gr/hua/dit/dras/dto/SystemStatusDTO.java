package gr.hua.dit.dras.dto;

public class SystemStatusDTO {

    private String macroPredictionsFile;
    private String macroPredictionsLastModified;
    private String macroPredictionsAge;
    private boolean macroPredictionsLoaded;

    private String pricePredictionsFile;
    private String pricePredictionsLastModified;
    private String pricePredictionsAge;
    private boolean pricePredictionsLoaded;

    // In-memory cache reload tracking
    private String lastReloadedTimestamp;
    private String lastReloadedAge;

    // Platform user stats
    private long totalUsers;
    private long totalOwners;
    private long totalTenants;

    // Platform listing stats
    private long totalListings;
    private long localListings;
    private long externalListings;

    // Listing status stats
    private long activeListings;
    private long pendingListings;
    private long rentedListings;
    private long rejectedListings;

    public SystemStatusDTO() {
    }

    public SystemStatusDTO(
            String macroPredictionsFile,
            String macroPredictionsLastModified,
            String macroPredictionsAge,
            boolean macroPredictionsLoaded,
            String pricePredictionsFile,
            String pricePredictionsLastModified,
            String pricePredictionsAge,
            boolean pricePredictionsLoaded,
            String lastReloadedTimestamp,
            String lastReloadedAge,
            long totalUsers,
            long totalOwners,
            long totalTenants,
            long totalListings,
            long localListings,
            long externalListings,
            long activeListings,
            long pendingListings,
            long rentedListings,
            long rejectedListings
    ) {
        this.macroPredictionsFile = macroPredictionsFile;
        this.macroPredictionsLastModified = macroPredictionsLastModified;
        this.macroPredictionsAge = macroPredictionsAge;
        this.macroPredictionsLoaded = macroPredictionsLoaded;
        this.pricePredictionsFile = pricePredictionsFile;
        this.pricePredictionsLastModified = pricePredictionsLastModified;
        this.pricePredictionsAge = pricePredictionsAge;
        this.pricePredictionsLoaded = pricePredictionsLoaded;
        this.lastReloadedTimestamp = lastReloadedTimestamp;
        this.lastReloadedAge = lastReloadedAge;
        this.totalUsers = totalUsers;
        this.totalOwners = totalOwners;
        this.totalTenants = totalTenants;
        this.totalListings = totalListings;
        this.localListings = localListings;
        this.externalListings = externalListings;
        this.activeListings = activeListings;
        this.pendingListings = pendingListings;
        this.rentedListings = rentedListings;
        this.rejectedListings = rejectedListings;
    }

    public String getMacroPredictionsFile() {
        return macroPredictionsFile;
    }

    public void setMacroPredictionsFile(String macroPredictionsFile) {
        this.macroPredictionsFile = macroPredictionsFile;
    }

    public String getMacroPredictionsLastModified() {
        return macroPredictionsLastModified;
    }

    public void setMacroPredictionsLastModified(String macroPredictionsLastModified) {
        this.macroPredictionsLastModified = macroPredictionsLastModified;
    }

    public String getMacroPredictionsAge() {
        return macroPredictionsAge;
    }

    public void setMacroPredictionsAge(String macroPredictionsAge) {
        this.macroPredictionsAge = macroPredictionsAge;
    }

    public boolean isMacroPredictionsLoaded() {
        return macroPredictionsLoaded;
    }

    public void setMacroPredictionsLoaded(boolean macroPredictionsLoaded) {
        this.macroPredictionsLoaded = macroPredictionsLoaded;
    }

    public String getPricePredictionsFile() {
        return pricePredictionsFile;
    }

    public void setPricePredictionsFile(String pricePredictionsFile) {
        this.pricePredictionsFile = pricePredictionsFile;
    }

    public String getPricePredictionsLastModified() {
        return pricePredictionsLastModified;
    }

    public void setPricePredictionsLastModified(String pricePredictionsLastModified) {
        this.pricePredictionsLastModified = pricePredictionsLastModified;
    }

    public String getPricePredictionsAge() {
        return pricePredictionsAge;
    }

    public void setPricePredictionsAge(String pricePredictionsAge) {
        this.pricePredictionsAge = pricePredictionsAge;
    }

    public boolean isPricePredictionsLoaded() {
        return pricePredictionsLoaded;
    }

    public void setPricePredictionsLoaded(boolean pricePredictionsLoaded) {
        this.pricePredictionsLoaded = pricePredictionsLoaded;
    }

    public String getLastReloadedTimestamp() {
        return lastReloadedTimestamp;
    }

    public void setLastReloadedTimestamp(String lastReloadedTimestamp) {
        this.lastReloadedTimestamp = lastReloadedTimestamp;
    }

    public String getLastReloadedAge() {
        return lastReloadedAge;
    }

    public void setLastReloadedAge(String lastReloadedAge) {
        this.lastReloadedAge = lastReloadedAge;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalOwners() {
        return totalOwners;
    }

    public void setTotalOwners(long totalOwners) {
        this.totalOwners = totalOwners;
    }

    public long getTotalTenants() {
        return totalTenants;
    }

    public void setTotalTenants(long totalTenants) {
        this.totalTenants = totalTenants;
    }

    public long getTotalListings() {
        return totalListings;
    }

    public void setTotalListings(long totalListings) {
        this.totalListings = totalListings;
    }

    public long getLocalListings() {
        return localListings;
    }

    public void setLocalListings(long localListings) {
        this.localListings = localListings;
    }

    public long getExternalListings() {
        return externalListings;
    }

    public void setExternalListings(long externalListings) {
        this.externalListings = externalListings;
    }

    public long getActiveListings() {
        return activeListings;
    }

    public void setActiveListings(long activeListings) {
        this.activeListings = activeListings;
    }

    public long getPendingListings() {
        return pendingListings;
    }

    public void setPendingListings(long pendingListings) {
        this.pendingListings = pendingListings;
    }

    public long getRentedListings() {
        return rentedListings;
    }

    public void setRentedListings(long rentedListings) {
        this.rentedListings = rentedListings;
    }

    public long getRejectedListings() {
        return rejectedListings;
    }

    public void setRejectedListings(long rejectedListings) {
        this.rejectedListings = rejectedListings;
    }
}
