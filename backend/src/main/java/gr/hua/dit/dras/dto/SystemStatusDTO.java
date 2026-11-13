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

    private long totalUsers;
    private long totalListings;
    private long pendingListings;
    private long activeListings;

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
            long totalUsers,
            long totalListings,
            long pendingListings,
            long activeListings
    ) {
        this.macroPredictionsFile = macroPredictionsFile;
        this.macroPredictionsLastModified = macroPredictionsLastModified;
        this.macroPredictionsAge = macroPredictionsAge;
        this.macroPredictionsLoaded = macroPredictionsLoaded;
        this.pricePredictionsFile = pricePredictionsFile;
        this.pricePredictionsLastModified = pricePredictionsLastModified;
        this.pricePredictionsAge = pricePredictionsAge;
        this.pricePredictionsLoaded = pricePredictionsLoaded;
        this.totalUsers = totalUsers;
        this.totalListings = totalListings;
        this.pendingListings = pendingListings;
        this.activeListings = activeListings;
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

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalListings() {
        return totalListings;
    }

    public void setTotalListings(long totalListings) {
        this.totalListings = totalListings;
    }

    public long getPendingListings() {
        return pendingListings;
    }

    public void setPendingListings(long pendingListings) {
        this.pendingListings = pendingListings;
    }

    public long getActiveListings() {
        return activeListings;
    }

    public void setActiveListings(long activeListings) {
        this.activeListings = activeListings;
    }
}
